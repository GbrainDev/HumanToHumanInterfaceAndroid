package com.gbrain.humantohuman.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.gbrain.humantohuman.R
import kotlinx.android.synthetic.main.fragment_main.*

interface UsbDeviceHolderInterface {
    fun hasDevice(): Boolean
    fun getDevice(): UsbDevice?
    fun searchDevice(): UsbDevice?
    fun fetchDevice(intent: Intent): UsbDevice?
    fun handleBroadcast(intent: Intent?)
    fun onDeviceAttached(device: UsbDevice)
    fun onDeviceDetached(device: UsbDevice)
}

class MainFragment : Fragment() {

    val deviceConnectionReceiver = UsbDeviceProvider()
    lateinit var usbDeviceHolder: UsbDeviceHolder
    lateinit var navController: NavController

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val usbManager = context?.getSystemService(Context.USB_SERVICE) as UsbManager
        navController = Navigation.findNavController(view)
        usbDeviceHolder = UsbDeviceHolder(usbManager)
        addReceiver()
        setupGUI()
    }

    private fun addReceiver() {
        val filter = IntentFilter()
        filter.addAction("android.hardware.usb.action.USB_DEVICE_DETACHED")
        filter.addAction("android.hardware.usb.action.USB_DEVICE_ATTACHED")

        try {
            activity?.unregisterReceiver(deviceConnectionReceiver)
        } catch (e: Exception) {
        }

        activity?.registerReceiver(deviceConnectionReceiver, filter)
    }

    private fun setupGUI() {
        setupButtons()
        guiApplyDeviceStatus(usbDeviceHolder.hasDevice())
    }

    private fun setupButtons() {
        btn_chart.setOnClickListener{
            navController.navigate(R.id.action_mainFragment_to_chartFragment)
        }
        btn_guide.setOnClickListener{
            navController.navigate(R.id.action_mainFragment_to_guideFragment)
        }
        btn_info.setOnClickListener{
            navController.navigate(R.id.action_mainFragment_to_infoFragment)
        }
    }

    private fun guiApplyDeviceStatus(deviceDetected: Boolean) {
        if (deviceDetected) {
            device_status.background =
                activity?.getDrawable(R.drawable.drawable_device_status_detected)
            device_status.text =
                activity?.getString(R.string.device_status_detected) + " " + usbDeviceHolder.getDevice()?.vendorId
        } else {
            device_status.background =
                activity?.getDrawable(R.drawable.drawable_device_status_lost)
            device_status.text =
                activity?.getString(R.string.device_status_lost)
        }
        device_status.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
    }

    inner class UsbDeviceProvider: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            intent?.also {
                usbDeviceHolder.handleBroadcast(intent)
            }
        }
    }

    inner class UsbDeviceHolder(val usbManager: UsbManager): UsbDeviceHolderInterface {
        private var device: UsbDevice? = null
        private val ARDUINO_VENDOR = 3368
        private val AVAILABLE_VENDOR_IDS = arrayListOf(ARDUINO_VENDOR, 9025)

        init {
            device = searchDevice()
        }

        override fun hasDevice(): Boolean {
            return device != null
        }

        override fun getDevice(): UsbDevice? {
            return device
        }

        override fun onDeviceAttached(device: UsbDevice) {
            this.device = device
            guiApplyDeviceStatus(true)
        }

        override fun onDeviceDetached(device: UsbDevice) {
            this.device = null
            guiApplyDeviceStatus(false)
        }

        override fun fetchDevice(intent: Intent): UsbDevice? {
            return intent.getParcelableExtra(UsbManager.EXTRA_DEVICE)
        }

        override fun searchDevice(): UsbDevice? {
            val deviceList = usbManager.deviceList.values
            deviceList.forEach {device->
                if (AVAILABLE_VENDOR_IDS.contains(device.vendorId))
                    return device
            }
            return null
        }

        override fun handleBroadcast(intent: Intent?) {
            intent?.also {
                val device = fetchDevice(intent)
                val action = intent.action
                when (action) {
                    UsbManager.ACTION_USB_DEVICE_ATTACHED->onDeviceAttached(device!!)
                    UsbManager.ACTION_USB_DEVICE_DETACHED->onDeviceDetached(device!!)
                }
            }
        }
    }
}