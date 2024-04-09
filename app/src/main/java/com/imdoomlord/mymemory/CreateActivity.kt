package com.imdoomlord.mymemory

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.ImageDecoder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.MediaStore
import android.text.Editable
import android.text.InputFilter
import android.text.TextWatcher
import android.util.Log
import android.view.MenuItem
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.storage.storage
import com.imdoomlord.mymemory.models.BoardSize
import com.imdoomlord.mymemory.utils.BitmapScaler
import com.imdoomlord.mymemory.utils.EXTRA_BOARD_SIZE
import com.imdoomlord.mymemory.utils.EXTRA_GAME_NAME
import com.imdoomlord.mymemory.utils.isPermissionGranted
import kotlinx.coroutines.handleCoroutineException
import java.io.ByteArrayOutputStream

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateActivity"
        private const val MIN_GAME_NAME_LENGTH = 3
        private const val MAX_GAME_NAME_LENGTH = 14

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        private const val READ_PHOTOS_PERMISSION = android.Manifest.permission.READ_MEDIA_IMAGES

        private const val READ_STORAGE_PERMISSION =
            android.Manifest.permission.READ_EXTERNAL_STORAGE

    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btSave: Button
    private lateinit var pbUploading: ProgressBar

    private lateinit var adapter: ImagePickerAdapter

    private lateinit var getContent: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private var chosenImageUris = mutableListOf<Uri>()

    private val storage = Firebase.storage
    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btSave = findViewById(R.id.btSave)
        pbUploading = findViewById(R.id.pbUploading)

        // Provides supports for menu in activities
        val toolbar = findViewById<androidx.appcompat.widget.Toolbar>(R.id.toolbar)
        setSupportActionBar(toolbar)

        supportActionBar?.setDisplayHomeAsUpEnabled(true)



        boardSize = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            intent.getSerializableExtra(EXTRA_BOARD_SIZE, BoardSize::class.java) as BoardSize
        } else {
            intent.getSerializableExtra(EXTRA_BOARD_SIZE) as BoardSize
        }


        numImagesRequired = boardSize.getNumPairs()
        supportActionBar?.title = "Choose pics (0 / $numImagesRequired)"

        btSave.setOnClickListener {
            saveDataToFirebase()
        }

        etGameName.filters = arrayOf(InputFilter.LengthFilter(MAX_GAME_NAME_LENGTH))
        etGameName.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}

            override fun afterTextChanged(s: Editable?) {
                btSave.isEnabled = shouldEnableSaveButton()
            }

        })

        adapter = ImagePickerAdapter(
            this,
            chosenImageUris,
            boardSize,
            object : ImagePickerAdapter.ImageClickListener {

                override fun onPlaceholderClicked() {
                    if (isPermissionGranted(
                            this@CreateActivity,
                            Manifest.permission.READ_MEDIA_IMAGES
                        )
                    ) {
                        // Permission is already granted, you can perform your operation here
                        launchIntentForPhotos()
                    } else {
                        // Permission is not granted, request the permission
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(READ_PHOTOS_PERMISSION)
                        } else {
                            requestPermissionLauncher.launch(READ_STORAGE_PERMISSION)
                        }

                    }

                }

            })
        rvImagePicker.adapter = adapter
        rvImagePicker.setHasFixedSize(true)
        rvImagePicker.layoutManager = GridLayoutManager(this, boardSize.getWidth())

        // Define the contract and the result handler
        getContent =
            registerForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris: List<Uri> ->
                // Handle the returned Uris
                if (uris.isEmpty() || uris == null) {
                    Log.w(
                        TAG,
                        "Did not get the data back from the launched activity, user likely cancelled flow"
                    )
                }
                // Multiple images were selected
                for (uri in uris) {
                    // Use the Uri
                    if (chosenImageUris.size < numImagesRequired) {
                        chosenImageUris.add(uri)
                    }
                }
                adapter.notifyDataSetChanged()
                supportActionBar?.title =
                    "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
                btSave.isEnabled = shouldEnableSaveButton()
            }




        requestPermissionLauncher =
            registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted: Boolean ->
                if (isGranted) {
                    // Permission has been granted, you can perform your operation here
                    launchIntentForPhotos()
                } else {
                    // Permission has been denied, handle accordingly
                    Toast.makeText(
                        this,
                        "permissions dene padeaga aise Photos kaise lunga mai...",
                        Toast.LENGTH_LONG
                    ).show()
                }
            }


    }

    private fun shouldEnableSaveButton(): Boolean {
        // Check if we should enable save button or not
        if (chosenImageUris.size < numImagesRequired) {
            return false
        }
        if (etGameName.text.length < MIN_GAME_NAME_LENGTH || etGameName.text.isBlank()) {
            return false
        }
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun saveDataToFirebase() {
        Log.i(TAG, "saveDataToFirebase")
        btSave.isEnabled = false
        val customGameName = etGameName.text.toString()

        // Check that we are not overwriting someone else's data
        db.collection("games").document(customGameName).get().addOnSuccessListener { document ->
            if (document != null && document.data != null) {
                AlertDialog.Builder(this)
                    .setTitle("Name Taken")
                    .setMessage("A Game already exists with this name $customGameName. Please Choose Another")
                    .setPositiveButton("OK", null)
                    .show()
                btSave.isEnabled = true
            } else {
                // Handle Image Uploading of unique game names
                handleImageUploading(customGameName)

            }
        }.addOnFailureListener { exception ->
            Log.e(TAG, "Encountered error while saving memory game", exception)
            Toast.makeText(this, "Encountered error while saving memory game", Toast.LENGTH_SHORT).show()
            btSave.isEnabled = true
        }

    }

    private fun handleImageUploading(gameName: String) {

        pbUploading.visibility = View.VISIBLE
        var didEncounterError = false
        val uploadedImageUrls = mutableListOf<String>()
        for ((index, photoUri) in chosenImageUris.withIndex()) {
            val imageByteArray = getImageByteArray(photoUri)

            // Defining image file path with its name
            val filePath = "images/$gameName/${System.currentTimeMillis()}-${index}.jpg"

            val photoReference = storage.reference.child(filePath)

            // Function that helps us uploading file to cloud
            photoReference.putBytes(imageByteArray)
                .continueWithTask { photoUploadTask ->
                    Log.i(TAG, "Upload Bytes: ${photoUploadTask.result?.bytesTransferred}")
                    // Task in which we download the url to file we just uploaded
                    photoReference.downloadUrl
                }.addOnCompleteListener { downloadUrlTask ->
                    if (!downloadUrlTask.isSuccessful) {
                        Log.e(TAG, "Exception with Firebase Storage", downloadUrlTask.exception)
                        Toast.makeText(this, "Failed to upload image", Toast.LENGTH_SHORT).show()
                        didEncounterError = true
                        return@addOnCompleteListener
                    }
                    if (didEncounterError) {
                        pbUploading.visibility = View.GONE
                        return@addOnCompleteListener
                    }
                    val downloadUrl = downloadUrlTask.result.toString()
                    uploadedImageUrls.add(downloadUrl)
                    pbUploading.progress = uploadedImageUrls.size / chosenImageUris.size
                    Log.i(
                        TAG,
                        "Finished Uploading $photoUri num uploaded ${uploadedImageUrls.size} "
                    )
                    if (uploadedImageUrls.size == chosenImageUris.size) {
                        handleAllImagesUploaded(gameName, uploadedImageUrls)
                    }
                }


        }

    }

    private fun handleAllImagesUploaded(gameName: String, imageUrls: MutableList<String>) {
        // Each memory game is document
        // Every document lives in a collection
        // here, collection is games
        db.collection("games").document(gameName)
            .set(mapOf("images" to imageUrls))
            .addOnCompleteListener { gameCreationTask ->
                pbUploading.visibility = View.GONE
                if (!gameCreationTask.isSuccessful) {
                    Log.e(TAG, "Exception with game creation", gameCreationTask.exception)
                    Toast.makeText(this, "Failed Game Creation", Toast.LENGTH_SHORT).show()
                    return@addOnCompleteListener
                }
                Log.i(TAG, "Successfully created game $gameName")
                AlertDialog.Builder(this)
                    .setTitle("Upload complete! Let's Play your game '$gameName'")
                    .setPositiveButton("OK") { _,_ ->
                        val resultData = Intent()
                        resultData.putExtra(EXTRA_GAME_NAME, gameName)
                        setResult(Activity.RESULT_OK, resultData)
                        finish()
                    }.show()
            }

    }

    private fun getImageByteArray(photoUri: Uri): ByteArray {
        val originalBitmap = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            val source = ImageDecoder.createSource(contentResolver, photoUri)
            ImageDecoder.decodeBitmap(source)

        } else {
            MediaStore.Images.Media.getBitmap(contentResolver, photoUri)
        }
        Log.i(TAG, "Original width ${originalBitmap.width} and height ${originalBitmap.height}")
        val scaledBitmap = BitmapScaler.scaleToFitHeight(originalBitmap, 250)
        Log.i(TAG, "Scaled width ${scaledBitmap.width} and height ${scaledBitmap.height}")
        val byteOutputStream = ByteArrayOutputStream()
        scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 60, byteOutputStream)
        return byteOutputStream.toByteArray()


    }

    private fun launchIntentForPhotos() {
        // intent to choose photos..
        getContent.launch("image/*")
    }
}
