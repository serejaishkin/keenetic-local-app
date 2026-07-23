package com.keenetic.local.util

/**
 * Офлайн-определение производителя устройства по первым трём октетам MAC
 * (OUI-префикс). Используется как запасной вариант отображаемого имени,
 * когда роутер не знает hostname устройства (DHCP/mDNS его не сообщили).
 * Список не претендует на полноту - только частые производители бытовой
 * электроники, чтобы вместо голого MAC показать хоть что-то осмысленное.
 */
object OuiLookup {

    private val prefixes: Map<String, String> = mapOf(
        "38:2C:E5" to "Xiaomi", "F0:B4:29" to "Xiaomi", "64:09:80" to "Xiaomi",
        "18:84:C1" to "Amazon/Roku", "AC:63:BE" to "Amazon",
        "DC:A6:32" to "Raspberry Pi", "B8:27:EB" to "Raspberry Pi",
        "3C:0B:4F" to "Huawei", "00:E0:FC" to "Huawei",
        "A2:3B:0B" to "Apple (случайный MAC)", "96:D5:E0" to "Apple (случайный MAC)",
        "F4:F5:D8" to "Google", "54:60:09" to "Google",
        "60:3D:61" to "Samsung", "8C:79:F5" to "Samsung", "E8:22:81" to "Samsung",
        "04:92:26" to "Sony", "56:6D:5F" to "случайный MAC",
        "C0:F8:53" to "Amazon", "80:64:7C" to "TP-Link",
        "56:DC:74" to "случайный MAC",
        "4C:E0:DB" to "Espressif (IoT/ESP)", "24:6F:28" to "Espressif (IoT/ESP)",
        "18:FE:34" to "Espressif (IoT/ESP)", "2C:F4:32" to "Espressif (IoT/ESP)",
        "00:1A:11" to "Google", "3C:5A:B4" to "Google",
        "D8:BB:2C" to "TP-Link", "50:C7:BF" to "TP-Link",
        "FC:A6:67" to "Xiaomi", "78:11:DC" to "Xiaomi",
        "AC:37:43" to "Xiaomi", "34:CE:00" to "Xiaomi"
    )

    /**
     * Возвращает "Неизвестное устройство (Vendor)" если производитель
     * определился по MAC, иначе null - вызывающий код сам решает, что
     * показать в качестве последнего фолбэка (например, сам MAC).
     */
    fun guessName(mac: String?): String? {
        if (mac.isNullOrBlank()) return null
        val normalized = mac.uppercase().replace("-", ":")
        val prefix = normalized.split(":").take(3).joinToString(":")
        val vendor = prefixes[prefix] ?: return null
        return "Устройство ($vendor)"
    }
}
