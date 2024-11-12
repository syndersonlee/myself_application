package com.example.childreneducationapplication.check

import android.app.AlertDialog
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class ChecklistOrderBroadCast : ComponentActivity() {

    private var title = ""
    private var userId = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_checklist_order_broadcast)

        // root_linear_layout을 가져오기
        val rootLinearLayout = findViewById<LinearLayout>(R.id.root_linear_layout)

        userId = intent.getIntExtra("user_id", 0)
        title = intent.getStringExtra("EXTRA_TITLE") ?: ""

        val objectiveTitleOrder = findViewById<TextView>(R.id.checklist_broadcast_title)
        objectiveTitleOrder.text = title

        showChecklistTyping(rootLinearLayout, userId)

        val exitButton = findViewById<LinearLayout>(R.id.checklist_order_exit)
        exitButton.setOnClickListener {
            finish() // Activity 종료
        }
    }

    private fun showChecklistTyping(rootLinearLayout: LinearLayout, userId: Int) {
        // Intent로부터 데이터를 받음
        val editTextValues = intent.getStringArrayListExtra("editTextValues")

        // 값이 있을 경우, 각 값을 새로운 항목으로 추가
        editTextValues?.forEach { value ->
            // 새로운 LinearLayout 생성
            val checklistOrderBroadcastRootContent = LinearLayout(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                ).apply {
                    setMargins(0, 10, 0, 10)
                }
                orientation = LinearLayout.HORIZONTAL
                setBackgroundResource(R.drawable.cr20bffffff)
                gravity = Gravity.CENTER_VERTICAL
                setPadding(17, 6, 17, 6) // paddingHorizontal과 paddingVertical 대체
            }

            // TextView 생성
            val textView = TextView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    0,
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    1f // layout_weight
                ).apply {
                    setPadding(10, 0, 0, 0)
                }
                text = value // EditText에서 수신한 값
                setTextColor(Color.parseColor("#000000")) // textColor 대체
                textSize = 20f
                // 텍스트 스타일을 bold로 설정
                paint.isFakeBoldText = true // textStyle 대체
                val params = layoutParams as LinearLayout.LayoutParams
                params.marginEnd = 4 // layoutMarginEnd 대체
                layoutParams = params // 수정된 LayoutParams를 다시 설정
            }

            // 체크박스 이미지 생성
            val yesCheckbox = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    100,
                    110
                )
                scaleType = ImageView.ScaleType.FIT_XY
                setImageResource(R.drawable.yes_checkbox)
                val params = layoutParams as LinearLayout.LayoutParams
                params.marginEnd = 2 // layoutMarginEnd 대체
                layoutParams = params // 수정된 LayoutParams를 다시 설정
            }

            val noCheckbox = ImageView(this).apply {
                layoutParams = LinearLayout.LayoutParams(
                    100,
                    110
                )
                scaleType = ImageView.ScaleType.FIT_XY
                setImageResource(R.drawable.no_checkbox)
            }

            yesCheckbox.setOnClickListener {
                saveOrderObjectiveHistory(value, true, userId) // true로 저장
                checklistOrderBroadcastRootContent.visibility = View.INVISIBLE // 클릭 후 INVISIBLE
                checkAllContentInvisible(rootLinearLayout)
                showAlert("O")
            }

            // No 체크박스 클릭 리스너
            noCheckbox.setOnClickListener {
                showFinalCheckPopup(
                    checklistOrderBroadcastRootContent,
                    value,
                    userId
                ) // 팝업 보여주기
                checkAllContentInvisible(rootLinearLayout)
            }

            // LinearLayout에 TextView 및 ImageView 추가
            checklistOrderBroadcastRootContent.apply {
                addView(textView)
                addView(yesCheckbox)
                addView(noCheckbox)
            }

            // 최상위 LinearLayout에 추가
            rootLinearLayout.addView(checklistOrderBroadcastRootContent)
        }
    }

    private fun saveOrderObjectiveHistory(content: String?, isPass: Boolean, userId: Int) {
        CoroutineScope(Dispatchers.IO).launch {
            val data = mapOf(
                "title" to title,
                "content" to content,
                "pass" to isPass,
                "user_id" to userId  // 삽입 시 user_id를 포함해야 함
            )

            // Upsert logic for TimerObjectiveHistory
            supabaseClient.from("order_objective_history")
                .insert(data)
        }
    }

    private fun showAlert(info: String) {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("알림")
            .setMessage("${info}를 클릭했어요")
            .setPositiveButton("확인", null)
            .create()
        alertDialog.show()
    }

    private fun showAlertCompletion() {
        AlertDialog.Builder(this)
            .setTitle("점검 완료")
            .setMessage("점검이 완료되었습니다")
            .setPositiveButton("확인", null)
            .create()
            .show()
    }

    private fun checkAllContentInvisible(rootLinearLayout: LinearLayout) {
        var allInvisible = true
        for (i in 0 until rootLinearLayout.childCount) {
            val child = rootLinearLayout.getChildAt(i)
            if (child.visibility != View.INVISIBLE) {
                allInvisible = false
                break
            }
        }
        if (allInvisible) {
            showAlertCompletion()
        }
    }

    private fun showFinalCheckPopup(
        rootLinearLayout: LinearLayout,
        value : String,
        userId: Int
    ) {
        // 커스텀 팝업 뷰 인플레이트
        val dialogView = layoutInflater.inflate(R.layout.fragment_final_check, null)

        // AlertDialog 생성 및 커스텀 뷰 설정
        val dialog = AlertDialog.Builder(this)
            .setView(dialogView)
            .create()

        // 배경을 흐리게 설정
        dialog.window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))

        val yesBox = dialogView.findViewById<LinearLayout>(R.id.checklist_broadcast_yes_box)
        yesBox.setOnClickListener {
            dialog.dismiss() // 팝업 닫기
        }

        val noBox = dialogView.findViewById<LinearLayout>(R.id.checklist_broadcast_no_box)
        noBox.setOnClickListener {
            rootLinearLayout.visibility = View.INVISIBLE
            saveOrderObjectiveHistory(value, false, userId) // false로 저장
            dialog.dismiss() // 팝업 닫기
        }

        dialog.show()
    }
}