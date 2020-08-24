package com.gbrain.humantohuman.emgcomm

import android.content.Context
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import com.gbrain.humantohuman.SHARED_PREF_NAME
import com.gbrain.humantohuman.SHARED_PREF_WIFI_NAME
import com.gbrain.humantohuman.SHARED_PREF_WIFI_PASSWD
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.nio.ByteBuffer
import java.util.concurrent.Executors

class EMGCommunication(private val context: Context,
                       manager: UsbManager,
                       private val devconn: UsbDeviceConnection,
                       private val deviceInfoPhaseListener: SerialInputOutputManager.Listener,
                       private val signalPhaseListener: SerialInputOutputManager.Listener) {

    private lateinit var serialPort: UsbSerialPort
    private var iomanager: SerialInputOutputManager? = null
    private var TIMEOUT = 10000

    init {
        setupSerialComm(manager)
        deviceInfoPhase()
        //wifiInfoPhase()
    }

    private fun setupSerialComm(manager: UsbManager) {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        val driver = drivers.get(0)
        serialPort = driver.ports.get(0)
        serialPort.open(devconn)

        setupSerialCommParams()
    }

    private fun setupSerialCommParams() {
        serialPort.setParameters(9600, UsbSerialPort.DATABITS_8,
            UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    private fun deviceInfoPhase() {
        setupSerialAsyncComm(deviceInfoPhaseListener)
        initiatingBytes()
    }

    private fun wifiInfoPhase() {
        val pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val wifiName = pref.getString(SHARED_PREF_WIFI_NAME, "")
        val wifiPassword = pref.getString(SHARED_PREF_WIFI_PASSWD, "")

        sendString(wifiName!!)
        sendString(wifiPassword!!)
    }

    private fun sendString(str: String ){
        val buffer = ByteBuffer.allocate(40)
        str.toCharArray().forEach {
            buffer.putChar(it)
        }
        buffer.position(39)
        serialPort.write(buffer.array(), TIMEOUT)
    }

    fun startSignalPhase() {
        setupSerialAsyncComm(signalPhaseListener)
        initiatingBytes()
    }

    private fun initiatingBytes() {
        serialPort.write(ByteArray(1), TIMEOUT)
    }

    fun stopSignalPhase() {
        iomanager?.stop()
    }

    private fun setupSerialAsyncComm(listener: SerialInputOutputManager.Listener) {
        iomanager?.stop()
        iomanager = SerialInputOutputManager(serialPort, listener)
        Executors.newSingleThreadExecutor().submit(iomanager)
    }
}