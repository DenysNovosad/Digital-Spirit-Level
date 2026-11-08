package com.example.digitalspiritlevel.repository

import com.example.digitalspiritlevel.helpers.NetworkStatusProvider
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import retrofit2.Response

class RemoteControlRepositoryTest {

    @Test
    fun responseMapsCompletedFieldToActuatorCommand() {
        val response = RemoteControlResponse(
            id = 4,
            title = "turn lamp on",
            completed = true
        )

        val command = response.toControlCommand()

        assertTrue(command.enabled)
    }

    @Test
    fun offlineStateDoesNotCallControlApi() = runBlocking {
        var apiCalled = false
        val api = object : ControlApi {
            override suspend fun getCommand(
                commandId: Int
            ): Response<RemoteControlResponse> {
                apiCalled = true
                return Response.success(
                    RemoteControlResponse(commandId, "test", true)
                )
            }
        }
        val repository = RemoteControlRepository(
            api = api,
            networkStatusProvider = NetworkStatusProvider { false }
        )

        val result = repository.fetchCommand(commandId = 4)

        assertTrue(result.exceptionOrNull() is NoInternetException)
        assertFalse(apiCalled)
    }
}
