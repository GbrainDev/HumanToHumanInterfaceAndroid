package com.gbrain.humantohuman.fragment

import android.hardware.usb.UsbDevice
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.gbrain.humantohuman.R
import com.gbrain.humantohuman.serialprovider.SerialPortProvider
import kotlinx.android.synthetic.main.fragment_main.*

class MainFragment : Fragment(),
    SerialPortProvider.DeviceAttachedListener,
    SerialPortProvider.DeviceDettachedListener {

    lateinit var portProvider: SerialPortProvider
    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subsribeProvider()
    }

    private fun subsribeProvider() {
        portProvider = SerialPortProvider.getInstance()
        portProvider.addDeviceAttachedListener("main", this)
        portProvider.addDeviceDetachedListener("main", this)
    }

    override fun postDeviceAttach(usbDevice: UsbDevice) {
        setupDeviceStatus(true)
    }

    override fun preDeviceDetach(usbDevice: UsbDevice) {
        setupDeviceStatus(false)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_main, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        setupButtons()
        setupDeviceStatus(portProvider.isDeviceAllocated())
    }

    private fun setupButtons() {
        btn_chart.setOnClickListener {
            navController.navigate(R.id.action_mainFragment_to_chartFragment)
        }

        btn_guide.setOnClickListener {
            navController.navigate(R.id.action_mainFragment_to_guideFragment)
        }

        btn_info.setOnClickListener {
            navController.navigate(R.id.action_mainFragment_to_infoFragment)
        }
    }

    private fun setupDeviceStatus(deviceDetected: Boolean) {
        if (deviceDetected) {
            device_status.background =
                activity?.getDrawable(R.drawable.drawable_device_status_detected)
            device_status.text =
                activity?.getString(R.string.device_status_detected) + " id:" + portProvider.getVendorId()
        } else {
            device_status.background =
                activity?.getDrawable(R.drawable.drawable_device_status_lost)
            device_status.text =
                activity?.getString(R.string.device_status_lost)
        }
        btn_chart.isEnabled = deviceDetected
        device_status.startAnimation(AnimationUtils.loadAnimation(context, android.R.anim.fade_in))
    }
}