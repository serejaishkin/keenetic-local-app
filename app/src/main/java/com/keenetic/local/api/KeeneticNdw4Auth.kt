package com.keenetic.local.api

import android.util.Base64
import java.security.SecureRandom
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * NDW4 interactive authentication for KeeneticOS 5.2+.
 * Protocol reverse-engineered from the router web UI (main JS):
 *  - Phase 1: POST {login, nonce(client)} -> 401 + X-NDM-Data {salt,nonce,iter,memcost}
 *    derive Argon2id key, compute clientProof
 *  - Phase 2: POST {login, nonce(server), proof} -> 401 + X-NDM-Data {signature}, verify, compute signature-proof
 *  - Phase 3: POST {login, nonce(server), signature-proof} -> 200
 *
 * Crypto: Argon2id (parallelism=1, hashLen=64), HMAC-SHA3-512, SHA3-512.
 */
object KeeneticNdw4Auth {

    private val CLIENT_KEY_CONST = "NDW4 Interactive Client Key".toByteArray(Charsets.UTF_8)
    private val SERVER_KEY_CONST = "NDW4 Interactive Server Key".toByteArray(Charsets.UTF_8)

    data class Phase1Params(val saltB64: String, val serverNonce: String, val iter: Int, val memcost: Int)
    data class Context(
        val login: String,
        val password: String,
        val clientNonceB64: String,
        val clientKey: ByteArray,
        val storedKey: ByteArray,
        val serverKey: ByteArray,
        val authMessage: String
    )

    fun generateClientNonceB64(): String {
        val b = ByteArray(16)
        SecureRandom().nextBytes(b)
        return b64e(b)
    }

    fun phase1Crypto(
        login: String,
        password: String,
        clientNonceB64: String,
        p: Phase1Params
    ): Pair<ByteArray, Context> {
        val salt = b64d(p.saltB64)
        val key = Argon2id.derive(
            password = password.toByteArray(Charsets.UTF_8),
            salt = salt,
            iterations = p.iter,
            memoryKiB = p.memcost,
            parallelism = 1,
            hashLength = 64
        )
        val clientKey = hmacSha3_512(key, CLIENT_KEY_CONST)
        val storedKey = sha3_512(clientKey)
        val serverKey = hmacSha3_512(key, SERVER_KEY_CONST)

        val msg1 = "login1=$login,nonce1=$clientNonceB64"
        val msg2 = "iter2=${p.iter},memcost2=${p.memcost},nonce2=${p.serverNonce},salt2=${p.saltB64}"
        val msg3 = "login3=$login,nonce3=${p.serverNonce}"
        val authMessage = "$msg1;$msg2;$msg3"

        val clientSignature = hmacSha3_512(storedKey, authMessage.toByteArray(Charsets.UTF_8))
        val clientProof = xor(clientKey, clientSignature)

        val ctx = Context(login, password, clientNonceB64, clientKey, storedKey, serverKey, authMessage)
        return clientProof to ctx
    }

    fun phase2Verify(serverSignatureB64: String, ctx: Context): ByteArray {
        val serverSig = b64d(serverSignatureB64)
        val expected = hmacSha3_512(ctx.serverKey, ctx.authMessage.toByteArray(Charsets.UTF_8))
        if (!timingSafeEqual(serverSig, expected)) {
            throw IllegalStateException("Server signature verification failed: неверный пароль или несовместимый NDW4")
        }
        val msg4 = "signature4=$serverSignatureB64"
        val s = "${ctx.authMessage};$msg4"
        val sig = hmacSha3_512(ctx.storedKey, s.toByteArray(Charsets.UTF_8))
        return xor(ctx.clientKey, sig)
    }

    fun b64e(b: ByteArray): String = Base64.encodeToString(b, Base64.NO_WRAP)
    fun b64d(s: String): ByteArray = Base64.decode(s, Base64.DEFAULT)

    fun sha3_512(data: ByteArray): ByteArray = Keccak.sha3_512(data)

    fun hmacSha3_512(key: ByteArray, data: ByteArray): ByteArray {
        // HMAC with SHA3-512 (block size 72 bytes for SHA3-512)
        val blockSize = 72
        var k = key
        if (k.size > blockSize) k = sha3_512(k)
        if (k.size < blockSize) k = k.copyOf(blockSize)
        val ipad = ByteArray(blockSize) { (k[it].toInt() xor 0x36).toByte() }
        val opad = ByteArray(blockSize) { (k[it].toInt() xor 0x5c).toByte() }
        val inner = sha3_512(ipad + data)
        return sha3_512(opad + inner)
    }

    fun xor(a: ByteArray, b: ByteArray): ByteArray {
        require(a.size == b.size) { "XOR length mismatch ${a.size}!=${b.size}" }
        return ByteArray(a.size) { (a[it].toInt() xor b[it].toInt()).toByte() }
    }

    fun timingSafeEqual(a: ByteArray, b: ByteArray): Boolean {
        if (a.size != b.size) return false
        var r = 0
        for (i in a.indices) r = r or (a[i].toInt() xor b[i].toInt())
        return r == 0
    }
}

/** Minimal Keccak / SHA3-512 implementation (NIST FIPS 202). */
object Keccak {
    private val RC = longArrayOf(
        0x0000000000000001L, 0x0000000000008082L, -0x7fffffffffff7f76L, -0x7fffffff7fff8000L,
        0x000000000000808bL, 0x0000000080000001L, -0x7fffffff7fff7f7fL, -0x7fffffffffff7ff7L,
        0x000000000000008aL, 0x0000000000000088L, 0x0000000080008009L, 0x000000008000000aL,
        0x000000008000808bL, -0x7fffffffffffff75L, -0x7fffffffffff7f77L, -0x7fffffffffff7ffdL,
        -0x7fffffffffff7ffeL, -0x7fffffffffffff80L, 0x000000000000800aL, -0x7fffffff7ffffff6L,
        -0x7fffffff7fff7f7fL, -0x7fffffffffff7f80L, 0x0000000080000001L, -0x7fffffff7fff7ff8L
    )

    fun sha3_512(data: ByteArray): ByteArray = sponge(data, 0x06, 72, 64)

    private fun sponge(data: ByteArray, domain: Byte, rate: Int, outLen: Int): ByteArray {
        val state = LongArray(25)
        var off = 0
        // absorb full blocks
        while (off + rate <= data.size) {
            xorIn(state, data, off, rate)
            keccakF(state)
            off += rate
        }
        // pad
        val block = ByteArray(rate)
        val rem = data.size - off
        if (rem > 0) System.arraycopy(data, off, block, 0, rem)
        block[rem] = (block[rem].toInt() xor (domain.toInt() and 0xFF)).toByte()
        block[rate - 1] = (block[rate - 1].toInt() xor 0x80).toByte()
        xorIn(state, block, 0, rate)
        keccakF(state)
        // squeeze
        val out = ByteArray(outLen)
        var outOff = 0
        while (outOff < outLen) {
            val take = minOf(rate, outLen - outOff)
            for (i in 0 until take) out[outOff + i] = ((state[i / 8] ushr (8 * (i % 8))) and 0xFF).toByte()
            outOff += take
            if (outOff < outLen) keccakF(state)
        }
        return out
    }

    private fun xorIn(state: LongArray, data: ByteArray, off: Int, len: Int) {
        for (i in 0 until len) {
            val lane = i / 8
            val shift = 8 * (i % 8)
            state[lane] = state[lane] xor ((data[off + i].toLong() and 0xFF) shl shift)
        }
    }

    private fun rotl64(x: Long, n: Int): Long = (x shl n) or (x ushr (64 - n))

    private fun keccakF(a: LongArray) {
        val c = LongArray(5)
        val d = LongArray(5)
        val b = LongArray(25)
        for (round in 0 until 24) {
            for (x in 0..4) c[x] = a[x] xor a[x + 5] xor a[x + 10] xor a[x + 15] xor a[x + 20]
            for (x in 0..4) d[x] = c[(x + 4) % 5] xor rotl64(c[(x + 1) % 5], 1)
            for (i in 0 until 25) a[i] = a[i] xor d[i % 5]
            b[0] = a[0]
            b[1] = rotl64(a[6], 44); b[2] = rotl64(a[12], 43); b[3] = rotl64(a[18], 21); b[4] = rotl64(a[24], 14)
            b[5] = rotl64(a[3], 28); b[6] = rotl64(a[9], 20); b[7] = rotl64(a[10], 3); b[8] = rotl64(a[16], 45); b[9] = rotl64(a[22], 61)
            b[10] = rotl64(a[1], 1); b[11] = rotl64(a[7], 6); b[12] = rotl64(a[13], 25); b[13] = rotl64(a[19], 8); b[14] = rotl64(a[20], 18)
            b[15] = rotl64(a[4], 27); b[16] = rotl64(a[5], 36); b[17] = rotl64(a[11], 10); b[18] = rotl64(a[17], 15); b[19] = rotl64(a[23], 56)
            b[20] = rotl64(a[2], 62); b[21] = rotl64(a[8], 55); b[22] = rotl64(a[14], 39); b[23] = rotl64(a[15], 41); b[24] = rotl64(a[21], 2)
            for (x in 0..4) {
                for (y in 0..4) {
                    val i = x + 5 * y
                    a[i] = b[i] xor (b[((x + 1) % 5) + 5 * y].inv() and b[((x + 2) % 5) + 5 * y])
                }
            }
            a[0] = a[0] xor RC[round]
        }
    }
}

/**
 * Minimal Argon2id implementation sufficient for Keenetic NDW4 params (p=1, small m,t).
 * Based on RFC 9106 / reference algorithm. Supports parallelism=1.
 */
object Argon2id {
    private const val BLOCK_BYTES = 1024
    private const val QWORDS = BLOCK_BYTES / 8
    private const val VERSION = 0x13

    fun derive(password: ByteArray, salt: ByteArray, iterations: Int, memoryKiB: Int, parallelism: Int, hashLength: Int): ByteArray {
        require(parallelism == 1) { "Only parallelism=1 supported" }
        val mPrime = 4 * parallelism * (memoryKiB / (4 * parallelism)).coerceAtLeast(2)
        val laneLen = mPrime / parallelism
        val segmentLen = laneLen / 4
        // H0
        val h0 = hPrime(password, salt, iterations, mPrime, parallelism, hashLength)
        // memory as blocks (qword arrays)
        val mem = Array(mPrime) { LongArray(QWORDS) }
        // init first two blocks per lane
        for (lane in 0 until parallelism) {
            mem[lane * laneLen] = hBlock(h0, 0, lane)
            mem[lane * laneLen + 1] = hBlock(h0, 1, lane)
        }
        // fill
        for (pass in 0 until iterations) {
            for (slice in 0..3) {
                for (lane in 0 until parallelism) {
                    val start = if (pass == 0 && slice == 0) 2 else 0
                    for (idx in start until segmentLen) {
                        val cur = lane * laneLen + slice * segmentLen + idx
                        val prev = if (cur % laneLen == 0) lane * laneLen + laneLen - 1 else cur - 1
                        val ref = indexAlpha(pass, slice, idx, lane, laneLen, segmentLen, parallelism, mem[prev][0], iterations)
                        val refBlock = mem[ref]
                        val prevBlock = mem[prev]
                        val mixed = compress(prevBlock, refBlock)
                        mem[cur] = if (pass == 0) mixed else xorBlock(mem[cur], mixed)
                    }
                }
            }
        }
        // finalize (p==1)
        var finalBlock = mem[laneLen - 1].copyOf()
        for (lane in 1 until parallelism) finalBlock = xorBlock(finalBlock, mem[(lane + 1) * laneLen - 1])
        return hOut(finalBlock, hashLength)
    }

    private fun hPrime(password: ByteArray, salt: ByteArray, t: Int, m: Int, p: Int, tagLen: Int): ByteArray {
        val msg = le32(p) + le32(tagLen) + le32(m) + le32(t) + le32(VERSION) + le32(2) /*Argon2id*/ +
            le32(password.size) + password + le32(salt.size) + salt + le32(0) + le32(0)
        return variableHash(msg, 64)
    }

    private fun hBlock(h0: ByteArray, blockIndex: Int, lane: Int): LongArray {
        val msg = le32(BLOCK_BYTES) + h0 + le32(blockIndex) + le32(lane)
        val bytes = variableHash(msg, BLOCK_BYTES)
        return bytesToBlock(bytes)
    }

    private fun hOut(finalBlock: LongArray, tagLen: Int): ByteArray {
        val msg = le32(tagLen) + blockToBytes(finalBlock)
        return variableHash(msg, tagLen)
    }

    private fun variableHash(msg: ByteArray, outLen: Int): ByteArray {
        if (outLen <= 64) {
            val b = blake2b(msg, le32(outLen), outLen)
            return b.copyOf(outLen)
        }
        var v = blake2b(msg, le32(outLen), 64)
        val out = ByteArray(outLen)
        System.arraycopy(v, 0, out, 0, 32)
        var off = 32
        var remaining = outLen - 32
        while (remaining > 64) {
            v = blake2b(v, byteArrayOf(), 64)
            System.arraycopy(v, 0, out, off, 32)
            off += 32
            remaining -= 32
        }
        v = blake2b(v, byteArrayOf(), remaining)
        System.arraycopy(v, 0, out, off, remaining)
        return out
    }

    // Minimal Blake2b for variable-length hash construction (Argon2 uses Blake2b)
    private fun blake2b(msg: ByteArray, keyOrEmpty: ByteArray, outLen: Int): ByteArray {
        return Blake2b.hash(msg, keyOrEmpty, outLen)
    }

    private fun indexAlpha(pass: Int, slice: Int, idx: Int, lane: Int, laneLen: Int, segmentLen: Int, p: Int, pseudoRand: Long, t: Int): Int {
        val j1 = (pseudoRand and 0xFFFFFFFFL).toInt()
        val refLane = if (pass == 0 && slice == 0) lane else ((pseudoRand ushr 32) and 0xFFFFFFFFL).toInt() % p
        val sameLane = refLane == lane
        val area: Int
        val startPos: Int
        if (pass == 0) {
            if (slice == 0) { area = idx - 1; startPos = 0 }
            else { area = if (sameLane) slice * segmentLen + idx - 1 else slice * segmentLen - (if (idx == 0) 1 else 0); startPos = 0 }
        } else {
            area = if (sameLane) laneLen - segmentLen + idx - 1 else laneLen - segmentLen - (if (idx == 0) 1 else 0)
            startPos = ((slice + 1) * segmentLen) % laneLen
        }
        val rel = ((j1.toLong() and 0xFFFFFFFFL) * (j1.toLong() and 0xFFFFFFFFL) ushr 32)
        val rel2 = area - 1 - ((area.toLong() * rel) ushr 32).toInt()
        return refLane * laneLen + (startPos + rel2) % laneLen
    }

    private fun compress(x: LongArray, y: LongArray): LongArray {
        val r = LongArray(QWORDS)
        for (i in 0 until QWORDS) r[i] = x[i] xor y[i]
        val q = r.copyOf()
        // rows
        for (row in 0..7) p(q, row*16, row*16+2, row*16+4, row*16+6, row*16+8, row*16+10, row*16+12, row*16+14)
        // cols
        for (col in 0..7) p(q, col*2, col*2+32, col*2+64, col*2+96, col*2+1, col*2+33, col*2+65, col*2+97)
        for (i in 0 until QWORDS) q[i] = q[i] xor r[i]
        return q
    }

    private fun p(v: LongArray, a: Int, b: Int, c: Int, d: Int, e: Int, f: Int, g: Int, h: Int) {
        fun g2(va: Int, vb: Int, vc: Int, vd: Int) {
            var A = v[va]; var B = v[vb]; var C = v[vc]; var D = v[vd]
            A += B + 2 * (A and 0xFFFFFFFFL) * (B and 0xFFFFFFFFL)
            D = java.lang.Long.rotateRight(D xor A, 32)
            C += D + 2 * (C and 0xFFFFFFFFL) * (D and 0xFFFFFFFFL)
            B = java.lang.Long.rotateRight(B xor C, 24)
            A += B + 2 * (A and 0xFFFFFFFFL) * (B and 0xFFFFFFFFL)
            D = java.lang.Long.rotateRight(D xor A, 16)
            C += D + 2 * (C and 0xFFFFFFFFL) * (D and 0xFFFFFFFFL)
            B = java.lang.Long.rotateRight(B xor C, 63)
            v[va]=A; v[vb]=B; v[vc]=C; v[vd]=D
        }
        g2(a,b,c,d); g2(e,f,g,h)
    }

    private fun xorBlock(a: LongArray, b: LongArray) = LongArray(QWORDS) { a[it] xor b[it] }
    private fun le32(v: Int) = byteArrayOf((v and 0xFF).toByte(), ((v ushr 8) and 0xFF).toByte(), ((v ushr 16) and 0xFF).toByte(), ((v ushr 24) and 0xFF).toByte())
    private fun bytesToBlock(b: ByteArray): LongArray {
        val out = LongArray(QWORDS)
        for (i in 0 until QWORDS) {
            var v = 0L
            for (j in 0..7) v = v or ((b[i*8+j].toLong() and 0xFF) shl (8*j))
            out[i] = v
        }
        return out
    }
    private fun blockToBytes(bl: LongArray): ByteArray {
        val out = ByteArray(BLOCK_BYTES)
        for (i in 0 until QWORDS) for (j in 0..7) out[i*8+j] = ((bl[i] ushr (8*j)) and 0xFF).toByte()
        return out
    }
}

/** Minimal Blake2b needed by Argon2. */
object Blake2b {
    private val IV = longArrayOf(
        0x6a09e667f3bcc908L, -0x4498517a7b3558c5L, 0x3c6ef372fe94f82bL, -0x5ab00ac5a0e2c90fL,
        0x510e527fade682d1L, -0x64fa9773d4c193e1L, 0x1f83d9abfb41bd6bL, 0x5be0cd19137e2179L
    )
    private val SIGMA = arrayOf(
        byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15),
        byteArrayOf(14,10,4,8,9,15,13,6,1,12,0,2,11,7,5,3),
        byteArrayOf(11,8,12,0,5,2,15,13,10,14,3,6,7,1,9,4),
        byteArrayOf(7,9,3,1,13,12,11,14,2,6,5,10,4,0,15,8),
        byteArrayOf(9,0,5,7,2,4,10,15,14,1,11,12,6,8,3,13),
        byteArrayOf(2,12,6,10,0,11,8,3,4,13,7,5,15,14,1,9),
        byteArrayOf(12,5,1,15,14,13,4,10,0,7,6,3,9,2,8,11),
        byteArrayOf(13,11,7,14,12,1,3,9,5,0,15,4,8,6,2,10),
        byteArrayOf(6,15,14,9,11,3,0,8,12,2,13,7,1,4,10,5),
        byteArrayOf(10,2,8,4,7,6,1,5,15,11,9,14,3,12,13,0),
        byteArrayOf(0,1,2,3,4,5,6,7,8,9,10,11,12,13,14,15),
        byteArrayOf(14,10,4,8,9,15,13,6,1,12,0,2,11,7,5,3)
    )

    fun hash(msg: ByteArray, key: ByteArray, outLen: Int): ByteArray {
        val h = IV.copyOf()
        h[0] = h[0] xor (0x01010000L or (key.size.toLong() shl 8) or outLen.toLong())
        var t0 = 0L; var t1 = 0L
        val buf = ByteArray(128)
        var bufLen = 0
        fun compress(block: ByteArray, off: Int, last: Boolean) {
            val m = LongArray(16)
            for (i in 0..15) {
                var v = 0L
                for (j in 0..7) v = v or ((block[off + i*8 + j].toLong() and 0xFF) shl (8*j))
                m[i] = v
            }
            val v = LongArray(16)
            for (i in 0..7) v[i] = h[i]
            for (i in 0..7) v[i+8] = IV[i]
            v[12] = v[12] xor t0; v[13] = v[13] xor t1
            if (last) v[14] = v[14].inv()
            fun g(a: Int, b: Int, c: Int, d: Int, x: Long, y: Long) {
                v[a] = v[a] + v[b] + x
                v[d] = java.lang.Long.rotateRight(v[d] xor v[a], 32)
                v[c] = v[c] + v[d]
                v[b] = java.lang.Long.rotateRight(v[b] xor v[c], 24)
                v[a] = v[a] + v[b] + y
                v[d] = java.lang.Long.rotateRight(v[d] xor v[a], 16)
                v[c] = v[c] + v[d]
                v[b] = java.lang.Long.rotateRight(v[b] xor v[c], 63)
            }
            for (r in 0..11) {
                val s = SIGMA[r]
                g(0,4,8,12,m[s[0].toInt()],m[s[1].toInt()])
                g(1,5,9,13,m[s[2].toInt()],m[s[3].toInt()])
                g(2,6,10,14,m[s[4].toInt()],m[s[5].toInt()])
                g(3,7,11,15,m[s[6].toInt()],m[s[7].toInt()])
                g(0,5,10,15,m[s[8].toInt()],m[s[9].toInt()])
                g(1,6,11,12,m[s[10].toInt()],m[s[11].toInt()])
                g(2,7,8,13,m[s[12].toInt()],m[s[13].toInt()])
                g(3,4,9,14,m[s[14].toInt()],m[s[15].toInt()])
            }
            for (i in 0..7) h[i] = h[i] xor v[i] xor v[i+8]
        }
        fun update(data: ByteArray, off: Int, len: Int) {
            var o = off; var l = len
            if (bufLen + l > 128) {
                val fill = 128 - bufLen
                System.arraycopy(data, o, buf, bufLen, fill)
                t0 += 128; if (t0 == 0L) t1++
                compress(buf, 0, false)
                bufLen = 0; o += fill; l -= fill
                while (l > 128) {
                    t0 += 128; if (t0 == 0L) t1++
                    compress(data, o, false)
                    o += 128; l -= 128
                }
            }
            System.arraycopy(data, o, buf, bufLen, l)
            bufLen += l
        }
        if (key.isNotEmpty()) {
            val kb = ByteArray(128)
            System.arraycopy(key, 0, kb, 0, key.size)
            update(kb, 0, 128)
        }
        update(msg, 0, msg.size)
        t0 += bufLen.toLong(); if (t0 < bufLen) t1++
        val pad = ByteArray(128 - bufLen)
        System.arraycopy(pad, 0, buf, bufLen, pad.size)
        compress(buf, 0, true)
        val out = ByteArray(outLen)
        for (i in 0 until outLen) out[i] = ((h[i/8] ushr (8*(i%8))) and 0xFF).toByte()
        return out
    }
}
