package org.ruifernandes.ble.aggregator.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class WrappedBleResult(
    val macAddress: String,
    val rssi: Short,
    val receivedFrom: String,
    val parser: String,
    val result: ParserResult?
)
