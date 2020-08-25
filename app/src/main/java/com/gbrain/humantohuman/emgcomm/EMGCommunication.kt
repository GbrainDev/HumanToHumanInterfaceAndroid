package com.gbrain.humantohuman.emgcomm

import android.content.Context
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.net.MacAddress
import android.os.Build
import android.widget.Toast
import androidx.annotation.RequiresApi
import com.gbrain.humantohuman.SHARED_PREF_NAME
import com.gbrain.humantohuman.SHARED_PREF_WIFI_NAME
import com.gbrain.humantohuman.SHARED_PREF_WIFI_PASSWD
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.nio.ByteBuffer
import java.util.*
import java.util.concurrent.Executors
import java.util.concurrent.Future

class EMGCommunication(private val context: Context,
                       manager: UsbManager,
                       private val devconn: UsbDeviceConnection,
                       private val signalPhaseListener: SerialInputOutputManager.Listener) {

    private lateinit var serialPort: UsbSerialPort
    private val deviceListReader: Thread = Thread {
        try {
            readDeviceInfo()
        } catch (e: Exception) {
        }
    }
    lateinit var deviceList: ArrayList<MacAddress>
    private var iomanager: SerialInputOutputManager? = null
    private var execService: Future<*>? = null
    private var TIMEOUT = 10000

    init {
        setupSerialComm(manager)
        sendWifiInfo()
        deviceListReader.start()
    }

    private fun setupSerialComm(manager: UsbManager) {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(manager)
        val driver = drivers.get(0)
        serialPort = driver.ports.get(0)
        serialPort.open(devconn)

        setupSerialParams()
    }

    private fun setupSerialParams() {
        serialPort.setParameters(9600, UsbSerialPort.DATABITS_8,
            UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    private fun sendWifiInfo() {
        val pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
//        val wifiName = pref.getString(SHARED_PREF_WIFI_NAME, "")
//        val wifiPassword = pref.getString(SHARED_PREF_WIFI_PASSWD, "")

        val wifiName = "gbrain"
        val wifiPassword = "gbrain1908!"
        sendString(wifiName, 40)
        sendString(wifiPassword, 40)
        log("$wifiName, $wifiPassword")
    }

    private fun sendString(str: String, bufferSize: Int){
        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.put(str.toByteArray())
        buffer.limit(bufferSize - 1)
        serialPort.write(buffer.array(), TIMEOUT)
    }

    private fun readDeviceInfo() {
        val rBytes = ByteArray(48*30)
        val readLen = serialPort.read(rBytes, TIMEOUT)
        if (readLen != rBytes.size)
            throw RuntimeException("Invalidated Device Information")

        deviceList = ArrayList()
        for (i in 0 until 30) {
            val sBytes = rBytes.sliceArray(48*i until 48*(i+1))
            val macAddress = MacAddress.fromBytes(sBytes)
            if (nullMacAddressString != macAddress.toOuiString()) {
                deviceList.add(macAddress)
                log(macAddress.toOuiString())
            }
        }
    }

    private fun log(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    fun startSignalListening() {
        deviceListReader.interrupt()
        setupAsyncListener(signalPhaseListener)
        Thread.sleep(2500)
        initiatingBytes()
    }

    private fun setupAsyncListener(listener: SerialInputOutputManager.Listener) {
        iomanager?.stop()
        iomanager = SerialInputOutputManager(serialPort, listener)
        execService = Executors.newSingleThreadExecutor().submit(iomanager)
    }

    private fun initiatingBytes() {
        serialPort.write(ByteArray(1), TIMEOUT)
    }

    fun stopSignalListening() {
        iomanager?.stop()
        execService?.cancel(true)
    }

    companion object {
        private val nullMacAddressString = "00:00:00:00:00:00"
    }
}