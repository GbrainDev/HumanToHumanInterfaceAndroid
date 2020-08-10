package com.gbrain.humantohuman.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.corndog.braoadcastprac.serialprotocol.SerialProtocol
import com.gbrain.humantohuman.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.fragment_chart.*

fun Fragment?.runOnUiThread(action: () -> Unit) {
    this ?: return
    if (!isAdded) return
    activity?.runOnUiThread(action)
}

class ChartFragment : Fragment() {
    var thread: Thread?= null
    lateinit var serialProtocol: SerialProtocol

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_chart, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()
    }

    private fun setupButtons() {
        startButton.setOnClickListener {
            startButton.text = "그래프 구현중"
            startButton.isClickable = false

            thread = ChartDrawer(30f,30f)
            thread?.start()
        }

        stopButton.setOnClickListener {
            startButton.text = "Start"
            startButton.isClickable = true

            thread?.interrupt()
        }
    }

    inner class ChartDrawer(val max: Float, val min: Float) : Thread() {
        var newest : Float = 0F

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

        private fun updateChart(initTime : Long ,data: LineData) {
            val timeElapsed = System.currentTimeMillis() - initTime
            lineChart.setVisibleXRangeMaximum(max)
            lineChart.setVisibleXRangeMinimum(min)
            lineChart.moveViewToX(data.entryCount.toFloat())

            data.addEntry(Entry(timeElapsed.toFloat()/100, newest), 0)
            data.notifyDataChanged()

            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }

        private fun sleepUntilAwake(){

        }

        private fun drawChart(){
            val data = chartInit()
            lineChart.data = data

            val initTime = System.currentTimeMillis()
            while (true) {
                sleepUntilAwake()
                updateChart(initTime, data)
            }
        }

        fun notifySignal(signal: Float) {
            newest = signal
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        thread?.interrupt()
    }

    override fun onDestroy() {
        super.onDestroy()
        thread?.interrupt()
    }

    override fun onPause() {
        super.onPause()
        thread?.interrupt()
    }
}