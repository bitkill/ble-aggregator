package org.ruifernandes.ble.aggregator.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

/**
 * Root object coming from mqtt, from blea2mqtt.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
data class WrappedBleResult(
    val macAddress: String,
    val rssi: Short,
    val receivedFrom: String,
    val parser: String,
    val deviceType: String,
    val result: Map<String, Float>?
)
