package org.ruifernandes.ble.aggregator.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.common.annotation.Blocking
import org.eclipse.microprofile.config.inject.ConfigProperty
import org.jboss.logging.Logger
import org.ruifernandes.ble.aggregator.model.BleDeviceInfo
import org.ruifernandes.ble.aggregator.model.WrappedBleResult
import java.io.Serializable
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneOffset
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Aggregates data published from the MQTT service over a eventBus.
 */
@ApplicationScoped
class AggregateConsumer(
    @ConfigProperty(name = "ble.aggregator.cache.time", defaultValue = "30m")
    val cacheDuration : Duration,
    @Inject
    val meterRegistry: MeterRegistry,
    @Inject
    val logger : Logger
) {

    final var allDevices: MutableSet<String> = mutableSetOf()

    private var deviceEventCache : Cache<String, Map<String, Short>> = Caffeine.newBuilder()
        .expireAfterWrite(cacheDuration)
        .maximumSize(100)
        .build()

    private var deviceInfoCache : Cache<String, BleDeviceInfo> = Caffeine.newBuilder()
        .expireAfterWrite(cacheDuration)
        .maximumSize(100)
        .build()

    init {
        meterRegistry.gaugeCollectionSize("devices.collected.size", Tags.of(Tag.of("type", "ble")), allDevices)
    }

    @Blocking
    @ConsumeEvent("aggregate_fn")
    fun process(wrapped : WrappedBleResult) {
        logger.debug(wrapped)
        allDevices.add(wrapped.macAddress)

        // Update device info
        val deviceInfo = deviceInfoCache.getIfPresent(wrapped.macAddress)
            ?: BleDeviceInfo(wrapped.rssi, LocalDateTime.now(), mutableSetOf(), mutableSetOf())

        deviceInfo.lastRssi = wrapped.rssi
        deviceInfo.providers.add(wrapped.parser)
        deviceInfo.lastBroadcast = LocalDateTime.now()
        wrapped.result?.let { deviceInfo.deviceTypes.add(it.deviceType) }
        deviceInfoCache.put(wrapped.macAddress, deviceInfo)
        registerDeviceMetrics(wrapped)

        // update device data (merged)
        val oldEntries = deviceEventCache.getIfPresent(wrapped.macAddress) ?: mapOf()
        val mergedEntries = mutableMapOf<String, Short>()

        wrapped.result?.let {
            mergedEntries.putAll(oldEntries)
            mergedEntries.putAll(it.event)
            deviceEventCache.put(wrapped.macAddress, mergedEntries)
        }

        mergedEntries.forEach { (t, u) ->
            meterRegistry.gauge(
                "devices.data",
                Tags.of(
                    Tag.of("device_addr", wrapped.macAddress),
                    Tag.of("device_type", wrapped.result?.deviceType ?: "none"),
                    Tag.of("device_provider", wrapped.parser),
                    Tag.of("data_type", t)
                ),
                u
            )
        }
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
            LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli()
        )
    }

    fun getAllCachedEventData(): Map<String, Map<String, Serializable>> {
        return deviceEventCache.getAllPresent(allDevices)
    }

    fun getAllCachedDeviceData(): Map<String, BleDeviceInfo> {
        return deviceInfoCache.getAllPresent(allDevices)
    }

}