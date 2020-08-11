package com.gbrain.humantohuman.fragment

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.usb.UsbDevice
import android.hardware.usb.UsbManager
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.corndog.braoadcastprac.serialprotocol.SerialConfig
import com.corndog.braoadcastprac.serialprotocol.SerialProtocol
import com.felhr.usbserial.UsbSerialDevice
import com.gbrain.humantohuman.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import dataprotocol.typehandle.ByteHandler
import kotlinx.android.synthetic.main.fragment_chart.*

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return
    activity?.runOnUiThread(action)
}

class ChartFragment : Fragment() {
    lateinit var navController: NavController

    lateinit var manager: UsbManager
    var device: UsbDevice? = null

    var chartDrawer: ChartDrawer? = null
    var protocol: SerialProtocol? = null
    var batch = 50

    private val ACTION_USB_PERMISSION = "com.android.example.USB_PERMISSION"
    private val usbEventReceiver = UsbEventReceiver()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            device = it.getParcelable(UsbManager.EXTRA_DEVICE)
            manager = requireContext().getSystemService(Context.USB_SERVICE) as UsbManager
        }
        runReceiver()
        requestUsbPermission()
    }

    private fun runReceiver() {
        val context = requireContext()
        val filter = IntentFilter().apply {
            addAction(ACTION_USB_PERMISSION)
            addAction(UsbManager.ACTION_USB_DEVICE_DETACHED)
        }

        try {
            context.unregisterReceiver(usbEventReceiver)
        } catch (e: Exception) {
        } finally {
            context.registerReceiver(usbEventReceiver, filter)
        }
    }

    private fun requestUsbPermission() {
        val permissionIntent = PendingIntent.getBroadcast(requireContext(), 0,
            Intent(ACTION_USB_PERMISSION), 0)
        manager.requestPermission(device, permissionIntent)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    private fun allocProtocol(doHandShake: Boolean) {
        val connection = manager.openDevice(device)
        val serialPort = UsbSerialDevice.createUsbSerialDevice("ch34x", device, connection, 0)

        serialPort?.also {serialPort->
            val serialConfig = SerialConfig.Builder().baudRate(115200).commit()

            protocol = SerialProtocol(
                requireContext(),
                serialPort,
                serialConfig,
                object: ByteHandler {
                    override fun handle(data: Byte, handlingHint: Int) {
                        chartDrawer?.addSignal(-data.toFloat())
                    }
                }, batch, 50)

            if (doHandShake) {
                protocol?.handShake()
                enableButtons()
            }
        }
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        navController = Navigation.findNavController(view)
        setupButtons()
    }

    private fun enableButtons() {
        startButton.isEnabled = true
        stopButton.isEnabled = true
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            startButton.text = "그래프 구현중"
            startButton.isClickable = false

            chartDrawer = ChartDrawer(batch, 30f,30f)
            chartDrawer?.start()
            allocProtocol(false)
            protocol?.start()
        }

        stopButton.setOnClickListener {
            startButton.text = "Start"
            startButton.isClickable = true

            protocol?.interrupt()
            protocol = null
            chartDrawer?.interrupt()
            chartDrawer = null
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        interruptWorker()
    }

    private fun interruptWorker() {
        chartDrawer?.interrupt()
        chartDrawer = null
        protocol?.interrupt()
        protocol = null
    }

    inner class ChartDrawer(val batch: Int, val max: Float, val min: Float) : Thread() {
        val lock = Object()
        val newestSignal =  ArrayList<Float>(batch)

        override fun run() {
            try {
                drawChart()
            } catch (e : InterruptedException){
                runOnUiThread {
                    Toast.makeText(context, "chart interrupted", Toast.LENGTH_SHORT).show()
                }
            }
        }

        private fun initChartData(): LineData {
            val entries: ArrayList<Entry> = ArrayList()
            var dataSet = LineDataSet(entries, "input")

            entries.add(Entry(0F , 0F))

            dataSet.setDrawValues(false)
            dataSet.setDrawCircles(false)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.cubicIntensity = 0.2f

            return LineData(dataSet)
        }

        private fun updateChart(initTime : Long , data: LineData) {
            //val timeElapsed = System.currentTimeMillis() - initTime
            lineChart.setVisibleXRangeMaximum(max)
            lineChart.setVisibleXRangeMinimum(min)
            lineChart.moveViewToX((data.entryCount.toFloat()))

            newestSignal.forEachIndexed {index, value->
                data.addEntry(Entry((data.entryCount.toFloat()/10), value), 0)
            }
            data.notifyDataChanged()

            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }

        private fun drawChart(){
            val initTime = System.currentTimeMillis()
            val data = initChartData()
            lineChart.data = data

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

        fun addSignal(signal: Float) {
            synchronized(lock) {
                newestSignal.add(signal)
                lock.notifyAll()
            }
        }
    }

    inner class UsbEventReceiver: BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent) {
            try {
                if (ACTION_USB_PERMISSION == intent.action) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        device?.apply {
                            allocProtocol(true)
                        }
                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                        interruptWorker()
                        navController.popBackStack()
                    }
                }
            } catch (e: Exception) {
            }
        }
    }
}