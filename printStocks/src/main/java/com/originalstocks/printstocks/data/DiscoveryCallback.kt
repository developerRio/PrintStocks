package com.originalstocks.printstocks.data

import android.bluetooth.BluetoothDevice

/**
 * Callbacks for connecting Bluetooth devices [can be any peripheral device]
 * */
interface DiscoveryCallback {
    fun onDiscoveryStarted()
    fun onDiscoveryFinished()
    fun onDeviceFound(device: BluetoothDevice)
    fun onDevicePaired(device: BluetoothDevice)
    fun onDeviceUnpaired(device: BluetoothDevice)
    fun onError(message: String)
}