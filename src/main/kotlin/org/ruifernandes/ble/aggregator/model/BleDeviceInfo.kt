package org.ruifernandes.ble.aggregator.model

import java.time.LocalDateTime

data class BleDeviceInfo(
    var lastRssi: Short,
    var lastBroadcast: LocalDateTime,
    val deviceTypes: MutableSet<String>,
    val providers: MutableSet<String>,
)
