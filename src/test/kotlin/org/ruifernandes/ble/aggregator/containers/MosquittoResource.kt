package org.ruifernandes.ble.aggregator.containers

import io.quarkus.test.common.QuarkusTestResourceLifecycleManager
import org.jboss.logging.Logger
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.utility.DockerImageName
import javax.inject.Inject

/**
 * Spawn a mosquitto server via TestContainers.
 */
class MosquittoResource : QuarkusTestResourceLifecycleManager {

    @Inject
    lateinit var logger : Logger

    @Container
    val mosquittoContainer: MosquittoContainer? = MosquittoContainer(DockerImageName.parse("eclipse-mosquitto"))
        .withExposedPorts(1883)


    override fun start(): MutableMap<String, String> {
        return if (mosquittoContainer != null) {
            mosquittoContainer.start()

            mutableMapOf(
                Pair("mp.messaging.incoming.ble-sensors-in.host", mosquittoContainer.host),
                Pair("mp.messaging.incoming.ble-sensors-in.port", mosquittoContainer.firstMappedPort.toString()),
                Pair("mp.messaging.outgoing.ble-sensors-out.host", mosquittoContainer.host),
                Pair("mp.messaging.outgoing.ble-sensors-out.port", mosquittoContainer.firstMappedPort.toString()),
            )
        } else {
            logger.error("Unable to start Mosquitto Container")
            mutableMapOf()
        }
    }

    override fun stop() {
        mosquittoContainer?.stop()
    }
}

