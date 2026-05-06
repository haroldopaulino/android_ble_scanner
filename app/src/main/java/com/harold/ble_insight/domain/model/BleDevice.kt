package com.harold.ble_insight.domain.model

data class BleDevice(
    val name: String,
    val address: String,
    val rssi: Int,
    val type: String,
    val bondState: String,
    val advertiseFlags: Int?
)
