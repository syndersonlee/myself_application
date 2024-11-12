package com.example.childreneducationapplication.check

import android.app.AlertDialog
import android.content.Context
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class ChecklistTimerBroadcast : ComponentActivity() {
    private lateinit var checkStatusBox: LinearLayout
    private lateinit var timerRemain: TextView
    private val handler = Handler(Looper.getMainLooper())

    // 인스턴스 변수
    private var title = ""
    private var content = ""
    private var maxExecutions = 3
    private var intervalTimerSeconds = 1 * 1000L
    private var totalMinutes = 1 * 1000L
    private var attempt = 0

    // 단순한 visibility 변경 로직
    private val visibilityRunnable = object : Runnable {
        @RequiresApi(Build.VERSION_CODES.O)
        override fun run() {
            // check_status_box를 VISIBLE로 설정
            if(attempt != 0) {
                if (checkStatusBox.visibility == View.VISIBLE) {
                    lifecycleScope.launch {
                        saveTimerObjectiveHistory(isPass = false)
                    }
                }
            }
            checkStatusBox.visibility = View.VISIBLE
            triggerVibration()
            attempt++
            // totalMinutes 후에 다시 GONE으로 설정하고 실행 반복
            handler.postDelayed({
                if (checkStatusBox.visibility == View.VISIBLE) {
                    lifecycleScope.launch {
                        saveTimerObjectiveHistory(isPass = false)
                    }
                }
                checkStatusBox.visibility = View.VISIBLE
                triggerVibration()
                attempt++
                handler.postDelayed(this, intervalTimerSeconds)
            }, intervalTimerSeconds) // intervalTimerSeconds 후에 실행
        }
    }

    private fun startTimer(timer: Long) {
        var remainingTime: Long = timer
        handler.post(object : Runnable {
            override fun run() {
                if (remainingTime > 0) {
                    // 남은 시간 포맷팅
                    val minutes = (remainingTime / 1000) / 60
                    val seconds = (remainingTime / 1000) % 60
                    timerRemain.text = String.format("%02d:%02d", minutes, seconds)

                    remainingTime -= 1000 // 1초 감소
                    handler.postDelayed(this, 1000) // 1초마다 업데이트
                } else {
                    timerRemain.text = "00:00" // 시간이 다 됐을 때
                    showAlert(" 점검이 완료되었습니다.")
                }
            }
        })
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_checklist_timer_broadcast)

        val broadcastTextView = findViewById<TextView>(R.id.checklist_content_broadcast)
        title = intent.getStringExtra("EXTRA_TITLE") ?: ""
        content = intent.getStringExtra("EXTRA_CHECKLIST_CONTENT") ?: ""
        val titleTimerTextView = findViewById<TextView>(R.id.title_timer)
        titleTimerTextView.text = title
        broadcastTextView.text = content

        // checklist_broadcast_yes_box와 checkStatusBox를 찾음
        checkStatusBox = findViewById(R.id.check_status_box)
        timerRemain = findViewById(R.id.timer_remain)

        val checklistBroadcastYesBox = findViewById<LinearLayout>(R.id.checklist_broadcast_yes_box)
        val checklistBroadcastNoBox = findViewById<LinearLayout>(R.id.checklist_broadcast_no_box)

        checklistBroadcastYesBox.setOnClickListener {
            lifecycleScope.launch {
                saveTimerObjectiveHistory(isPass = true)
            }
            checkStatusBox.visibility = View.INVISIBLE
        }

        checklistBroadcastNoBox.setOnClickListener {
            lifecycleScope.launch {
                saveTimerObjectiveHistory(isPass = false)
            }
            checkStatusBox.visibility = View.INVISIBLE
        }

        // Intent에서 interval 및 visible 값을 가져옴
        val intervalMinutes = intent.getIntExtra("EXTRA_INTERVAL_MINUTE", 10)
        val visibleSeconds = intent.getIntExtra("EXTRA_VISIBLE_SECONDS", 30)
        val intervalMaxExecution = intent.getIntExtra("EXTRA_MAX_EXECUTION", 3)

        totalMinutes = intervalMinutes.toLong() * 60 * 1000
        intervalTimerSeconds = visibleSeconds.toLong() * 1000
        maxExecutions = intervalMaxExecution

        // 타이머 시작
        startTimer(totalMinutes)
        handler.post(visibilityRunnable)

        val exitButton = findViewById<LinearLayout>(R.id.checklist_timer_exit)
        exitButton.setOnClickListener {
            finish() // 현재 Activity 종료
        }
    }

    private fun triggerVibration() {
        val vibrator = getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasVibrator()) {
            val vibrationEffect = VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE)
            vibrator.vibrate(vibrationEffect)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun saveTimerObjectiveHistory(isPass: Boolean) {
        val userId = intent.getIntExtra("user_id", 1)

        val data = mapOf(
            "title" to title,
            "content" to content,
            "pass" to isPass,
            "user_id" to userId  // 삽입 시 user_id를 포함해야 함
        )

        // Upsert logic for TimerObjectiveHistory
        supabaseClient.from("timer_objective_history")
            .insert(data)

        showAlert(if (isPass) "성공" else "실패")
    }

    private fun showAlert(info: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("알림")
            .setMessage(info)
            .setPositiveButton("확인", null)
            .create()
        alertDialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(visibilityRunnable) // Activity 종료 시 핸들러 제거
    }
}