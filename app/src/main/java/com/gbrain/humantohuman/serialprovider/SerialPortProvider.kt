package com.gbrain.humantohuman.serialprovider

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager

class SerialPortProvider private constructor(private val context: Context, private val deviceType: DeviceType) {
    var manager: UsbManager
        private set
    var device: UsbDevice? = null
        private set

    private val aSubsribers = HashMap<String, DeviceAttachedListener>()
    private val dSubsribers = HashMap<String, DeviceDettachedListener>()

    init {
        manager = context.getSystemService(Context.USB_SERVICE) as UsbManager
    }

    fun addDeviceAttachedListener(key: String, listener: DeviceAttachedListener) {
        aSubsribers[key] = listener
    }

    fun addDeviceDetachedListener(key: String, listener: DeviceDettachedListener) {
        dSubsribers[key] = listener
    }

    fun allocDevice(): Boolean {
        val devices = manager.deviceList.values
        devices.forEach { device->
            if (deviceType.vid == device.vendorId) {
                return allocDevice(device)
            }
        }
        return false
    }

    fun allocDevice(device: UsbDevice): Boolean {
        this.device = device
        notifyPostAttach()
        return true
    }

    private fun notifyPostAttach() {
        aSubsribers.values.forEach {
            it.postDeviceAttach(device!!)
        }
    }

    fun deallocDevice() {
        if (device != null) {
            notifyPreDetached()
            device = null
        }
    }

    private fun notifyPreDetached() {
        dSubsribers.values.forEach {
            it.preDeviceDetach(device!!)
        }
    }

    fun isDeviceAllocated() = device != null

    fun getVendorId(): Int {
        return device!!.vendorId
    }

    fun requestUsbPermission() {
        val permissionIntent = PendingIntent.getBroadcast(
            context, 0,
            Intent(ACTION_USB_PERMISSION), 0
        )
        manager.requestPermission(device, permissionIntent)
    }

    fun getOpened(): UsbDeviceConnection {
        return manager.openDevice(device)
    }

    interface DeviceAttachedListener {
        fun postDeviceAttach(usbDevice: UsbDevice)
    }

    interface DeviceDettachedListener {
        fun preDeviceDetach(usbDevice: UsbDevice)
    }

    companion object {
        val ACTION_USB_PERMISSION = "com.corndog.usb.USB_PERMISSION"
        private var instance: SerialPortProvider? = null
        fun getInstance(context: Context, deviceType: DeviceType): SerialPortProvider {
            if (instance == null)
                instance = SerialPortProvider(context, deviceType)
            return instance!!
        }

        fun getInstance(): SerialPortProvider {
            return instance!!
        }
    }
}

enum class DeviceType(val vid: Int, val iface: Int){
    ARDUINO(9025, 0),
    PL2303(1659, 0),
}