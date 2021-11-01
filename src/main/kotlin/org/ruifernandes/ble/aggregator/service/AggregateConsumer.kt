package org.ruifernandes.ble.aggregator.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.github.mweirauch.micrometer.jvm.extras.ProcessMemoryMetrics
import io.github.mweirauch.micrometer.jvm.extras.ProcessThreadMetrics
import io.micrometer.core.instrument.MeterRegistry
import io.micrometer.core.instrument.MultiGauge
import io.micrometer.core.instrument.Tag
import io.micrometer.core.instrument.Tags
import io.quarkus.vertx.ConsumeEvent
import io.smallrye.mutiny.Multi
import org.jboss.logging.Logger
import org.ruifernandes.ble.aggregator.configs.CacheConfigs
import org.ruifernandes.ble.aggregator.model.BleDeviceInfo
import org.ruifernandes.ble.aggregator.model.WrappedBleResult
import java.time.Duration
import java.time.LocalDateTime
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

/**
 * Aggregates data published from the MQTT service over a eventBus.
 */
@ApplicationScoped
class AggregateConsumer(
    @Inject
    var cacheConfigs : CacheConfigs,
    @Inject
    var meterRegistry: MeterRegistry,
    @Inject
    var logger : Logger
) {

    private var allDevices: MutableSet<String> = mutableSetOf()

    private var deviceInfoCache : Cache<String, BleDeviceInfo> = Caffeine.newBuilder()
        .expireAfterWrite(cacheConfigs.cacheDuration)
        .maximumSize(cacheConfigs.cacheMaxSize)
        .build()

    private var statuses: MultiGauge
    private var signalStrength: MultiGauge
    private var registerCadence = Duration.ofSeconds(2)

    init {
        // register micrometer jvm extras
        ProcessMemoryMetrics().bindTo(meterRegistry)
        ProcessThreadMetrics().bindTo(meterRegistry)

        // custom gauges
        meterRegistry.gaugeCollectionSize(
            "devices.collected.size",
            Tags.of(Tag.of("type", "ble")),
            allDevices
        )
        statuses = MultiGauge.builder("device.data")
            .description("Data decoded from ble advertisements")
            .register(meterRegistry)

        signalStrength = MultiGauge.builder("device.info.rssi")
            .description("Signal strength from ble advertisements")
            .register(meterRegistry)

        // use a scheduler or other method
        Multi.createFrom()
            .ticks()
            .every(registerCadence)
            .subscribe()
            .with { registerData() }
    }

    /**
     * Consumes incoming event and aggregates it with the existing objects in cache.
     * TODO: Use async cache.
     */
    @ConsumeEvent("aggregate_fn", blocking = true)
    fun consume(wrapped : WrappedBleResult) {
        logger.debug(wrapped)

        // register device in the index
        allDevices.add(wrapped.macAddress)

        // Update device info
        val deviceInfo = deviceInfoCache.getIfPresent(wrapped.macAddress)
            ?: BleDeviceInfo(wrapped.macAddress, wrapped.rssi, LocalDateTime.now(), mutableSetOf(), mutableSetOf(), mutableMapOf())

        // update device data (merged)
        val oldEntries = deviceInfo.data
        val mergedEntries = mutableMapOf<String, Float>()

        wrapped.result?.let {
            mergedEntries.putAll(oldEntries)
            mergedEntries.putAll(it)
        }

        // update the device info with new entries
        deviceInfo.apply {
            lastRssi = wrapped.rssi
            providers.add(wrapped.parser)
            deviceTypes.add((wrapped.deviceType))
            lastBroadcast = LocalDateTime.now()
            data = mergedEntries
        }

        deviceInfoCache.put(deviceInfo.macAddress, deviceInfo)
    }

    /**
     * Registers data in micrometer gauges.
     */
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
        signalStrength.register(
            getAllCachedDeviceData().map { (macAddr, device) ->
                    MultiGauge.Row.of(
                        Tags.of(
                            Tag.of("device_addr", macAddr),
                            Tag.of("device_type", device.deviceTypes.toString()),
                            Tag.of("device_provider", device.providers.toString()),
                        ),
                        device.lastRssi
                    )
            },
            true
        )
        // TODO: last broadcast data
    }

    fun getAllCachedDeviceData(): Map<String, BleDeviceInfo> {
        return deviceInfoCache.getAllPresent(allDevices)
    }

}