package com.example.balanceassistantmtb.ui.main

import android.Manifest
import android.app.AlertDialog
import android.bluetooth.BluetoothAdapter
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.WindowManager
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.FragmentTransaction
import com.example.balanceassistantmtb.R
import com.example.balanceassistantmtb.databinding.ActivityHomeBinding
import com.example.balanceassistantmtb.interfaces.RecodingClickInterface
import com.example.balanceassistantmtb.interfaces.ScanClickInterface
import com.example.balanceassistantmtb.utlils.Utils
import com.example.balanceassistantmtb.viewmodels.BluetoothViewModel
import com.example.balanceassistantmtb.viewmodels.SensorViewModel
import com.google.android.material.bottomnavigation.BottomNavigationView

class HomeActivity : AppCompatActivity() {
    private val tAG = HomeActivity::class.java.simpleName

    private lateinit var homeFragment: HomeFragment
    private lateinit var scanFragment: ScanFragment
    private lateinit var dashboardFragment: DashboardFragment
    private var fragmentPos = 0 //position of the fragment
    private var mBinding: ActivityHomeBinding? = null   // The view binder of MainActivity
    private val requestEnableBLUETOOTH = 1001   // The code of request
    private val requestPermissionLOCATION = 1002    // The code of request
    private var mBluetoothViewModel: BluetoothViewModel? = null  // The Bluetooth view model instance
    private var mSensorViewModel: SensorViewModel? = null   // The sensor view model instance
    private var mIsScanning = false   // A variable for scanning flag
    private var mScanListener: ScanClickInterface? = null   // Send the start/stop scan click event to fragment
    private var mRecordingListener: RecodingClickInterface? = null  // Send the start/stop streaming click event to fragment

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        mBinding = ActivityHomeBinding.inflate(LayoutInflater.from(this));
        setContentView(mBinding!!.root)
        // Get the fragment to return to from extras 1:group, 2:feed, 0:profile
        val fragment = intent.extras?.get("fragment")
        bindViewModel()
        if(!checkBluetoothAndPermission()) {
            doToast("Failed to acquire permissions for scanning. App functionality not guaranteed!")
        }
        registerReceiver(mBluetoothStateReceiver, IntentFilter(BluetoothAdapter.ACTION_STATE_CHANGED))
        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)

        // display and set checked fragment in BottomNav
        when (fragment) {
            "1" -> {
                scanFragment = ScanFragment()
                fragmentPos = 1
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, scanFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
                mBinding?.btmNav?.menu?.findItem(R.id.scan)?.isChecked =true
            }
            "2" -> {
                dashboardFragment = DashboardFragment()
                fragmentPos = 2
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, dashboardFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
                mBinding?.btmNav?.menu?.findItem(R.id.dashboard)?.isChecked =true
            }
            else -> {
                homeFragment = HomeFragment()
                fragmentPos = 0
                supportFragmentManager
                    .beginTransaction()
                    .replace(R.id.frame_layout, homeFragment)
                    .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                    .commit()
                mBinding?.btmNav?.menu?.findItem(R.id.home)?.isChecked = true
            }
        }

        mBinding?.btmNav?.setOnNavigationItemSelectedListener { item ->
            when (item.itemId) {
                R.id.home -> {
                    homeFragment =
                        HomeFragment()
                    fragmentPos = 0
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, homeFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                R.id.scan -> {
                    scanFragment =
                        ScanFragment()
                    fragmentPos = 1
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, scanFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
                R.id.dashboard -> {
                    dashboardFragment =
                        DashboardFragment()
                    fragmentPos = 2
                    supportFragmentManager
                        .beginTransaction()
                        .replace(R.id.frame_layout, dashboardFragment)
                        .setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
                        .commit()
                }
            }
            true
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == requestPermissionLOCATION) {
            for (i in grantResults.indices) {
                if (permissions[i] == Manifest.permission.ACCESS_FINE_LOCATION) {
                    if (grantResults[i] == PackageManager.PERMISSION_GRANTED) checkBluetoothAndPermission()
                    else doToast(getString(R.string.hint_allow_location))
                }
            }
        }
    }

    /**
     * shows message, which can come back from callBack functions
     * */
    private fun doToast (msg: String) {
        Toast.makeText(this@HomeActivity, msg, Toast.LENGTH_SHORT).show()
    }

    /**
     * Check the state of Bluetooth adapter and location permission.
     */
    private fun checkBluetoothAndPermission(): Boolean {
        val isBluetoothEnabled: Boolean = Utils.isBluetoothAdapterEnabled(this)
        val isPermissionGranted: Boolean = Utils.isLocationPermissionGranted(this)
        if (isBluetoothEnabled) {
            if (!isPermissionGranted) Utils.requestLocationPermission(
                this,
                requestPermissionLOCATION
            )
        } else {
            Utils.requestEnableBluetooth(this, requestEnableBLUETOOTH)
        }
        val status = isBluetoothEnabled && isPermissionGranted
        mBluetoothViewModel?.updateBluetoothEnableState(status)
        return status
    }

    /**
     * Initialize and observe view models.
     */
    private fun bindViewModel() {
        mBluetoothViewModel = BluetoothViewModel.getInstance(this)
        mBluetoothViewModel!!.isScanning().observe(this
        ) { t -> // If the status of scanning is changed, try to refresh the menu.
            mIsScanning = t
            invalidateOptionsMenu()
        }
        mSensorViewModel = SensorViewModel.getInstance(this)
        mSensorViewModel!!.isRecording().observe(this) {
            invalidateOptionsMenu()
        }
    }

    /**
     * Set the trigger of scan button.
     * @param listener The class which implemented ScanClickInterface
     */
    fun setScanTriggerListener(listener: ScanClickInterface) {
        mScanListener = listener
    }

    /**
     * Set the trigger of streaming button.
     * @param listener The class which implemented StreamingClickInterface
     */
    fun setRecordingTriggerListener(listener: RecodingClickInterface) {
        mRecordingListener = listener
    }


    /**
     * A receiver for Bluetooth status.
     */
    private val mBluetoothStateReceiver: BroadcastReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            val action = intent.action
            if (action != null) {
                if (action == BluetoothAdapter.ACTION_STATE_CHANGED) {
                    when (intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)) {
                        BluetoothAdapter.STATE_OFF -> mBluetoothViewModel!!.updateBluetoothEnableState(
                            false
                        )
                        BluetoothAdapter.STATE_ON -> mBluetoothViewModel!!.updateBluetoothEnableState(
                            true
                        )
                    }
                }
            }
        }
    }

    override fun onPostResume() {
        super.onPostResume()
        bindViewModel()
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(mBluetoothStateReceiver)
    }

    /**
     * close the app, when user is in HomeTab and press the android device back button. User gets
     * ask via alert dialog if he want to exit the app
     * */
    override fun onBackPressed() {
        if(fragmentPos == 0) {
            val builder = AlertDialog.Builder(this)
            builder.setTitle(R.string.exit)
            builder.setMessage(R.string.exit_message)
            builder.setPositiveButton(R.string.yes) { _, _ ->
                finishAffinity()
            }
            builder.setNegativeButton(R.string.no) { _, _ ->
            }
            builder.show()
        }
    }
}