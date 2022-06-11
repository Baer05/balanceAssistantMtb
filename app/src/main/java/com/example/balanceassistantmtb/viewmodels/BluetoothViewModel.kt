package com.example.balanceassistantmtb.viewmodels

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStoreOwner

class BluetoothViewModel: ViewModel() {
    companion object {
        /**
         * Get the instance of BluetoothViewModel
         *
         * @param owner The life cycle owner from activity/fragment
         * @return The BluetoothViewModel
         */
        fun getInstance(owner: ViewModelStoreOwner): BluetoothViewModel {
            return ViewModelProvider(
                owner,
                ViewModelProvider.NewInstanceFactory()
            )[BluetoothViewModel::class.java]
        }
    }
    // A variable to notify the Bluetooth status
    private val mIsBluetoothEnabled = MutableLiveData<Boolean>()

    // A variable to notify the scanning status
    private val mIsScanning = MutableLiveData<Boolean>()

    /**
     * Observe this function to listen the status of Bluetooth adapter.
     *
     * @return The latest status
     */
    fun isBluetoothEnabled(): MutableLiveData<Boolean> {
        return mIsBluetoothEnabled
    }

    /**
     * Notify the Bluetooth adapter status to activity/fragment
     *
     * @param enabled he status of Bluetooth
     */
    fun updateBluetoothEnableState(enabled: Boolean) {
        mIsBluetoothEnabled.postValue(enabled)
    }

    /**
     * Observe this function to listen the scanning status.
     *
     * @return The latest scan status
     */
    fun isScanning(): MutableLiveData<Boolean> {
        return mIsScanning
    }

    /**
     * Notify the scan status to activity/fragment
     *
     * @param scanning The status of scanning
     */
    fun updateScanState(scanning: Boolean) {
        mIsScanning.postValue(scanning)
    }
}