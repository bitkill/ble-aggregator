package org.ruifernandes.ble.aggregator.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.common.annotation.Blocking
import io.smallrye.mutiny.Multi
import io.smallrye.mutiny.Uni
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import org.ruifernandes.ble.aggregator.model.BleDeviceInfo
import org.ruifernandes.ble.aggregator.model.WrappedBleResult
import java.io.Serializable
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.util.function.Consumer
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject


/**
 * Aggregates data published from the MQTT service over a eventBus.
 */
@ApplicationScoped
class AggregateConsumer(
    @ConfigProperty(name = "ble.aggregator.cache.time", defaultValue = "30m")
    val cacheDuration : Duration,
    @ConfigProperty(name = "ble.aggregator.max.size", defaultValue = "100")
    val cacheMaxSize : Long,
    @Inject
    val meterRegistry: MeterRegistry,
    @Inject
    val logger : Logger
) {

    private var allDevices: MutableSet<String> = mutableSetOf()

    private var deviceInfoCache : Cache<String, BleDeviceInfo> = Caffeine.newBuilder()
        .expireAfterWrite(cacheDuration)
        .maximumSize(cacheMaxSize)
        .build()

    private var statuses: MultiGauge

    init {
        meterRegistry.gaugeCollectionSize("devices.collected.size", Tags.of(Tag.of("type", "ble")), allDevices)
        statuses = MultiGauge.builder("device.data")
            .description("Data decoded from ble advertisements")
            .register(meterRegistry)

        // use a scheduler or other method
        Multi.createFrom()
            .ticks()
            .every(Duration.ofSeconds(2))
            .subscribe()
            .with { registerData() }
    }

    @Blocking
    @ConsumeEvent("aggregate_fn")
    fun process(wrapped : WrappedBleResult) {
        logger.debug(wrapped)
        allDevices.add(wrapped.macAddress)

        // Update device info
        val deviceInfo = deviceInfoCache.getIfPresent(wrapped.macAddress)
            ?: BleDeviceInfo(wrapped.rssi, LocalDateTime.now(), mutableSetOf(), mutableSetOf(), mutableMapOf())

        deviceInfo.lastRssi = wrapped.rssi
        deviceInfo.providers.add(wrapped.parser)
        deviceInfo.lastBroadcast = LocalDateTime.now()
        wrapped.result?.let { deviceInfo.deviceTypes.add(it.deviceType) }

        //registerDeviceMetrics(wrapped)

        // update device data (merged)
        val oldEntries = deviceInfo.data
        val mergedEntries = mutableMapOf<String, Float>()

        wrapped.result?.let {
            mergedEntries.putAll(oldEntries)
            mergedEntries.putAll(it.event)
        }
        deviceInfo.data = mergedEntries
        deviceInfoCache.put(wrapped.macAddress, deviceInfo)
        //registerData(mergedEntries, wrapped)
    }

    private fun registerData() {
        statuses.register(
            getAllCachedDeviceData().flatMap { (macAddr, device) ->
                device.data.map { (type, numberVal) ->
                    MultiGauge.Row.of(
                        Tags.of(
                            Tag.of("device_addr", macAddr),
                            Tag.of("device_type", device.deviceTypes.toString()),
                            Tag.of("device_provider", device.providers.toString()),
                            Tag.of("data_type", type)
                        ),
                        numberVal
                    )
                }
            },
            true
        )
    }

    private fun registerDeviceMetrics(wrapped: WrappedBleResult) {
        meterRegistry.gauge(
            "devices.info.rssi",
            Tags.of(
                Tag.of("device_addr", wrapped.macAddress),
                Tag.of("device_type", wrapped.result?.deviceType ?: "none"),
                Tag.of("device_provider", wrapped.parser)
            ),
            wrapped.rssi
        )

        meterRegistry.gauge(
            "devices.info.last.broadcast",
            Tags.of(
                Tag.of("device_addr", wrapped.macAddress),
                Tag.of("device_type", wrapped.result?.deviceType ?: "none"),
                Tag.of("device_provider", wrapped.parser)
            ),
            Instant.now().epochSecond
        )
    }

    fun getAllCachedDeviceData(): Map<String, BleDeviceInfo> {
        return deviceInfoCache.getAllPresent(allDevices)
    }

}