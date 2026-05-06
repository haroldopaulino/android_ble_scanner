package com.harold.ble_insight.data.ble

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import com.harold.ble_insight.domain.model.BleDevice
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.launch

class AndroidBleScanner(
    private val context: Context
) : BleScanner {

    private val bluetoothManager by lazy {
        context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val bluetoothAdapter
        get() = bluetoothManager.adapter

    private val scanner
        get() = bluetoothAdapter?.bluetoothLeScanner

    private var activeCallback: ScanCallback? = null

    @SuppressLint("MissingPermission")
    override fun scan(scanPeriodMillis: Long): Flow<BleDevice> = callbackFlow {
        stop()

        val bluetoothLeScanner = scanner
        if (bluetoothLeScanner == null) {
            close(IllegalStateException("Bluetooth LE scanner is not available."))
            return@callbackFlow
        }

        val settings = ScanSettings.Builder()
            .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
            .setCallbackType(ScanSettings.CALLBACK_TYPE_ALL_MATCHES)
            .setMatchMode(ScanSettings.MATCH_MODE_AGGRESSIVE)
            .setNumOfMatches(ScanSettings.MATCH_NUM_MAX_ADVERTISEMENT)
            .setReportDelay(0L)
            .build()

        val callback = object : ScanCallback() {
            override fun onScanResult(callbackType: Int, result: ScanResult) {
                trySend(result.toBleDevice())
            }

            override fun onBatchScanResults(results: MutableList<ScanResult>) {
                results.forEach { result ->
                    trySend(result.toBleDevice())
                }
            }

            override fun onScanFailed(errorCode: Int) {
                close(IllegalStateException(errorCode.toScanErrorMessage()))
            }
        }

        activeCallback = callback

        val startResult = runCatching {
            bluetoothLeScanner.startScan(null, settings, callback)
        }

        startResult.onFailure { exception ->
            activeCallback = null
            close(exception)
        }

        if (startResult.isSuccess) {
            launch {
                delay(scanPeriodMillis)
                stop()
                close()
            }
        }

        awaitClose {
            stop()
        }
    }

    @SuppressLint("MissingPermission")
    override fun stop() {
        activeCallback?.let { callback ->
            runCatching {
                scanner?.stopScan(callback)
            }
        }
        activeCallback = null
    }

    @SuppressLint("MissingPermission")
    private fun ScanResult.toBleDevice(): BleDevice {
        return BleDevice(
            name = device.safeName(),
            address = device.safeAddress(),
            rssi = rssi,
            type = device.type.toDeviceType(),
            bondState = device.bondState.toBondState(),
            advertiseFlags = scanRecord?.advertiseFlags
        )
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeName(): String {
        return runCatching {
            name?.takeIf { it.isNotBlank() }
        }.getOrNull() ?: "Unknown Device"
    }

    @SuppressLint("MissingPermission")
    private fun BluetoothDevice.safeAddress(): String {
        return runCatching {
            address
        }.getOrNull() ?: "Unknown"
    }

    private fun Int.toDeviceType(): String {
        return when (this) {
            BluetoothDevice.DEVICE_TYPE_CLASSIC -> "Classic"
            BluetoothDevice.DEVICE_TYPE_LE -> "BLE"
            BluetoothDevice.DEVICE_TYPE_DUAL -> "Dual"
            else -> "Unknown"
        }
    }

    private fun Int.toBondState(): String {
        return when (this) {
            BluetoothDevice.BOND_BONDED -> "Bonded"
            BluetoothDevice.BOND_BONDING -> "Bonding"
            BluetoothDevice.BOND_NONE -> "Not bonded"
            else -> "Unknown"
        }
    }

    private fun Int.toScanErrorMessage(): String {
        return when (this) {
            ScanCallback.SCAN_FAILED_ALREADY_STARTED -> "BLE scan is already running."
            ScanCallback.SCAN_FAILED_APPLICATION_REGISTRATION_FAILED -> "BLE scan registration failed. Toggle Bluetooth off and on, then scan again."
            ScanCallback.SCAN_FAILED_FEATURE_UNSUPPORTED -> "BLE scan is not supported on this device."
            ScanCallback.SCAN_FAILED_INTERNAL_ERROR -> "Android Bluetooth stack reported an internal scan error."
            ScanCallback.SCAN_FAILED_OUT_OF_HARDWARE_RESOURCES -> "The device does not have enough Bluetooth scan resources available."
            ScanCallback.SCAN_FAILED_SCANNING_TOO_FREQUENTLY -> "Android blocked the scan because scans are being started too frequently. Wait a few seconds and try again."
            else -> "BLE scan failed with error code $this."
        }
    }
}