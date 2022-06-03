package com.example.balanceassistantmtb.ui.Main

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import com.example.balanceassistantmtb.R

class DashboardFragment : Fragment() {

    private lateinit var thisContext: Context

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?,
                              savedInstanceState: Bundle?): View? {
        if (container != null) {
            thisContext = container.context
        }
        // Inflate the layout for this fragment
        val view = inflater.inflate(R.layout.fragment_dashboard, container, false)
        val actionBar = view.findViewById<Toolbar>(R.id.toolbar)
        actionBar.title = "Dashboard"
        (requireActivity() as AppCompatActivity).setSupportActionBar(actionBar)
        return view
    }

}