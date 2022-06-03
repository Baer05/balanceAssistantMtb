package com.example.balanceassistantmtb.ui.Main

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.lifecycle.MutableLiveData
import com.example.balanceassistantmtb.R
import com.example.balanceassistantmtb.service.AsyncstorageService
import com.example.balanceassistantmtb.service.UDPListenerService
import kotlinx.coroutines.*
import java.lang.Runnable
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import kotlin.coroutines.CoroutineContext


class HomeFragment : Fragment(), CoroutineScope {

    companion object {
        private lateinit var listener: Job
    }

    override val coroutineContext: CoroutineContext
        get() =Dispatchers.Main + listener

    private lateinit var thisContext: Context
    private val isTrackingActive = MutableLiveData<Boolean>(false)

    private val t = Thread {
        while (isTrackingActive.value == true) {
            // Your code
            try {
                val data = UDPListenerService.receiveUDPPackage("192.168.178.23", 2390)
                val handler = Handler(Looper.getMainLooper())
                handler.post(Runnable {
                    // update the ui from here
                    if (data == "ip_address_not_found") {
                        isTrackingActive.value = false
                    }
                })
                Thread.sleep(500)
            } catch (e: Exception) {
                Log.d("ThreadException", "$e")
            }
        }
    }

    //@OptIn(InternalCoroutinesApi::class)
    @OptIn(InternalCoroutinesApi::class)
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (container != null) {
            thisContext = container.context
        }

        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_home, container, false)
        val actionBar = view.findViewById<Toolbar>(R.id.toolbar)
        actionBar.title = "Home"
        (requireActivity() as AppCompatActivity).setSupportActionBar(actionBar)
        view.findViewById<Button>(R.id.start_tracking_btn).setOnClickListener {
            isTrackingActive.value = true
            startBtnOnClick(view)
        }
        view.findViewById<Button>(R.id.end_tracking_btn).isVisible = false;
        view.findViewById<Button>(R.id.end_tracking_btn).setOnClickListener {
            isTrackingActive.value = false
            endBtnOnClick(view)
        }
        isTrackingActive.observe(viewLifecycleOwner) {
            if (isTrackingActive.value == true) {
                view.findViewById<Button>(R.id.start_tracking_btn).isVisible = false;
                view.findViewById<Button>(R.id.end_tracking_btn).isVisible = true;
            } else {
                view.findViewById<Button>(R.id.start_tracking_btn).isVisible = true;
                view.findViewById<Button>(R.id.end_tracking_btn).isVisible = false;
            }
        }
        return view
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
     * start Job
     * val job = startRepeatingJob()
     * cancels the job and waits for its completion
     * job.cancelAndJoin()
     * Params
     * timeInterval: time milliSeconds
     */
    private fun startRepeatingJob(): Job {
        return CoroutineScope(Dispatchers.Default).launch {
            while (NonCancellable.isActive) {
                val handler = Handler(Looper.getMainLooper())
                if(isTrackingActive.value == true) {
                    // add your task here
                    val data = UDPListenerService.receiveUDPPackage("192.168.178.24", 2390)
                    handler.post(Runnable {
                        // update the ui from here
                        if (data == "invalid_ip_address") {
                            isTrackingActive.value = false
                            listener.cancel()
                            doToast("open fun receiveUDP catch exception. Address not found!")
                        }
                    })
                    delay(900)
                } else {
                    handler.post(Runnable {
                        // update the ui from here
                        isTrackingActive.value = false
                    })
                }
            }
        }
    }

    @InternalCoroutinesApi
    fun endBtnOnClick(view: View?) {
        listener.cancel()
    }

    @InternalCoroutinesApi
    fun startBtnOnClick(view: View?) {
        listener = startRepeatingJob()
    }

    override fun onDestroy() {
        super.onDestroy()
        isTrackingActive.value = false
    }
}

