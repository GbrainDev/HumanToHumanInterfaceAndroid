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
    lateinit var thread : Thread

    fun Fragment?.runOnUiThread(action: () -> Unit) {
        this ?: return
        if (!isAdded) return // Fragment not attached to an Activity
        activity?.runOnUiThread(action)
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {

        }
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

        startButton.setOnClickListener {

            startButton.text = "그래프 구현중"
            startButton.isClickable = false
            thread = ThreadClass()
            thread.start()

        }

        stopButton.setOnClickListener {
            //쓰레드를 죽인다
            //그래프를 없앤다
            startButton.text = "Start"
            startButton.isClickable = true
            thread.interrupt()

            //initChart()
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

    fun initChart() {
        var entries: ArrayList<Entry> = ArrayList()
        // Entry 배열 초기값 입력
        entries.add(Entry(0F , 0F))
        // 그래프 구현을 위한 LineDataSet 생성
        var dataset: LineDataSet = LineDataSet(entries, "input")
        // 그래프 data 생성 -> 최종 입력 데이터
        var data: LineData = LineData(dataset)
        // chart.xml에 배치된 lineChart에 데이터 연결
        lineChart.data = data
        lineChart.notifyDataSetChanged()
    }

    inner class ThreadClass : Thread() {


        override fun run() {
            try {
                drawChart()
            } catch (e : InterruptedException){
                runOnUiThread {
                    Toast.makeText(context, "thread interrupted", Toast.LENGTH_SHORT).show()
                }
            }

        }

        private fun drawChart(){
            val input = Array<Double>(100,{Math.random()})
            // Entry 배열 생성
            var entries: ArrayList<Entry> = ArrayList()
            // Entry 배열 초기값 입력
            entries.add(Entry(0F , 0F))
            // 그래프 구현을 위한 LineDataSet 생성
            var dataset: LineDataSet = LineDataSet(entries, "input")
            // 그래프 data 생성 -> 최종 입력 데이터
            var data: LineData = LineData(dataset)
            // chart.xml에 배치된 lineChart에 데이터 연결
            lineChart.data = data

            //runOnUiThread {
            //    // 데이터업데이트를 이곳에서?
            //    //lineChart.animateXY(1, 1)
            //}

            for (i in 0 until input.size){

                sleep(200)
                lineChart.setVisibleXRangeMaximum(30f)
                lineChart.setVisibleXRangeMinimum(30f)
                lineChart.moveViewToX(data.entryCount.toFloat()) //x값에따라 차트왼쪽으로이동
                data.addEntry(Entry(i.toFloat(), input[i].toFloat()), 0)
                data.notifyDataChanged()
                lineChart.notifyDataSetChanged()
                lineChart.invalidate()
            }
            //startButton.text = "난수 생성 시작"
            //startButton.isClickable = true

        }
    }
}