package com.example.digitalspiritlevel

import android.app.Application
import com.example.digitalspiritlevel.helpers.NetworkMonitor
import com.example.digitalspiritlevel.repository.CloudApi
import com.example.digitalspiritlevel.repository.ControlApi
import com.example.digitalspiritlevel.repository.RemoteControlRepository
import com.example.digitalspiritlevel.repository.SensorRepository

class DigitalSpiritLevelApplication : Application() {

    private val networkMonitor by lazy { NetworkMonitor(this) }

    val sensorRepository: SensorRepository by lazy {
        SensorRepository(
            api = CloudApi.create(),
            networkStatusProvider = networkMonitor
        )
    }

    val remoteControlRepository: RemoteControlRepository by lazy {
        RemoteControlRepository(
            api = ControlApi.create(),
            networkStatusProvider = networkMonitor
        )
    }
}
