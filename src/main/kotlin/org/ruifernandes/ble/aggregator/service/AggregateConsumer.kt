package org.ruifernandes.ble.aggregator.service

import com.github.benmanes.caffeine.cache.Cache
import com.github.benmanes.caffeine.cache.Caffeine
import io.quarkus.vertx.ConsumeEvent
import org.jboss.logging.Logger
import org.ruifernandes.ble.aggregator.model.WrappedBleResult
import java.io.Serializable
import java.util.concurrent.CompletableFuture
import java.util.concurrent.CompletionStage
import java.util.concurrent.TimeUnit
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
class AggregateConsumer(deviceEventCache: Any) {

    @Inject
    lateinit var logger : Logger

    var allDevices: MutableSet<String> = mutableSetOf()

    final var deviceEventCache : Cache<String, Map<String, Serializable>> = Caffeine.newBuilder()
        .expireAfterWrite(30, TimeUnit.MINUTES)
        .maximumSize(100)
        .build()


    @ConsumeEvent("aggregate_fn")
    fun process(wrapped : WrappedBleResult) : CompletionStage<Void> {
        logger.debug(wrapped)
        allDevices.add(wrapped.macAddress)

        val oldEntries = deviceEventCache.getIfPresent(wrapped.macAddress) ?: mapOf()
        val mergedEntries = mutableMapOf<String, Serializable>()


        wrapped.result?.let {
            mergedEntries.putAll(oldEntries)
            mergedEntries.putAll(it.event)
            deviceEventCache.put(wrapped.macAddress, mergedEntries)
        }
        return CompletableFuture.completedFuture(null)
    }

    fun getAllCacheKeys(): Map<String, Map<String, Serializable>> {
        return deviceEventCache.getAllPresent(allDevices)
    }

}