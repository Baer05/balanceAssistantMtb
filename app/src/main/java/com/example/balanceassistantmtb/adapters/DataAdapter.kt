package com.example.balanceassistantmtb.adapters

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.balanceassistantmtb.R
import com.xsens.dot.android.sdk.events.XsensDotData


class DataAdapter(var context: Context, var mDataList: ArrayList<HashMap<String, Any>>):
    RecyclerView.Adapter<DataAdapter.DataViewHolder>() {

    companion object {
        // The keys of HashMap
        const val keyADDRESS = "address"
        const val keyTAG = "tag"
        const val keyDATA = "data"
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): DataViewHolder {
        val itemView: View =
            LayoutInflater.from(parent.context).inflate(R.layout.item_data, parent, false)
        return DataViewHolder(itemView)
    }

    @SuppressLint("DefaultLocale")
    override fun onBindViewHolder(holder: DataViewHolder, position: Int) {
        val tag = mDataList[position][keyTAG] as String?
        val xsData = mDataList[position][keyDATA] as XsensDotData?
        holder.sensorName.text = tag
        val quats = xsData!!.quat
        val quatsStr = String.format("%.2f", quats[0]) + ",\t" + String.format(
            "%.2f",
            quats[1]
        ) + ",\t" + String.format("%.2f", quats[2]) + ",\t" + String.format(
            "%.2f",
            quats[3]
        )
        holder.orientationData.text = quatsStr
        val freeAcc = xsData.freeAcc
        val freeAccStr = String.format("%.2f", freeAcc[0]) + ",\t" + String.format(
            "%.2f",
            freeAcc[1]
        ) + ",\t" + String.format("%.2f", freeAcc[2])
        holder.freeAccData.text = freeAccStr
    }

    override fun getItemCount(): Int {
        return mDataList.size
    }

    /**
     * A Customized class for ViewHolder of RecyclerView.
     */
    class DataViewHolder(v: View) : RecyclerView.ViewHolder(v) {
        var rootView: View
        var sensorName: TextView
        var orientationData: TextView
        var freeAccData: TextView

        init {
            rootView = v
            sensorName = v.findViewById(R.id.sensor_name)
            orientationData = v.findViewById(R.id.orientation_data)
            freeAccData = v.findViewById(R.id.free_acc_data)
        }
    }
}