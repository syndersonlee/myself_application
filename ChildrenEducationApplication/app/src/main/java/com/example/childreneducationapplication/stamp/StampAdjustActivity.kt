package com.example.childreneducationapplication.stamp

import android.app.AlertDialog
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.LinearLayout
import android.widget.Spinner
import androidx.activity.ComponentActivity
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.TodayStamp
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch

class StampAdjustActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_adjust_stamp)

        val userId = intent.getIntExtra("user_id", 0)

        lifecycleScope.launch {
            setupSpinner(userId)
        }

        // Handle evaluation_save_button click to save the selected stamp count
        findViewById<LinearLayout>(R.id.evaluation_save_button).setOnClickListener {
            lifecycleScope.launch {
                saveSelectedStamp(userId)
                showSavedAlert()
            }
        }
    }

    private suspend fun setupSpinner(userId: Int) {
        // Find the Spinner by its ID
        val stampNumberSpinner: Spinner = findViewById(R.id.stamp_number_count)

        // Define values for the spinner (0 to 3)
        val stampNumbers = listOf(0, 1, 2, 3)

        // Set up ArrayAdapter for spinner
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, stampNumbers)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        stampNumberSpinner.adapter = adapter

        // Set default value from the database or 1 if not found
        stampNumberSpinner.setSelection(stampNumbers.indexOf(selectTodayStamp(userId)))
    }

    private suspend fun selectTodayStamp(userId: Int): Int {
        // Query today_stamp table for user's current stamp count
        val existedTodayStamp = supabaseClient.from("today_stamp").select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<TodayStamp>()

        return if (existedTodayStamp == null) {
            // Insert default value of 1 if no record found
            val newTodayStamp = mapOf("user_id" to userId, "today_stamp" to 1)
            supabaseClient.from("today_stamp").delete {
                filter { eq("user_id", userId) }
            }
            supabaseClient.from("today_stamp").insert(newTodayStamp)
            1
        } else {
            existedTodayStamp.today_stamp ?: 1
        }
    }

    private suspend fun saveSelectedStamp(userId: Int) {
        // Get selected stamp count from the spinner
        val stampNumberSpinner: Spinner = findViewById(R.id.stamp_number_count)
        val selectedStamp = stampNumberSpinner.selectedItem as Int

        // Update today_stamp table with selected value
        val updateTodayStamp = mapOf("today_stamp" to selectedStamp)
        supabaseClient.from("today_stamp").update(updateTodayStamp) {
            filter { eq("user_id", userId) }
        }
    }

    private fun showSavedAlert() {
        // Show an alert dialog to indicate save success
        AlertDialog.Builder(this)
            .setMessage("저장되었습니다")
            .setPositiveButton("OK", null)
            .show()
    }
}