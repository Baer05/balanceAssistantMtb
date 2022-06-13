package com.example.balanceassistantmtb.ui.main

import android.bluetooth.BluetoothDevice
import android.bluetooth.le.ScanSettings
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.balanceassistantmtb.R
import com.example.balanceassistantmtb.adapters.ScanAdapter
import com.example.balanceassistantmtb.adapters.ScanAdapter.Companion.keyBatteryPERCENTAGE
import com.example.balanceassistantmtb.adapters.ScanAdapter.Companion.keyBatterySTATE
import com.example.balanceassistantmtb.adapters.ScanAdapter.Companion.keyConnectionSTATE
import com.example.balanceassistantmtb.adapters.ScanAdapter.Companion.keyDEVICE
import com.example.balanceassistantmtb.adapters.ScanAdapter.Companion.keyTAG
import com.example.balanceassistantmtb.databinding.FragmentScanBinding
import com.example.balanceassistantmtb.interfaces.BatteryChangedInterface
import com.example.balanceassistantmtb.interfaces.ScanClickInterface
import com.example.balanceassistantmtb.interfaces.SensorClickInterface
import com.example.balanceassistantmtb.viewmodels.BluetoothViewModel
import com.example.balanceassistantmtb.viewmodels.SensorViewModel
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton
import com.xsens.dot.android.sdk.interfaces.XsensDotScannerCallback
import com.xsens.dot.android.sdk.models.XsensDotDevice.*
import com.xsens.dot.android.sdk.utils.XsensDotScanner

class ScanFragment : Fragment(), XsensDotScannerCallback, SensorClickInterface, ScanClickInterface,
    BatteryChangedInterface {

    private val tAG = ScanFragment::class.java.simpleName
    private lateinit var thisContext: Context
    private var mBinding: FragmentScanBinding? = null   // The view binder of ScanFragment
    private var mXsDotScanner: XsensDotScanner? = null   // The XsensDotScanner object
    private val mScannedSensorList: ArrayList<HashMap<String, Any>> = ArrayList()   // A list contains scanned Bluetooth device
    private var mBluetoothViewModel: BluetoothViewModel? = null  // The Bluetooth view model instance
    private var mSensorViewModel: SensorViewModel? = null     // The devices view model instance
    private var mScanAdapter: ScanAdapter? = null    // The adapter for scanned device item
    private var mIsScanning = false     // A variable for scanning flag
    private var mConnectionDialog: AlertDialog? = null   // A dialog during the connection

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (container != null) {
            thisContext = container.context
        }
        // Inflate the layout for this fragment
        mBinding = FragmentScanBinding.inflate(LayoutInflater.from(context))
        mBinding!!.toolbar.title = getString(R.string.title_scan)
        (requireActivity() as AppCompatActivity).setSupportActionBar(mBinding!!.toolbar)

        mScanAdapter = context?.let { ScanAdapter(it, mScannedSensorList) }
        mScanAdapter!!.setSensorClickListener(this)

        mBinding?.fab?.shrink()
        mBinding!!.fab.setOnClickListener { onScanTriggered(!mIsScanning) }


        val recyclerViewLayoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(context)
        mBinding?.sensorRecyclerView?.layoutManager = recyclerViewLayoutManager
        mBinding?.sensorRecyclerView?.itemAnimator = DefaultItemAnimator()
        mBinding?.sensorRecyclerView?.adapter = mScanAdapter

        // Set the SensorClickInterface instance to main activity.
        if (activity != null) {
            (activity as HomeActivity?)?.setScanTriggerListener(this)
        }

        val connectionDialogBuilder = AlertDialog.Builder(
            activity!!
        )
        connectionDialogBuilder.setTitle(getString(R.string.connecting))
        connectionDialogBuilder.setMessage(getString(R.string.hint_connecting))
        mConnectionDialog = connectionDialogBuilder.create()

        return mBinding?.root
    }

    override fun onScanTriggered(started: Boolean) {
        mIsScanning = if (started) {
            // Disconnect to all sensors to make sure the connection has been released.
            mSensorViewModel!!.disconnectAllSensors()
            // This line is for connecting and reconnecting device.
            // Because they don't triggered onXsensDotConnectionChanged() function to remove sensor from list.
            mSensorViewModel!!.removeAllDevice()
            mScannedSensorList.clear()
            mScanAdapter!!.notifyDataSetChanged()
            mXsDotScanner!!.startScan()
        } else {
            // If success for stopping, it will return True from SDK. So use !(not) here.
            !mXsDotScanner!!.stopScan()
        }
        updateFABState()
        mBluetoothViewModel!!.updateScanState(mIsScanning)
    }


    private fun updateFABState() {
        if(mIsScanning)
            mBinding?.fab?.extend();
        else
            mBinding?.fab?.shrink();

    }

    override fun onXsensDotScanned(p0: BluetoothDevice?, p1: Int) {
        if (p0 != null) {
            if (isAdded) {
                // Use the mac address as UID to filter the same scan result.
                var isExist = false
                for (map in mScannedSensorList) {
                    if ((map[keyDEVICE] as BluetoothDevice).address.equals(p0.address)) isExist =
                        true
                }
                if (!isExist) {
                    // The original connection state is Disconnected.
                    // Also set tag, battery state, battery percentage to default value.
                    val map: HashMap<String, Any> = HashMap()
                    map[keyDEVICE] = p0
                    map[keyConnectionSTATE] = CONN_STATE_DISCONNECTED
                    map[keyTAG] = ""
                    map[keyBatterySTATE] = -1
                    map[keyBatteryPERCENTAGE] = -1
                    mScannedSensorList.add(map)
                    mScanAdapter?.notifyItemInserted(mScannedSensorList.size - 1)
                }
            }
        }
    }

    override fun onSensorClick(v: View?, position: Int) {
        // If success for stopping, it will return True from SDK. So use !(not) here.
        mIsScanning = !mXsDotScanner?.stopScan()!!
        updateFABState()
        // Notify main activity to update the scan button.
        mBluetoothViewModel?.updateScanState(false)
        val state: Int? = mScanAdapter?.getConnectionState(position)
        val device: BluetoothDevice? = mScanAdapter?.getDevice(position)
        /**
         * state = 0 : Disconnected
         * state = 1 : Connecting
         * state = 2 : Connected
         * state = 4 : Reconnecting
         */
        when (state) {
            CONN_STATE_DISCONNECTED -> {
                mConnectionDialog?.show()
                // The sensor isn't exist in the mSensorList(SensorViewModel), try to connect and add it.
                mSensorViewModel?.connectSensor(context, device)
            }
            CONN_STATE_CONNECTING -> {
                mScanAdapter?.updateConnectionState(position, CONN_STATE_DISCONNECTED)
                mScanAdapter?.notifyItemChanged(position)
                // This line is necessary to close Bluetooth gatt.
                if (device != null) {
                    mSensorViewModel?.disconnectSensor(device.address)
                }
                // Remove this sensor from device list.
                if (device != null) {
                    mSensorViewModel?.removeDevice(device.address)
                }
            }
            CONN_STATE_CONNECTED -> if (device != null) {
                mSensorViewModel?.disconnectSensor(device.address)
            }
            CONN_STATE_RECONNECTING -> {
                mScanAdapter?.updateConnectionState(position, CONN_STATE_DISCONNECTED)
                mScanAdapter?.notifyItemChanged(position)
                // This line is necessary to close Bluetooth gatt.
                if (device != null) {
                    mSensorViewModel?.cancelReconnection(device.address)
                }
                // Remove this sensor from device list.
                if (device != null) {
                    mSensorViewModel?.removeDevice(device.address)
                }
            }
        }
    }

    /**
     * Initialize and observe view models.
     */
    private fun bindViewModel() {
        if (activity != null) {
            mBluetoothViewModel = BluetoothViewModel.getInstance(activity!!)
            mSensorViewModel = SensorViewModel.getInstance(activity!!)
            mBluetoothViewModel!!.isBluetoothEnabled().observe(this
            ) { t ->
                if (t == true) {
                    initXsDotScanner()
                } else {
                    mIsScanning = false
                    mBluetoothViewModel!!.updateScanState(false)
                }
            }
            mSensorViewModel!!.getConnectionChangedDevice()
                .observe(this
                ) { t ->
                    val address = t.address
                    val state = t.connectionState
                    for (map in mScannedSensorList) {
                        val device = map[keyDEVICE] as BluetoothDevice?
                        Log.d(tAG, "device, $device")
                        if (device != null) {
                            val deviceAddress = device.address
                            // Update connection state by the same mac address.
                            if (deviceAddress.equals(address)) {
                                map[keyConnectionSTATE] = state
                                mScanAdapter!!.notifyDataSetChanged()
                            }
                        }
                    }
                    when (state) {
                        CONN_STATE_CONNECTED -> if (mConnectionDialog!!.isShowing) mConnectionDialog!!.dismiss()
                    }
                }
            mSensorViewModel!!.getTagChangedDevice()
                .observe(this
                ) { t ->
                    val address = t.address
                    val tag = t.tag
                    mScanAdapter!!.updateTag(address, tag)
                    mScanAdapter!!.notifyDataSetChanged()
                    Log.d(tAG, "getTagChangedDevice() - address = $address, tag = $tag")
                }
            mSensorViewModel!!.getFirmwareChangedDevice()
                .observe(this
                ) { t ->
                    val address = t.address
                    val firmware = t.firmwareVersion
                    mScanAdapter!!.updateFirmware(address, firmware)
                    mScanAdapter!!.notifyDataSetChanged()
                    Log.d(
                        tAG,
                        "getFirmwareChangedDevice() - address = $address, firmware = $firmware"
                    )
                }
            mSensorViewModel!!.setBatteryChangedCallback(this)
        }
    }


    /**
     * Setup for Xsens DOT scanner.
     */
    private fun initXsDotScanner() {
        if (mXsDotScanner == null) {
            mXsDotScanner = XsensDotScanner(context, this)
            mXsDotScanner!!.setScanMode(ScanSettings.SCAN_MODE_BALANCED)
        }
    }


    override fun onBatteryChanged(address: String?, status: Int, percentage: Int) {
        Log.d(
            tAG,
            "onBatteryChanged() - address = " + address.toString() + ", state = " + status.toString() + ", percentage = " + percentage
        )
        if (address != null) {
            mScanAdapter?.updateBattery(address, status, percentage)
        }
        if (activity != null) {
            // This event is coming from background thread, use UI thread to update item.
            activity!!.runOnUiThread { mScanAdapter?.notifyDataSetChanged() }
        }

    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop scanning to let other apps to use scan function.
        mXsDotScanner?.stopScan();
        mBluetoothViewModel?.updateScanState(false);
    }

    override fun onDetach() {
        super.onDetach()
        // Release all connections when app is destroyed.
        mSensorViewModel!!.disconnectAllSensors()
    }
}