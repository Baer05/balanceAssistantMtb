package com.example.balanceassistantmtb.service

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatActivity
import com.example.balanceassistantmtb.models.BalanceData
import com.google.gson.GsonBuilder

object AsyncstorageService {

    fun writeJSONToRef (context: Context, ref: String, data: Any) {
        val prefs: SharedPreferences = context.getSharedPreferences(
            "balanceAssistantMtb", AppCompatActivity.MODE_PRIVATE
        )
        val gSon = GsonBuilder().create()
        val json = gSon.toJson(data)
        prefs.edit().putString(ref, json).apply()
    }

    fun loadDataFromPref (context: Context): BalanceData {
        val prefs: SharedPreferences = context.getSharedPreferences(
            "balanceAssistantMtb", AppCompatActivity.MODE_PRIVATE
        )
        val gSon = GsonBuilder().create()
        return gSon.fromJson(prefs.getString("BalanceData", null), BalanceData::class.java)
    }

    fun clearPrefs (context: Context, ref: String) {
        val prefs: SharedPreferences = context.getSharedPreferences(
            "balanceAssistantMtb", AppCompatActivity.MODE_PRIVATE
        )
        prefs.edit().putString(ref, null).apply()
    }
}