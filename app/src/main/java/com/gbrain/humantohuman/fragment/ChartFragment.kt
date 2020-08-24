package com.gbrain.humantohuman.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.net.MacAddress
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.RequiresApi
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.gbrain.humantohuman.chartdrawer.ChartDrawer
import com.gbrain.humantohuman.R
import com.gbrain.humantohuman.emgcomm.EMGCommunication
import com.gbrain.humantohuman.serialprovider.SerialPortProvider
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.android.synthetic.main.fragment_chart.*

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return
    activity?.runOnUiThread(action)
}

class ChartFragment : Fragment(),
    SerialPortProvider.DeviceAttachedListener,
    SerialPortProvider.DeviceDettachedListener {

    var batch = 20
    lateinit var portProvider: SerialPortProvider
    lateinit var emgComm: EMGCommunication
    var chartDrawer: ChartDrawer? = null

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeProvider()
        runReceiver()
    }

    private fun subscribeProvider() {
        portProvider = SerialPortProvider.getInstance()
        portProvider.addDeviceAttachedListener("chart", this)
        portProvider.addDeviceDetachedListener("chart" , this)
    }

    private fun runReceiver() {
        val context = requireContext()
        val receiver = UsbPermissionReceiver()
        val filter = IntentFilter(SerialPortProvider.ACTION_USB_PERMISSION)

        try {
            context.unregisterReceiver(receiver)
        } catch (e: Exception) {
        } finally {
            context.registerReceiver(receiver, filter)
        }
    }

    override fun postDeviceAttach(usbDevice: UsbDevice) {
        portProvider.requestUsbPermission()
    }

    override fun preDeviceDetach(usbDevice: UsbDevice) {
        deactivateWorkers()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        navController = Navigation.findNavController(view)

        setupButtons()
        setupDeviceInfo()

        if (portProvider.isDeviceAllocated())
            portProvider.requestUsbPermission()
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            startButton.text = "그래프 구현중"
            startButton.isClickable = false
            activateWorkers()
        }

        stopButton.setOnClickListener {
            startButton.text = "Start"
            startButton.isClickable = true
            deactivateWorkers()
        }
    }

    private fun activateWorkers() {
        chartDrawer = ChartDrawer(lineChart, batch, 30f, 30f)
        chartDrawer!!.start()
        emgComm.startSignalPhase()
    }

    private fun deactivateWorkers() {
        emgComm.stopSignalPhase()
        chartDrawer?.interrupt()
        chartDrawer = null
    }

    private fun setupDeviceInfo() {
        device_info.setText("vendor: ${portProvider.getVendorId()}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        deactivateWorkers()
    }

    inner class UsbPermissionReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            try {
                if (SerialPortProvider.ACTION_USB_PERMISSION == intent.action) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Toast.makeText(requireContext(), "permission granted", Toast.LENGTH_SHORT)
                            .show()
                        setupCommunication()
                        enableButtons()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun setupCommunication() {
        emgComm = EMGCommunication(requireContext(),
            portProvider.manager, portProvider.getOpened(),
            DeviceInfoPhaseListener(),
            SignalPhaseListener())
    }

    private fun enableButtons() {
        startButton.isEnabled = true
        stopButton.isEnabled = true
    }

    private fun textViewAppend(textView: TextView, text: String) {
        textView.setText(text)
    }

    inner class DeviceInfoPhaseListener: StringChunkHandler(18, 1) {
        @RequiresApi(Build.VERSION_CODES.P)
        override fun handleChunk(chunk: String) {
            textViewAppend(logcat, chunk)
            val macaddr = MacAddress.fromString(chunk)
        }

        override fun onRunError(e: java.lang.Exception?) {
            Toast.makeText(requireContext(), "Device List Error", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    inner class SignalPhaseListener: StringChunkHandler(4, batch) {
        override fun handleChunk(chunk: String) {
            textViewAppend(logcat, chunk)
            chartDrawer?.addSignal(chunk.toFloat())
        }

        override fun onRunError(e: java.lang.Exception?) {
            Toast.makeText(requireContext(), "Port Released", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }
}

abstract class StringChunkHandler(val unitSize: Int, val batch: Int): SerialInputOutputManager.Listener {
    private val sb = StringBuilder()
    private val limit = unitSize * batch

    override fun onNewData(data: ByteArray?) {
        if (data != null) {
            sb.append(String(data))
            if (sb.length >= limit) {
                val hasRemain = sb.length != limit
                val remains = sb.slice(limit until sb.length)
                val signals = sb.toString()
                for (i in 0 until batch) {
                    val signal = signals.slice(unitSize*i .. unitSize*i + unitSize - 1)
                    handleChunk(signal)
                }
                sb.clear()
                if (hasRemain)
                    sb.append(remains)
            }
        }
    }

    abstract fun handleChunk(chunk: String)
    abstract override fun onRunError(e: java.lang.Exception?)
}