package com.example.balanceassistantmtb

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import com.example.balanceassistantmtb.ui.main.HomeActivity
import com.xsens.dot.android.sdk.XsensDotSdk




class MainActivity : AppCompatActivity() {
    private val tAG: String = MainActivity::class.java.name


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

      val startButton = findViewById<View>(R.id.main_start_btn)
        startButton.setOnClickListener{
            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
        }

        initXsensDotSdk();
    }

    /**
     * Setup for Xsens DOT SDK.
     */
    private fun initXsensDotSdk() {
        // Get the version name of SDK.
        val version = XsensDotSdk.getSdkVersion()
        Log.i(tAG, "initXsensDotSdk() - version: $version")
        // Enable this feature to monitor logs from SDK.
        XsensDotSdk.setDebugEnabled(true)
        // Enable this feature then SDK will start reconnection when the connection is lost.
        XsensDotSdk.setReconnectEnabled(true)
    }

}