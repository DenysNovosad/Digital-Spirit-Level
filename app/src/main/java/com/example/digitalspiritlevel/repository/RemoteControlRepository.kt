package com.example.digitalspiritlevel.repository

import android.util.Log
import com.example.digitalspiritlevel.helpers.NetworkStatusProvider
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class RemoteControlRepository(
    private val api: ControlApi,
    private val networkStatusProvider: NetworkStatusProvider
) {

    suspend fun fetchCommand(commandId: Int): Result<ControlCommand> =
        withContext(Dispatchers.IO) {
            if (!networkStatusProvider.isOnline()) {
                return@withContext Result.failure(NoInternetException())
            }

            try {
                val response = api.getCommand(commandId)
                if (!response.isSuccessful) {
                    throw HttpException(response)
                }

                val body = response.body()
                    ?: error("Сервер повернув порожню команду")
                val command = body.toControlCommand()
                Log.d(
                    TAG,
                    "Command #${command.id}: enabled=${command.enabled}"
                )
                Result.success(command)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Log.e(TAG, "Command polling failed", error)
                Result.failure(error)
            }
        }

    private companion object {
        const val TAG = "RemoteControlRepository"
    }
}
