package com.apptmo

import android.annotation.SuppressLint
import android.bluetooth.*
import android.bluetooth.le.ScanCallback
import android.bluetooth.le.ScanFilter
import android.bluetooth.le.ScanResult
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Handler
import android.os.ParcelUuid
import java.util.*
import java.util.concurrent.ConcurrentHashMap

interface BleListener {
    fun onScanStarted()
    fun onScanResult(device: BluetoothDevice)
    fun onScanFinished()
    fun onDeviceStateChanged(address: String, status: String, isReady: Boolean)
}

@SuppressLint("MissingPermission")
class BLEManager(private val context: Context, private val listener: BleListener) {

    companion object {
        val SERVICE_UUID: UUID = UUID.fromString("0A66A21A-422A-4A97-9AF3-575E67A55C7E")
        val MOTOR_CHARACTERISTIC_UUID: UUID = UUID.fromString("CC67E36C-323A-4E36-A33E-039B3E452285")
    }

    private val bluetoothAdapter: BluetoothAdapter? = (context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager).adapter
    private val gattMap = ConcurrentHashMap<String, BluetoothGatt>()
    private val characteristicMap = ConcurrentHashMap<String, BluetoothGattCharacteristic>()
    private var isScanning = false

    private val gattCallback = object : BluetoothGattCallback() {
        override fun onConnectionStateChange(gatt: BluetoothGatt, status: Int, newState: Int) {
            val address = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                if (newState == BluetoothProfile.STATE_CONNECTED) {
                    listener.onDeviceStateChanged(address, "Połączono, odkrywam usługi...", false)
                    gatt.discoverServices()
                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {
                    closeConnection(address)
                }
            } else {
                closeConnection(address)
            }
        }

        override fun onServicesDiscovered(gatt: BluetoothGatt, status: Int) {
            val address = gatt.device.address
            if (status == BluetoothGatt.GATT_SUCCESS) {
                val service = gatt.getService(SERVICE_UUID)
                service?.getCharacteristic(MOTOR_CHARACTERISTIC_UUID)?.let { char ->
                    characteristicMap[address] = char
                    listener.onDeviceStateChanged(address, "Połączono", true)
                } ?: run {
                    listener.onDeviceStateChanged(address, "Błąd: Brak charakterystyki", false)
                }
            }
        }
    }

    fun closeConnection(address: String) {
        gattMap[address]?.close()
        gattMap.remove(address)
        characteristicMap.remove(address)
        listener.onDeviceStateChanged(address, "Rozłączono", false)
    }

    private val leScanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            listener.onScanResult(result.device)
        }
    }

    fun startScan() {
        if (isScanning) return
        isScanning = true
        listener.onScanStarted()
        val scanFilter = ScanFilter.Builder().setServiceUuid(ParcelUuid(SERVICE_UUID)).build()
        val settings = ScanSettings.Builder().setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY).build()
        bluetoothAdapter?.bluetoothLeScanner?.startScan(listOf(scanFilter), settings, leScanCallback)
        Handler(context.mainLooper).postDelayed({ stopScan() }, 5000)
    }

    private fun stopScan() {
        if (!isScanning) return
        isScanning = false
        bluetoothAdapter?.bluetoothLeScanner?.stopScan(leScanCallback)
        listener.onScanFinished()
    }

    fun connectToDevice(device: BluetoothDevice) {
        if (gattMap.containsKey(device.address)) return
        listener.onDeviceStateChanged(device.address, "Łączenie...", false)
        val gatt = device.connectGatt(context, false, gattCallback)
        gattMap[device.address] = gatt
    }

    fun sendMotorCommand(address: String, command: Byte) {
        val characteristic = characteristicMap[address]
        if (characteristic == null) return
        characteristic.value = byteArrayOf(command)
        characteristic.writeType = BluetoothGattCharacteristic.WRITE_TYPE_DEFAULT
        gattMap[address]?.writeCharacteristic(characteristic)
    }

    fun disconnectAll() {
        gattMap.keys.forEach { closeConnection(it) }
    }
}