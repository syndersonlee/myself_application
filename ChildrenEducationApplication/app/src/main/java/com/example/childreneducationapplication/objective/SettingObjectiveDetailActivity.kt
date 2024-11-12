package com.example.childreneducationapplication.objective

import android.os.Bundle
import android.view.View
import android.widget.EditText
import android.widget.ImageView
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.SettingObjective
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class SettingObjectiveDetailActivity : ComponentActivity() {

    var userId : Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_setting_objective_detail)
        userId = intent.getIntExtra("user_id", 0)
        val role = intent.getStringExtra("role")
        fetchingInitial(userId, role)

        toogleStudyButton()
        toogleLiveButton(role)

    }

    private fun fetchingInitial(userId: Int, role: String?) {
        // EditText 참조
        val studyObjective0 = findViewById<EditText>(R.id.study_objective0)
        val studyObjective1 = findViewById<EditText>(R.id.study_objective1)
        val studyObjective2 = findViewById<EditText>(R.id.study_objective2)

        val livingObjective0 = findViewById<EditText>(R.id.living_objective0)
        val livingObjective1 = findViewById<EditText>(R.id.living_objective1)
        val livingObjective2 = findViewById<EditText>(R.id.living_objective2)

        // 코루틴으로 데이터 가져오기
        lifecycleScope.launch {
            val objectives = fetchObjectives(userId)
            objectives?.let {
                // EditText에 설정
                studyObjective0.setText(it.study_objective0 ?: "")
                studyObjective1.setText(it.study_objective1 ?: "")
                studyObjective2.setText(it.study_objective2 ?: "")

                livingObjective0.setText(it.living_objective0 ?: "")
                livingObjective1.setText(it.living_objective1 ?: "")
                livingObjective2.setText(it.living_objective2 ?: "")
            }
        }

        if (role == "보호자") {
            studyObjective0.isEnabled = false
            studyObjective1.isEnabled = false
            studyObjective2.isEnabled = false
            livingObjective0.isEnabled = false
            livingObjective1.isEnabled = false
            livingObjective2.isEnabled = false
        }
    }

    private fun toogleStudyButton() {
        // 첫 번째 LinearLayout
        val layoutOne = findViewById<View>(R.id.study_select_one)
        val imageOne = findViewById<ImageView>(R.id.checkbox_study_image0)

        // 두 번째 LinearLayout
        val layoutTwo = findViewById<View>(R.id.study_select_two)
        val imageTwo = findViewById<ImageView>(R.id.checkbox_study_image1)

        // 세 번째 LinearLayout
        val layoutThree = findViewById<View>(R.id.study_select_three)
        val imageThree = findViewById<ImageView>(R.id.checkbox_study_image2)

        lifecycleScope.launch {
            val objectives = fetchObjectives(userId)

            if (objectives?.study_flag == 0) {
                imageOne.visibility = View.VISIBLE
                imageTwo.visibility = View.INVISIBLE
                imageThree.visibility = View.INVISIBLE
            } else if (objectives?.study_flag == 1) {
                imageOne.visibility = View.INVISIBLE
                imageTwo.visibility = View.VISIBLE
                imageThree.visibility = View.INVISIBLE
            } else if (objectives?.study_flag == 2) {
                imageOne.visibility = View.INVISIBLE
                imageTwo.visibility = View.INVISIBLE
                imageThree.visibility = View.VISIBLE
            }
        }

            // 첫 번째 클릭 리스너
            layoutOne.setOnClickListener {
                showOnlySelectedImage(imageOne, imageTwo, imageThree)
            }

            // 두 번째 클릭 리스너
            layoutTwo.setOnClickListener {
                showOnlySelectedImage(imageTwo, imageOne, imageThree)
            }

            // 세 번째 클릭 리스너
            layoutThree.setOnClickListener {
                showOnlySelectedImage(imageThree, imageOne, imageTwo)
            }
    }

    private fun toogleLiveButton(role: String?) {
        // 첫 번째 LinearLayout
        val layoutOne = findViewById<View>(R.id.live_select_one)
        val imageOne = findViewById<ImageView>(R.id.checkbox_live_image0)

        // 두 번째 LinearLayout
        val layoutTwo = findViewById<View>(R.id.live_select_two)
        val imageTwo = findViewById<ImageView>(R.id.checkbox_live_image1)

        // 세 번째 LinearLayout
        val layoutThree = findViewById<View>(R.id.live_select_three)
        val imageThree = findViewById<ImageView>(R.id.checkbox_live_image2)

        lifecycleScope.launch {
            val objectives = fetchObjectives(userId)
            if (objectives?.living_flag == 0) {
                imageOne.visibility = View.VISIBLE
                imageTwo.visibility = View.INVISIBLE
                imageThree.visibility = View.INVISIBLE
            } else if (objectives?.living_flag == 1) {
                imageOne.visibility = View.INVISIBLE
                imageTwo.visibility = View.VISIBLE
                imageThree.visibility = View.INVISIBLE
            } else if (objectives?.living_flag == 2) {
                imageOne.visibility = View.INVISIBLE
                imageTwo.visibility = View.INVISIBLE
                imageThree.visibility = View.VISIBLE
            }
        }

        // 첫 번째 클릭 리스너
        if (role == "학생") {
            layoutOne.setOnClickListener {
                showOnlySelectedImage(imageOne, imageTwo, imageThree)
            }

            // 두 번째 클릭 리스너
            layoutTwo.setOnClickListener {
                showOnlySelectedImage(imageTwo, imageOne, imageThree)
            }

            // 세 번째 클릭 리스너
            layoutThree.setOnClickListener {
                showOnlySelectedImage(imageThree, imageOne, imageTwo)
            }
        }

    }

    // 선택한 이미지 뷰만 보이게 하고 나머지를 숨기는 함수
    private fun showOnlySelectedImage(
        selectedImage: ImageView,
        otherImage1: ImageView,
        otherImage2: ImageView
    ) {
        // 선택된 이미지는 보이도록 설정
        selectedImage.visibility = View.VISIBLE

        // 나머지 두 이미지는 숨기기
        otherImage1.visibility = View.INVISIBLE
        otherImage2.visibility = View.INVISIBLE
    }

    override fun onPause() {
        super.onPause()

        // EditText의 값 가져오기
        val studyObjective0 = findViewById<EditText>(R.id.study_objective0).text.toString()
        val studyObjective1 = findViewById<EditText>(R.id.study_objective1).text.toString()
        val studyObjective2 = findViewById<EditText>(R.id.study_objective2).text.toString()

        // EditText의 값 가져오기
        val livingObjective0 = findViewById<EditText>(R.id.living_objective0).text.toString()
        val livingObjective1 = findViewById<EditText>(R.id.living_objective1).text.toString()
        val livingObjective2 = findViewById<EditText>(R.id.living_objective2).text.toString()

        // CheckBox의 visible 상태에 따라 study_flag 설정
        val checkbox0 = findViewById<ImageView>(R.id.checkbox_study_image0)
        val checkbox1 = findViewById<ImageView>(R.id.checkbox_study_image1)
        val checkbox2 = findViewById<ImageView>(R.id.checkbox_study_image2)

        var studyFlag = -1 // 기본 값, 만약 아무것도 선택되지 않았으면 이 값으로 설정됨

        when {
            checkbox0.visibility == View.VISIBLE -> studyFlag = 0
            checkbox1.visibility == View.VISIBLE -> studyFlag = 1
            checkbox2.visibility == View.VISIBLE -> studyFlag = 2
        }

        // CheckBox의 visible 상태에 따라 study_flag 설정
        val checkbox3 = findViewById<ImageView>(R.id.checkbox_live_image0)
        val checkbox4 = findViewById<ImageView>(R.id.checkbox_live_image1)
        val checkbox5 = findViewById<ImageView>(R.id.checkbox_live_image2)

        var livingFlag = -1 // 기본 값, 만약 아무것도 선택되지 않았으면 이 값으로 설정됨

        when {
            checkbox3.visibility == View.VISIBLE -> livingFlag = 0
            checkbox4.visibility == View.VISIBLE -> livingFlag = 1
            checkbox5.visibility == View.VISIBLE -> livingFlag = 2
        }

        // 코루틴을 시작하여 데이터 저장
        lifecycleScope.launch {
            saveStudyObjectivesAndFlag(
                userId,
                studyObjective0,
                studyObjective1,
                studyObjective2,
                studyFlag,
                livingObjective0,
                livingObjective1,
                livingObjective2,
                livingFlag
            )
        }
    }

    suspend fun saveStudyObjectivesAndFlag(
        userId: Int,
        obj0: String?,
        obj1: String?,
        obj2: String?,
        flag0: Int?,
        obj3: String?,
        obj4: String?,
        obj5: String?,
        flag1: Int?
    ) {
        // 업데이트할 데이터 생성
        val existingRecord = supabaseClient.from("setting_objective").select {
            filter {
                eq("user_id", userId) // user_id로 필터링
            }
        }.decodeList<SettingObjective>()

        // 업데이트 또는 삽입할 데이터 생성
        val data = mapOf(
            "study_objective0" to obj0,
            "study_objective1" to obj1,
            "study_objective2" to obj2,
            "study_flag" to flag0,
            "living_objective0" to obj3,
            "living_objective1" to obj4,
            "living_objective2" to obj5,
            "living_flag" to flag1,
            "user_id" to userId  // 삽입 시 user_id를 포함해야 함
        )

        if (existingRecord.isNotEmpty()) {
            // 레코드가 존재하면 업데이트
            supabaseClient.from("setting_objective").update(data) {
                filter {
                    eq("user_id", userId)
                }
            }
        } else {
            // 레코드가 존재하지 않으면 삽입
            supabaseClient.from("setting_objective").insert(data)
        }
    }


    // 데이터를 불러오는 함수
    suspend fun fetchObjectives(userId: Int): SettingObjective? {
        val existingRecord = supabaseClient.from("setting_objective").select {
            filter {
                eq("user_id", userId) // user_id로 필터링
            }
        }.decodeList<SettingObjective>()

        return if (existingRecord.isNotEmpty()) {
            existingRecord[0]
        } else {
            null
        }
    }
}