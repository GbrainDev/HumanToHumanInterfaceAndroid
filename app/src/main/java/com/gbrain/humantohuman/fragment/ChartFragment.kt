package com.gbrain.humantohuman.fragment

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.AsyncTask
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import androidx.recyclerview.widget.LinearLayoutManager
import com.gbrain.humantohuman.chartdrawer.ChartDrawer
import com.gbrain.humantohuman.R
import com.gbrain.humantohuman.devicemacadapter.DeviceMacAdapter
import com.gbrain.humantohuman.emgcomm.EMGCommunication
import com.gbrain.humantohuman.serialprovider.SerialPortProvider
import com.hoho.android.usbserial.util.SerialInputOutputManager
import dataprotocol.typehandle.TypeHandler
import kotlinx.android.synthetic.main.fragment_chart.*
import java.util.logging.Handler

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return
    activity?.runOnUiThread(action)
}

class ChartFragment : Fragment(),
    SerialPortProvider.DeviceAttachedListener,
    SerialPortProvider.DeviceDettachedListener {

    lateinit var portProvider: SerialPortProvider
    lateinit var emgComm: EMGCommunication
    var chartDrawer: ChartDrawer? = null
    var batch = 10
    lateinit var deviceMacListAdapter: DeviceMacAdapter

    lateinit var navController: NavController

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        subscribeProvider()
        runReceiver()
    }

    private fun setupRecyclerView() {
        deviceMacListAdapter = DeviceMacAdapter()
        device_recycler.adapter = deviceMacListAdapter
        device_recycler.layoutManager = LinearLayoutManager(requireContext(),
                                            LinearLayoutManager.VERTICAL, false)
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
        setupRecyclerView()

        if (portProvider.isDeviceAllocated())
            portProvider.requestUsbPermission()
    }

    private fun setupButtons() {

        calibButton.setOnClickListener {
            calibButton.isEnabled = false
            activateSignalReceive()
            CalibrationWaitingTask().execute(5)
        }

        drawButton.setOnClickListener {
            drawButton.isEnabled = false
            drawButton.text = "그래프 구현중"
            stopButton.isEnabled = true
            activateChart()
        }

        stopButton.setOnClickListener {
            stopButton.isEnabled = false
            drawButton.text = "Start"
            deactivateWorkers()
        }
    }

    private fun activateSignalReceive() {
        emgComm.startSignalingPhase()
    }

    private fun activateChart() {
        chartDrawer = ChartDrawer(lineChart, batch, 30f, 30f)
        chartDrawer!!.start()
    }

    private fun deactivateWorkers() {
        chartDrawer?.interrupt()
        chartDrawer = null
        emgComm.stopSignalListening()
    }

    private fun setupDeviceInfo() {
        device_info.setText("master vendor: ${portProvider.getVendorId()}\n")
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
                        setupCommunication()
                        enableButtons()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }

    private fun setupCommunication() {
        emgComm = EMGCommunication(requireContext(), portProvider,
            DeviceInfoPhaseListener(),
            SignalPhaseListener())
    }

    private fun enableButtons() {
        calibButton.isEnabled = true
        drawButton.isEnabled = false
        stopButton.isEnabled = false
    }

    private fun textViewAppend(textView: TextView, text: String) {
        textView.setText(text)
    }

    inner class SignalPhaseListener: StringChunkHandler(5, batch) {

        init {
            addChunkHandler {data, handlingHint->
                if (data.contains("*")) {
                    val value = data.replace("*", "")
                    chartDrawer?.addSignal(value.toFloat())
                }
            }
        }

        override fun onRunError(e: java.lang.Exception?) {
            Toast.makeText(requireContext(), "Port Released", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    inner class DeviceInfoPhaseListener: StringChunkHandler(17, 1) {
        init {
            addChunkHandler { data, handlingHint ->
                runOnUiThread {
                    deviceMacListAdapter.addItem(data)
                }
            }
        }

        override fun onRunError(e: java.lang.Exception?) {
            Toast.makeText(requireContext(), "Port Released", Toast.LENGTH_SHORT).show()
            navController.popBackStack()
        }
    }

    inner class CalibrationWaitingTask: AsyncTask<Int, Int, Unit>() {
        override fun doInBackground(vararg sec: Int?): Unit {
            var waitSecond = sec[0]!!
            while (waitSecond-- > 0) {
                Thread.sleep(1000)
                calibButton.text = "Wait ${waitSecond}s"
            }
        }

        override fun onPostExecute(result: Unit?) {
            drawButton.isEnabled = true
            calibButton.text = "Calibrate"
        }
    }
}

abstract class StringChunkHandler(val unitSize: Int, val batch: Int): SerialInputOutputManager.Listener {
    private val sb = StringBuilder()
    private val limit = unitSize * batch
    protected var chunkHandler: TypeHandler<String>? = null

    override fun onNewData(data: ByteArray?) {
        if (data != null) {
            sb.append(String(data))
            if (sb.length >= limit) {
                val hasRemain = sb.length != limit
                val remains = sb.slice(limit until sb.length)
                val signals = sb.toString()
                for (i in 0 until batch) {
                    val signal = signals.slice(unitSize*i .. unitSize*i + unitSize - 1)
                    chunkHandler?.invoke(signal, 0)
                }
                sb.clear()
                if (hasRemain)
                    sb.append(remains)
            }
        }
    }

    protected fun addChunkHandler(handler: TypeHandler<String>) {
        this.chunkHandler = handler
    }

    abstract override fun onRunError(e: java.lang.Exception?)
}