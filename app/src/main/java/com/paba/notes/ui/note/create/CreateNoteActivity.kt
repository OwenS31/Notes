package com.paba.notes.ui.note.create

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paba.notes.R
import com.paba.notes.data.model.Note
import com.paba.notes.databinding.ActivityCreateNoteBinding
import com.paba.notes.helper.generateToken
import kotlinx.coroutines.launch
import java.util.Date

class CreateNoteActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    private lateinit var binding: ActivityCreateNoteBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityCreateNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        initView()
    }

    private fun initView() = binding.apply {
        toolbar.setNavigationOnClickListener { finish() }
        toolbar.setOnMenuItemClickListener(this@CreateNoteActivity)
    }

    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        progressBar.isVisible = isShowLoading
        inputTitle.isEnabled = !isShowLoading
        inputContent.isEnabled = !isShowLoading
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        when (item?.itemId) {

            R.id.action_save -> {
                // Validate the input fields
                validateInput()
                return true
            }
        }
        return false
    }

//     Validates the input fields for the note.
//     If the input is valid, it proceeds to store the note.

    private fun validateInput() = binding.apply {
        // Show loading state and hide input errors
        showLoading(true)
        // Hide input errors
        hideInputErrors()

        // Get the input values
        val title = inputTitle.text.toString().trim()
        val content = inputContent.text.toString()
        var isInputValid = true

        // Validate the input fields
        if (title.isBlank()) {
            // Show error for empty title
            inputTitle.error =
                getString(R.string.error_empty_field, getString(R.string.hint_title))
            isInputValid = false
        }

        // Show error for empty content
        if (content.isBlank()) {
            // Show error for empty content
            inputContent.error =
                getString(R.string.error_empty_field, getString(R.string.hint_content))
            isInputValid = false
        }


        // Store the note if the input is valid
        if (isInputValid) storeNote(
            title,
            content,
        ) else showLoading(
            false
        )
    }


    private fun storeNote(
        title: String,
        content: String
    ) {
        // Show loading state
        showLoading(true)

        lifecycleScope.launch {
            // Get the current user ID
            firebaseAuth.currentUser?.uid?.let { userId ->
                // Create a new note with the provided details
                val currentDate = Date()
                val note = Note(
                    title = title,
                    content = content,
                    userIds = listOf(userId),
                    createdAt = currentDate,
                    updatedAt = currentDate,
                    token = generateToken()
                )

                // Store the note in Firestore
                firebaseFirestore.collection("notes")
                    .add(note)
                    .addOnSuccessListener {
                        // Finish the activity
                        finish()
                    }
                    .addOnFailureListener { exception ->
                        // Hide loading state and show error message
                        showLoading(false)
                        // Log the error
                        Log.e(this.javaClass.simpleName, exception.localizedMessage, exception)

                        // Show error message
                        Snackbar.make(
                            binding.main,
                            R.string.message_error_please_try_again,
                            Snackbar.LENGTH_LONG
                        ).show()
                    }
            }
        }
    }

    // Hides any input errors displayed on the form.
    private fun hideInputErrors() {
        binding.inputTitle.error = null
        binding.inputContent.error = null
    }
}