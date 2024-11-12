package com.example.childreneducationapplication.check.parents

import android.app.AlertDialog
import android.os.Build
import android.os.Bundle
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.ObjectiveHistory
import com.example.childreneducationapplication.entity.SettingObjective
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

class TimerParentsActivity : ComponentActivity() {
    private var userId = 0
    private var title = ""
    private var role = ""
    private lateinit var timerHistoryContainer: LinearLayout

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_checklist_timer_broadcast_parents)
        userId = intent.getIntExtra("user_id", 0)
        role = intent.getStringExtra("role") ?: ""

        timerHistoryContainer = findViewById(R.id.timer_history_content) // history 항목을 담을 컨테이너

        lifecycleScope.launch {
            val objectiveTitleTimer = findViewById<TextView>(R.id.objective_title_timer_parents)
            // Ensure title is selected before proceeding
            title = getObjectiveTitleSelection(objectiveTitleTimer)

            // Once the title is set, proceed to fetch history and populate UI
            val historyList = fetchObjectiveHistory()
            populateTimerHistory(historyList)
        }
    }

    private suspend fun getObjectiveTitleSelection(objectiveTitleTimer: TextView): String {
        return suspendCoroutine { continuation ->
            lifecycleScope.launch {
                val objectives = fetchTitles()
                objectives?.let {
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

                    val items = arrayOf(presidentStudyObjective, presidentLivingObjective).filter {
                        it.isNotBlank()
                    }.toTypedArray()

                    val builder = AlertDialog.Builder(this@TimerParentsActivity)
                    builder.setTitle("목표를 선택해주세요")
                    builder.setItems(items) { dialog, which ->
                        title = items[which]
                        objectiveTitleTimer.text = items[which]
                        continuation.resume(items[which]) // Resume with selected title
                    }
                    builder.show()
                }
            }
        }
    }

    private suspend fun fetchObjectiveHistory(): List<ObjectiveHistory> {
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

    @RequiresApi(Build.VERSION_CODES.O)
    private fun populateTimerHistory(historyList: List<ObjectiveHistory>) {
        val reversedHistoryList = historyList.reversed()
        for (history in reversedHistoryList) {
            addHistoryItem(history)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addHistoryItem(history: ObjectiveHistory) {
        // LayoutInflater를 사용해 템플릿 뷰를 inflate
        val inflater = LayoutInflater.from(this)
        val historyItemView =
            inflater.inflate(R.layout.timer_parents_history_item, timerHistoryContainer, false)

        // 각 뷰에 데이터를 설정
        val createTimeTextView = historyItemView.findViewById<TextView>(R.id.timer_history_create_time)
        createTimeTextView.text = convertToKST(history.created_at ?: "") // KST로 변환된 값을 설정

        // children_check의 ImageView를 pass 값에 따라 설정
        val childrenCheckView = historyItemView.findViewById<LinearLayout>(R.id.children_check)
        val childrenCheckImageView = childrenCheckView.findViewById<ImageView>(R.id.children_check_image)

        if (history.pass == true) {
            childrenCheckImageView.setImageResource(R.drawable.yes_checkbox) // pass가 true일 경우
        } else {
            childrenCheckImageView.setImageResource(R.drawable.no_checkbox) // pass가 false일 경우
        }

        // parents_check를 toggle로 설정
        val parentsCheckView = historyItemView.findViewById<LinearLayout>(R.id.parents_check)
        val parentsCheckImageView = parentsCheckView.findViewById<ImageView>(R.id.parents_check_image)

        // 초기 상태에 따라 parents_check의 이미지를 설정
        if (history.parents_approve == true) {
            parentsCheckImageView.setImageResource(R.drawable.yes_checkbox)
        } else {
            parentsCheckImageView.setImageResource(R.drawable.no_checkbox)
        }

        // rxi2q6ssc2de의 TextView 설정 (children_check_image와 parents_check_image 비교)
        val statusTextView = historyItemView.findViewById<TextView>(R.id.rxi2q6ssc2de)
        updateStatusText(childrenCheckImageView, parentsCheckImageView, statusTextView)


        if(role != "history") {
            // parentsCheckView에 클릭 리스너를 설정
            parentsCheckView.setOnClickListener {
                // parents_approve 값을 toggle
                val newApproveState = !(history.parents_approve ?: false)
                history.parents_approve = newApproveState

                // 업데이트된 값에 따라 이미지 리소스를 변경
                if (newApproveState) {
                    parentsCheckImageView.setImageResource(R.drawable.yes_checkbox)
                } else {
                    parentsCheckImageView.setImageResource(R.drawable.no_checkbox)
                }

                // children_check_image와 parents_check_image 비교 후 statusTextView 업데이트
                updateStatusText(childrenCheckImageView, parentsCheckImageView, statusTextView)

                // DB를 업데이트하는 로직 추가 (Supabase 업데이트 쿼리)
                lifecycleScope.launch {
                    updateParentsApproveStateInDatabase(history.id, newApproveState)
                }
            }
        }

        // 추가된 뷰를 컨테이너에 삽입
        timerHistoryContainer.addView(historyItemView)
    }

    // DB에 parents_approve 상태를 업데이트하는 함수
    private suspend fun updateParentsApproveStateInDatabase(historyId: Int?, newApproveState: Boolean) {
        if (historyId != null) {
            supabaseClient
                .from("timer_objective_history")
                .update(mapOf("parents_approve" to newApproveState)) {
                    filter {
                        eq("id", historyId)
                    }
                }
            println("Successfully updated parents_approve state in the database.")
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun convertToKST(utcTimestamp: String): String {
        // Parse the UTC timestamp
        val utcDateTime = ZonedDateTime.parse(utcTimestamp)

        // Convert to KST (Korea Standard Time)
        val kstDateTime = utcDateTime.withZoneSameInstant(ZoneId.of("Asia/Seoul"))

        // Format the datetime in "yyyy-MM-dd HH:mm:ss"
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        return kstDateTime.format(formatter)
    }

    private fun updateStatusText(
        childrenCheckImageView: ImageView,
        parentsCheckImageView: ImageView,
        statusTextView: TextView
    ) {
        val childrenCheckDrawable = childrenCheckImageView.drawable
        val parentsCheckDrawable = parentsCheckImageView.drawable

        if (childrenCheckDrawable.constantState == parentsCheckDrawable.constantState) {
            statusTextView.text = "일치"
        } else {
            statusTextView.text = "불일치"
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
}