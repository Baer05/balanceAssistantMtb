package com.example.balanceassistantmtb.ui.main

import android.content.Context
import android.os.Bundle
import android.os.Handler
import android.os.Message
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
import com.example.balanceassistantmtb.service.UdpClient
import java.lang.ref.WeakReference

class HomeFragment : Fragment() {

    private lateinit var thisContext: Context
    private val isTrackingActive = MutableLiveData<Boolean>(false)
    private var udpSocket: UdpClient? = null
    private val outerClass = WeakReference<HomeFragment>(this)
    private var handler: Handler =  MyHandler(outerClass)

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

        udpSocket = UdpClient(handler)

        view.findViewById<Button>(R.id.start_tracking_btn).setOnClickListener {
            isTrackingActive.value = true
            udpSocket?.startUDPSocket()
        }
        view.findViewById<Button>(R.id.end_tracking_btn).isVisible = false
        view.findViewById<Button>(R.id.end_tracking_btn).setOnClickListener {
            isTrackingActive.value = false
            udpSocket?.stopUDPSocket()
        }
        isTrackingActive.observe(viewLifecycleOwner) {
            if (isTrackingActive.value == true) {
                view.findViewById<Button>(R.id.start_tracking_btn).isVisible = false
                view.findViewById<Button>(R.id.end_tracking_btn).isVisible = true
            } else {
                view.findViewById<Button>(R.id.start_tracking_btn).isVisible = true
                view.findViewById<Button>(R.id.end_tracking_btn).isVisible = false
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
        udpSocket?.stopUDPSocket()
    }
}

