package org.ruifernandes.ble.aggregator.model

import com.fasterxml.jackson.annotation.JsonIgnoreProperties
import java.io.Serializable

@JsonIgnoreProperties(ignoreUnknown = true)
data class ParserResult(
    val event: Map<String, Float>,
    val deviceType: String,
    val eventType: Short
)
