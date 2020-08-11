package com.corndog.braoadcastprac.serialprotocol

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.felhr.usbserial.SerialInputStream
import com.felhr.usbserial.SerialOutputStream
import com.felhr.usbserial.UsbSerialDevice
import dataprotocol.typehandle.ByteHandler
import dataprotocol.typehandle.ShortHandler
import java.io.Closeable
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SerialProtocol(
    private val context: Context,
    private val usbDevice: UsbSerialDevice,
    private val serialConfig: SerialConfig,
    private val signalHandler: ByteHandler,
    private val readAmount: Int = 1,
    private val pollingInterval: Long = 2L,
    private val doHandShake: Boolean = false
) : Closeable, Thread() {

    private var instream: SerialInputStream
    private var outstream: SerialOutputStream
    private val rbytes: ByteArray
    private val wbytes: ByteArray

    lateinit private var buffer: ByteBuffer

    init {sendSerialConfig()
        usbDevice.syncOpen()
        instream = usbDevice.inputStream
        outstream = usbDevice.outputStream

        if (doHandShake)
            sendSerialConfig()

        rbytes = ByteArray(readAmount * ARDUINO_SHORT_SIZE)
        wbytes = ByteArray(readAmount)
    }

    private fun sendSerialConfig(){
        var read_one = 0

        do {
            if (!usbDevice.isOpen)
                usbDevice.syncOpen()

            with(usbDevice) {
                setBaudRate(serialConfig.baudRate)
                setDataBits(serialConfig.dataBits)
                setStopBits(serialConfig.stopBits)
                setParity(serialConfig.parity)
                setFlowControl(serialConfig.flowControl)
            }

            instream = usbDevice.inputStream
            outstream = usbDevice.outputStream

            read_one = instream.read()

            if (read_one == 0)
                usbDevice.syncClose()
            else
                break

        } while (true)

        (context as Activity).runOnUiThread {
            Toast.makeText(context, "serial config done", Toast.LENGTH_SHORT).show()
        }
    }

    fun setupInputStream() {
        if (!usbDevice.isOpen)
            usbDevice.syncOpen()
        instream = usbDevice.inputStream
        outstream = usbDevice.outputStream
    }

    override fun run() {
        try {
            signalIoProcess()
        } catch (e: Exception) {
            close()
        }
    }

    private fun sendAndroidReady() {
        sleep(2500) //wait for arduino initialization
        outstream.write(ANDROID_READY);
    }

    private fun handleCalibConstant() {
        writeCalibConstant(readCalibConstantIndex())
    }

    private fun readCalibConstantIndex(): Int {
        var amount = 0
        val rbytes = ByteArray(ARDUINO_SHORT_SIZE)
        while (amount < ARDUINO_SHORT_SIZE) {
            amount += instream.read(rbytes, amount, rbytes.size - amount)
        }
        buffer = ByteBuffer.wrap(rbytes)
        buffer.rewind()

        var index: Short = 0
        while (buffer.hasRemaining())
            index = buffer.order(ByteOrder.LITTLE_ENDIAN).getShort()

        (context as Activity).runOnUiThread {
            Toast.makeText(context, "$index selected", Toast.LENGTH_SHORT).show()
        }
        return index.toInt()
    }

    private fun writeCalibConstant(index: Int) {
        val constants = CalibConstant.get(index)
        val buffer = ByteBuffer.allocate(constants.size * ARDUINO_DOUBLE_SIZE)
        constants.forEach {value: Double ->
            buffer.order(ByteOrder.BIG_ENDIAN).putFloat(value.toFloat())
        }
        outstream.write(buffer.array())
    }

    private fun waitArduinoReady() {
        instream.read()
    }

    private fun signalIoProcess() {
        (context as Activity).runOnUiThread {
            Toast.makeText(context, "start reading", Toast.LENGTH_SHORT).show()
        }
        while (true) {
//            sendInitiator()
            readSignal()
            handleSignal()
            sleep(pollingInterval)
        }
    }

    private fun sendInitiator() {
        outstream.write(wbytes)
    }

    private fun readSignal() {
        var amount = 0
        while (amount < readAmount * ARDUINO_SHORT_SIZE) {
            amount += instream.read(rbytes, amount, rbytes.size - amount)
        }
        buffer = ByteBuffer.wrap(rbytes)
        buffer.rewind()
    }

    private fun handleSignal() {
        while (buffer.hasRemaining()) {
            val data = buffer.order(ByteOrder.LITTLE_ENDIAN).get()
            signalHandler.handle(data, 0)
        }
    }

    override fun close() {
        usbDevice.syncClose()
        instream.close()
        outstream.close()
    }

    companion object {
        private val ARDUINO_SHORT_SIZE = 1
        private val ARDUINO_DOUBLE_SIZE = 4
        private val ANDROID_READY = ByteArray(1)
    }
}