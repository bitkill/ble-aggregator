package org.ruifernandes.ble.aggregator.resource

import org.eclipse.microprofile.openapi.annotations.Operation
import org.ruifernandes.ble.aggregator.model.BleDeviceInfo
import org.ruifernandes.ble.aggregator.service.AggregateConsumer
import java.io.Serializable
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject
import javax.ws.rs.GET
import javax.ws.rs.Path
import javax.ws.rs.Produces
import javax.ws.rs.core.MediaType

@Path("")
@Produces(MediaType.APPLICATION_JSON)
@ApplicationScoped
class AccumulatedEndpoint {

    @Inject
    internal lateinit var aggregateConsumer: AggregateConsumer

    @GET
    @Path("")
    @Operation(summary = "Gets all device info and events in cache")
    fun getDeviceOverview(): Map<String, BleDeviceInfo> {
        return aggregateConsumer.getAllCachedDeviceData()
    }

}