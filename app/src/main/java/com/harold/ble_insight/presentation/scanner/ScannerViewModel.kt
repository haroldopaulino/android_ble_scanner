package com.harold.ble_insight.presentation.scanner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.harold.ble_insight.domain.model.BleDevice
import com.harold.ble_insight.domain.repository.BleDeviceRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

class ScannerViewModel(
    private val repository: BleDeviceRepository
) : ViewModel() {

    var state by mutableStateOf(ScannerState())
        private set

    private var scanJob: Job? = null
    private val scanPeriodMillis = 60_000L

    fun onPermissionResult(isGranted: Boolean) {
        state = state.copy(
            hasPermission = isGranted,
            message = if (isGranted) null else "Bluetooth and location permissions are required to scan."
        )
    }

    fun onBluetoothUnavailable() {
        state = state.copy(
            isScanning = false,
            message = "Bluetooth is not available on this device."
        )
    }

    fun onBluetoothEnableDeclined() {
        state = state.copy(
            isScanning = false,
            message = "Bluetooth must be turned on before scanning."
        )
    }

    fun onBluetoothEnableRequestFailed() {
        state = state.copy(
            isScanning = false,
            message = "Unable to open the Bluetooth enable prompt."
        )
    }

    fun startScan() {
        if (state.isScanning) return

        state = state.copy(
            isScanning = true,
            devices = emptyList(),
            message = "Scanning for nearby BLE devices..."
        )

        scanJob?.cancel()
        scanJob = viewModelScope.launch {
            try {
                repository.scanDevices(scanPeriodMillis).collect { device ->
                    state = state.copy(
                        devices = state.devices.upsert(device),
                        message = null
                    )
                }
                state = state.copy(
                    isScanning = false,
                    message = if (state.devices.isEmpty()) {
                        "No BLE devices found. Make sure location is enabled, Bluetooth is enabled, and nearby devices are advertising."
                    } else {
                        null
                    }
                )
            } catch (exception: CancellationException) {
                throw exception
            } catch (exception: SecurityException) {
                repository.stopScan()
                state = state.copy(
                    isScanning = false,
                    message = "BLE scan was blocked by Android permissions. Check Nearby Devices and Location permissions."
                )
            } catch (exception: Throwable) {
                repository.stopScan()
                state = state.copy(
                    isScanning = false,
                    message = exception.message ?: "Unable to start BLE scan."
                )
            }
        }
    }

    fun stopScan() {
        scanJob?.cancel()
        repository.stopScan()
        state = state.copy(isScanning = false)
    }

    override fun onCleared() {
        stopScan()
        super.onCleared()
    }

    private fun List<BleDevice>.upsert(device: BleDevice): List<BleDevice> {
        val existingIndex = indexOfFirst { it.address == device.address }
        return if (existingIndex >= 0) {
            toMutableList().also { it[existingIndex] = device }
        } else {
            this + device
        }.sortedByDescending { it.rssi }
    }
}

class ScannerViewModelFactory(
    private val repository: BleDeviceRepository
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return ScannerViewModel(repository) as T
    }
}