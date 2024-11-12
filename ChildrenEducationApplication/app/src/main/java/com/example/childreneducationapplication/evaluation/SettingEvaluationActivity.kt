package com.example.childreneducationapplication.evaluation

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.check.parents.OrderParentsActivity
import com.example.childreneducationapplication.check.parents.TimerParentsActivity
import com.example.childreneducationapplication.entity.ObjectiveHistory
import com.example.childreneducationapplication.entity.SelfEvaluation
import com.example.childreneducationapplication.entity.SettingObjective
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class SettingEvaluationActivity : ComponentActivity() {

    private var objectiveStudy: String? = null
    private var objectiveLiving: String? = null
    private lateinit var objectiveTitle: TextView
    private lateinit var missionComplishPercent: TextView
    private lateinit var missionCorrectionPercent: TextView
    private lateinit var todayAccomplish: TextView
    private lateinit var todayAccuracy: TextView
    private lateinit var goalPercentSpinner: Spinner
    private lateinit var todayFeeling: EditText
    private lateinit var parentsFeeling: EditText
    var userId: Int = 0
    var title = ""
    var role = ""

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_setting_evaluation)

        userId = intent.getIntExtra("user_id", 0)
        objectiveStudy = intent.getStringExtra("objective_study")
        objectiveLiving = intent.getStringExtra("objective_living")
        role = intent.getStringExtra("role") ?: ""

        objectiveTitle = findViewById(R.id.objective_title_order)
        missionComplishPercent = findViewById(R.id.misson_complish_percent)
        missionCorrectionPercent = findViewById(R.id.misson_correction_percent)
        todayAccomplish = findViewById(R.id.today_accomplish)
        todayAccuracy = findViewById(R.id.today_accuracy)
        todayFeeling = findViewById(R.id.today_feeling)
        parentsFeeling = findViewById(R.id.parents_feeling)

        settingObjectiveTitle()

        // 버튼 클릭 이벤트 설정
        val timerHistoryButton = findViewById<LinearLayout>(R.id.timer_history_button)
        val checklistHistoryButton = findViewById<LinearLayout>(R.id.checklist_history_button)
        val graphButton = findViewById<LinearLayout>(R.id.button_check_graph)
        val saveButton = findViewById<LinearLayout>(R.id.evaluation_save_button)

        lifecycleScope.launch {
            val timerHistoryList = fetchObjectiveHistoryTimer()
            val orderHistoryList = fetchObjectiveHistoryOrder()

            // 두 목록을 합침
            val combinedHistoryList = timerHistoryList + orderHistoryList
            calculateAndDisplayPercentages(combinedHistoryList)
        }

        lifecycleScope.launch {
            spinnerObjective()
            fetchTodayImpressions()
        }

        timerHistoryButton.setOnClickListener {
            // TimerHistoryActivity 호출
            val intent = Intent(this, TimerParentsActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("role", "history")
            startActivity(intent)
        }

        checklistHistoryButton.setOnClickListener {
            // OrderHistoryActivity 호출
            val intent = Intent(this, OrderParentsActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("role", "history")
            startActivity(intent)
        }

        graphButton.setOnClickListener {
            val selectedGoalPercent =
                goalPercentSpinner.selectedItem.toString().replace("%", "").toInt()
            val intent = Intent(this, CheckEvaluationGraphActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("title", title)
            intent.putExtra("target_percentage", selectedGoalPercent)
            startActivity(intent)
        }

        if (role == "학생") {
            parentsFeeling.isEnabled = false
        } else if (role == "보호자") {
            todayFeeling.isEnabled = false
        }

        saveButton.setOnClickListener {
            evaluationSave()
            showAlert("저장 완료")
        }

    }

    private suspend fun spinnerObjective() {
        goalPercentSpinner = findViewById(R.id.goal_percent)
        val percentOptions = arrayOf("80%", "90%", "100%")
        val adapter = ArrayAdapter(this, R.layout.spinner_item, percentOptions)
        adapter.setDropDownViewResource(R.layout.spinner_item)
        goalPercentSpinner.adapter = adapter

        val initialPercent = fetchImpression()?.target_percent ?: 80
        val initialIndex = when (initialPercent) {
            80 -> 0
            90 -> 1
            100 -> 2
            else -> 0
        }

        goalPercentSpinner.setSelection(initialIndex)
        goalPercentSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>,
                view: View?,
                position: Int,
                id: Long
            ) {
                val selectedPercent = percentOptions[position]
            }

            override fun onNothingSelected(parent: AdapterView<*>) {
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun settingObjectiveTitle() {
        val userId = intent.getIntExtra("user_id", 0)

        // 데이터를 불러옴
        lifecycleScope.launch {
            val objectives = fetchTitles(userId)
            objectives?.let {
                // study_objective 및 living_objective 값을 가져옴
                val studyObjective0 = it.study_objective0 ?: ""
                val studyObjective1 = it.study_objective1 ?: ""
                val studyObjective2 = it.study_objective2 ?: ""
                val studyFlag = it.study_flag
                val presidentStudyObjective = when (studyFlag) {
                    0 -> studyObjective0
                    1 -> studyObjective1
                    2 -> studyObjective2
                    else -> ""
                }

                val livingObjective0 = it.living_objective0 ?: ""
                val livingObjective1 = it.living_objective1 ?: ""
                val livingObjective2 = it.living_objective2 ?: ""
                val livingFlag = it.living_flag
                val presidentLivingObjective = when (livingFlag) {
                    0 -> livingObjective0
                    1 -> livingObjective1
                    2 -> livingObjective2
                    else -> ""
                }

                // 드릴다운 다이얼로그 설정
                val items = arrayOf(presidentStudyObjective, presidentLivingObjective).filter {
                    it.isNotBlank()
                }.toTypedArray()

                val builder = AlertDialog.Builder(this@SettingEvaluationActivity)
                builder.setTitle("목표를 선택해주세요")
                builder.setItems(items) { dialog, which ->
                    // 선택된 값을 TextView에 설정
                    title = items[which]
                    objectiveTitle.text = items[which]

                    // title에 따라 데이터 가져와서 계산
                    lifecycleScope.launch {
                        val timerHistoryList = fetchObjectiveHistoryTimer()
                        val orderHistoryList = fetchObjectiveHistoryOrder()

                        // 두 목록을 합침
                        val combinedHistoryList = timerHistoryList + orderHistoryList
                        calculateAndDisplayPercentages(combinedHistoryList)
                    }
                }
                // 다이얼로그 표시
                builder.show()
            }
        }
    }

    private suspend fun fetchObjectiveHistoryTimer(): List<ObjectiveHistory> {
        val result = supabaseClient
            .from("timer_objective_history").select {
                filter {
                    eq("user_id", userId)
                    eq("title", title)
                }
            }
            .decodeList<ObjectiveHistory>()
        return result
    }

    private suspend fun fetchObjectiveHistoryOrder(): List<ObjectiveHistory> {
        val result = supabaseClient
            .from("order_objective_history").select {
                filter {
                    eq("user_id", userId)
                    eq("title", title)
                }
            }
            .decodeList<ObjectiveHistory>()
        return result
    }

    @RequiresApi(Build.VERSION_CODES.O)
    @SuppressLint("DefaultLocale")
    private fun calculateAndDisplayPercentages(historyList: List<ObjectiveHistory>) {
        if (historyList.isEmpty()) {
            // 리스트가 비어있으면 0%로 설정
            missionComplishPercent.text = "0%"
            missionCorrectionPercent.text = "0%"
            todayAccomplish.text = "0%"
            todayAccuracy.text = "0%"
            return
        }

        // 현재 시간 (KST) 기준으로 오늘의 데이터만 필터링
        val nowKST = ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
        val todayStart = nowKST.toLocalDate().atStartOfDay(ZoneId.of("Asia/Seoul"))
        val todayEnd = todayStart.plusDays(1)

        val todayHistoryList = historyList.filter {
            val createdAtKST =
                it.created_at?.let { it1 -> convertToKST(it1) } // Assuming created_at is in UTC
            createdAtKST!!.isAfter(todayStart) && createdAtKST!!.isBefore(todayEnd)
        }

        if (todayHistoryList.isEmpty()) {
            todayAccomplish.text = "0%"
            todayAccuracy.text = "0%"
        } else {
            // pass 값이 true인 오늘 항목 계산
            val todayPassCount = todayHistoryList.count { it.pass == true }
            val todayPassPercentage = (todayPassCount.toDouble() / todayHistoryList.size) * 100

            // pass와 parents_approve가 같은 오늘 항목 계산
            val todayMatchingCount =
                todayHistoryList.count { it.pass == (it.parents_approve ?: false) }
            val todayMatchingPercentage =
                (todayMatchingCount.toDouble() / todayHistoryList.size) * 100

            // 오늘의 비율을 TextView에 설정
            todayAccomplish.text = String.format("%.2f%%", todayPassPercentage)
            todayAccuracy.text = String.format("%.2f%%", todayMatchingPercentage)
        }

        // pass 값이 true인 전체 항목 계산
        val passCount = historyList.count { it.pass == true }
        val passPercentage = (passCount.toDouble() / historyList.size) * 100

        // pass와 parents_approve가 같은 전체 항목 계산
        val matchingCount = historyList.count { it.pass == (it.parents_approve ?: false) }
        val matchingPercentage = (matchingCount.toDouble() / historyList.size) * 100

        // 전체 비율을 TextView에 설정
        missionComplishPercent.text = String.format("%.2f%%", passPercentage)
        missionCorrectionPercent.text = String.format("%.2f%%", matchingPercentage)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun convertToKST(utcTime: String): ZonedDateTime {
        // Assuming created_at is in the format of UTC datetime string like "2024-10-13T14:51:48.435685Z"
        val utcZonedDateTime = ZonedDateTime.parse(utcTime)
        return utcZonedDateTime.withZoneSameInstant(ZoneId.of("Asia/Seoul"))
    }

    suspend fun fetchTitles(userId: Int): SettingObjective? {
        val existingRecord = supabaseClient.from("setting_objective").select {
            filter {
                eq("user_id", userId)
            }
        }.decodeList<SettingObjective>()

        return if (existingRecord.isNotEmpty()) {
            existingRecord[0]
        } else {
            null
        }
    }

    private fun evaluationSave() {
        val selectedGoalPercent =
            goalPercentSpinner.selectedItem.toString().replace("%", "").toInt()

        // Get user input from EditTexts
        val missionImpression = (todayFeeling.text ?: "").toString()
        val parentsImpression = (parentsFeeling.text ?: "").toString()

        CoroutineScope(Dispatchers.IO).launch {
            // userId로 기존 데이터가 있는지 확인
            val existingRecords = supabaseClient.from("self_evaluation")
                .select {
                    filter { eq("user_id", userId) }
                }.decodeList<SelfEvaluation>()

            if (existingRecords.isNotEmpty()) {
                // 기존 데이터가 있으면 삭제
                supabaseClient.from("self_evaluation")
                    .delete {
                        filter { eq("user_id", userId) }
                    }
            }

            // 새 데이터 삽입
            val data = mapOf(
                "user_id" to userId,
                "target_percent" to selectedGoalPercent,
                "mission_impression" to missionImpression,
                "parents_impression" to parentsImpression
            )

            supabaseClient.from("self_evaluation")
                .insert(data)

        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getCurrentKSTTime(): String {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        val kstZone = ZoneId.of("Asia/Seoul")
        return ZonedDateTime.now(kstZone).format(formatter)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun fetchTodayImpressions() {
        val todayDate =
            getCurrentKSTTime().substring(0, 10) // Get the current KST date (yyyy-MM-dd)

        // Fetch today's impressions from the database
        val todayImpression = fetchImpressionForDate(todayDate)
        todayImpression?.let {
            // If today's impressions exist, set them in the EditText fields
            todayFeeling.setText(it.mission_impression ?: "")
            parentsFeeling.setText(it.parents_impression ?: "")
        }
    }

    private suspend fun fetchImpressionForDate(date: String): SelfEvaluation? {
        // Fetch from the database based on user_id and today's date
        val result = supabaseClient
            .from("self_evaluation")
            .select {
                filter {
                    eq("user_id", userId)
                    gte("created_at", "$date 00:00:00")
                    lte("created_at", "$date 23:59:59")
                }
            }
            .decodeList<SelfEvaluation>()

        return if (result.isNotEmpty()) {
            result[0] // Return the first record for today
        } else {
            null
        }
    }

    private suspend fun fetchImpression(): SelfEvaluation? {
        // Fetch from the database based on user_id and today's date
        val result = supabaseClient
            .from("self_evaluation")
            .select {
                filter {
                    eq("user_id", userId)
                }
            }
            .decodeList<SelfEvaluation>()

        return if (result.isNotEmpty()) {
            result[0] // Return the first record for today
        } else {
            null
        }
    }

    private fun showAlert(message: String) {
        // Show an alert dialog with the provided message
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}