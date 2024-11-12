package com.example.childreneducationapplication.menu

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.check.ChecklistSubMenuActivity
import com.example.childreneducationapplication.stamp.StampCollectingActivity
import com.example.childreneducationapplication.entity.SettingObjective
import com.example.childreneducationapplication.evaluation.SettingEvaluationActivity
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import com.example.childreneducationapplication.objective.SettingProfileActivity
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

/**
 * A simple [Fragment] subclass.
 * Use the [MenuActivity.newInstance] factory method to
 * create an instance of this fragment.
 */
class MenuActivity : AppCompatActivity() {

    private var objectiveStudy: String? = null
    private var objectiveLiving: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_menu)
        val userId = intent.getIntExtra("user_id", 0)
        val role = intent.getStringExtra("role")

        // menu_objective_button 클릭 시 SettingObjectiveActivity 호출
        val objectiveMenuButton = findViewById<LinearLayout>(R.id.menu_objective_button)
        objectiveMenuButton.setOnClickListener {
            val intent = Intent(this, SettingProfileActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("role", role)
            startActivity(intent)
        }

        objectiveSettingCheck(userId)

        val checklistSubMenuActivity = findViewById<LinearLayout>(R.id.menu_checklist_button)
        checklistSubMenuActivity.setOnClickListener {
            objectiveSettingCheck(userId)

            if (objectiveStudy != null || objectiveLiving != null) {
                val intent = Intent(this, ChecklistSubMenuActivity::class.java)
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
                showAlert()
            }
        }

        val settingEvaluationActivity = findViewById<LinearLayout>(R.id.menu_evaluation_button)
        settingEvaluationActivity.setOnClickListener {
            objectiveSettingCheck(userId)

            if (objectiveStudy != null || objectiveLiving != null) {
                val intent = Intent(this, SettingEvaluationActivity::class.java)
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
                showAlert()
            }
        }

        val stampCollectionActivity = findViewById<LinearLayout>(R.id.menu_cheer_button)
        stampCollectionActivity.setOnClickListener {
            objectiveSettingCheck(userId)

            if (objectiveStudy != null || objectiveLiving != null) {
                val intent = Intent(this, StampCollectingActivity::class.java)
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
                showAlert()
            }
        }

    }

    private fun objectiveSettingCheck(userId: Int) {
        lifecycleScope.launch {
            val existingRecord = supabaseClient.from("setting_objective").select {
                filter {
                    eq("user_id", userId) // user_id로 필터링
                }
            }.decodeList<SettingObjective>()

            if (existingRecord.isNotEmpty()) {
                val record = existingRecord[0]
                if (record.study_flag == 0 && !record.study_objective0.isNullOrBlank()) {
                    objectiveStudy = record.study_objective0
                } else if (record.study_flag == 1 && !record.study_objective1.isNullOrBlank()) {
                    objectiveStudy = record.study_objective1
                } else if (record.study_flag == 2 && !record.study_objective2.isNullOrBlank()) {
                    objectiveStudy = record.study_objective2
                }

                if (record.living_flag == 0 && !record.living_objective0.isNullOrBlank()) {
                    objectiveLiving = record.living_objective0
                } else if (record.living_flag == 1 && !record.living_objective1.isNullOrBlank()) {
                    objectiveLiving = record.living_objective1
                } else if (record.living_flag == 2 && !record.living_objective2.isNullOrBlank()) {
                    objectiveLiving = record.living_objective2
                }
            }
        }
    }

    private fun showAlert() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle("목표 입력 필요")
            .setMessage("목표 입력을 누락했어요")
            .setPositiveButton("확인", null)
            .create()
        alertDialog.show()
    }
}