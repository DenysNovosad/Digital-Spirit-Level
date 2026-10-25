package com.example.digitalspiritlevel.helpers

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities

fun interface NetworkStatusProvider {
    fun isOnline(): Boolean
}

class NetworkMonitor(context: Context) : NetworkStatusProvider {

    private val connectivityManager =
        context.getSystemService(ConnectivityManager::class.java)

    override fun isOnline(): Boolean {
        val activeNetwork = connectivityManager.activeNetwork ?: return false
        val capabilities =
            connectivityManager.getNetworkCapabilities(activeNetwork) ?: return false

        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) &&
            capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }
}
