package com.example.balanceassistantmtb.viewmodels

import android.bluetooth.BluetoothDevice
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner
import com.example.balanceassistantmtb.interfaces.BatteryChangedInterface
import com.example.balanceassistantmtb.interfaces.DataChangeInterface
import com.xsens.dot.android.sdk.events.XsensDotData
import com.xsens.dot.android.sdk.interfaces.XsensDotDeviceCallback
import com.xsens.dot.android.sdk.models.FilterProfileInfo
import com.xsens.dot.android.sdk.models.XsensDotDevice
import com.xsens.dot.android.sdk.models.XsensDotDevice.*

class SensorViewModel: ViewModel(), XsensDotDeviceCallback {
    private val tAG = SensorViewModel::class.java.simpleName
    private val lOCKER = Any()   // A variable to queue multiple threads.
    private var mBatteryChangeInterface: BatteryChangedInterface? = null     // A callback function to notify battery information
    private var mDataChangeInterface: DataChangeInterface? = null   // A callback function to notify data changes event
    private val mSensorList = MutableLiveData<ArrayList<XsensDotDevice>>()       // A list contains XsensDotDevice
    private val mConnectionChangedSensor = MutableLiveData<XsensDotDevice>()    // A variable to notify the connection state
    private val mTagChangedSensor = MutableLiveData<XsensDotDevice>()    // A variable to notify the tag name
    private val mFirmwareChangedSensor = MutableLiveData<XsensDotDevice>()   // A variable to notify the firmware version
    private val mIsRecording = MutableLiveData<Boolean>()    // A variable to notify the streaming status

    companion object {
        /**
         * Get the instance of SensorViewModel
         * @param owner The life cycle owner from activity/fragment
         * @return The SensorViewModel
         */
        fun getInstance(owner: ViewModelStoreOwner): SensorViewModel {
            return ViewModelProvider(
                owner,
                ViewModelProvider.NewInstanceFactory()
            )[SensorViewModel::class.java]
        }
    }

    /**
     * Initialize data changes interface.
     * @param callback The class which implemented DataChangeInterface
     */
    fun setDataChangeCallback(callback: DataChangeInterface) {
        mDataChangeInterface = callback
    }

    /**
     * Initialize battery changes interface.
     * @param callback The class which implemented setBatteryChangedCallback
     */
    fun setBatteryChangedCallback(callback: BatteryChangedInterface) {
        mBatteryChangeInterface = callback
    }

    /**
     * Get the XsensDotDevice object from list by mac address.
     * @param address The mac address of device
     * @return The XsensDotDevice object
     */
    private fun getSensor(address: String): XsensDotDevice? {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                if (device.address == address) return device
            }
        }
        return null
    }

    /**
     * Get all XsensDotDevice objects from list.
     * @return The list contains all devices
     */
    fun getAllSensors(): ArrayList<XsensDotDevice>? {
        return if (mSensorList.value == null) ArrayList() else mSensorList.value
    }

    /**
     * Initialize, connect the XsensDotDevice and put it into a list.
     * @param context The application context
     * @param device  The scanned Bluetooth device
     */
    fun connectSensor(context: Context?, device: BluetoothDevice?) {
        val xsDevice = XsensDotDevice(context, device, this)
        addDevice(xsDevice)
        xsDevice.connect()
    }

    /**
     * Disconnect one device by mac address.
     * @param address The mac address of device
     */
    fun disconnectSensor(address: String) {
        if (mSensorList.value != null) {
            for (device in mSensorList.value!!) {
                if (device.address == address) {
                    device.disconnect()
                    break
                }
            }
        }
    }

    /**
     * Disconnect all devices which are exist in the list.
     */
    fun disconnectAllSensors() {
        if (mSensorList.value != null) {
            synchronized(lOCKER) {
                val it: Iterator<XsensDotDevice> = mSensorList.value!!
                    .iterator()
                while (it.hasNext()) {
                    // Use Iterator to make sure it's thread safety.
                    val device = it.next()
                    device.disconnect()
                }
            }
        }
    }

    /**
     * Cancel reconnection of one sensor.
     * @param address The mac address of device
     */
    fun cancelReconnection(address: String) {
        if (mSensorList.value != null) {
            for (device in mSensorList.value!!) {
                if (device.address == address) {
                    device.cancelReconnecting()
                    break
                }
            }
        }
    }

    /**
     * Check the connection state of all sensors.
     * @return True - If all sensors are connected
     */
    fun checkConnection(): Boolean {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                val state = device.connectionState
                if (state != CONN_STATE_CONNECTED) return false
            }
        } else {
            return false
        }
        return true
    }

    /**
     * Get the tag name from sensor.
     * @param address The mac address of device
     * @return The tag name
     */
    fun getTag(address: String?): String? {
        val device = getSensor(address!!)
        if (device != null) {
            val tag = device.tag
            return tag ?: device.name
        }
        return ""
    }

    /**
     * Set the plotting and logging states for each device.
     * @param plot The plot state
     * @param log  The log state
     */
    fun setStates(plot: Int, log: Int) {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                device.plotState = plot
                device.logState = log
            }
        }
    }

    /**
     * Set the measurement mode to all sensors.
     * @param mode The measurement mode
     */
    fun setMeasurementMode(mode: Int) {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                device.measurementMode = mode
            }
        }
    }

    /**
     * Set one sensor for root of synchronization.
     * @param isRoot True - If set to root
     */
    fun setRootDevice(isRoot: Boolean) {
        val devices = mSensorList.value
        if (devices != null && devices.size > 0) devices[0].isRootDevice = isRoot
    }

    /**
     * Start/Stop measuring for each sensor.
     * @param enabled True - Start outputting data
     */
    fun setMeasurement(enabled: Boolean) {
        val devices = mSensorList.value
        if (devices != null) {
            for (device in devices) {
                if (enabled) device.startMeasuring() else device.stopMeasuring()
            }
        }
    }

    /**
     * Add the XsensDotDevice to a list, the UID is mac address.
     * @param xsDevice The XsensDotDevice object
     */
    private fun addDevice(xsDevice: XsensDotDevice) {
        if (mSensorList.value == null) mSensorList.value = ArrayList()
        val devices = mSensorList.value
        Log.d(tAG, "devices: $devices")
        var isExist = false
        for (_xsDevice in devices!!) {
            if (xsDevice.address == _xsDevice.address) {
                isExist = true
                break
            }
        }
        Log.d(tAG, "device: $xsDevice")
        if (!isExist) devices.add(xsDevice)
    }

    /**
     * If device is disconnected by user means don't need to reconnect. So remove this device from list by mac address.
     * @param address The mac address of device
     */
    fun removeDevice(address: String) {
        if (mSensorList.value == null) {
            mSensorList.value = ArrayList()
            return
        }
        synchronized(lOCKER) {
            val it = mSensorList.value!!
                .iterator()
            while (it.hasNext()) {
                // Use Iterator to make sure it's thread safety.
                val device = it.next()
                if (device.address == address) {
                    it.remove()
                    break
                }
            }
        }
    }

    /**
     * Remove all sensor from device list directly.
     */
    fun removeAllDevice() {
        if (mSensorList.value != null) {
            synchronized(lOCKER) { mSensorList.value!!.clear() }
        }
    }

    /**
     * Observe this function to listen which device's connection state is changed.
     * @return The latest updated device
     */
    fun getConnectionChangedDevice(): MutableLiveData<XsensDotDevice> {
        return mConnectionChangedSensor
    }

    /**
     * Observe this function to listen which device's tag name is changed.
     * @return The latest updated device
     */
    fun getTagChangedDevice(): MutableLiveData<XsensDotDevice> {
        return mTagChangedSensor
    }

    /**
     * Observe this function to listen which device's firmware is changed.
     * @return The latest updated device
     */
    fun getFirmwareChangedDevice(): MutableLiveData<XsensDotDevice> {
        return mFirmwareChangedSensor
    }

    override fun onXsensDotConnectionChanged(address: String, state: Int) {
        Log.i(tAG, "onXsensDotConnectionChanged() - address = $address, state = $state")
        val xsDevice = getSensor(address)
        if (xsDevice != null) mConnectionChangedSensor.postValue(xsDevice)
        when (state) {
            CONN_STATE_DISCONNECTED -> synchronized(this) { removeDevice(address) }
            CONN_STATE_CONNECTING -> {}
            CONN_STATE_CONNECTED -> {}
            CONN_STATE_RECONNECTING -> {}
        }
    }

    override fun onXsensDotServicesDiscovered(address: String, status: Int) {
        Log.i(tAG, "onXsensDotServicesDiscovered() - address = $address, status = $status")
    }

    override fun onXsensDotFirmwareVersionRead(address: String, version: String) {
        // This callback function will be triggered in the connection precess.
        Log.i(tAG, "onXsensDotFirmwareVersionRead() - address = $address, version = $version")
        val device = getSensor(address)
        if (device != null) mFirmwareChangedSensor.postValue(device)
    }

    override fun onXsensDotTagChanged(address: String, tag: String) {
        // This callback function will be triggered in the connection precess.
        Log.i(tAG, "onXsensDotTagChanged() - address = $address, tag = $tag")
        // The default value of tag is an empty string.
        if (tag != "") {
            val device = getSensor(address)
            if (device != null) mTagChangedSensor.postValue(device)
        }
    }

    override fun onXsensDotBatteryChanged(address: String, status: Int, percentage: Int) {
        // This callback function will be triggered in the connection precess.
        Log.i(
            tAG,
            "onXsensDotBatteryChanged() - address = $address, status = $status, percentage = $percentage"
        )
        // The default value of status and percentage is -1.
        if (status != -1 && percentage != -1) {
            // Use callback function instead of LiveData to notify the battery information.
            // Because when user removes the USB cable from housing, this function will be triggered 5 times.
            // Use LiveData will lose some notification.
            if (mBatteryChangeInterface != null) mBatteryChangeInterface!!.onBatteryChanged(
                address,
                status,
                percentage
            )
        }
    }

    override fun onXsensDotDataChanged(address: String, data: XsensDotData?) {
        Log.i(tAG, "onXsensDotDataChanged() - address = $address")
        // Don't use LiveData variable to transfer data to activity/fragment.
        // The main (UI) thread isn't fast enough to store data by 60Hz.
        if (mDataChangeInterface != null) mDataChangeInterface!!.onDataChanged(address, data)
    }

    override fun onXsensDotInitDone(address: String) {
        Log.i(tAG, "onXsensDotInitDone() - address = $address")
    }

    override fun onXsensDotButtonClicked(address: String, timestamp: Long) {
        Log.i(
            tAG,
            "onXsensDotButtonClicked() - address = $address, timestamp = $timestamp"
        )
    }

    override fun onXsensDotPowerSavingTriggered(address: String) {
        Log.i(tAG, "onXsensDotPowerSavingTriggered() - address = $address")
    }

    override fun onReadRemoteRssi(address: String, rssi: Int) {
        Log.i(tAG, "onReadRemoteRssi() - address = $address, rssi = $rssi")
    }

    override fun onXsensDotOutputRateUpdate(address: String, outputRate: Int) {
        Log.i(
            tAG,
            "onXsensDotOutputRateUpdate() - address = $address, outputRate = $outputRate"
        )
    }

    override fun onXsensDotFilterProfileUpdate(address: String, filterProfileIndex: Int) {
        Log.i(
            tAG,
            "onXsensDotFilterProfileUpdate() - address = $address, filterProfileIndex = $filterProfileIndex"
        )
    }

    override fun onXsensDotGetFilterProfileInfo(
        address: String,
        filterProfileInfoList: ArrayList<FilterProfileInfo?>
    ) {
        Log.i(
            tAG,
            "onXsensDotGetFilterProfileInfo() - address = " + address + ", size = " + filterProfileInfoList.size
        )
    }

    override fun onSyncStatusUpdate(address: String, isSynced: Boolean) {
        Log.i(tAG, "onSyncStatusUpdate() - address = $address, isSynced = $isSynced")
    }

    /**
     * Observe this function to listen the recording status.
     * @return The latest streaming status
     */
    fun isRecording(): MutableLiveData<Boolean> {
        if (mIsRecording.value == null) mIsRecording.value = false
        return mIsRecording
    }

    /**
     * Notify the recording status to activity/fragment
     * @param status The status of recording
     */
    fun updateRecordingStatus(status: Boolean) {
        mIsRecording.postValue(status)
    }
}