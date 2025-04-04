package com.noke.ble_scanner

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.BluetoothLeScanner
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanResult
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.app.ActivityCompat
import com.noke.ble_scanner.ui.theme.BluetoothTestTheme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

data class ListAdapterDataModel(
    val name: String?,
    val address: String,
    val type: Int,
    val bondState: Int,
    val alias: String?,
    val uuids: Array<android.os.ParcelUuid>?,
    val rssi: Int,
    val advertiseFlags: Int?
)

class MainActivity : ComponentActivity() {
    val TAG: String = MainActivity::class.java.simpleName
    private lateinit var context: Context
    private lateinit var bluetoothManager: BluetoothManager
    private lateinit var bluetoothAdapter: BluetoothAdapter
    private lateinit var bluetoothLeScanner: BluetoothLeScanner

    private val requestBluetoothPermissionCode = 5
    private var scanning = false
    private var selectedMode = true
    private val mainScope = CoroutineScope(Dispatchers.Main)
    private var missingPermission = false
    private var foundMacs: ArrayList<String> = ArrayList()
    private val scanPeriod: Long = 10000
    private var permissions = arrayOf(
        Manifest.permission.BLUETOOTH,
        Manifest.permission.BLUETOOTH_ADMIN,
        Manifest.permission.BLUETOOTH_CONNECT,
        Manifest.permission.BLUETOOTH_SCAN,
        Manifest.permission.BLUETOOTH_ADVERTISE,
        Manifest.permission.ACCESS_FINE_LOCATION
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        context = this
        setContent {
            var isScanning by remember { mutableStateOf(false) } // Hoisted state
            BluetoothTestTheme {
                MainScreen(
                    isScanning = isScanning,
                    onScanStateChanged = { scanning ->
                        isScanning = scanning
                        if (scanning) {
                            scanLeDevice { isScanning = false }
                        }
                    }
                )
            }
        }
        checkBluetoothPermissions()
    }

    @SuppressLint("MissingPermission")
    private fun scanLeDevice(onScanStateChanged: (Boolean) -> Unit) {
        if (!scanning) {
            scanning = true
            mainScope.launch {
                delay(scanPeriod)
                scanning = false
                bluetoothLeScanner.stopScan(leScanCallback)
                onScanStateChanged(false) // Call onScanStateChanged to update the UI
            }

            if (permissions.all {
                    ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED
                }) {
                bluetoothLeScanner.startScan(leScanCallback)
            } else {
                requestPermissions(permissions)
            }
        }
    }

    @SuppressLint("MissingPermission")
    private fun stopScanning() {
        scanning = false
        bluetoothLeScanner.stopScan(leScanCallback)
        Log.d(TAG, "Scanning stopped")
    }

    private val leScanCallback: ScanCallback = object : ScanCallback() {
        @SuppressLint("MissingPermission")
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)
            if (!foundMacs.contains(result.device.address)) {
                foundMacs.add(result.device.address)
                // Update the list of devices in the Compose UI
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                    addDevice(
                        ListAdapterDataModel(
                            result.device.name,
                            result.device.address,
                            result.device.type,
                            result.device.bondState,
                            result.device.alias,
                            result.device.uuids,
                            result.rssi,
                            result.scanRecord?.advertiseFlags
                        )
                    )
                } else {
                    addDevice(
                        ListAdapterDataModel(
                            result.device.name,
                            result.device.address,
                            result.device.type,
                            result.device.bondState,
                            "",
                            result.device.uuids,
                            result.rssi,
                            result.scanRecord?.advertiseFlags
                        )
                    )
                }
            }
        }
    }

    private fun checkBluetoothPermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
            permissions = arrayOf(
                Manifest.permission.BLUETOOTH,
                Manifest.permission.BLUETOOTH_ADMIN,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
        if (permissions.any {
                ActivityCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
            }) {
            missingPermission = true
            requestPermissions(permissions)
        } else {
            initBluetooth()
        }
    }

    private fun requestPermissions(permissions: Array<String>) {
        registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { results ->
            if (results.values.all { it }) {
                initBluetooth()
            } else {
                // Handle permission denial, e.g., show a message to the user
            }
        }.launch(permissions)
    }

    private fun initBluetooth() {
        bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bluetoothLeScanner = bluetoothAdapter.bluetoothLeScanner
    }

    // Compose UI
    private val devices = mutableStateListOf<ListAdapterDataModel>()

    private fun addDevice(device: ListAdapterDataModel) {
        devices.add(device)
    }

    @SuppressLint("MissingPermission")
    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun MainScreen(
        isScanning: Boolean,
        onScanStateChanged: (Boolean) -> Unit
    ) {
        var deviceCount by remember { mutableStateOf(0) }
        var uiSelectedMode by remember { mutableStateOf("BLE") } // Initial selection

        // Define stopScanning inside the composable, with access to onScanStateChanged
        val stopScanning = {
            scanning = false
            bluetoothLeScanner.stopScan(leScanCallback)
            onScanStateChanged(false) // Call onScanStateChanged to update the UI
            Log.d(TAG, "Scanning stopped")
        }

        Scaffold(
            topBar = {
                TopAppBar(title = { Text("BLE Scanner") })
            }
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Button(onClick = {
                        if (!isScanning) {
                            devices.clear()
                        }
                        val newScanningState = !isScanning
                        onScanStateChanged(newScanningState)
                        selectedMode = uiSelectedMode == "BLE" // Update bleMode based on selection
                        if (newScanningState) {
                            scanLeDevice(onScanStateChanged) // Pass the callback
                        } else {
                            stopScanning() // Call the local stopScanning
                        }
                    }) {
                        Text(if (isScanning) "STOP SCANNING" else "START SCANNING")
                    }
                    Text(text = "Listing $deviceCount devices", fontWeight = FontWeight.Bold)
                }
                Spacer(modifier = Modifier.height(16.dp))
                LazyColumn {
                    items(devices) { device ->
                        DeviceItem(device)
                    }
                }
            }
        }
        LaunchedEffect(devices.size) {
            deviceCount = devices.size
        }
    }

    @Composable
    fun DeviceItem(device: ListAdapterDataModel) {
        androidx.compose.material3.Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(text = "Name: ${device.name ?: "N/A"}")
                Text(text = "Address: ${device.address}")
                // Add other device details as needed
            }
        }
    }
}