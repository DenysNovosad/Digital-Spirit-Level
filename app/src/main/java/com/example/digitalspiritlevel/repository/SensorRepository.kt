package com.example.digitalspiritlevel.repository

import android.util.Log
import com.example.digitalspiritlevel.helpers.NetworkStatusProvider
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.HttpException

class SensorRepository(
    private val api: CloudApi,
    private val networkStatusProvider: NetworkStatusProvider
) {

    suspend fun save(data: SensorData): Result<Unit> = withContext(Dispatchers.IO) {
        if (!networkStatusProvider.isOnline()) {
            return@withContext Result.failure(NoInternetException())
        }

        try {
            val response = api.uploadSensorData(data.toPayload())
            if (!response.isSuccessful) {
                throw HttpException(response)
            }

            Log.d(
                TAG,
                "POST successful: HTTP ${response.code()}, timestamp=${data.timestamp}"
            )
            Result.success(Unit)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            Log.e(TAG, "POST failed", error)
            Result.failure(error)
        }
    }

    private companion object {
        const val TAG = "SensorRepository"
    }
}

class NoInternetException : IOException("Відсутнє підключення до інтернету")
