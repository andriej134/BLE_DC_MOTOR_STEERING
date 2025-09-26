package com.apptmo

import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.apptmp.R

data class DeviceModel(
    val address: String,
    val name: String,
    var status: String = "Nierozpoznany",
    var isConnected: Boolean = false
)

interface DeviceInteractionListener {
    fun onConnectClicked(device: DeviceModel)
    fun onMotorCommand(device: DeviceModel, command: Byte)
}

class DeviceAdapter(
    private val listener: DeviceInteractionListener
) : RecyclerView.Adapter<DeviceAdapter.DeviceViewHolder>() {

    private val devices = mutableListOf<DeviceModel>()
    private val deviceMap = mutableMapOf<String, DeviceModel>()

    @SuppressLint("NotifyDataSetChanged", "MissingPermission")
    fun addDevice(device: BluetoothDevice) {
        if (!deviceMap.containsKey(device.address)) {
            val model = DeviceModel(device.address, device.name ?: "Nieznane urządzenie", "Dostępny")
            devices.add(model)
            deviceMap[device.address] = model
            notifyDataSetChanged()
        }
    }

    fun updateDeviceStatus(address: String, status: String, isConnected: Boolean) {
        deviceMap[address]?.let {
            it.status = status
            it.isConnected = isConnected
            val index = devices.indexOf(it)
            if (index != -1) {
                notifyItemChanged(index)
            }
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clear() {
        devices.clear()
        deviceMap.clear()
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DeviceViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.list_item_motor_device, parent, false)
        return DeviceViewHolder(view, listener)
    }

    override fun onBindViewHolder(holder: DeviceViewHolder, position: Int) {
        holder.bind(devices[position])
    }

    override fun getItemCount(): Int = devices.size

    @SuppressLint("ClickableViewAccessibility")
    class DeviceViewHolder(
        itemView: View,
        private val listener: DeviceInteractionListener
    ) : RecyclerView.ViewHolder(itemView) {

        private val nameTextView: TextView = itemView.findViewById(R.id.deviceName)
        private val statusTextView: TextView = itemView.findViewById(R.id.deviceStatus)
        private val connectButton: Button = itemView.findViewById(R.id.connectButton)
        private val controlPanel: LinearLayout = itemView.findViewById(R.id.controlPanel)
        private val leftButton: Button = itemView.findViewById(R.id.leftButton)
        private val rightButton: Button = itemView.findViewById(R.id.rightButton)

        fun bind(device: DeviceModel) {
            nameTextView.text = device.name
            statusTextView.text = "Status: ${device.status}"

            if (device.isConnected) {
                connectButton.visibility = View.GONE
                controlPanel.visibility = View.VISIBLE
            } else {
                connectButton.visibility = View.VISIBLE
                controlPanel.visibility = View.GONE
            }

            connectButton.setOnClickListener {
                connectButton.isEnabled = false
                listener.onConnectClicked(device)
            }

            val touchListener = View.OnTouchListener { view, motionEvent ->
                val command: Byte = when (view.id) {
                    R.id.rightButton -> 0x01
                    R.id.leftButton -> 0x02
                    else -> 0x00
                }

                when (motionEvent.action) {
                    MotionEvent.ACTION_DOWN -> {
                        listener.onMotorCommand(device, command)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        listener.onMotorCommand(device, 0x00)
                        true
                    }
                    else -> false
                }
            }

            rightButton.setOnTouchListener(touchListener)
            leftButton.setOnTouchListener(touchListener)
        }
    }
}