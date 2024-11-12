package com.example.childreneducationapplication.stamp

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.os.Build
import android.os.Bundle
import android.view.Gravity
import android.widget.Button
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.annotation.RequiresApi
import androidx.lifecycle.lifecycleScope
import androidx.viewpager2.widget.ViewPager2
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.ObjectiveHistory
import com.example.childreneducationapplication.entity.SelfEvaluation
import com.example.childreneducationapplication.entity.StampCollection
import com.example.childreneducationapplication.entity.TodayStamp
import com.example.childreneducationapplication.entity.User
import com.example.childreneducationapplication.externals.Supabase.downloadImage
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.io.File
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter


class StampCollectingActivity : ComponentActivity() {
    private lateinit var giftNameTextView: TextView // Define TextView for gift name
    private lateinit var giftImageView: ImageView // ImageView for displaying the gift image
    private lateinit var userAccomplishTextView: TextView // TextView for user accomplish
    private lateinit var userAccuracyTextView: TextView // TextView for user accuracy
    private lateinit var userStampAllTextView: TextView // TextView for total stamps
    private lateinit var todayAccomplishStamp: TextView // TextView for today accomplish
    private lateinit var todayAccuracyStamp: TextView // TextView for today accuracy

    private lateinit var giftStampViewPager: ViewPager2
    private lateinit var giftStampAdapter: GiftStampAdapter

    private var userId: Int = 0 // Declare userId as a class property

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_stamp_collecting)

        // Get userId from intent
        val userId = intent.getIntExtra("user_id", 0)
        val role = intent.getStringExtra("role")
        this.userId = userId

        // Initialize TextViews
        giftNameTextView = findViewById(R.id.gift_name)
        giftImageView = findViewById(R.id.gift_image)
        userAccomplishTextView = findViewById(R.id.user_accomplish)
        userAccuracyTextView = findViewById(R.id.user_accuracy)
        userStampAllTextView = findViewById(R.id.user_stamp_all)
        todayAccomplishStamp = findViewById(R.id.today_accomplish_stamp)
        todayAccuracyStamp = findViewById(R.id.today_accuracy_stamp)
        giftStampViewPager = findViewById(R.id.gift_stamp_view_pager)

        // Fetch and display the gift name and image based on userId
        lifecycleScope.launch {
            val stampCollection = fetchStampCollection(userId)
            stampCollection?.let {
                updateGiftName(it)
                updateGiftImage(it, userId) // Fetch and display the image
            }
            fetchUserAchievements(userId)
            calculateTodayMetrics(userId)
            setupAdjustTodayStampButton(userId)
            setupDeleteStampForGift(userId)

            val userStampCount = (userStampAllTextView.text.toString().toIntOrNull() ?: 0)
            val giftStamp: Int = when (stampCollection?.gift_flag) {
                0 -> stampCollection.stamp_number0
                1 -> stampCollection.stamp_number1
                2 -> stampCollection.stamp_number2
                3 -> stampCollection.stamp_number3
                4 -> stampCollection.stamp_number4
                else -> null
            } ?: 0

            giftStampAdapter = GiftStampAdapter(
                this@StampCollectingActivity,
                userStampCount,
                giftStamp
            )

            giftStampViewPager.adapter = giftStampAdapter
        }


        // Button click to navigate to StampDetailActivity
        val giftButton = findViewById<LinearLayout>(R.id.button_gift)
        giftButton.setOnClickListener {
            val intent = Intent(this, StampDetailActivity::class.java)
            intent.putExtra("user_id", userId)
            intent.putExtra("role", role)
            startActivity(intent)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupDeleteStampForGift(userId: Int) {
        val adjustTodayStampButton = findViewById<Button>(R.id.remove_stamp_gift)

        adjustTodayStampButton.setOnClickListener {
            lifecycleScope.launch {
                val stampCollection: StampCollection? = fetchStampCollection(userId)
                val user = findUserByUserId(userId) ?: return@launch
                val giftFlag = stampCollection?.gift_flag
                if (giftFlag == null) {
                    showAlert("선택한 선물이 없습니다")
                    return@launch
                }

                val giftStamp: Int = when (giftFlag) {
                    0 -> stampCollection.stamp_number0
                    1 -> stampCollection.stamp_number1
                    2 -> stampCollection.stamp_number2
                    3 -> stampCollection.stamp_number3
                    4 -> stampCollection.stamp_number4
                    else -> return@launch
                } ?: 0

                val missionStamp = user.mission_stamp ?: 0
                val accuracyStamp = user.accuracy_stamp ?: 0

                if (missionStamp + accuracyStamp >= giftStamp) {
                    var renewalMissionStamp = missionStamp - giftStamp
                    val renewalAccuracyStamp = if (renewalMissionStamp < 0) {
                        missionStamp + accuracyStamp - giftStamp
                    } else accuracyStamp

                    if (renewalMissionStamp < 0) {
                        renewalMissionStamp = 0
                    }

                    updateMissionStamp(renewalMissionStamp, renewalAccuracyStamp, userId)
                    showAlert("스탬프를 차감했어요! 선물을 받아가세요")
                    refreshUI(userId)
                } else {
                    showAlert("스탬프가 모자라요!")
                }

            }
        }
    }

    private suspend fun updateMissionStamp(
        renewalMissionStamp: Int,
        renewalAccuracyStamp: Int,
        userId: Int
    ) {
        supabaseClient.from("user")
            .update(
                mapOf(
                    "mission_stamp" to renewalMissionStamp,
                    "accuracy_stamp" to renewalAccuracyStamp
                )
            ) {
                filter {
                    eq("id", userId)
                }
            }
    }


    private suspend fun fetchStampCollection(userId: Int): StampCollection? {

        val stampCollection = supabaseClient
            .from("stamp_collection")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeSingleOrNull<StampCollection>()

        if (stampCollection == null) {
            supabaseClient
                .from("stamp_collection")
                .insert(
                    mapOf(
                        "user_id" to userId,
                        "gift_name0" to "",
                        "stamp_number0" to 5,
                        "gift_flag" to 0
                    )
                )
        }

        return stampCollection ?: supabaseClient
            .from("stamp_collection")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeSingleOrNull<StampCollection>()
    }

    private fun updateGiftName(stampCollection: StampCollection) {
        val giftFlag = stampCollection.gift_flag ?: return // Exit if gift_flag is null

        val giftName = when (giftFlag) {
            0 -> stampCollection.gift_name0
            1 -> stampCollection.gift_name1
            2 -> stampCollection.gift_name2
            3 -> stampCollection.gift_name3
            4 -> stampCollection.gift_name4
            else -> null // In case gift_flag is out of expected range
        }

        // Set the gift name to the TextView
        giftNameTextView.text = giftName ?: "No gift available" // Default message if null
    }

    private suspend fun updateGiftImage(stampCollection: StampCollection, userId: Int) {
        val giftFlag = stampCollection.gift_flag ?: return // Exit if gift_flag is null
        val imageIndex = giftFlag // Using gift_flag as the index

        // Construct the image file name
        val imageFileName = "stamp_gift_user_${userId}_image_${imageIndex}.jpeg"

        val localFile = File(cacheDir, imageFileName) // Use cacheDir or other preferred directory

        // Check if the file exists locally and delete it if necessary
        if (localFile.exists()) {
            localFile.delete()
        }

        try {
            val bitmap = downloadImage(imageFileName)
            bitmap?.let {
                localFile.outputStream().use { outputStream ->
                    it.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                }
                // Display the downloaded image in the ImageView
                giftImageView.setImageBitmap(bitmap)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

    @SuppressLint("SetTextI18n")
    private suspend fun fetchUserAchievements(userId: Int) {
        // Fetch user accomplishment and accuracy
        val userRecord =
            findUserByUserId(userId) // Assuming User class exists with mission_stamp and accuracy_stamp fields

        userRecord?.let {
            userAccomplishTextView.text = it.mission_stamp.toString()
            userAccuracyTextView.text = it.accuracy_stamp.toString()
            userStampAllTextView.text =
                ((it.mission_stamp ?: 0) + (it.accuracy_stamp ?: 0)).toString()
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private suspend fun calculateTodayMetrics(userId: Int) {
        val yesterdayDate = LocalDate.now(ZoneId.of("Asia/Seoul")).minusDays(1)

        // Define the start and end timestamps for yesterday
        val startOfDay = yesterdayDate.atStartOfDay(ZoneId.of("Asia/Seoul")).toInstant()
        val endOfDay = yesterdayDate.atTime(23, 59, 59).atZone(ZoneId.of("Asia/Seoul")).toInstant()

        // Fetch timer_objective_history and order_objective_history for yesterday
        val timerHistoryList =
            fetchHistoryForYesterday("timer_objective_history", userId, startOfDay, endOfDay)
        val orderHistoryList =
            fetchHistoryForYesterday("order_objective_history", userId, startOfDay, endOfDay)

        // Combine both lists
        val todayHistoryList = timerHistoryList + orderHistoryList

        // Calculate todayPassCount
        val todayPassCount = todayHistoryList.count { it.pass == true }
        val todayPassPercentage = (todayPassCount.toDouble() / todayHistoryList.size) * 100

        // Fetch self_evaluation to compare with target_percent
        val selfEvaluation = fetchSelfEvaluation(userId)
        val todayStampNumber = selectTodayStamp(userId)
        // Check if todayPassPercentage exceeds target_percent
        todayAccomplishStamp.text =
            if (todayPassPercentage > (selfEvaluation?.target_percent
                    ?: 0)
            ) todayStampNumber.toString() else "0"

        // Calculate todayMatchingCount
        val todayMatchingCount = todayHistoryList.count { it.pass == (it.parents_approve ?: false) }
        val todayMatchingPercentage = (todayMatchingCount.toDouble() / todayHistoryList.size) * 100

        // Check if todayMatchingPercentage exceeds target_percent
        todayAccuracyStamp.text =
            if (todayMatchingPercentage > (selfEvaluation?.target_percent
                    ?: 0)
            ) todayStampNumber.toString() else "0"
    }

    private suspend fun fetchHistoryForYesterday(
        tableName: String,
        userId: Int,
        startOfDay: Instant,
        endOfDay: Instant
    ): List<ObjectiveHistory> {
        return supabaseClient
            .from(tableName)
            .select {
                filter {
                    eq("user_id", userId)
                    gte("created_at", startOfDay)
                    lte("created_at", endOfDay)
                }
            }
            .decodeList<ObjectiveHistory>() // Assuming History class exists with the required fields
    }

    private suspend fun fetchSelfEvaluation(userId: Int): SelfEvaluation? {
        return supabaseClient
            .from("self_evaluation")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeSingleOrNull<SelfEvaluation>()
    }

    private suspend fun selectTodayStamp(userId: Int): Int {
        // Query today_stamp table for user's current stamp count
        val existedTodayStamp = supabaseClient.from("today_stamp").select {
            filter { eq("user_id", userId) }
        }.decodeSingleOrNull<TodayStamp>()

        return if (existedTodayStamp == null) {
            // Insert default value of 1 if no record found
            val newTodayStamp = mapOf("user_id" to userId, "today_stamp" to 1)
            supabaseClient.from("today_stamp").insert(newTodayStamp)
            1
        } else {
            existedTodayStamp.today_stamp ?: 1
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun setupAdjustTodayStampButton(userId: Int) {
        val adjustTodayStampButton = findViewById<Button>(R.id.adjust_today_stamp)

        adjustTodayStampButton.setOnClickListener {
            lifecycleScope.launch {
                // Fetch the user record by user_id
                val userRecord = findUserByUserId(userId)

                userRecord?.let { user ->
                    val lastStampUpdate = user.last_stamp_update
                    println("lastStampUpdate : $lastStampUpdate")
                    if (lastStampUpdate == null) {
                        showAlert("이미 모든 스탬프를 등록했어요!")
                        return@launch
                    }

                    val parsingLocalDate =
                        LocalDate.parse(lastStampUpdate, DateTimeFormatter.ofPattern("yyyy-MM-dd"))

                    // Get today's date
                    val todayDate = LocalDate.now(ZoneId.of("Asia/Seoul"))

                    if (parsingLocalDate.isEqual(todayDate)) {
                        showAlert("이미 모든 스탬프를 등록했어요!")
                        return@launch
                    }

                    // Get the values from the TextViews
                    val todayAccomplish =
                        todayAccomplishStamp.text.toString().toIntOrNull() ?: 0
                    val todayAccuracy = todayAccuracyStamp.text.toString().toIntOrNull() ?: 0

                    // Update the user table by adding to mission_stamp and accuracy_stamp
                    val updatedMissionStamp = (user.mission_stamp ?: 0) + todayAccomplish
                    val updatedAccuracyStamp = (user.accuracy_stamp ?: 0) + todayAccuracy

                    // Update the user table
                    supabaseClient.from("user").update(
                        {
                            set("mission_stamp", updatedMissionStamp)
                            set("accuracy_stamp", updatedAccuracyStamp)
                            set("last_stamp_update", LocalDate.now().toString())
                        }
                    ) {
                        filter {
                            eq("id", userId) // Filter by userId
                        }
                    }
                    // Refresh the UI after the update
                    refreshUI(userId)

                }
            }
        }
    }

    private suspend fun findUserByUserId(userId: Int): User? {
        val userRecord = supabaseClient
            .from("user")
            .select {
                filter { eq("id", userId) }
            }
            .decodeSingleOrNull<User>()
        return userRecord
    }

    private fun showAlert(message: String) {
        // Show an alert dialog with the provided message
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    @RequiresApi(Build.VERSION_CODES.O)
    override fun onResume() {
        super.onResume()
        // Refresh the UI every time the activity is resumed
        lifecycleScope.launch {
            val stampCollection = fetchStampCollection(userId)
            stampCollection?.let {
                updateGiftName(it)
                updateGiftImage(it, userId) // Fetch and display the image
            }
            calculateTodayMetrics(userId)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun refreshUI(userId: Int) {
        lifecycleScope.launch {
            fetchUserAchievements(userId)
            calculateTodayMetrics(userId)
        }
    }

}