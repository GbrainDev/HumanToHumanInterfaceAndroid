package com.gbrain.humantohuman

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.gbrain.humantohuman.serialprovider.DeviceType
import com.gbrain.humantohuman.serialprovider.SerialPortProvider

class MainActivity: AppCompatActivity() {

    private val deviceType = DeviceType.PL2303
    lateinit var portProvider: SerialPortProvider

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        portProvider = SerialPortProvider.getInstance(this, deviceType)
        runReceiver()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        portProvider.allocDevice()
    }

    private fun runReceiver() {
        val receiver = UsbEventReceiver()
        with (IntentFilter()) {
            addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)

            try {
                unregisterReceiver(receiver)
            } catch (e: Exception){
            } finally {
                registerReceiver(receiver, this)
            }
        }

    }

    inner class UsbEventReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            handleBroadcast(intent)
        }

        private fun handleBroadcast(intent: Intent?) {
            intent?.also {
                val device = intent.getParcelableExtra<UsbDevice>(UsbManager.EXTRA_DEVICE)!!
                val action = intent.action
                when (action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED -> portProvider.allocDevice(device)
                    UsbManager.ACTION_USB_DEVICE_DETACHED -> portProvider.deallocDevice()
                }
            }
        }
    }
}