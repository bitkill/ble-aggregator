package org.ruifernandes.ble.aggregator.configs

import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import javax.enterprise.context.ApplicationScoped
import javax.inject.Inject

@ApplicationScoped
data class CacheConfigs (
    @Inject
    @ConfigProperty(name = "ble.aggregator.cache.time", defaultValue = "30m")
    val cacheDuration : Duration,
    @Inject
    @ConfigProperty(name = "ble.aggregator.cache.max.size", defaultValue = "100")
    val cacheMaxSize : Long
)
