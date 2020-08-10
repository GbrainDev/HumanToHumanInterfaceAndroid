package com.gbrain.humantohuman.fragment

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbDeviceConnection
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.corndog.braoadcastprac.serialprotocol.SerialConfig
import com.corndog.braoadcastprac.serialprotocol.SerialProtocol
import com.felhr.usbserial.UsbSerialDevice
import com.gbrain.humantohuman.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dataprotocol.typehandle.ShortHandler
import kotlinx.android.synthetic.main.fragment_chart.*

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return
    activity?.runOnUiThread(action)
}

class ChartFragment : Fragment() {

    lateinit var manager: UsbManager
    var device: UsbDevice? = null
    var serialPort: UsbSerialDevice? = null
    var connection: UsbDeviceConnection? = null

    private lateinit var protocol: SerialProtocol
    private var batch = 25

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private val usbReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            try {
                if (ACTION_USB_PERMISSION == intent.action) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            allocProtocol(true)
                        }
                    } else {
                        Log.d("TAG", "permission denied for device $device")
                    }
                }
            } catch (e: java.lang.Exception) {
                val sb = StringBuilder()
                e.stackTrace.forEach { it ->
                    sb.appendln(it.toString())
                    sb.appendln(e::class.java)
                }
            }
        }
    }

    var chartDrawer: ChartDrawer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            device = it.getParcelable<UsbDevice>(UsbManager.EXTRA_DEVICE)
            manager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
        }
        runReceiver()
        requestPermission()
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    private fun runReceiver() {
        val filter = IntentFilter(ACTION_USB_PERMISSION)
        val context = requireContext()
        try {
            context.unregisterReceiver(usbReceiver)
        } catch (e: Exception) {
            e.printStackTrace()
        }
        context.registerReceiver(usbReceiver, filter)
    }

    private fun requestPermission() {
        val permissionIntent = PendingIntent.getBroadcast(requireContext(),
            0, Intent(ACTION_USB_PERMISSION), 0)
        manager.requestPermission(device, permissionIntent)
    }

    private fun allocProtocol(doHandShake: Boolean) {
        connection = manager.openDevice(device)
        serialPort = UsbSerialDevice.createUsbSerialDevice("cdc", device, connection, 1)
        serialPort?.also { serialPort ->
            val serialConfig = SerialConfig.getDefaultConfig()
            protocol = SerialProtocol(requireContext(),
                serialPort,
                serialConfig,
                object: ShortHandler {
                    override fun handle(data: Short, handlingHint: Int) {
                        chartDrawer?.notifySignal(data.toFloat())
                    }
                },
                batch, 20)

            if (doHandShake)
                protocol.handShake()
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            startButton.text = "그래프 구현중"
            startButton.isClickable = false

            chartDrawer = ChartDrawer(batch, 30f,30f)
            chartDrawer?.start()

            allocProtocol(false)
            protocol.start()
        }

        stopButton.setOnClickListener {
            startButton.text = "Start"
            startButton.isClickable = true

            protocol.interrupt()
            chartDrawer?.interrupt()
        }
    }

    inner class ChartDrawer(val batch: Int, val max: Float, val min: Float) : Thread() {
        val lock = Object()
        val newestSignal =  ArrayList<Float>(batch)

        override fun run() {
            try {
                drawChart()
            } catch (e : InterruptedException){
                runOnUiThread {
                    Toast.makeText(context, "thread interrupted", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun chartInit(): LineData {
            val entries: ArrayList<Entry> = ArrayList()
            entries.add(Entry(0F , 0F))
            return LineData(LineDataSet(entries, "input"))
        }

        private fun updateChart(initTime : Long , data: LineData) {
            val timeElapsed = System.currentTimeMillis() - initTime
            lineChart.setVisibleXRangeMaximum(max)
            lineChart.setVisibleXRangeMinimum(min)
            lineChart.moveViewToX(timeElapsed.toFloat()/10)

            newestSignal.forEach {
                data.addEntry(Entry(timeElapsed.toFloat()/10, it), 0)
            }
            data.notifyDataChanged()

            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }

        private fun drawChart(){
            val data = chartInit()
            lineChart.data = data

            val initTime = System.currentTimeMillis()
            while (true) {
                synchronized(lock) {
                    if (newestSignal.size < batch) {
                        lock.wait()
                    }
                    else {
                        updateChart(initTime, data)
                        newestSignal.clear()
                    }
                }
            }
        }

        fun notifySignal(signal: Float) {
            synchronized(lock) {
                newestSignal.add(signal)
                lock.notifyAll()
            }
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        chartDrawer?.interrupt()
    }

    override fun onDestroy() {
        super.onDestroy()
        chartDrawer?.interrupt()
    }

    override fun onPause() {
        super.onPause()
        chartDrawer?.interrupt()
    }
}