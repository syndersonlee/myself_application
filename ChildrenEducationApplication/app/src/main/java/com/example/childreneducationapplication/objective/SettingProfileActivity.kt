package com.example.childreneducationapplication.objective

import android.app.AlertDialog
import android.content.Intent
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.Profile
import com.example.childreneducationapplication.externals.Supabase.downloadImage
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import com.example.childreneducationapplication.externals.Supabase.uploadFileWithUpsert
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class SettingProfileActivity : ComponentActivity() {
    private val scope = MainScope() // Coroutine scope
    private var userIdScope: String = "user_id" // 실제 사용자 ID로 바꿔주세요

    private lateinit var profileImageView: ImageView
    private lateinit var profileName: EditText
    private lateinit var profileSchool: EditText
    private lateinit var profileGrade: EditText
    private lateinit var profileAdvantage: EditText
    private lateinit var profilePractice: EditText
    private lateinit var profileDream: EditText
    private lateinit var profileSignature: EditText

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let {
            val file = uriToFile(uri)
            file?.let {
                // 업로드 수행
                scope.launch {
                    val response = uploadFileWithUpsert(it, "profile/profile_$userIdScope")
                    if (response != null) {
                        // 업로드 성공 시 이미지 설정
                        profileImageView.setImageBitmap(BitmapFactory.decodeFile(it.path))
                    } else {
                        Toast.makeText(
                            this@SettingProfileActivity,
                            "이미지 업로드 실패",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            }
        }
    }

    // Uri를 File로 변환하는 함수
    private fun uriToFile(uri: Uri): File? {
        val file = File(cacheDir, "temp_image")
        try {
            val inputStream = contentResolver.openInputStream(uri)
            val outputStream = FileOutputStream(file)
            inputStream?.copyTo(outputStream)
            inputStream?.close()
            outputStream.close()
            Log.d("FileUpload", "File saved at: ${file.absolutePath}") // 파일 경로 로그 출력
            return file
        } catch (e: Exception) {
            e.printStackTrace()
            return null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_setting_profile)

        profileName = findViewById(R.id.profile_name)
        profileSchool = findViewById(R.id.profile_school)
        profileGrade = findViewById(R.id.profile_grade)
        profileAdvantage = findViewById(R.id.profile_advantage)
        profilePractice = findViewById(R.id.profile_practice)
        profileDream = findViewById(R.id.profile_future)
        profileSignature = findViewById(R.id.profile_signature)

        val userId = intent.getIntExtra("user_id", 0)
        val role = intent.getStringExtra("role")

        if (role == "보호자") {
            disableProfileEditing()
        }

        userIdScope = userId.toString()

        fetchOrCreateUserProfile(userId)

        // LinearLayout 클릭 시 갤러리 또는 카메라 선택
        profileImageView = findViewById(R.id.profile_image_button)

        MainScope().launch {
            val bitmap = downloadImage("profile/profile_${userId}.jpeg")

            if (bitmap != null) {
                profileImageView.setImageBitmap(bitmap)
            }
        }
        if (role == "학생") {
            profileImageView.setOnClickListener {
                selectImageLauncher.launch("image/*")
            }
        }

        val saveButton = findViewById<LinearLayout>(R.id.profile_save_button)
        saveButton.setOnClickListener {
            lifecycleScope.launch {
                // 입력된 값들 가져오기
                val name = profileName.text.toString()
                val school = profileSchool.text.toString()
                val grade = profileGrade.text.toString()

                val advantage = profileAdvantage.text.toString()
                val practice = profilePractice.text.toString()
                val dream = profileDream.text.toString()
                val signature = profileSignature.text.toString()

                // 데이터 업데이트 로직 호출
                updateUserProfile(
                    userId,
                    name,
                    school,
                    grade,
                    advantage,
                    practice,
                    dream,
                    signature
                )
            }
            showAlert("저장 완료")
        }

        val callObjectiveDetailButton =
            findViewById<LinearLayout>(R.id.profile_move_objective_setting)
        callObjectiveDetailButton.setOnClickListener {

            val intent = Intent(this, SettingObjectiveDetailActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("role", role)
            startActivity(intent)
        }
    }

    private fun fetchOrCreateUserProfile(userId: Int) {
        // 비동기 작업을 위한 코루틴 사용
        if (userId == 0) {
            return
        }
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // 프로필 데이터 가져오기
                val response = supabaseClient.from("profile").select {
                    filter {
                        eq("user_id", userId)
                    }
                }.decodeList<Profile>()

                withContext(Dispatchers.Main) {
                    if (response.isNotEmpty()) {
                        // 데이터가 존재하면 EditText에 값 채우기
                        val profile = response[0]
                        profileName.setText(profile.name)
                        profileSchool.setText(profile.school)
                        profileGrade.setText(profile.age) // age 필드를 grade로 매핑
                        profileAdvantage.setText(profile.advantage)
                        profilePractice.setText(profile.need_practice)
                        profileDream.setText(profile.dream)
                        profileSignature.setText(profile.signature)
                    }
                }
            } catch (e: Exception) {
                Log.e("SupabaseError", "Error fetching profile: ${e.message}")
            }
        }
    }

    suspend fun updateUserProfile(
        userId: Int,
        name: String?,
        school: String?,
        grade: String?,
        advantage: String?,
        practice: String?,
        dream: String?,
        signature: String?
    ) {
        val existingProfile = supabaseClient.from("profile").select {
            filter {
                eq("user_id", userId) // user_id로 필터링
            }
        }.decodeList<Profile>()

        // 업서트할 데이터 준비
        val profileData = mapOf(
            "user_id" to userId,
            "name" to name,
            "school" to school,
            "age" to grade,
            "advantage" to advantage,
            "need_practice" to practice,
            "dream" to dream,
            "signature" to signature
        )

        if (existingProfile.isNotEmpty()) {
            supabaseClient.from("profile").update(profileData) {
                filter {
                    eq("user_id", userId) // user_id로 조건 설정
                }
            }
        } else {
            supabaseClient.from("profile").insert(profileData)
        }
    }

    private fun disableProfileEditing() {
        // 모든 EditText를 비활성화
        profileName.isEnabled = false
        profileSchool.isEnabled = false
        profileGrade.isEnabled = false
        profileAdvantage.isEnabled = false
        profilePractice.isEnabled = false
        profileDream.isEnabled = false
        profileSignature.isEnabled = false
    }



    private fun showAlert(message: String) {
        // Show an alert dialog with the provided message
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

}