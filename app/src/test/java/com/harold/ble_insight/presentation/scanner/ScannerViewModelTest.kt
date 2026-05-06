package com.harold.ble_insight.presentation.scanner

import com.harold.ble_insight.domain.model.BleDevice
import com.harold.ble_insight.domain.repository.BleDeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class ScannerViewModelTest {

    private lateinit var viewModel: ScannerViewModel
    private lateinit var fakeRepository: FakeBleDeviceRepository
    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeRepository = FakeBleDeviceRepository()
        viewModel = ScannerViewModel(fakeRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state is correct`() {
        val state = viewModel.state
        assertFalse(state.isScanning)
        assertFalse(state.hasPermission)
        assertTrue(state.devices.isEmpty())
        assertNull(state.message)
    }

    @Test
    fun `onPermissionResult updates state correctly`() {
        viewModel.onPermissionResult(true)
        assertTrue(viewModel.state.hasPermission)
        assertNull(viewModel.state.message)

        viewModel.onPermissionResult(false)
        assertFalse(viewModel.state.hasPermission)
        assertEquals("Bluetooth and location permissions are required to scan.", viewModel.state.message)
    }

    @Test
    fun `startScan updates state and collects devices`() = runTest {
        viewModel.onPermissionResult(true)
        viewModel.startScan()

        assertTrue(viewModel.state.isScanning)
        assertEquals("Scanning for nearby BLE devices...", viewModel.state.message)

        advanceUntilIdle()

        val device1 = BleDevice("Device 1", "00:11:22:33:44:55", -60, "Unknown", "Bonded", null)
        fakeRepository.emitDevice(device1)
        advanceUntilIdle()

        assertEquals(1, viewModel.state.devices.size)
        assertEquals(device1, viewModel.state.devices[0])
        assertNull(viewModel.state.message)

        val device2 = BleDevice("Device 2", "66:77:88:99:AA:BB", -50, "Unknown", "Not Bonded", null)
        fakeRepository.emitDevice(device2)
        advanceUntilIdle()

        assertEquals(2, viewModel.state.devices.size)
        assertEquals(device2, viewModel.state.devices[0])
        assertEquals(device1, viewModel.state.devices[1])
    }

    @Test
    fun `stopScan stops repository scan and updates state`() = runTest {
        viewModel.startScan()
        assertTrue(viewModel.state.isScanning)

        viewModel.stopScan()
        assertFalse(viewModel.state.isScanning)
        assertTrue(fakeRepository.stopScanCalled)
    }

    @Test
    fun `startScan handles error from repository`() = runTest {
        fakeRepository.shouldThrowError = true
        viewModel.onPermissionResult(true)
        viewModel.startScan()

        advanceUntilIdle()

        assertFalse(viewModel.state.isScanning)
        assertEquals("Test error", viewModel.state.message)
        assertTrue(fakeRepository.stopScanCalled)
    }

    private class FakeBleDeviceRepository : BleDeviceRepository {
        private val _devicesFlow = MutableSharedFlow<BleDevice>()
        var stopScanCalled = false
        var shouldThrowError = false

        override fun scanDevices(scanPeriodMillis: Long): Flow<BleDevice> {
            return if (shouldThrowError) {
                kotlinx.coroutines.flow.flow { throw Exception("Test error") }
            } else {
                _devicesFlow
            }
        }

        override fun stopScan() {
            stopScanCalled = true
        }

        suspend fun emitDevice(device: BleDevice) {
            _devicesFlow.emit(device)
        }
    }
}
