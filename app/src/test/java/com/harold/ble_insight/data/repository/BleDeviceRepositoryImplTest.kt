package com.harold.ble_insight.data.repository

import app.cash.turbine.test
import com.harold.ble_insight.data.ble.BleScanner
import com.harold.ble_insight.domain.model.BleDevice
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class BleDeviceRepositoryImplTest {

    private lateinit var repository: BleDeviceRepositoryImpl
    private lateinit var fakeScanner: FakeBleScanner

    @Before
    fun setUp() {
        fakeScanner = FakeBleScanner()
        repository = BleDeviceRepositoryImpl(fakeScanner)
    }

    @Test
    fun `scanDevices calls scanner scan`() = runTest {
        val device = BleDevice("Test", "00:00:00:00:00:00", -50, "BLE", "Bonded", null)
        fakeScanner.devicesToEmit = listOf(device)

        repository.scanDevices(1000L).test {
            assertEquals(device, awaitItem())
            awaitComplete()
        }
        
        assertEquals(1000L, fakeScanner.lastScanPeriod)
    }

    @Test
    fun `stopScan calls scanner stop`() {
        repository.stopScan()
        assertTrue(fakeScanner.stopCalled)
    }

    private class FakeBleScanner : BleScanner {
        var lastScanPeriod: Long? = null
        var stopCalled = false
        var devicesToEmit = emptyList<BleDevice>()

        override fun scan(scanPeriodMillis: Long): Flow<BleDevice> {
            lastScanPeriod = scanPeriodMillis
            return flowOf(*devicesToEmit.toTypedArray())
        }

        override fun stop() {
            stopCalled = true
        }
    }
}
