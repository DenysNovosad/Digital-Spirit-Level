package com.example.digitalspiritlevel.repository

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Path

interface ControlApi {

    @GET("todos/{commandId}")
    suspend fun getCommand(
        @Path("commandId") commandId: Int
    ): Response<RemoteControlResponse>

    companion object {
        private const val BASE_URL = "https://jsonplaceholder.typicode.com/"

        fun create(): ControlApi = Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ControlApi::class.java)
    }
}
