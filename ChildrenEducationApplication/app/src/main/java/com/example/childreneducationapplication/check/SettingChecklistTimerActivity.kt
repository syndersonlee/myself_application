package com.example.childreneducationapplication.check

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.SettingObjective
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class SettingChecklistTimerActivity : ComponentActivity() {
    private var objectiveStudy: String? = null
    private var objectiveLiving: String? = null
    private lateinit var objectiveTitleTimer: TextView
    private lateinit var intervalMinutesSpinner: Spinner

    @SuppressLint("CutPasteId")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_setting_checklist_timer)

        objectiveTitleTimer = findViewById(R.id.objective_title_timer)

        settingObjectiveTitle(objectiveTitleTimer)

        val userId = intent.getIntExtra("user_id", 0)
        val role = intent.getStringExtra("role")
        objectiveStudy = intent.getStringExtra("objective_study")
        objectiveLiving = intent.getStringExtra("objective_living")

        objectiveTitleTimer.setOnClickListener {
            showObjectiveSelectionDialog()
        }

        intervalMinutesSpinner = findViewById(R.id.interval_minute)
        val minutesOptions = listOf(10, 15, 20, 30, 35, 40, 45, 50, 55, 60)
        val adapterMinutes = ArrayAdapter(this, android.R.layout.simple_spinner_item, minutesOptions)
        adapterMinutes.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        intervalMinutesSpinner.adapter = adapterMinutes
        val defaultValue = 10
        val defaultPosition = minutesOptions.indexOf(defaultValue)
        intervalMinutesSpinner.setSelection(defaultPosition)

        val visibleSecondsSpinner = findViewById<Spinner>(R.id.visible_seconds)
        val secondsOptions = listOf(10, 20, 30, 60, 120, 300, 600)
        val adapterSeconds = ArrayAdapter(this, android.R.layout.simple_spinner_item, secondsOptions)
        adapterSeconds.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        visibleSecondsSpinner.adapter = adapterSeconds
        visibleSecondsSpinner.setSelection(0)

        // interval_max_execution 값 업데이트 로직 추가
        val intervalMaxExecutionTextView = findViewById<TextView>(R.id.interval_max_execution)

        fun updateIntervalMaxExecution() {
            val intervalMinutes = intervalMinutesSpinner.selectedItem.toString().toInt()
            val visibleSeconds = visibleSecondsSpinner.selectedItem.toString().toInt()

            // 계산식: interval_minute * 60 / visible_seconds
            val result = intervalMinutes * 60 / visibleSeconds

            // TextView에 값 설정
            intervalMaxExecutionTextView.text = result.toString()
        }

        intervalMinutesSpinner.onItemSelectedListener =
            object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: AdapterView<*>?,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    updateIntervalMaxExecution() // interval_minute 변경 시 호출
                }

                override fun onNothingSelected(parent: AdapterView<*>?) {}
            }

        visibleSecondsSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?,
                view: View?,
                position: Int,
                id: Long
            ) {
                updateIntervalMaxExecution() // visible_seconds 변경 시 호출
            }

            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }


        val startButton = findViewById<LinearLayout>(R.id.checklist_timer_start_button)
        startButton.setOnClickListener {
            val checklistContent = findViewById<EditText>(R.id.checklist_content).text.toString()
            val intervalMinutes =
                findViewById<Spinner>(R.id.interval_minute).selectedItem.toString()
            val visibleSeconds = findViewById<Spinner>(R.id.visible_seconds).selectedItem.toString()
            val intervalMaxExecution =
                findViewById<TextView>(R.id.interval_max_execution).text.toString()

            if (checklistContent.isBlank() || intervalMinutes.isBlank() || visibleSeconds.isBlank() || intervalMaxExecution.isBlank()) {
                showAlertDialog("모든 값을 입력해주세요.", "빈 칸에 모든 값을 넣어주세요.")
            } else if (!isNumeric(intervalMinutes) || !isNumeric(visibleSeconds) || !isNumeric(
                    intervalMaxExecution
                )
            ) {
                showAlertDialog("숫자를 입력해 주세요", "숫자가 아닌 문자가 입력되었습니다.")

            } else {

                val intent = Intent(this, ChecklistTimerBroadcast::class.java)
                intent.putExtra("user_id", userId)
                intent.putExtra("EXTRA_TITLE", objectiveTitleTimer.text.toString())
                intent.putExtra("EXTRA_CHECKLIST_CONTENT", checklistContent)
                intent.putExtra("EXTRA_INTERVAL_MINUTE", intervalMinutes.toInt())
                intent.putExtra("EXTRA_VISIBLE_SECONDS", visibleSeconds.toInt())
                intent.putExtra("EXTRA_MAX_EXECUTION", intervalMaxExecution.toInt())
                startActivity(intent)
            }

        }
    }


    fun settingObjectiveTitle(objectiveTitleTimer: TextView) {
        val userId = intent.getIntExtra("user_id", 0)

        // 데이터를 불러옴
        lifecycleScope.launch {
            val objectives = fetchTitles(userId)
            objectives?.let {
                // study_objective0 및 living_objective0 값을 가져옴
                val studyObjective0 = it.study_objective0 ?: ""
                val studyObjective1 = it.study_objective1 ?: ""
                val studyObjective2 = it.study_objective0 ?: ""
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
                val builder = AlertDialog.Builder(this@SettingChecklistTimerActivity)
                builder.setTitle("목표를 선택해주세요")
                builder.setItems(items) { dialog, which ->
                    // 선택된 값을 TextView에 설정
                    objectiveTitleTimer.text = items[which]
                }
                // 다이얼로그 표시
                builder.show()
            }
        }
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

    // 입력이 숫자인지 확인하는 함수
    private fun isNumeric(input: String): Boolean {
        return input.toIntOrNull() != null
    }

    // AlertDialog를 표시하는 함수
    private fun showAlertDialog(title: String, message: String) {
        val builder = AlertDialog.Builder(this)
        builder.setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK") { dialog, _ ->
                dialog.dismiss()
            }
        builder.create().show()
    }

    private fun showObjectiveSelectionDialog() {
        // 선택 옵션 목록 생성
        val options =
            arrayOf(objectiveStudy ?: "", objectiveLiving ?: "").filter {
                it.isNotBlank()
            }.toTypedArray()

        AlertDialog.Builder(this)
            .setTitle("Select Objective")
            .setItems(options) { _, which ->
                // 선택된 옵션에 따라 TextView 업데이트
                objectiveTitleTimer.text = options[which]
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
}