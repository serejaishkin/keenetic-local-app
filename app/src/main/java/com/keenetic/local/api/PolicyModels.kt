package com.keenetic.local.api

/**
 * Политика доступа в интернет (страница «Приоритеты подключений» на сайте).
 * Читается из show.sc.ip.policy — формат подтверждён HAR (route.har, /policies):
 * { "Policy0": { "description": "...", "permit": [ {"interface": "...", "enabled": true} ],
 *                "multipath": true } }
 */
data class PolicyPermit(
    val interfaceId: String,
    val enabled: Boolean
)

data class PolicyDetails(
    val id: String,
    val description: String = "",
    val permits: List<PolicyPermit> = emptyList(),
    val multipath: Boolean = false
)
