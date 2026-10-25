package com.example.digitalspiritlevel

import android.app.Application
import com.example.digitalspiritlevel.helpers.NetworkMonitor
import com.example.digitalspiritlevel.repository.CloudApi
import com.example.digitalspiritlevel.repository.SensorRepository

class DigitalSpiritLevelApplication : Application() {

    val sensorRepository: SensorRepository by lazy {
        SensorRepository(
            api = CloudApi.create(),
            networkStatusProvider = NetworkMonitor(this)
        )
    }
}
