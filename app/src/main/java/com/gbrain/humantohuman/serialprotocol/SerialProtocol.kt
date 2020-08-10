package com.corndog.braoadcastprac.serialprotocol

import android.app.Activity
import android.content.Context
import android.widget.Toast
import com.felhr.usbserial.SerialInputStream
import com.felhr.usbserial.SerialOutputStream
import com.felhr.usbserial.UsbSerialDevice
import dataprotocol.typehandle.ShortHandler
import java.io.Closeable
import java.nio.ByteBuffer
import java.nio.ByteOrder

class SerialProtocol(
    private val context: Context,
    private val usbDevice: UsbSerialDevice,
    private val serialConfig: SerialConfig,
    private val signalHandler: ShortHandler,
    private val readAmount: Int = 1,
    private val pollingInterval: Long = 2L,
    private val skipHandShake: Boolean = false
) : Closeable, Thread() {

    private val instream: SerialInputStream
    private val outstream: SerialOutputStream
    private val rbytes: ByteArray
    private val wbytes: ByteArray

    lateinit private var buffer: ByteBuffer

    init {
        usbDevice.syncOpen()
        sendSerialConfig()

        instream = usbDevice.inputStream
        outstream = usbDevice.outputStream
        rbytes = ByteArray(readAmount * ARDUINO_SHORT_SIZE)
        wbytes = ByteArray(readAmount)

        if (!skipHandShake) {
            sendAndroidReady()
            handleCalibConstant()
            waitArduinoReady()
        }
    }

    private fun sendSerialConfig() {
        with(usbDevice) {
            setBaudRate(serialConfig.baudRate)
            setDataBits(serialConfig.dataBits)
            setStopBits(serialConfig.stopBits)
            setParity(serialConfig.parity)
            setFlowControl(serialConfig.flowControl)
        }
    }

    override fun run() {
        try {
            signalIoProcess()
        } catch (e: InterruptedException) {
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
        while (true) {
            sendInitiator()
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
            val data = buffer.order(ByteOrder.LITTLE_ENDIAN).getShort()
            signalHandler.handle(data, 0)
        }
    }

    override fun close() {
        usbDevice.syncClose()
        instream.close()
        outstream.close()
    }

    companion object {
        private val ARDUINO_SHORT_SIZE = 2
        private val ARDUINO_DOUBLE_SIZE = 4
        private val ANDROID_READY = ByteArray(1)
    }
}