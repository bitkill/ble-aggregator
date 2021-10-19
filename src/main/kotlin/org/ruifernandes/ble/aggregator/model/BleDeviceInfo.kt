package org.ruifernandes.ble.aggregator.model

import java.time.LocalDateTime

/**
 * Internal representation of a BLE device.
 */
data class BleDeviceInfo(
    var lastRssi: Short,
    var lastBroadcast: LocalDateTime,
    val deviceTypes: MutableSet<String>,
    val providers: MutableSet<String>,
    var data: Map<String, Float>
)
