package com.gbrain.humantohuman.emgcomm

import android.content.Context
import android.widget.Toast
import com.gbrain.humantohuman.SHARED_PREF_NAME
import com.gbrain.humantohuman.SHARED_PREF_WIFI_NAME
import com.gbrain.humantohuman.SHARED_PREF_WIFI_PASSWD
import com.gbrain.humantohuman.serialprovider.SerialPortProvider
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import java.nio.ByteBuffer
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class EMGCommunication(private val context: Context,
                       private val portProvider: SerialPortProvider,
                       private val deviceInfoPhaseListener: SerialInputOutputManager.Listener,
                       private val signalPhaseListener: SerialInputOutputManager.Listener) {

    private lateinit var serialPort: UsbSerialPort
    private var iomanager: SerialInputOutputManager? = null
    private var execService: ExecutorService? = null
    private var TIMEOUT = 10000
    private var isWifiDevicePhase = true

    init {
        wifiDeviceInfoPhase()
    }

    private fun wifiDeviceInfoPhase() {
        setupNewPhase()
        sendWifiInfo()
        setAsyncListener(deviceInfoPhaseListener)
    }

    private fun setupNewPhase() {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(portProvider.manager)
        val driver = drivers.get(0)

        serialPort = driver.ports.get(0)
        serialPort.open(portProvider.getOpened())

        setupSerialParams()
    }

    private fun setupSerialParams() {
        serialPort.setParameters(9600, UsbSerialPort.DATABITS_8,
            UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)
    }

    private fun sendWifiInfo() {
        val pref = context.getSharedPreferences(SHARED_PREF_NAME, Context.MODE_PRIVATE)
        val wifiName = pref.getString(SHARED_PREF_WIFI_NAME, "")
        val wifiPassword = pref.getString(SHARED_PREF_WIFI_PASSWD, "")

//        val wifiName = "gbrain"
//        val wifiPassword = "gbrain1908!"
        sendString(wifiName!!, 40)
        sendString(wifiPassword!!, 40)
        log("$wifiName, $wifiPassword")
    }

    private fun sendString(str: String, bufferSize: Int){
        val buffer = ByteBuffer.allocate(bufferSize)
        buffer.put(str.toByteArray())
        buffer.limit(bufferSize - 1)
        serialPort.write(buffer.array(), TIMEOUT)
    }

    private fun log(text: String) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show()
    }

    private fun setAsyncListener(listener: SerialInputOutputManager.Listener) {
        Thread.sleep(1000)
        iomanager = SerialInputOutputManager(serialPort, listener)
        execService = Executors.newSingleThreadExecutor()
        execService?.submit(iomanager)
    }

    private fun releasePhase() {
        iomanager?.stop()
        execService?.shutdownNow()
        serialPort.close()
    }

    fun startSignalingPhase() {
        if (isWifiDevicePhase)
            releasePhase()

        setupNewPhase()
        setAsyncListener(signalPhaseListener)
        sendInitiator()

        isWifiDevicePhase = false
    }

    private fun sendInitiator() {
        serialPort.write(ByteArray(1), TIMEOUT)
    }

    fun stopSignalListening() {
        sendTerminator()
        releasePhase()
    }

    private fun sendTerminator() {
        sendInitiator()
    }

    companion object {
        private val nullMacAddressString = "00:00:00:00:00:00"
    }
}