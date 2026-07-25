package com.keenetic.local.api

/**
 * Модель одного устройства из `show device-list` (SSH CLI). Эта команда
 * даёт гораздо больше, чем /rci/show/ip/hotspot: реальный трафик rx/tx на
 * устройство, оффлайн-устройства в том же списке (link: down), назначенную
 * политику и приоритет прямо на хосте, и различает проводные/беспроводные
 * подключения. Подтверждено реальным выводом с роутера (не HAR).
 */
data class DeviceListEntry(
    val mac: String,
    val ip: String? = null,
    val hostname: String? = null,   // имя, которое сообщило само устройство (DHCP)
    val customName: String? = null, // имя, заданное вручную в веб-морде/приложении
    val interfaceDescription: String? = null, // например "Home network"
    val policy: String? = null,
    val priority: Int? = null,
    val access: String? = null,     // permit / deny
    val active: Boolean = false,    // link up прямо сейчас
    val rxbytes: Long = 0,
    val txbytes: Long = 0,
    val ssid: String? = null,       // если Wi-Fi
    val rssi: Int? = null,
    val security: String? = null,
    val wired: Boolean = false      // проводное подключение (есть "port")
) {
    /** Лучшее доступное имя для показа пользователю. */
    val displayName: String?
        get() = customName?.takeIf { it.isNotBlank() } ?: hostname?.takeIf { it.isNotBlank() }
}

/**
 * `show device-list` отдаёт не JSON, а текст с отступами (человекочитаемый
 * CLI-формат), в отличие от большинства других show-команд через REST.
 * Парсим построчно: новый блок устройства начинается с одиночной строки
 * "host:" (без значения после двоеточия).
 */
object DeviceListParser {

    private val lineRegex = Regex("""^\s*([\w-]+):\s*(.*?)\s*$""")

    fun parse(raw: String): List<DeviceListEntry> {
        val entries = mutableListOf<DeviceListEntry>()
        var current: MutableMap<String, String>? = null
        var seenInterfaceSection = false

        fun flush() {
            val fields = current ?: return
            val mac = fields["mac"] ?: return
            entries += DeviceListEntry(
                mac = mac,
                ip = fields["ip"]?.takeIf { it.isNotBlank() && it != "0.0.0.0" },
                hostname = fields["hostname"],
                customName = fields["name"],
                interfaceDescription = fields["description"],
                policy = fields["policy"]?.takeIf { it.isNotBlank() },
                priority = fields["priority"]?.toIntOrNull(),
                access = fields["access"],
                active = fields["link"]?.equals("up", ignoreCase = true) == true,
                rxbytes = fields["rxbytes"]?.toLongOrNull() ?: 0,
                txbytes = fields["txbytes"]?.toLongOrNull() ?: 0,
                ssid = fields["ssid"],
                rssi = fields["rssi"]?.toIntOrNull(),
                security = fields["security"],
                wired = fields.containsKey("port")
            )
        }

        raw.lineSequence().forEach { rawLine ->
            val trimmed = rawLine.trim()
            if (trimmed == "host:") {
                flush()
                current = mutableMapOf()
                seenInterfaceSection = false
                return@forEach
            }
            val fields = current ?: return@forEach
            if (trimmed == "interface:") {
                seenInterfaceSection = true
                return@forEach
            }
            val match = lineRegex.matchEntire(rawLine) ?: return@forEach
            val (key, value) = match.destructured
            if (value.isBlank()) return@forEach

            // "name" встречается дважды: имя устройства (нужное нам, идёт до
            // "interface:") и имя самого сетевого сегмента внутри interface:
            // ("name: Home") - его игнорируем. "description" берём только
            // из секции interface (описание сегмента, напр. "Home network").
            when (key) {
                "name" -> if (!seenInterfaceSection) fields["name"] = value
                "description" -> if (seenInterfaceSection) fields["description"] = value
                "mac" -> fields.putIfAbsent("mac", value) // только первое вхождение (у host, не у via)
                else -> fields.putIfAbsent(key, value)
            }
        }
        flush()
        return entries
    }
}
