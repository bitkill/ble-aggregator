package org.ruifernandes.ble.aggregator.configs

import org.eclipse.microprofile.config.inject.ConfigProperty
import java.time.Duration
import javax.enterprise.context.ApplicationScoped

@ApplicationScoped
class CacheConfigs (
    @ConfigProperty(name = "ble.aggregator.cache.time", defaultValue = "30m")
    var cacheDuration: Duration,
    @ConfigProperty(name = "ble.aggregator.cache.max.size", defaultValue = "100")
    var cacheMaxSize: Long
)