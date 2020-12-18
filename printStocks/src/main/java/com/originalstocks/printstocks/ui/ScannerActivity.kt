package com.originalstocks.printstocks.ui

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothDevice
import android.content.Context
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.originalstocks.printstocks.PrintStocks
import com.originalstocks.printstocks.R
import com.originalstocks.printstocks.data.DiscoveryCallback
import com.originalstocks.printstocks.databinding.ActivityScannerBinding
import com.originalstocks.printstocks.utilities.Bluetooth
import com.originalstocks.printstocks.utilities.showToast

/**
 * Author : [Himanshu Raj]
 * Base class Responsible for Scanning Bluetooth peripheral devices.
 */

class ScannerActivity : AppCompatActivity() {
    private val TAG = "ScannerActivity"

    companion object {
        const val SCANNING_FOR_PRINTER = 115
        const val LOCATION_PERMISSIONS_REQUEST_CODE = 200
    }

    private lateinit var binding: ActivityScannerBinding
    private lateinit var bluetooth: Bluetooth
    private var devices = ArrayList<BluetoothDevice>()
    private lateinit var adapter: BluetoothDevicesAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityScannerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        adapter = BluetoothDevicesAdapter(this)
        bluetooth = Bluetooth(this)
        setup()

    }

    private fun setup() {
        initViews()
        initListeners()
        initDeviceCallback()
    }

    private fun initDeviceCallback() {
        bluetooth.setDiscoveryCallback(object : DiscoveryCallback {
            override fun onDiscoveryStarted() {
                binding.refreshLayout.isRefreshing = true
                Toast.makeText(
                    this@ScannerActivity,
                    "Scanning for Printers nearby...",
                    Toast.LENGTH_LONG
                ).show()
                devices.clear()
                devices.addAll(bluetooth.pairedDevices)
                adapter.notifyDataSetChanged()
            }

            override fun onDiscoveryFinished() {
                val title = if (devices.isNotEmpty()) "Select a Printer" else "No devices"
                showToast(this@ScannerActivity, title)
                binding.refreshLayout.isRefreshing = false
            }

            override fun onDeviceFound(device: BluetoothDevice) {
                if (!devices.contains(device)) {
                    devices.add(device)
                    adapter.notifyDataSetChanged()
                }
            }

            override fun onDevicePaired(device: BluetoothDevice) {
                PrintStocks.setPrinter(device.name, device.address)
                showToast(this@ScannerActivity, "Device Paired")
                adapter.notifyDataSetChanged()
                setResult(Activity.RESULT_OK)
                this@ScannerActivity.finish()
            }

            override fun onDeviceUnpaired(device: BluetoothDevice) {
                showToast(this@ScannerActivity, "Device unpaired")
                val pairedPrinter = PrintStocks.getPairedPrinter()
                if (pairedPrinter != null && pairedPrinter.address == device.address)
                    PrintStocks.removeCurrentPrinter()
                devices.remove(device)
                adapter.notifyDataSetChanged()
                bluetooth.startScanning()
            }

            override fun onError(message: String) {
                showToast(this@ScannerActivity, "Error while pairing")
                adapter.notifyDataSetChanged()
            }
        })
    }

    private fun initListeners() {
        binding.refreshLayout.setOnRefreshListener { bluetooth.startScanning() }
        binding.printers.setOnItemClickListener { _, _, i, _ ->
            val device = devices[i]
            if (device.bondState == BluetoothDevice.BOND_BONDED) {
                PrintStocks.setPrinter(device.name, device.address)
                setResult(Activity.RESULT_OK)
                this@ScannerActivity.finish()
            } else if (device.bondState == BluetoothDevice.BOND_NONE)
                bluetooth.pair(devices[i])
            adapter.notifyDataSetChanged()
        }
    }

    private fun initViews() {
        binding.printers.adapter = adapter
    }

    override fun onStart() {
        super.onStart()
        checkPermission()
    }

    override fun onStop() {
        super.onStop()
        bluetooth.onStop()
    }

    private fun checkPermission() {
        when {
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED -> {
                Log.e(TAG, "checkPermission_permission already granted")
                // Fetch the data
                bluetooth.onStart()
                if (!bluetooth.isEnabled)
                    bluetooth.enable()
                Handler().postDelayed({
                    bluetooth.startScanning()
                }, 1000)

            }
            shouldShowRequestPermissionRationale(
                Manifest.permission.ACCESS_FINE_LOCATION
            ) -> {
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSIONS_REQUEST_CODE
                )
                // We've been denied once before. Explain why we need the permission, then ask again.
                Log.i(TAG, "checkPermission_permission once denied, asking permission again")
            }
            else -> {
                // We've never asked. Just do it.
                Log.i(TAG, "checkPermission_permission asking permission")
                requestPermissions(
                    arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                    LOCATION_PERMISSIONS_REQUEST_CODE
                )
            }
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            LOCATION_PERMISSIONS_REQUEST_CODE -> {

                if ((grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED)) {
                    // permission granted for bluetooth
                    Log.i(TAG, "onRequestPermissionsResult: Permission granted")
                    bluetooth.onStart()
                    if (!bluetooth.isEnabled) {
                        bluetooth.enable()
                    }
                    Handler().postDelayed(Runnable { bluetooth.startScanning() }, 1000)
                } else {
                    Log.e(TAG, "onRequestPermissionsResult: Permission denied")
                }
            }
        }
    }

    /** Bluetooth device list adapter */
    inner class BluetoothDevicesAdapter(context: Context) :
        ArrayAdapter<BluetoothDevice>(context, android.R.layout.simple_list_item_1) {
        override fun getCount(): Int {
            return devices.size
        }

        override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
            return LayoutInflater.from(context)
                .inflate(R.layout.bluetooth_device_row, parent, false).apply {
                    findViewById<TextView>(R.id.name).text =
                        if (devices[position].name.isNullOrEmpty()) devices[position].address else devices[position].name
                    findViewById<TextView>(R.id.pairStatus).visibility =
                        if (devices[position].bondState != BluetoothDevice.BOND_NONE) View.VISIBLE else View.INVISIBLE
                    findViewById<TextView>(R.id.pairStatus).text =
                        when (devices[position].bondState) {
                            BluetoothDevice.BOND_BONDED -> "Paired"
                            BluetoothDevice.BOND_BONDING -> "Pairing.."
                            else -> ""
                        }
                    findViewById<ImageView>(R.id.pairedPrinter).visibility =
                        if (PrintStocks.getPairedPrinter()?.address == devices[position].address) View.VISIBLE else View.GONE
                }
        }
    }

}