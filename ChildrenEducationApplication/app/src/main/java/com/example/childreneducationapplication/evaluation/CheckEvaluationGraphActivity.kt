package com.example.childreneducationapplication.evaluation

import android.os.Bundle
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.ObjectiveHistory
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import com.github.mikephil.charting.charts.BarChart
import com.github.mikephil.charting.components.AxisBase
import com.github.mikephil.charting.components.LimitLine
import com.github.mikephil.charting.components.XAxis
import com.github.mikephil.charting.data.BarData
import com.github.mikephil.charting.data.BarDataSet
import com.github.mikephil.charting.data.BarEntry
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

class CheckEvaluationGraphActivity : ComponentActivity() {

    private lateinit var barChartGrowth: BarChart
    private lateinit var barChartAccuracy: BarChart
    private var title = ""
    private var userId = 0
    private var targetPercentage = 80

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_check_evaluation_graph)

        barChartGrowth = findViewById(R.id.bar_chart_growth)
        barChartAccuracy = findViewById(R.id.bar_chart_accuracy)

        userId = intent.getIntExtra("user_id", 0)
        title = intent.getStringExtra("title") ?: ""
        targetPercentage = intent.getIntExtra("target_percentage", 80)

        val graphBackButton = findViewById<ImageView>(R.id.graph_back_button)
        graphBackButton.setOnClickListener {
            finish() // 현재 Activity 종료
        }

        // 데이터 가져오기 및 그래프에 매핑 (Coroutine 사용)
        lifecycleScope.launch {
            fetchAndMapDataToGraph()
        }
    }

    // 데이터를 가져오고 그래프에 매핑하는 함수
    private suspend fun fetchAndMapDataToGraph() {
        val timerGroupedData = getGroupedTimerObjectiveHistory()
        val orderGroupedData = getGroupedOrderObjectiveHistory() // 10일 이내의 데이터를 그룹화해서 가져오기

        // Combine both datasets by date
        val combinedGroupedData = mergeGroupedData(timerGroupedData, orderGroupedData)

        // Calculate combined percentages
        val combinedPassPercentages =
            calculatePercentagePerDate(combinedGroupedData) { historyList ->
                val totalCount = historyList.size
                val passCount = historyList.count { it.pass == true }
                if (totalCount == 0) 0.toFloat() else (passCount.toDouble() / totalCount * 100).toFloat()
            }

        val combinedCorrectionPercentages =
            calculatePercentagePerDate(combinedGroupedData) { historyList ->
                val totalCount = historyList.size
                val matchingCount = historyList.count { it.pass == (it.parents_approve ?: false) }
                if (totalCount == 0) 0.toFloat() else (matchingCount.toDouble() / totalCount * 100).toFloat()
            }

        // 그래프에 데이터 매핑
        setupBarChart(
            combinedPassPercentages,
            barChartGrowth,
            "하루 미션 달성도 비율",
            android.R.color.holo_green_light
        )
        setupBarChart(
            combinedCorrectionPercentages,
            barChartAccuracy,
            "하루 점검 정확도 비율",
            android.R.color.holo_blue_light
        )
    }

    // 두 개의 그룹 데이터를 합치는 함수
    private fun mergeGroupedData(
        timerGroupedData: Map<String, List<ObjectiveHistory>>,
        orderGroupedData: Map<String, List<ObjectiveHistory>>
    ): Map<String, List<ObjectiveHistory>> {
        val mergedData = mutableMapOf<String, MutableList<ObjectiveHistory>>()

        val calendar = Calendar.getInstance()
        for (i in 0 until 10) {
            val dateKey = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
            mergedData[dateKey] = mutableListOf() // Initialize with an empty list
            calendar.add(Calendar.DAY_OF_MONTH, -1)
        }

        // 타이머 데이터를 먼저 추가
        timerGroupedData.forEach { (date, historyList) ->
            mergedData[date] = historyList.toMutableList()
        }

        // 주문 데이터를 추가, 중복되는 날짜는 타이머 데이터에 추가
        orderGroupedData.forEach { (date, historyList) ->
            mergedData[date]?.addAll(historyList) ?: run {
                mergedData[date] = historyList.toMutableList()
            }
        }

        return mergedData.toSortedMap()
    }

    private fun setupBarChart(
        percentages: List<Float>,
        barChart: BarChart,
        title: String,
        color: Int
    ) {
        val entries = percentages.mapIndexed { index, percentage ->
            BarEntry(index.toFloat(), percentage)
        }

        val dataSet = BarDataSet(entries, title)
        dataSet.color = resources.getColor(color)

        val barData = BarData(dataSet)
        barChart.data = barData

        barChart.axisLeft.axisMinimum = 0f
        barChart.axisLeft.axisMaximum = 100f

        barChart.axisRight.isEnabled = false // 오른쪽 Y축 비활성화
        // X축 설정
        configureXAxis(barChart)

        // Description 라벨 제거
        barChart.description.isEnabled = false

        addLimitLineToYAxis(targetPercentage.toFloat(), barChart) // 기준선을 80으로 설정
        barChart.invalidate() // Refresh the chart
    }

    private fun addLimitLineToYAxis(limit: Float, barChart: BarChart) {
        val limitLine = LimitLine(limit, "기준 목표").apply {
            lineColor = resources.getColor(android.R.color.holo_red_light)
            lineWidth = 2f
            textColor = resources.getColor(android.R.color.holo_red_light)
            textSize = 12f
        }

        // Y축에 기준선 추가
        barChart.axisLeft.addLimitLine(limitLine)
        barChart.axisLeft.setDrawLimitLinesBehindData(false) // 데이터 뒤에 기준선을 그리도록 설정
    }

    private fun configureXAxis(barChart: BarChart) {
        val calendar = Calendar.getInstance()
        val labels = mutableListOf<String>()

        // Generate labels for the past 10 days from today
        for (i in 0 until 10) {
            val date = SimpleDateFormat("MM/dd", Locale.getDefault()).format(calendar.time)
            labels.add(date)
            calendar.add(Calendar.DAY_OF_MONTH, -1) // Move one day back
        }

        barChart.xAxis.apply {
            valueFormatter =
                MyXAxisValueFormatter(labels.reversed()) // Use labels in correct order without extra reversing
            position = XAxis.XAxisPosition.BOTTOM
            granularity = 1f
            labelCount = labels.size
            setDrawGridLines(false)
        }
    }

    // 최근 10일의 데이터를 그룹화해서 가져오는 함수
    private suspend fun getGroupedTimerObjectiveHistory(): Map<String, List<ObjectiveHistory>> {
        val response = supabaseClient.from("timer_objective_history")
            .select {
                filter {
                    // 최근 10일 데이터 필터링
                    eq("user_id", userId)
                    eq("title", title)
                    gte("created_at", getTenDaysAgoDateString())
                }
            }
            .decodeList<ObjectiveHistory>()

        // 날짜별로 그룹화
        return response.groupBy { it.created_at?.substring(0, 10) ?: "" }
    }

    // 최근 10일의 데이터를 그룹화해서 가져오는 함수
    private suspend fun getGroupedOrderObjectiveHistory(): Map<String, List<ObjectiveHistory>> {
        val response = supabaseClient.from("order_objective_history")
            .select {
                filter {
                    // 최근 10일 데이터 필터링
                    eq("user_id", userId)
                    eq("title", title)
                    gte("created_at", getTenDaysAgoDateString())
                }
            }
            .decodeList<ObjectiveHistory>()

        // 날짜별로 그룹화
        return response.groupBy { it.created_at?.substring(0, 10) ?: "" }
    }

    private fun getTenDaysAgoDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, -10)
        return SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(calendar.time)
    }

    // 날짜별로 TRUE 비율을 계산하는 함수
    private fun calculatePercentagePerDate(
        groupedData: Map<String, List<ObjectiveHistory>>,
        calculate: (List<ObjectiveHistory>) -> Float
    ): List<Float> {
        return groupedData.map { (_, historyList) ->
            calculate(historyList)
        }
    }
}


class MyXAxisValueFormatter(private val labels: List<String>) :
    com.github.mikephil.charting.formatter.ValueFormatter() {
    override fun getAxisLabel(value: Float, axis: AxisBase?): String {
        return labels[value.toInt()] // X축 레이블 반환
    }
}