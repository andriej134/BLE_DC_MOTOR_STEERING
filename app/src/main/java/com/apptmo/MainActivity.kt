package com.apptmo

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.apptmp.R

@SuppressLint("MissingPermission")
class MainActivity : AppCompatActivity(), BleListener, DeviceInteractionListener {

    private lateinit var bleManager: BLEManager
    private lateinit var deviceAdapter: DeviceAdapter
    private lateinit var scanButton: Button
    private lateinit var devicesRecyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private val PERMISSION_REQUEST_CODE = 101

    private val bluetoothAdapter: BluetoothAdapter by lazy {
        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothManager.adapter
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        bleManager = BLEManager(this, this)

        scanButton = findViewById(R.id.scanButton)
        devicesRecyclerView = findViewById(R.id.devicesRecyclerView)
        emptyView = findViewById(R.id.emptyView)

        deviceAdapter = DeviceAdapter(this)
        devicesRecyclerView.adapter = deviceAdapter
        devicesRecyclerView.layoutManager = LinearLayoutManager(this)

        scanButton.setOnClickListener {
            if (checkPermissions()) {
                bleManager.startScan()
            } else {
                requestPermissions()
            }
        }
    }

    override fun onScanStarted() {
        runOnUiThread {
            scanButton.isEnabled = false
            scanButton.text = "Skanowanie..."
            emptyView.visibility = View.GONE
            deviceAdapter.clear()
        }
    }

    override fun onScanResult(device: BluetoothDevice) {
        runOnUiThread {
            deviceAdapter.addDevice(device)
        }
    }

    override fun onScanFinished() {
        runOnUiThread {
            scanButton.isEnabled = true
            scanButton.text = "Skanuj ponownie"
            if (deviceAdapter.itemCount == 0) {
                emptyView.visibility = View.VISIBLE
            }
        }
    }

    override fun onDeviceStateChanged(address: String, status: String, isReady: Boolean) {
        runOnUiThread {
            deviceAdapter.updateDeviceStatus(address, status, isReady)
        }
    }

    override fun onConnectClicked(device: DeviceModel) {
        bleManager.connectToDevice(bluetoothAdapter.getRemoteDevice(device.address))
    }

    override fun onMotorCommand(device: DeviceModel, command: Byte) {
        bleManager.sendMotorCommand(device.address, command)
    }

    private fun checkPermissions(): Boolean {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        }
        return requiredPermissions.all { ActivityCompat.checkSelfPermission(this, it) == PackageManager.PERMISSION_GRANTED }
    }

    private fun requestPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            arrayOf(Manifest.permission.BLUETOOTH_SCAN, Manifest.permission.BLUETOOTH_CONNECT)
        } else {
            arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.BLUETOOTH, Manifest.permission.BLUETOOTH_ADMIN)
        }
        ActivityCompat.requestPermissions(this, requiredPermissions, PERMISSION_REQUEST_CODE)
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED }) {
                bleManager.startScan()
            } else {
                Toast.makeText(this, "Uprawnienia są wymagane do skanowania urządzeń", Toast.LENGTH_LONG).show()
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        bleManager.disconnectAll()
    }
}