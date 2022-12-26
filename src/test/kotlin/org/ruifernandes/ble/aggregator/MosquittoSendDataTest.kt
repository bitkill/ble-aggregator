package org.ruifernandes.ble.aggregator

import io.quarkus.test.common.QuarkusTestResource
import io.quarkus.test.junit.QuarkusTest
import io.smallrye.mutiny.Multi
import org.eclipse.microprofile.reactive.messaging.Outgoing
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.ruifernandes.ble.aggregator.containers.MosquittoResource
import org.ruifernandes.ble.aggregator.service.AggregateConsumer
import java.time.Duration

//@Disabled
@QuarkusTest
@QuarkusTestResource(MosquittoResource::class)
class MosquittoSendDataTest {

    @Test
    fun testReceivingData() {
        // TODO
    }

    @Outgoing("ble-sensors-out")
    fun sendOutgoing() : Multi<String> {
        return Multi.createFrom()
            .ticks()
            .every(Duration.ofMillis(100))
            .map { "{\"macAddress\":\"592d3511e28c\",\"rssi\":-81,\"receivedFrom\":\"node1\",\"parser\":\"qingping\",\"deviceType\":\"CGDK2\",\"result\":{\"eventType\":1,\"temperature\":20.6,\"humidity\":58}}" }
    }
}