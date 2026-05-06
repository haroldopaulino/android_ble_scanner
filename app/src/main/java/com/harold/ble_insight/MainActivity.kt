package com.harold.ble_insight

import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import com.harold.ble_insight.data.ble.AndroidBleScanner
import com.harold.ble_insight.data.repository.BleDeviceRepositoryImpl
import com.harold.ble_insight.presentation.scanner.ScannerScreen
import com.harold.ble_insight.presentation.scanner.ScannerViewModel
import com.harold.ble_insight.presentation.scanner.ScannerViewModelFactory
import com.harold.ble_insight.ui.theme.BleInsightTheme

class MainActivity : ComponentActivity() {

    private val permissions by lazy {
        BlePermissionProvider.requiredPermissions()
    }

    private val bluetoothManager by lazy {
        getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
    }

    private val scannerViewModel: ScannerViewModel by viewModels {
        ScannerViewModelFactory(
            BleDeviceRepositoryImpl(
                AndroidBleScanner(applicationContext)
            )
        )
    }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { grants ->
        scannerViewModel.onPermissionResult(grants.values.all { it })
    }

    private val enableBluetoothLauncher = registerForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        if (isBluetoothEnabled()) {
            scannerViewModel.startScan()
        } else {
            scannerViewModel.onBluetoothEnableDeclined()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BleInsightTheme {
                LaunchedEffect(Unit) {
                    permissionLauncher.launch(permissions)
                }
                ScannerScreen(
                    state = scannerViewModel.state,
                    onStartScan = ::startScanWithRequirements,
                    onStopScan = scannerViewModel::stopScan
                )
            }
        }
    }

    override fun onStop() {
        scannerViewModel.stopScan()
        super.onStop()
    }

    private fun startScanWithRequirements() {
        if (!BlePermissionProvider.hasPermissions(this, permissions)) {
            permissionLauncher.launch(permissions)
            return
        }

        if (!hasBluetoothAdapter()) {
            scannerViewModel.onBluetoothUnavailable()
            return
        }

        if (!isBluetoothEnabled()) {
            requestEnableBluetooth()
            return
        }

        scannerViewModel.startScan()
    }

    private fun hasBluetoothAdapter(): Boolean {
        return bluetoothManager.adapter != null
    }

    private fun isBluetoothEnabled(): Boolean {
        return bluetoothManager.adapter?.isEnabled == true
    }

    @SuppressLint("MissingPermission")
    private fun requestEnableBluetooth() {
        enableBluetoothLauncher.launch(Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE))
    }
}