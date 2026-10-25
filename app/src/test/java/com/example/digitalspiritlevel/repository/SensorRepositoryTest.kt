package com.example.digitalspiritlevel.repository

import com.example.digitalspiritlevel.helpers.NetworkStatusProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class SensorRepositoryTest {

    @Test
    fun sensorDataMapsToJsonPayload() {
        val data = sampleData()

        val payload = data.toPayload()

        assertEquals("emulator-01", payload.deviceId)
        assertEquals("accelerometer", payload.sensorType)
        assertEquals(9.81f, payload.value)
        assertEquals(1_234L, payload.timestamp)
    }

    @Test
    fun saveReturnsPendingErrorWithoutCallingApiWhenOffline() = runBlocking {
        var apiCalled = false
        val api = object : CloudApi {
            override suspend fun uploadSensorData(
                payload: SensorPayload
            ): Response<SensorPayload> {
                apiCalled = true
                return Response.success(payload)
            }
        }
        val repository = SensorRepository(
            api = api,
            networkStatusProvider = NetworkStatusProvider { false }
        )

        val result = repository.save(sampleData())

        assertTrue(result.exceptionOrNull() is NoInternetException)
        assertFalse(apiCalled)
    }

    private fun sampleData() = SensorData(
        deviceId = "emulator-01",
        timestamp = 1_234L,
        value = 9.81f,
        type = "accelerometer",
        x = 0.1f,
        y = 0.2f,
        z = 9.8f
    )
}
