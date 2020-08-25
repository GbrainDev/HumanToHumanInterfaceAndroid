package com.gbrain.humantohuman.chartdrawer

import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet

class ChartDrawer(val lineChart: LineChart,
                  val batch: Int,
                  val max: Float,
                  val min: Float) : Thread() {

    val lock = Object()
    val newestSignal = ArrayList<Float>(batch)

    override fun run() {
        try {
            drawChart()
        } catch (e: Exception) {
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