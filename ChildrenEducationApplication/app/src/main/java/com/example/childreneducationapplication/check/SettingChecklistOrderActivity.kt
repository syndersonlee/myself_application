package com.example.childreneducationapplication.check

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.SettingObjective
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class SettingChecklistOrderActivity : ComponentActivity() {
    private var counter = 1
    private var userId = 0


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_setting_checklist_order)

        userId = intent.getIntExtra("user_id", 0)

        val objectiveTitleOrder = findViewById<TextView>(R.id.objective_title_order)
        settingObjectiveTitle(objectiveTitleOrder)

        // checkbox_order_add ImageView와 root LinearLayout을 찾음
        val checkboxOrderAdd = findViewById<ImageView>(R.id.checkbox_order_add)
        val rootLayout = findViewById<LinearLayout>(R.id.root_linear_layout)
        val checklistTotalStep = findViewById<TextView>(R.id.checklist_total_step)

        // ImageView 클릭 시 새로운 LinearLayout 추가
        checkboxOrderAdd.setOnClickListener {
            mapNonEmptyFields(rootLayout, checklistTotalStep)
            addNewField(rootLayout)
        }

        val startButton = findViewById<LinearLayout>(R.id.checklist_order_start)
        startButton.setOnClickListener {
            sendDataToChecklistOrderBroadCast(rootLayout)
        }
    }

    private fun settingObjectiveTitle(objectiveTitleTimer: TextView) {
        lifecycleScope.launch {
            val objectives = fetchTitles()
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
                val builder = AlertDialog.Builder(this@SettingChecklistOrderActivity)
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

    private suspend fun fetchTitles(): SettingObjective? {
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

    // 새로운 LinearLayout을 추가하는 함수
    private fun addNewField(rootLayout: LinearLayout) {
        // LayoutInflater를 사용하여 기존의 LinearLayout 복제
        val newField = LayoutInflater.from(this)
            .inflate(R.layout.fragment_layout_checklist_add, rootLayout, false)

        val textView = newField.findViewById<TextView>(R.id.checklist_add_title_number)
        counter++;
        textView.text = counter.toString()
        // 추가한 새로운 LinearLayout을 rootLayout에 붙인다
        rootLayout.addView(newField)

    }

    private fun mapNonEmptyFields(rootLayout: LinearLayout, checklistTotalStep: TextView) {
        var nonEmptyCount = 1

        // rootLayout 내부의 모든 LinearLayout을 순회
        for (i in 0 until rootLayout.childCount) {
            val childLayout = rootLayout.getChildAt(i) as LinearLayout
            nonEmptyCount++
        }

        // 결과를 checklist_total_step에 표시
        checklistTotalStep.text = "$nonEmptyCount"
    }

    // EditText 값들을 리스트로 수집하여 ChecklistOrderBroadCast로 전송하는 함수
    private fun sendDataToChecklistOrderBroadCast(rootLayout: LinearLayout) {
        val objectiveTitleOrder = (findViewById<TextView>(R.id.objective_title_order)?.text ?: "").toString()

        val editTextValues = mutableListOf<String>()

        // rootLayout 내부의 모든 LinearLayout을 순회하여 EditText 값을 수집
        for (i in 0 until rootLayout.childCount) {
            val childLayout = rootLayout.getChildAt(i) as LinearLayout
            val editText = childLayout.findViewById<EditText>(R.id.order_checklist_edit_text_box)

            // 빈 값이 아닌 경우 리스트에 추가
            if (editText.text.isNotEmpty()) {
                editTextValues.add(editText.text.toString())
            }
        }

        // ChecklistOrderBroadCast 액티비티로 Intent를 통해 데이터 전송
        val intent = Intent(this, ChecklistOrderBroadCast::class.java)
        intent.putExtra("user_id", userId)
        intent.putExtra("EXTRA_TITLE", objectiveTitleOrder)
        intent.putStringArrayListExtra("editTextValues", ArrayList(editTextValues))
        startActivity(intent)
    }
}