package com.gbrain.humantohuman.devicemacadapter

import android.graphics.drawable.Drawable
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.graphics.drawable.DrawableCompat
import androidx.recyclerview.widget.RecyclerView
import com.gbrain.humantohuman.R

class DeviceMacAdapter: RecyclerView.Adapter<DeviceMacAdapter.ViewHolder>() {
    private var inflater: LayoutInflater? = null
    private val deviceMacList = ArrayList<String>()

    fun addItem(item: String) {
        deviceMacList.add(item)
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        if (inflater == null)
            inflater = LayoutInflater.from(parent.context)

        val view = inflater!!.inflate(R.layout.layout_item_device_mac, parent, false)
        return ViewHolder(view)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val macAddress = deviceMacList[position]
        holder.showNumber(position)
        holder.showMacAddress(macAddress)
    }

    override fun getItemCount(): Int {
        return deviceMacList.size
    }

    inner class ViewHolder(v: View): RecyclerView.ViewHolder(v) {
        private val numberingTextView: TextView
        private val macAddressTextView: TextView

        init {
            numberingTextView = v.findViewById(R.id.numbering)
            macAddressTextView = v.findViewById(R.id.mac_address)
        }

        fun showNumber(num: Int) {
            numberingTextView.setText((num+1).toString())
            numberingTextView.setBackground(getColorDrawable(num))
        }

        private fun getColorDrawable(num: Int): Drawable {
            val colorArray = itemView.resources.getIntArray(R.array.deviceNumberingColorArray)
            val drawable: Drawable = AppCompatResources.getDrawable(itemView.context, R.drawable.drawable_round)!!
            val wrapped = DrawableCompat.wrap(drawable)

            DrawableCompat.setTint(wrapped, colorArray.get(num % colorArray.size));
            return wrapped
        }

        fun showMacAddress(text: String) {
            macAddressTextView.setText(text)
        }
    }
}