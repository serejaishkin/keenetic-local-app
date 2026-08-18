package com.keenetic.local.utils

import com.google.gson.JsonObject
import com.keenetic.local.api.InterfaceMapper
import com.keenetic.local.api.SwitchPort

/**
 * Compatibility extension used by RouterViewModel.
 * The actual SwitchPort mapping lives in InterfaceMapper so that all
 * interface parsing remains in one place.
 */
fun Map<String, JsonObject>.toSwitchPorts(): List<SwitchPort> =
    InterfaceMapper.toSwitchPorts(this)
