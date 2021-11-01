package org.ruifernandes.ble.aggregator.service

import io.smallrye.mutiny.Uni
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import org.eclipse.microprofile.reactive.messaging.Incoming
import org.eclipse.microprofile.reactive.messaging.Message
import org.jboss.logging.Logger
import org.ruifernandes.ble.aggregator.model.WrappedBleResult
import java.nio.charset.StandardCharsets
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class MqttService {

    @Inject
    lateinit var logger : Logger

    @Inject
    lateinit var eventBus : EventBus

    @Incoming("ble-sensors-in")
    fun consume(message: Message<ByteArray?>): Uni<Void> {
        val stringPayload = String(message.payload!!, StandardCharsets.UTF_8)

        try {
            val parserResult = JsonObject(stringPayload)
                .mapTo(WrappedBleResult::class.java)
            eventBus.send("aggregate_fn", parserResult)

        } catch (ex : Exception) {
            logger.error("could not parse message {}", stringPayload, ex)
            return Uni.createFrom().failure<Void>(ex)
        }

        return Uni.createFrom()
            .completionStage(message.ack())
    }

}
