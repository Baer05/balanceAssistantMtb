package com.example.balanceassistantmtb.ui.main

import android.content.Context
import android.content.DialogInterface
import android.os.Bundle
import android.os.Handler
import android.os.Message
import android.os.PowerManager
import android.os.PowerManager.WakeLock
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import androidx.recyclerview.widget.DefaultItemAnimator
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.balanceassistantmtb.R
import com.example.balanceassistantmtb.adapters.DataAdapter
import com.example.balanceassistantmtb.adapters.DataAdapter.Companion.keyADDRESS
import com.example.balanceassistantmtb.adapters.DataAdapter.Companion.keyDATA
import com.example.balanceassistantmtb.adapters.DataAdapter.Companion.keyTAG
import com.example.balanceassistantmtb.databinding.FragmentHomeBinding
import com.example.balanceassistantmtb.interfaces.DataChangeInterface
import com.example.balanceassistantmtb.interfaces.RecodingClickInterface
import com.example.balanceassistantmtb.service.AsyncstorageService
import com.example.balanceassistantmtb.service.UdpClient
import com.example.balanceassistantmtb.viewmodels.SensorViewModel
import com.xsens.dot.android.sdk.events.XsensDotData
import com.xsens.dot.android.sdk.interfaces.XsensDotSyncCallback
import com.xsens.dot.android.sdk.models.XsensDotDevice.LOG_STATE_ON
import com.xsens.dot.android.sdk.models.XsensDotDevice.PLOT_STATE_ON
import com.xsens.dot.android.sdk.models.XsensDotPayload.PAYLOAD_TYPE_COMPLETE_QUATERNION
import com.xsens.dot.android.sdk.models.XsensDotSyncManager
import java.io.IOException
import java.lang.ref.WeakReference

class HomeFragment : Fragment(), DataChangeInterface, XsensDotSyncCallback, RecodingClickInterface {

    private val tAG = HomeFragment::class.java.simpleName
    private lateinit var thisContext: Context
    private var mBinding: FragmentHomeBinding? = null   // The view binder of ScanFragment
    private val isButtonStart = MutableLiveData<Boolean>(false)
    private var udpSocket: UdpClient? = null
    private val outerClass = WeakReference<HomeFragment>(this)
    private var handler: Handler =  MyHandler(outerClass)
    private val syncingRequestCode = 1001     // The code of request
    private var mSensorViewModel: SensorViewModel? = null    // The devices view model instance
    private var mDataAdapter: DataAdapter? = null   // The adapter for data item
    private val mDataList: ArrayList<HashMap<String, Any>> = ArrayList() // A list contains tag and data from each sensor
    private var mSyncingDialog: AlertDialog? = null     // A dialog during the synchronization
    private var mWakeLock: WakeLock? = null

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (container != null) {
            thisContext = container.context
        }
        // Inflate the layout for this fragment
        mBinding = FragmentHomeBinding.inflate(LayoutInflater.from(context));
        mBinding!!.toolbar.title = getString(R.string.title_home)
        (requireActivity() as AppCompatActivity).setSupportActionBar(mBinding?.toolbar)

        udpSocket = UdpClient(handler)

        mBinding?.trackingBtn?.setOnClickListener {
            isButtonStart.value = !isButtonStart.value!!
            buttonAction()
        }

        isButtonStart.observe(viewLifecycleOwner) {
            if (isButtonStart.value == false) {
                mBinding?.trackingBtn?.text =  getString(R.string.start_tracking)
            } else {
                mBinding?.trackingBtn?.text =  getString(R.string.end_tracking)
            }
        }

        mSensorViewModel?.setStates(PLOT_STATE_ON, LOG_STATE_ON);
        mDataAdapter = DataAdapter(context!!, mDataList)

        val recyclerViewLayoutManager: RecyclerView.LayoutManager =
            LinearLayoutManager(context)
        mBinding?.dataRecyclerView?.layoutManager = recyclerViewLayoutManager
        mBinding?.dataRecyclerView?.itemAnimator = DefaultItemAnimator()
        mBinding?.dataRecyclerView?.adapter = mDataAdapter

        val syncingDialogBuilder = AlertDialog.Builder(
            activity!!
        )
        syncingDialogBuilder.setView(R.layout.dialog_syncing)
        syncingDialogBuilder.setCancelable(false)
        mSyncingDialog = syncingDialogBuilder.create()
        mSyncingDialog?.setOnDismissListener(DialogInterface.OnDismissListener {
            val bar = mSyncingDialog?.findViewById<ProgressBar>(R.id.syncing_progress)
            // Reset progress to 0 for next time to use.
            if (bar != null) bar.progress = 0
        })

        // Set the StreamingClickInterface instance to main activity.
        if (activity != null) (activity as HomeActivity?)?.setRecordingTriggerListener(this)

        val powerManager = context!!.getSystemService(Context.POWER_SERVICE) as PowerManager
        mWakeLock =
            powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "XSensLSLStreamer::Streaming")

        return mBinding?.root
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        bindViewModel()
    }

    private fun buttonAction() {
        if(isButtonStart.value == true) {
            udpSocket?.startUDPSocket()
            onRecordingTriggered()
        } else {
            udpSocket?.stopUDPSocket()
        }
    }

    /**
     * write the tracked workout to prefs
     * */
    private fun writeUserToSharedPreferences (data: Any) {
        AsyncstorageService.writeJSONToRef(thisContext, "balanceAssistantMtb", data)
    }

    /**
     * shows message, which can come back from callBack functions
     * */
    private fun doToast (msg: String) {
        Toast.makeText(this@HomeFragment.context, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Initialize and observe view models.
     */
    private fun bindViewModel() {
        if (activity != null) {
            mSensorViewModel = SensorViewModel.getInstance(activity!!)
            // Implement DataChangeInterface and override onDataChanged() function to receive data.
            mSensorViewModel!!.setDataChangeCallback(this)
        }
    }

    override fun onSyncingStarted(address: String, isSuccess: Boolean, requestCode: Int) {
        Log.i(tAG, "onSyncingStarted() - address = $address, isSuccess = $isSuccess," +
                " requestCode = $requestCode")
    }

    override fun onSyncingProgress(progress: Int, requestCode: Int) {
        Log.i(
            tAG,
            "onSyncingProgress() - progress = $progress, requestCode = $requestCode"
        )
        if (requestCode == syncingRequestCode) {
            if (mSyncingDialog?.isShowing == true) {
                if (activity != null) {
                    activity!!.runOnUiThread { // Find the view of progress bar in dialog layout and update.
                        val bar: ProgressBar? = mSyncingDialog?.findViewById(R.id.syncing_progress)
                        bar?.progress = progress
                    }
                }
            }
        }
    }

    override fun onSyncingResult(address: String, isSuccess: Boolean, requestCode: Int) {
        Log.i(tAG, "onSyncingResult() - address = $address, isSuccess = $isSuccess, " +
                "requestCode = $requestCode")
    }

    override fun onSyncingDone(
        syncingResultMap: HashMap<String?, Boolean?>, isSuccess: Boolean, requestCode: Int) {
        Log.i(tAG, "onSyncingDone() - isSuccess = $isSuccess, requestCode = $requestCode")
        if (requestCode == syncingRequestCode) {
            if (activity != null) {
                activity!!.runOnUiThread {
                    if (mSyncingDialog?.isShowing == true) mSyncingDialog!!.dismiss()
                    mSensorViewModel!!.setRootDevice(false)
                    if (isSuccess) {
                        mBinding?.syncResult?.text =
                            (R.string.sync_result_success.toString())
                        // Syncing precess is success, choose one measurement mode to start measuring.
                        mSensorViewModel!!.setMeasurementMode(PAYLOAD_TYPE_COMPLETE_QUATERNION)
                        //createFiles();
                        mSensorViewModel!!.setMeasurement(true)
                        //all sensors are in syncingResultsMaps -> if success is true -> all syncing is true
                        try {
                            //acquire wakelock
                            if (!mWakeLock?.isHeld!!) {
                                mWakeLock?.acquire()
                                doToast(getString(R.string.wake_lock_acquired))
                            } else {
                                doToast(getString(R.string.wake_lock_already_acquired))
                            }
                        } catch (ex: IOException) {
                            doToast(getString(R.string.lsl_io_exception))
                            if (mWakeLock?.isHeld == true) mWakeLock?.release()
                        }
                    } else {
                        mBinding?.syncResult?.text =
                            (R.string.sync_result_fail).toString()

                        // If the syncing result is fail, show a message to user
                        doToast(getString(R.string.hint_syncing_failed))
                        for ((address, value) in syncingResultMap.entries) {
                            if (!value!!) {
                                // Get the key of this failed device.
                                // It's preferred to stop measurement of all sensors.
                                mSensorViewModel!!.setMeasurement(false)
                            }
                        }
                    }
                }
            }
        }
    }

    override fun onSyncingStopped(address: String, isSuccess: Boolean, requestCode: Int) {
        Log.i(tAG, "onSyncingStopped() - address = $address, " +
                "isSuccess = $isSuccess, requestCode = $requestCode"
        )
    }

    override fun onDataChanged(address: String?, data: XsensDotData?) {
        Log.i(tAG, "onDataChanged() - address = $address")
        val tag = mSensorViewModel?.getTag(address)
        if (address != null && data != null && tag != null) {
            var isExist = false
            for (map in mDataList) {
                val addressData = map[keyADDRESS] as String
                if (addressData == address) {
                    // If the data is exist, try to update it.
                    map[keyDATA] = data
                    isExist = true
                    break
                }
            }
            if (!isExist) {
                // It's the first data of this sensor, create a new set and add it.
                val map: HashMap<String, Any> = HashMap()
                map[keyADDRESS] = address
                map[keyTAG] = tag
                map[keyDATA] = data
                mDataList.add(map)
            }
            //updateFiles(address, data);
            if (activity != null) {
                activity!!.runOnUiThread { // The data is coming from background thread, change to UI thread for updating.
                    mDataAdapter?.notifyDataSetChanged()
                }
            }
        }
    }


    class MyHandler(private val outerClass: WeakReference<HomeFragment>) : Handler() {

        override fun handleMessage(msg: Message) {
            // Your logic code here.
            // ...
            // Make all references to members of the outer class
            // using the WeakReference object.
            outerClass.get()?.doToast(msg.obj.toString())
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        if(udpSocket?.isThreadRunning == true)
            udpSocket?.stopUDPSocket()
    }

    /**
     * Reset page UI to default.
     */
    private fun resetPage() {
        mBinding?.syncResult?.text = ("-")
        mDataList.clear()
        mDataAdapter?.notifyDataSetChanged()
        if (mWakeLock?.isHeld == true) mWakeLock?.release()
    }


    override fun onRecordingTriggered() {
        if (mSensorViewModel?.isRecording()?.value == true) {
            // To stop.
            mSensorViewModel?.setMeasurement(false)
            mSensorViewModel?.updateRecordingStatus(false)
            XsensDotSyncManager.getInstance(this).stopSyncing()
            if (mWakeLock!!.isHeld)// mlslManager.reset()
            mWakeLock!!.release()
            //closeFiles();
        } else {
            // To start.
            resetPage()
            Log.d(tAG,"sensors: ${mSensorViewModel?.getAllSensors()}")
            Log.d(tAG,"sensors: ${mSensorViewModel?.checkConnection()}")
            if (!mSensorViewModel?.checkConnection()!!) {
                doToast(getString(R.string.hint_check_connection))
                return
            }
            // Set first device to root.
            mSensorViewModel?.setRootDevice(true)
            val devices = mSensorViewModel?.getAllSensors()
            Log.d(tAG, "Test, $devices" )
            // Devices will disconnect during the syncing, and do reconnection automatically.
            XsensDotSyncManager.getInstance(this).startSyncing(devices!!, syncingRequestCode)
            Log.d(tAG, "Test, ${mSyncingDialog?.isShowing}" )
            if (!mSyncingDialog?.isShowing!!) mSyncingDialog?.show()
        }
    }

    override fun onDetach() {
        super.onDetach()
        // Stop measurement for each sensor when exiting this page.
        mSensorViewModel!!.setMeasurement(false)
        // It's necessary to update this status, because user may enter this page again.
        mSensorViewModel!!.updateRecordingStatus(false)
        if (mWakeLock!!.isHeld) mWakeLock!!.release()
    }

}

