package com.gbrain.humantohuman.fragment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.gbrain.humantohuman.R
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import kotlinx.android.synthetic.main.fragment_chart.*



class ChartFragment : Fragment() {
    // TODO: Rename and change types of parameters
    var isrunning = false
    var thread: Thread?= null

    fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_chart, container, false)

    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        setupButtons()

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

    private fun setupButtons() {

        startButton.setOnClickListener {

            startButton.text = "그래프 구현중"
            startButton.isClickable = false
            thread = ChartDrawer(30f,30f)
            thread?.start()

        }

        stopButton.setOnClickListener {
            //쓰레드를 죽인다
            //그래프를 없앤다
            startButton.text = "Start"
            startButton.isClickable = true
            thread?.interrupt()

            //initChart()
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
            var entries: ArrayList<Entry> = ArrayList()
            // Entry 배열 초기값 입력
            entries.add(Entry(0F , 0F))
            // 그래프 구현을 위한 LineDataSet 생성
            var dataset: LineDataSet = LineDataSet(entries, "input")
            // 그래프 data 생성 -> 최종 입력 데이터
            var data: LineData = LineData(dataset)
            // chart.xml에 배치된 lineChart에 데이터 연결
            return data
        }

        private fun updateChart(initTime : Long ,data: LineData) {
            val timeElapsed = System.currentTimeMillis() - initTime
            lineChart.setVisibleXRangeMaximum(max)
            lineChart.setVisibleXRangeMinimum(min)
            lineChart.moveViewToX(data.entryCount.toFloat()) //x값에따라 차트왼쪽으로이동
            data.addEntry(Entry(timeElapsed.toFloat()/100, newest!!), 0)
            data.notifyDataChanged()
            lineChart.notifyDataSetChanged()
            lineChart.invalidate()
        }

        private fun sleepUntilAwake(){ // 깨울때까지잔

        }

        private fun drawChart(){

            var data = chartInit()
            lineChart.data = data

            val initTime = System.currentTimeMillis()
            while (true) {
                //깨울때 까지 잔다다
                sleepUntilAwake()
                updateChart(initTime, data)
            }
        }

        fun notifySignal(signal: Float) {
            newest = signal
            //깨운
        }
    }
}