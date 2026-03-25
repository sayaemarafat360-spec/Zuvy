package com.zuvy.app.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged

/**
 * Network Manager for connectivity monitoring and management
 */
class NetworkManager(private val context: Context) {
    
    private val connectivityManager = 
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    
    enum class ConnectionType {
        WIFI, MOBILE, ETHERNET, UNKNOWN, NONE
    }
    
    sealed class NetworkState {
        object Available : NetworkState()
        object Unavailable : NetworkState()
        object Lost : NetworkState()
        data class Changed(val type: ConnectionType, val isMetered: Boolean) : NetworkState()
    }
    
    /**
     * Observe network state changes
     */
    fun observeNetworkState(): Flow<NetworkState> = callbackFlow {
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(NetworkState.Available)
            }
            
            override fun onUnavailable() {
                trySend(NetworkState.Unavailable)
            }
            
            override fun onLost(network: Network) {
                trySend(NetworkState.Lost)
            }
            
            override fun onCapabilitiesChanged(
                network: Network,
                networkCapabilities: NetworkCapabilities
            ) {
                val type = when {
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> 
                        ConnectionType.WIFI
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> 
                        ConnectionType.MOBILE
                    networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> 
                        ConnectionType.ETHERNET
                    else -> ConnectionType.UNKNOWN
                }
                
                val isMetered = !networkCapabilities.hasCapability(
                    NetworkCapabilities.NET_CAPABILITY_NOT_METERED
                )
                
                trySend(NetworkState.Changed(type, isMetered))
            }
        }
        
        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        
        connectivityManager.registerNetworkCallback(request, callback)
        
        awaitClose {
            connectivityManager.unregisterNetworkCallback(callback)
        }
    }.distinctUntilChanged()
    
    /**
     * Check if device is online
     */
    fun isOnline(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
    
    /**
     * Check if connected to WiFi
     */
    fun isWifi(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Check if connected to mobile data
     */
    fun isMobileData(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
    
    /**
     * Check if connection is metered
     */
    fun isMetered(): Boolean {
        val network = connectivityManager.activeNetwork ?: return true
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return true
        return !capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_NOT_METERED)
    }
    
    /**
     * Get current connection type
     */
    fun getConnectionType(): ConnectionType {
        val network = connectivityManager.activeNetwork ?: return ConnectionType.NONE
        val capabilities = connectivityManager.getNetworkCapabilities(network) 
            ?: return ConnectionType.NONE
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> ConnectionType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> ConnectionType.MOBILE
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> ConnectionType.ETHERNET
            else -> ConnectionType.UNKNOWN
        }
    }
    
    /**
     * Get network strength (0-4)
     */
    fun getSignalStrength(): Int {
        val network = connectivityManager.activeNetwork ?: return 0
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0
        
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                // WiFi signal strength would need WifiManager
                4
            }
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> {
                // Cellular signal strength would need TelephonyManager
                3
            }
            else -> 0
        }
    }
    
    /**
     * Get download speed estimate (bits per second)
     */
    fun getEstimatedDownloadSpeed(): Long {
        val network = connectivityManager.activeNetwork ?: return 0
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return 0
        
        return capabilities.linkDownstreamBandwidthKbps * 1000L
    }
    
    /**
     * Check if network is suitable for HD streaming
     */
    fun canStreamHD(): Boolean {
        if (!isOnline()) return false
        if (isWifi()) return true
        if (isMetered()) return false
        return getEstimatedDownloadSpeed() >= 5_000_000 // 5 Mbps
    }
    
    /**
     * Get network info as string
     */
    fun getNetworkInfo(): String {
        return when (getConnectionType()) {
            ConnectionType.WIFI -> "WiFi (${getEstimatedDownloadSpeed() / 1_000_000} Mbps)"
            ConnectionType.MOBILE -> "Mobile Data ${if (isMetered()) "(Metered)" else ""}"
            ConnectionType.ETHERNET -> "Ethernet"
            ConnectionType.UNKNOWN -> "Unknown"
            ConnectionType.NONE -> "No Connection"
        }
    }
}
