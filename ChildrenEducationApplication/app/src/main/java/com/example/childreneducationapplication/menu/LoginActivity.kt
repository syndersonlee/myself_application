package com.example.childreneducationapplication.menu

import android.content.Intent
import android.os.Bundle
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.User
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class LoginActivity : ComponentActivity() {

    private lateinit var studentLayout: LinearLayout
    private lateinit var guardianLayout: LinearLayout
    private var selectedRole: String = "학생" // 클릭된 역할

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_login)

        studentLayout = findViewById(R.id.studentLayout)
        guardianLayout = findViewById(R.id.guardianLayout)

        // 학생 레이아웃 클릭 시
        studentLayout.setOnClickListener {
            switchSelection("학생")
        }

        // 보호자 레이아웃 클릭 시
        guardianLayout.setOnClickListener {
            switchSelection("보호자")
        }


        val idValue = findViewById<EditText>(R.id.id_value)
        val loginButton = findViewById<LinearLayout>(R.id.login_button)

        loginButton.setOnClickListener {
            val userName = idValue.text.toString().trim()

            if (userName.isNotEmpty()) {
                lifecycleScope.launch {
                    handleLogin(userName)
                }
            } else {
                Toast.makeText(this, "ID를 입력해주세요.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun switchSelection(role: String) {
        selectedRole = role

        // 학생이 선택된 경우
        if (role == "학생") {
            studentLayout.setBackgroundResource(R.drawable.cr20bafe2ff)
            guardianLayout.setBackgroundResource(R.drawable.cr20ffec8a)
        }
        // 보호자가 선택된 경우
        else if (role == "보호자") {
            guardianLayout.setBackgroundResource(R.drawable.cr20bafe2ff)
            studentLayout.setBackgroundResource(R.drawable.cr20ffec8a)
        }
    }

    private suspend fun handleLogin(userName: String) {
        // user 테이블에서 name으로 조회
        val existingUser = supabaseClient.from("user").select {
            filter { eq("name", userName) }
        }.decodeSingleOrNull<User>()

        val userId = if (existingUser == null) {
            // 없으면 저장
            val newUser = mapOf(
                "name" to userName,
            )
            supabaseClient.from("user").insert(newUser)
            val insertedUser = supabaseClient.from("user").select {
                filter {
                    eq("name", userName)
                }
            }.decodeSingleOrNull<User>()
            insertedUser?.id
        } else {
            // 있으면 id 가져옴
            existingUser.id
        }

        // MenuActivity로 이동, userId 전달
        val intent = Intent(this@LoginActivity, MenuActivity::class.java)

        intent.putExtra("role", selectedRole)
        intent.putExtra("user_id", userId)

        println("role : ${selectedRole}, userId : $userId")
        startActivity(intent)
    }
}