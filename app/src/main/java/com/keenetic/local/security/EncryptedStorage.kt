package com.keenetic.local.security

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

class EncryptedStorage(context: Context) {
    private val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    private val sharedPreferences = EncryptedSharedPreferences.create(
        "secret_shared_prefs",
        masterKeyAlias,
        context,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    fun savePassword(password: String) {
        sharedPreferences.edit().putString("saved_password", password).apply()
    }

    fun getPassword(): String? {
        return sharedPreferences.getString("saved_password", null)
    }

    fun clearPassword() {
        sharedPreferences.edit().remove("saved_password").apply()
    }
}
