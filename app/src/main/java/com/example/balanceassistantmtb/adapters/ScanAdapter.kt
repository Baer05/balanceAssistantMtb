package com.example.balanceassistantmtb.adapters

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.core.view.isVisible
import androidx.recyclerview.widget.RecyclerView
import com.example.balanceassistantmtb.R
import com.example.balanceassistantmtb.interfaces.SensorClickInterface
import com.xsens.dot.android.sdk.models.XsensDotDevice.*

class ScanAdapter(var context: Context, var scannedSensorList: ArrayList<HashMap<String, Any>>):
    RecyclerView.Adapter<ScanAdapter.ScanViewHolder>() {

    companion object {
        // The keys of HashMap
        const val keyDEVICE = "device"
        const val keyConnectionSTATE = "state"
        const val keyTAG = "tag"
        const val keyBatterySTATE = "battery_state"
        const val keyBatteryPERCENTAGE = "battery_percentage"
        var keyFIRMWARE = "firmware_version"
    }

    // Send the click event to fragment
    private var mListener: SensorClickInterface? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ScanViewHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_sensor, parent, false)
        return ScanViewHolder(itemView)
    }

    override fun onBindViewHolder(holder: ScanViewHolder, position: Int) {
        val device = scannedSensorList[position][keyDEVICE] as BluetoothDevice?
        if (device != null) {
            val tag = scannedSensorList[position][keyTAG] as String?
            if (tag != null) holder.sensorName.text = tag.ifEmpty { device.name } else holder.sensorName.text = device.name
            val batteryPercentage = scannedSensorList[position][keyBatteryPERCENTAGE] as Int
            val batteryState = scannedSensorList[position][keyBatterySTATE] as Int
            var batteryStr = ""
            if (batteryPercentage != -1) batteryStr = "$batteryPercentage% "
            if (batteryState == BATT_STATE_CHARGING) batteryStr += context.getString(R.string.batt_state_charging)
            holder.sensorBattery.text = batteryStr
            holder.sensorMacAddress.text = device.address
            val firmware = scannedSensorList[position][keyFIRMWARE] as String?
            if (firmware != null) holder.firmwareVersion.text = "FW v$firmware" else holder.firmwareVersion.text = ""
        }
        when (scannedSensorList[position][keyConnectionSTATE] as Int) {
            CONN_STATE_DISCONNECTED -> {
                holder.sensorState.isVisible = false
                holder.sensorState.text = (context.resources.getString(R.string.disconnected))
            }
            CONN_STATE_CONNECTING -> {
                holder.sensorState.isVisible = true
                holder.sensorState.text = (context.resources.getString(R.string.connecting))
            }
            CONN_STATE_CONNECTED -> {
                holder.sensorState.isVisible = true
                holder.sensorState.text =(context.resources.getString(R.string.connected))
            }
            CONN_STATE_RECONNECTING -> {
                holder.sensorState.isVisible = true
                holder.sensorState.text =(context.resources.getString(R.string.reconnecting))
            }
        }
        holder.v.setOnClickListener(View.OnClickListener { v -> // Notify the position of click event to fragment.
            mListener?.onSensorClick(v, position)
        })
    }

    /**
     * returns the number of sensors
     */
    override fun getItemCount(): Int {
        return scannedSensorList.size
    }

    /**
     * Get the Bluetooth device.
     *
     * @param position The position of item view
     * @return The scanned Bluetooth device
     */
    fun getDevice(position: Int): BluetoothDevice? {
        return scannedSensorList[position][keyDEVICE] as BluetoothDevice?
    }

    /**
     * Get the connection state of device.
     *
     * @param position The position of item view
     * @return The connection state
     */
    fun getConnectionState(position: Int): Int {
        return scannedSensorList[position][keyConnectionSTATE] as Int
    }

    /**
     * Update the connection state to list.
     *
     * @param position The position of item view
     * @param state    The connection state
     */
    fun updateConnectionState(position: Int, state: Int) {
        scannedSensorList[position][keyConnectionSTATE] = state
    }

    /**
     * Update tag name to the list.
     *
     * @param address The mac address of device
     * @param tag     The device tag
     */
    fun updateTag(address: String, tag: String) {
        for (map in scannedSensorList) {
            val device = map[keyDEVICE] as BluetoothDevice?
            if (device != null) {
                val deviceAddress = device.address
                if (deviceAddress == address) {
                    map[keyTAG] = tag
                }
            }
        }
    }

    /**
     * Update firmware to the list.
     *
     * @param address The mac address of device
     * @param firmware The device firmware
     */
    fun updateFirmware(address: String, firmware: String) {
        for (map in scannedSensorList) {
            val device = map[keyDEVICE] as BluetoothDevice?
            if (device != null) {
                val deviceAddress = device.address
                if (deviceAddress == address) {
                    map[keyFIRMWARE] = firmware
                }
            }
        }
    }

    /**
     * Update battery information to the list.
     *
     * @param address    The mac address of device
     * @param state      This state can be one of BATT_STATE_NOT_CHARGING or BATT_STATE_CHARGING
     * @param percentage The range of battery level is 0 to 100
     */
    fun updateBattery(address: String, state: Int, percentage: Int) {
        for (map in scannedSensorList) {
            val device = map[keyDEVICE] as BluetoothDevice?
            if (device != null) {
                val deviceAddress = device.address
                if (deviceAddress == address) {
                    map[keyBatterySTATE] = state
                    map[keyBatteryPERCENTAGE] = percentage
                }
            }
        }
    }

    /**
     * Initialize click listener of item view.
     *
     * @param listener The fragment which implemented SensorClickInterface
     */
    fun setSensorClickListener(listener: SensorClickInterface) {
        mListener = listener
    }

    /**
     * A Customized class for ViewHolder of RecyclerView.
     */
    class ScanViewHolder(var v: View) : RecyclerView.ViewHolder(v) {
        val sensorName: TextView = v.findViewById(R.id.sensor_name)
        val sensorMacAddress: TextView = v.findViewById(R.id.sensor_mac_address)
        val sensorBattery: TextView = v.findViewById(R.id.sensor_battery)
        val sensorState: TextView = v.findViewById(R.id.sensor_battery)
        val firmwareVersion: TextView = v.findViewById(R.id.firmware_version)
    }
}