package org.ruifernandes.ble.aggregator

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import io.restassured.module.kotlin.extensions.Given
import io.restassured.module.kotlin.extensions.Then
import io.restassured.module.kotlin.extensions.When
import org.junit.jupiter.api.DisplayName
import org.junit.jupiter.api.Test
import javax.ws.rs.core.Response.Status.*

@QuarkusTest
class HealthCheckTest {

    // usage: https://github.com/rest-assured/rest-assured/wiki/Usage#kotlin-extension-module
    @Test
    @DisplayName("GET /q/health/live")
    fun testHealthLive() {
        Given {
            that()
        } When {
            get("/q/health/live")
        } Then {
            statusCode(OK.statusCode)
        }
    }
}