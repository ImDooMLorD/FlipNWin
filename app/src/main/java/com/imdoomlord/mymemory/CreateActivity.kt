package com.imdoomlord.mymemory

import android.Manifest
import android.content.ClipData
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.annotation.RequiresApi
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.imdoomlord.mymemory.models.BoardSize
import com.imdoomlord.mymemory.utils.EXTRA_BOARD_SIZE
import com.imdoomlord.mymemory.utils.isPermissionGranted

class CreateActivity : AppCompatActivity() {

    companion object {
        private const val TAG = "CreateActivity"
    }

    private lateinit var rvImagePicker: RecyclerView
    private lateinit var etGameName: EditText
    private lateinit var btSave: Button
    private lateinit var adapter: ImagePickerAdapter

    private lateinit var getContent: ActivityResultLauncher<String>
    private lateinit var requestPermissionLauncher: ActivityResultLauncher<String>

    private lateinit var boardSize: BoardSize
    private var numImagesRequired = -1
    private var chosenImageUris = mutableListOf<Uri>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create)

        rvImagePicker = findViewById(R.id.rvImagePicker)
        etGameName = findViewById(R.id.etGameName)
        btSave = findViewById(R.id.btSave)

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
                        if (Build.VERSION.SDK_INT >=Build.VERSION_CODES.TIRAMISU) {
                            requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_IMAGES)
                        } else {
                            requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
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
                    if(chosenImageUris.size < numImagesRequired){
                        chosenImageUris.add(uri)
                    }
                }
                adapter.notifyDataSetChanged()
                supportActionBar?.title = "Choose pics (${chosenImageUris.size} / $numImagesRequired)"
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
        return true

    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    private fun launchIntentForPhotos() {
        // intent to choose photos..
    getContent.launch("image/*")
    }
}
