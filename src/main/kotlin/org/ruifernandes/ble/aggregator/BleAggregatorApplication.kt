package org.ruifernandes.ble.aggregator

import org.eclipse.microprofile.openapi.annotations.OpenAPIDefinition
import org.eclipse.microprofile.openapi.annotations.info.Info
import org.eclipse.microprofile.openapi.annotations.tags.Tag
import javax.ws.rs.core.Application

@OpenAPIDefinition(
    info = Info(
        title = "Ble Aggregator for blea2mqtt",
        version = "0.0.0"
    ),
    tags = [Tag(name = "ble-aggregator")]
)
class BleAggregatorApplication : Application()
