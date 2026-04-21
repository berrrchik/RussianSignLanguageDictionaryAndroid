package com.rsl.dictionary.services.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import com.rsl.dictionary.testing.rules.MainDispatcherRule
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class NetworkMonitorTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun connectionIsValid_whenInternetCapabilityIsPresent() {
        val network = mockk<Network>()
        val onlineManager = connectivityManager()
        every { onlineManager.activeNetwork } returns network
        every { onlineManager.getNetworkCapabilities(network) } returns capabilities(
            hasInternet = true,
            hasValidated = false
        )

        val offlineManager = connectivityManager()
        every { offlineManager.activeNetwork } returns network
        every { offlineManager.getNetworkCapabilities(network) } returns capabilities(
            hasInternet = false,
            hasValidated = true
        )

        val monitorWithInternet = NetworkMonitor(context(onlineManager))
        val monitorWithoutInternet = NetworkMonitor(context(offlineManager))

        assertTrue(monitorWithInternet.isConnected())
        assertFalse(monitorWithoutInternet.isConnected())
    }

    @Test
    fun noActiveNetwork_returnsFalse() {
        val manager = connectivityManager()
        every { manager.activeNetwork } returns null

        val monitor = NetworkMonitor(context(manager))

        assertFalse(monitor.isConnected())
    }

    @Test
    fun checkConnection_returnsFalseOnTimeout() = runTest {
        val manager = connectivityManager()
        every { manager.activeNetwork } returns null

        val monitor = NetworkMonitor(context(manager))
        val result = async { monitor.checkConnection() }

        advanceTimeBy(3_001)
        runCurrent()

        assertFalse(result.await())
    }

    @Test
    fun checkConnection_returnsTrueWhenConnectionAppearsBeforeTimeout() = runTest {
        val manager = connectivityManager()
        val callback = slot<ConnectivityManager.NetworkCallback>()
        val network = mockk<Network>()
        every { manager.registerDefaultNetworkCallback(capture(callback)) } just Runs
        every { manager.activeNetwork } returns null

        val monitor = NetworkMonitor(context(manager))
        val result = async { monitor.checkConnection() }

        val validCapabilities = capabilities(
            hasInternet = true,
            hasValidated = false
        )

        every { manager.activeNetwork } returns network
        every { manager.getNetworkCapabilities(network) } returns validCapabilities
        callback.captured.onCapabilitiesChanged(network, validCapabilities)

        assertTrue(result.await())
    }

    private fun context(connectivityManager: ConnectivityManager): Context {
        return mockk {
            every { getSystemService(ConnectivityManager::class.java) } returns connectivityManager
        }
    }

    private fun connectivityManager(): ConnectivityManager {
        return mockk(relaxed = true) {
            every { registerDefaultNetworkCallback(any()) } just Runs
        }
    }

    private fun capabilities(hasInternet: Boolean, hasValidated: Boolean): NetworkCapabilities {
        return mockk {
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) } returns hasInternet
            every { hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) } returns hasValidated
        }
    }
}
