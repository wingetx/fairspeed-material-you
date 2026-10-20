package com.fairspeed.test

import com.fairspeed.test.domain.model.Server
import com.fairspeed.test.domain.model.SpeedTestResult
import org.junit.Test
import org.junit.Assert.assertEquals

class SpeedTestResultTest {

    @Test
    fun testSpeedTestResultCreation() {
        val server = Server(name = "Test Server", location = "Test Location")
        val result = SpeedTestResult(
            downloadSpeed = 100.0,
            uploadSpeed = 50.0,
            ping = 20.0,
            jitter = 5.0,
            server = server
        )

        assertEquals(100.0, result.downloadSpeed, 0.01)
        assertEquals(50.0, result.uploadSpeed, 0.01)
        assertEquals(20.0, result.ping, 0.01)
        assertEquals(5.0, result.jitter, 0.01)
        assertEquals("Test Server", result.server.name)
    }

    @Test
    fun testServerCreation() {
        val server = Server(
            name = "Test Server",
            location = "Test Location",
            country = "Test Country",
            distance = 100.0
        )

        assertEquals("Test Server", server.name)
        assertEquals("Test Location", server.location)
        assertEquals("Test Country", server.country)
        assertEquals(100.0, server.distance, 0.01)
    }
}
