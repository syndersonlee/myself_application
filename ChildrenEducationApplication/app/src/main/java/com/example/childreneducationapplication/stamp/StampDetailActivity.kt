package com.example.childreneducationapplication.stamp

import android.app.AlertDialog
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.media.ExifInterface
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Spinner
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.contract.ActivityResultContracts
import androidx.lifecycle.lifecycleScope
import com.example.childreneducationapplication.R
import com.example.childreneducationapplication.entity.StampCollection
import com.example.childreneducationapplication.externals.Supabase.downloadImage
import com.example.childreneducationapplication.externals.Supabase.supabaseClient
import com.example.childreneducationapplication.externals.Supabase.uploadFileWithUpsert
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.launch
import java.io.File

class StampDetailActivity : ComponentActivity() {
    private var userId: Int = 0
    private lateinit var giftNames: List<TextView>
    private lateinit var stampNumbers: List<Spinner>
    private lateinit var giftButtons: List<ImageView>
    private lateinit var giftBoxs: List<LinearLayout>
    private var giftFlag: Int? = null
    private lateinit var giftImages: List<ImageView>
    private var selectedImageIndex: Int? = null
    private val selectedImageUris = mutableMapOf<Int, Uri>() // Store selected URIs

    private val selectImageLauncher = registerForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { selectedImageUri ->
            selectedImageIndex?.let { index ->
                // Save the selected URI to be uploaded later
                selectedImageUris[index] = selectedImageUri
                giftImages[index].setImageURI(selectedImageUri) // Show selected image immediately
            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.fragment_stamp_collecting_detail)

        // Get userId from intent
        userId = intent.getIntExtra("user_id", 0)
        val role = intent.getStringExtra("role") ?: ""
        val saveButton = findViewById<LinearLayout>(R.id.stamp_save_button)
        val adjustStampButton = findViewById<LinearLayout>(R.id.adjust_stamp_number_button)

        // Initialize views
        initializeViews()


        lifecycleScope.launch {
            val stampCollection = fetchStampCollection(userId)
            stampCollection?.let {
                giftFlag = it.gift_flag
                loadStampData(it)
                toggleSingleButtonVisibility(it.gift_flag ?: -1)
            }
        }

        lifecycleScope.launch {
            initializeGiftImageViews()
            fetchGiftImages(userId)
        }

        giftButtonToggle()
        drillDownNumber(role)

        saveButton.setOnClickListener {
            lifecycleScope.launch {
                saveStampData(userId)
                showAlert("저장 완료")
            }
        }


        if(role == "학생") {
            adjustStampButton.visibility = View.INVISIBLE
        } else {
            adjustStampButton.setOnClickListener {
                val intent = Intent(this, StampAdjustActivity::class.java)
                intent.putExtra("user_id", userId)
                intent.putExtra("role", role)
                startActivity(intent)
            }
        }
    }


    private fun initializeViews() {
        // Initialize gift name TextViews
        giftNames = (0..4).map { index ->
            val giftNameId = "gift_name$index"
            val resId = resources.getIdentifier(giftNameId, "id", packageName)
            findViewById(resId)
        }

        // Initialize stamp number Spinners
        stampNumbers = (0..4).map { index ->
            val stampNumberId = "number_spinner$index"
            val resId = resources.getIdentifier(stampNumberId, "id", packageName)
            findViewById(resId)
        }
    }

    private fun initializeGiftImageViews() {
        giftImages = (0..4).map { index ->
            val imageId = "gift_image$index"
            val resId = resources.getIdentifier(imageId, "id", packageName)
            findViewById<ImageView>(resId).apply {
                setOnClickListener {
                    selectedImageIndex = index
                    selectImageLauncher.launch("image/*")
                }
            }
        }
    }

    private suspend fun fetchStampCollection(userId: Int): StampCollection? {
        return supabaseClient
            .from("stamp_collection")
            .select {
                filter { eq("user_id", userId) }
            }
            .decodeSingleOrNull<StampCollection>()
    }

    // Load data into the views
    private fun loadStampData(stampCollection: StampCollection) {
        stampCollection.let { collection ->
            // Set gift names
            giftNames[0].text = collection.gift_name0 ?: ""
            giftNames[1].text = collection.gift_name1 ?: ""
            giftNames[2].text = collection.gift_name2 ?: ""
            giftNames[3].text = collection.gift_name3 ?: ""
            giftNames[4].text = collection.gift_name4 ?: ""

            // Set stamp numbers in Spinners
            setSpinnerValue(stampNumbers[0], collection.stamp_number0)
            setSpinnerValue(stampNumbers[1], collection.stamp_number1)
            setSpinnerValue(stampNumbers[2], collection.stamp_number2)
            setSpinnerValue(stampNumbers[3], collection.stamp_number3)
            setSpinnerValue(stampNumbers[4], collection.stamp_number4)
        }
    }

    // Helper function to set Spinner value based on the stamp number
    private fun setSpinnerValue(spinner: Spinner, stampNumber: Int?) {
        stampNumber?.let {
            val adapter = spinner.adapter as ArrayAdapter<Int>
            val position = adapter.getPosition(it) // Find position in Spinner
            spinner.setSelection(position)
        }
    }

    private fun drillDownNumber(role: String) {
        // 5부터 30까지의 숫자를 리스트로 생성
        val numberList = (5..30).toList()

        // 각 Spinner에 대해 ArrayAdapter 설정
        for (i in 0..4) {
            val spinnerId = "number_spinner$i"
            val spinnerResId = resources.getIdentifier(spinnerId, "id", packageName)
            val numberSpinner: Spinner = findViewById(spinnerResId)

            // ArrayAdapter를 사용하여 Spinner에 데이터 연결
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, numberList)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            numberSpinner.adapter = adapter
        }
    }

    private fun giftButtonToggle() {
        // 버튼 리스트 초기화
        giftButtons = (0..4).map { index ->
            val buttonId = "icon_check_gift_list_$index"
            val resId = resources.getIdentifier(buttonId, "id", packageName)
            findViewById(resId)
        }

        // 버튼 리스트 초기화
        giftBoxs = (0..4).map { index ->
            val buttonId = "icon_check_gift_list_box_$index"
            val resId = resources.getIdentifier(buttonId, "id", packageName)
            findViewById(resId)
        }

        (0..4).map { index ->
            giftBoxs[index].setOnClickListener {
                giftFlag = index // Update the gift_flag when clicked
                toggleSingleButtonVisibility(index)
            }
        }
    }

    // 하나의 버튼을 VISIBLE로 만들고, 나머지는 INVISIBLE로 만드는 메서드
    private fun toggleSingleButtonVisibility(selectedIndex: Int) {
        giftButtons.forEachIndexed { index, button ->
            // 선택된 인덱스의 버튼만 VISIBLE, 나머지는 INVISIBLE로 설정
            button.visibility = if (index == selectedIndex) {
                ImageView.VISIBLE
            } else {
                ImageView.INVISIBLE
            }
        }
    }

    private suspend fun fetchGiftImages(userId: Int) {
        giftImages.forEachIndexed { index, imageView ->
            val imageName = "stamp_gift_user_${userId}_image_${index}.jpeg"

            // Define local file path
            val localFile = File(cacheDir, imageName) // Use cacheDir or other preferred directory

            // Check if the file exists locally and delete it if necessary
            if (localFile.exists()) {
                localFile.delete()
            }

            try {
                // Download the image from Supabase
                val bitmap = downloadImage(imageName)

                // Save the downloaded image to local storage
                bitmap?.let {
                    localFile.outputStream().use { outputStream ->
                        it.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }
                    // Display the downloaded image in the ImageView
                    imageView.setImageBitmap(bitmap)
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // Handle error (e.g., show a default image or log the error)
            }
        }
    }

    private suspend fun saveStampData(userId: Int) {
        val stampCollectionMap = mapOf(
            "user_id" to userId,
            "gift_name0" to giftNames[0].text.toString(),
            "stamp_number0" to (stampNumbers[0].selectedItem as? Int),
            "gift_name1" to giftNames[1].text.toString(),
            "stamp_number1" to (stampNumbers[1].selectedItem as? Int),
            "gift_name2" to giftNames[2].text.toString(),
            "stamp_number2" to (stampNumbers[2].selectedItem as? Int),
            "gift_name3" to giftNames[3].text.toString(),
            "stamp_number3" to (stampNumbers[3].selectedItem as? Int),
            "gift_name4" to giftNames[4].text.toString(),
            "stamp_number4" to (stampNumbers[4].selectedItem as? Int),
            "gift_flag" to giftFlag
        )

        val existingCollection = fetchStampCollection(userId)
        if (existingCollection == null) {
            supabaseClient.from("stamp_collection").insert(stampCollectionMap)
        } else {
            supabaseClient.from("stamp_collection").update(stampCollectionMap) {
                filter { eq("user_id", userId) }
            }
        }

        // Upload selected images
        selectedImageUris.forEach { (index, uri) ->
            val file = uriToFile(uri)
            file?.let {
                val response = uploadFileWithUpsert(it, "stamp_gift_user_${userId}_image_$index")
                if (response == null) {
                    Toast.makeText(this@StampDetailActivity, "Image upload failed", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    // Convert URI to File for upload
    private fun uriToFile(uri: Uri): File? {
        val filePathColumn = arrayOf(android.provider.MediaStore.Images.Media.DATA)
        val cursor = contentResolver.query(uri, filePathColumn, null, null, null)
        return if (cursor != null) {
            val columnIndex = cursor.getColumnIndexOrThrow(filePathColumn[0])
            cursor.moveToFirst()
            val filePath = cursor.getString(columnIndex)
            cursor.close()
            File(filePath)
        } else {
            null
        }
    }

    // Update ImageView with EXIF rotation handling
    private fun updateImageViewWithExifRotation(imagePath: String, imageView: ImageView) {
        try {
            val exifInterface = ExifInterface(imagePath)
            val orientation = exifInterface.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
            )

            val bitmap = BitmapFactory.decodeFile(imagePath)
            val rotatedBitmap = when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotateImage(bitmap, 90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotateImage(bitmap, 180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotateImage(bitmap, 270f)
                else -> bitmap // No rotation needed
            }

            imageView.setImageBitmap(rotatedBitmap)
        } catch (e: Exception) {
            e.printStackTrace()
            // If there's an error, handle it (e.g., show a default image or a toast message)
            Toast.makeText(this, "Failed to rotate image", Toast.LENGTH_SHORT).show()
        }
    }

    // Rotate the bitmap based on the given angle
    private fun rotateImage(source: Bitmap, angle: Float): Bitmap {
        val matrix = Matrix().apply { postRotate(angle) }
        return Bitmap.createBitmap(source, 0, 0, source.width, source.height, matrix, true)
    }

    private fun showAlert(message: String) {
        // Show an alert dialog with the provided message
        AlertDialog.Builder(this)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }
}