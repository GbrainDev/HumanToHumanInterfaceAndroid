package com.corndog.braoadcastprac.serialprotocol

import TypeHandler
import android.content.Context
import com.gbrain.humantohuman.serialprovider.SerialPortProvider
import java.io.Closeable

interface HandShake {
    fun handShake()
}

class WaitingHandShake: HandShake {
    override fun handShake() {
        Thread.sleep(2550)
    }
}

interface SerialInput {
    fun read(amount: Int): ByteArray
}


class SerialProtocol(
    val context: Context,
    portProvider: SerialPortProvider,
    private val signalHandler: TypeHandler<Byte>,
    private val readAmount: Int = 1,
    private val pollingInterval: Long = 2L,
    doHandShake: Boolean = false
) : Closeable, Thread() {

    private lateinit var handShake: HandShake

    init {

        if (doHandShake) {
            handShake = WaitingHandShake()
            handShake.handShake()
        }

    }

    override fun run() {
        try {
            signalIoProcess()
        } catch (e: Exception) {
            close()
        }
    }

//    private fun readCalibConstantIndex(): Int {
//        var amount = 0
//        val rbytes = ByteArray(ARDUINO_SHORT_SIZE)
//        while (amount < ARDUINO_SHORT_SIZE) {
//            amount += inputStream.read(rbytes, amount, rbytes.size - amount)
//        }
//        signalBuffer = ByteBuffer.wrap(rbytes)
//        signalBuffer.rewind()
//
//        var index: Short = 0
//        while (signalBuffer.hasRemaining())
//            index = signalBuffer.order(ByteOrder.LITTLE_ENDIAN).getShort()
//
//        return index.toInt()
//    }
//
//    private fun writeCalibConstant(index: Int) {
//        val constants = CalibConstant.get(index)
//        val buffer = ByteBuffer.allocate(constants.size * ARDUINO_DOUBLE_SIZE)
//        constants.forEach { value: Double ->
//            buffer.order(ByteOrder.BIG_ENDIAN).putFloat(value.toFloat())
//        }
//        outputStream.write(buffer.array())
//    }

    private fun signalIoProcess() {
        while (true) {

            sleep(pollingInterval)
        }
    }

    override fun close() {
//        inputStream.close()
//        outputStream.close()
    }
}