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
import android.widget.TextView
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.navigation.NavController
import androidx.navigation.Navigation
import com.gbrain.humantohuman.R
import com.gbrain.humantohuman.serialprovider.SerialPortProvider
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.hoho.android.usbserial.driver.UsbSerialPort
import com.hoho.android.usbserial.driver.UsbSerialProber
import com.hoho.android.usbserial.util.SerialInputOutputManager
import kotlinx.android.synthetic.main.fragment_chart.*
import java.util.concurrent.Executors

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return
    activity?.runOnUiThread(action)
}

class ChartFragment : Fragment(),
    SerialPortProvider.DeviceAttachedListener,
    SerialPortProvider.DeviceDettachedListener,
    SerialInputOutputManager.Listener {

    private lateinit var portProvider: SerialPortProvider
    lateinit var navController: NavController

    var chartDrawer: ChartDrawer? = null
    var batch = 30

    var port: UsbSerialPort? = null

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
        Toast.makeText(requireContext(), "Device Detached", Toast.LENGTH_SHORT).show()
        Thread.sleep(3000)
        interruptWorker()
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
            initiateWorker()
        }

        stopButton.setOnClickListener {
            startButton.text = "Start"
            startButton.isClickable = true
            interruptWorker()
        }
    }

    private fun initiateWorker() {
        allocProtocol(false)
        chartDrawer = ChartDrawer(batch, 30f, 30f)
        chartDrawer!!.start()
    }

    private fun interruptWorker() {
        chartDrawer?.interrupt()
        chartDrawer = null
        port?.close()
    }

    private fun setupDeviceInfo() {
        device_info.setText("vendor: ${portProvider.getVendorId()}")
    }

    override fun onDestroyView() {
        super.onDestroyView()
        interruptWorker()
    }

    inner class ChartDrawer(val batch: Int, val max: Float, val min: Float) : Thread() {

        val lock = Object()

        val newestSignal = ArrayList<Float>(batch)
        override fun run() {
            try {
                drawChart()
            } catch (e: InterruptedException) {
                runOnUiThread {
                    Toast.makeText(context, "chart interrupted", Toast.LENGTH_SHORT).show()
                }
            }
        }
        private fun initChartData(): LineData {
            val entries: ArrayList<Entry> = ArrayList()
            entries.add(Entry(0F, 0F))
            val dataSet = LineDataSet(entries, "input")

            dataSet.setDrawValues(false)
            dataSet.setDrawCircles(false)
            dataSet.mode = LineDataSet.Mode.CUBIC_BEZIER
            dataSet.cubicIntensity = 0.2f

            return LineData(dataSet)
        }

        private fun updateChart(data: LineData) {
            lineChart.setVisibleXRangeMaximum(max)
            lineChart.setVisibleXRangeMinimum(min)
            lineChart.moveViewToX(data.entryCount.toFloat())

            newestSignal.forEach { value ->
                data.addEntry(Entry((data.entryCount.toFloat() / 10), value), 0)
            }
            data.notifyDataChanged()

            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }

        private fun drawChart() {

            val data = initChartData()
            lineChart.data = data

            while (true) {
                synchronized(lock) {
                    if (newestSignal.size < batch) {
                        lock.wait()
                    } else {
                        updateChart(data)
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

    inner class UsbPermissionReceiver : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent) {
            try {
                if (SerialPortProvider.ACTION_USB_PERMISSION == intent.action) {
                    if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                        Toast.makeText(requireContext(), "permission granted", Toast.LENGTH_SHORT)
                            .show()

                        allocProtocol(true)

                    } else if (UsbManager.ACTION_USB_DEVICE_DETACHED == intent.action) {
                        interruptWorker()
                        navController.popBackStack()
                    }
                }
            } catch (e: Exception) {
            }
        }

    }

    private fun allocProtocol(isFirstAllocation: Boolean) {
        val drivers = UsbSerialProber.getDefaultProber().findAllDrivers(portProvider.manager)
        val driver = drivers.get(0)
        val connection = portProvider.manager.openDevice(driver.device)
        val port: UsbSerialPort = driver.ports.get(0)
        port.open(connection)
        port.setParameters(115200, UsbSerialPort.DATABITS_8,
                            UsbSerialPort.STOPBITS_1, UsbSerialPort.PARITY_NONE)

        val iomanager = SerialInputOutputManager(port, this)
        Executors.newSingleThreadExecutor().submit(iomanager)
        enableButtons()
    }

    override fun onRunError(e: java.lang.Exception?) {
        Toast.makeText(requireContext(), "Error", Toast.LENGTH_SHORT).show()
    }

    private fun textViewAppend(textView: TextView, text: String) {
        textView.setText(text)
    }

    private fun enableButtons() {
        startButton.isEnabled = true
        stopButton.isEnabled = true
    }

    override fun onNewData(data: ByteArray?) {
        textViewAppend(logcat, String(data!!))
    }
}
