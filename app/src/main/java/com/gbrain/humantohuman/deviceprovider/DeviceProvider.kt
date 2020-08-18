package com.gbrain.humantohuman.deviceprovider

import android.hardware.usb.UsbDeviceConnection
import com.felhr.usbserial.UsbSerialDevice

interface DeviceProvider {
    fun allocDevice(): Boolean
    fun deallocDevice(): Boolean
    fun openDevice()
    fun closeDevice()
    fun isOpen(): Boolean
}

object UsbDeviceHolder: DeviceProvider {
    init {
        //first find the best matched usb device.
    }

    override fun allocDevice(): Boolean {
        TODO("Not yet implemented")
    }

    override fun deallocDevice(): Boolean {
        TODO("Not yet implemented")
    }

    override fun openDevice() {
        TODO("Not yet implemented")
    }

    override fun closeDevice() {
        TODO("Not yet implemented")
    }

    override fun isOpen(): Boolean {
        TODO("Not yet implemented")
    }
}