package com.example.balanceassistantmtb

import android.content.Intent
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

      val startButton = findViewById<View>(R.id.main_start_btn)
        startButton.setOnClickListener{
            val intent = Intent(this@MainActivity, HomeActivity::class.java)
            startActivity(intent)
        }
    }

}