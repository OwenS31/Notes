package com.paba.notes.ui.note.security_password

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paba.notes.R
import com.paba.notes.databinding.ActivitySecurityPasswordBinding
import com.paba.notes.helper.COLLECTION_NOTES
import com.paba.notes.ui.note.edit.EditNoteActivity
import com.paba.notes.ui.note.edit.EditNoteActivity.Companion.EXTRA_NOTE_ID

class NoteSecurityPasswordActivity : AppCompatActivity() {
    private lateinit var binding: ActivitySecurityPasswordBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

    private lateinit var noteId: String

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivitySecurityPasswordBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        // Get the note ID from the intent
        intent.getStringExtra(EXTRA_NOTE_ID).apply {
            noteId = this.toString()
        } ?: run { finish() }

        initView()
    }
    private fun initView() = binding.apply {
        btnLogout.isVisible = false
        btnVerify.setOnClickListener { validateInput() }
    }
    private fun validateInput() = binding.apply {

        showLoading(true)

        inputSecurityPassword.error = null

        val securityPassword = inputSecurityPassword.editText?.text.toString().trim()

        if (securityPassword.isBlank()) {

            inputSecurityPassword.error =
                getString(R.string.error_empty_field, getString(R.string.label_security_password))
            showLoading(false)
        } else {
            validateSecurityPassword()
        }
    }
    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        inputSecurityPassword.isEnabled = !isShowLoading

        btnVerify.apply {
            isEnabled = !isShowLoading
            text = if (isShowLoading) {
                // Show verifying text when loading
                getString(R.string.action_verifying)
            } else {
                // Show verify text when not loading
                getString(R.string.action_login)
            }
        }
    }

    private fun validateSecurityPassword() {
        // Get the security password from the input field
        val securityPassword = binding.inputSecurityPassword.editText?.text.toString().trim()

        // Get the security password from Firestore
        firebaseFirestore.collection(
            COLLECTION_NOTES
        ).document(noteId).get()
            .addOnSuccessListener { document ->
                // Get the security password from Firestore
                val securityPasswordDb = document["securityPassword"].toString()

                // Check if the input security password matches the security password from Firestore
                if (securityPassword == securityPasswordDb) {
                    // Navigate to the EditNoteActivity if the security password is correct
                    navigateToEditNoteActivity()
                } else {
                    // Show error if the security password is incorrect
                    showLoading(false)
                    binding.inputSecurityPassword.error =
                        getString(
                            R.string.error_invalid_field,
                            getString(R.string.label_security_password)
                        )
                }
            }
            .addOnFailureListener {
                // Show error if the security password is incorrect
                showLoading(false)
                binding.inputSecurityPassword.error =
                    getString(
                        R.string.error_invalid_field,
                        getString(R.string.label_security_password)
                    )
            }
    }

    private fun navigateToEditNoteActivity() {
        val intent = Intent(this, EditNoteActivity::class.java)
        intent.putExtra(EXTRA_NOTE_ID, noteId)
        startActivity(intent)
        finish()
    }

    companion object {
        /**
         * Key for passing the note ID as an argument.
         */
        const val EXTRA_NOTE_ID = "extra_note_id"
    }

}