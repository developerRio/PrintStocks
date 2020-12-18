package com.originalstocks.printstocks.data

/**
 * Bluetooth Callback which shows device's Bluetooth availability
 */

interface BluetoothCallback {
    fun onBluetoothTurningOn()
    fun onBluetoothOn()
    fun onBluetoothTurningOff()
    fun onBluetoothOff()
    fun onUserDeniedActivation()
}
