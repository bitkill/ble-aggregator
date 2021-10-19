package org.ruifernandes.ble.aggregator

import io.quarkus.test.junit.QuarkusTest
import io.restassured.RestAssured
import org.junit.jupiter.api.Test

@QuarkusTest
class HealthCheckTest {

    @Test
    fun testHealthLive() {

        RestAssured.given()
            .queryParam("", "")
            .`when`()
            .get("/q/health/live")
            .then()
            .statusCode(200)

    }
}