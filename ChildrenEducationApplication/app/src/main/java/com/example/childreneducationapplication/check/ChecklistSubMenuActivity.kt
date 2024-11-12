package com.example.childreneducationapplication.check

import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.activity.ComponentActivity
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.check.parents.OrderParentsActivity
import com.example.childreneducationapplication.check.parents.TimerParentsActivity

class ChecklistSubMenuActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_checklist_sub_menu)
        val userId = intent.getIntExtra("user_id", 0)
        val role = intent.getStringExtra("role")
        val objectiveStudy = intent.getStringExtra("objective_study")
        val objectiveLiving = intent.getStringExtra("objective_living")

        // menu_objective_button 클릭 시 SettingObjectiveActivity 호출
        val settingTimerButton = findViewById<LinearLayout>(R.id.button_checklist_sub_menu_timer)
        settingTimerButton.setOnClickListener {
            if (role == "학생") {
                val intent = Intent(this, SettingChecklistTimerActivity::class.java)
                intent.putExtra("user_id", userId)
                intent.putExtra("role", role)
                if (objectiveStudy != null) {
                    intent.putExtra("objective_study", objectiveStudy)
                }
                if (objectiveLiving != null) {
                    intent.putExtra("objective_living", objectiveLiving)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, TimerParentsActivity::class.java)
                intent.putExtra("user_id", userId)
                intent.putExtra("role", role)
                if (objectiveStudy != null) {
                    intent.putExtra("objective_study", objectiveStudy)
                }
                if (objectiveLiving != null) {
                    intent.putExtra("objective_living", objectiveLiving)
                }
                startActivity(intent)
            }
        }

        val settingChecklistButton =
            findViewById<LinearLayout>(R.id.button_checklist_sub_menu_order)
        settingChecklistButton.setOnClickListener {
            if (role == "학생") {
                val intent = Intent(this, SettingChecklistOrderActivity::class.java)
                intent.putExtra("user_id", userId)
                intent.putExtra("role", role)
                if (objectiveStudy != null) {
                    intent.putExtra("objective_study", objectiveStudy)
                }
                if (objectiveLiving != null) {
                    intent.putExtra("objective_living", objectiveLiving)
                }
                startActivity(intent)
            } else {
                val intent = Intent(this, OrderParentsActivity::class.java)
                intent.putExtra("user_id", userId)
                intent.putExtra("role", role)
                if (objectiveStudy != null) {
                    intent.putExtra("objective_study", objectiveStudy)
                }
                if (objectiveLiving != null) {
                    intent.putExtra("objective_living", objectiveLiving)
                }
                startActivity(intent)
            }
        }
    }
}