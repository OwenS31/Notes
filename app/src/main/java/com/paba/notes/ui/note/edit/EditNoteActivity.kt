package com.paba.notes.ui.note.edit

import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paba.notes.R
import com.paba.notes.data.model.Note
import com.paba.notes.databinding.ActivityEditNoteBinding
import com.paba.notes.helper.COLLECTION_NOTES
import com.paba.notes.helper.generateToken
import com.paba.notes.ui.note.share.ShareNoteDialog
import java.util.Date

class EditNoteActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    private lateinit var binding: ActivityEditNoteBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

    private lateinit var noteId: String

    private var note: Note? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityEditNoteBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        // Get the note ID from the intent
        intent.getStringExtra(EXTRA_NOTE_ID)?.apply {
            // If the note ID is found, assign it to the noteId variable
            noteId = this
        } ?: run {
            // If the note ID is not found, show an error message and finish the activity
            showToast(getString(R.string.message_error_not_found, getString(R.string.note)))
            finish()
        }

        initView()
        loadData()
    }

    private fun loadData() {
        // Show the loading indicator
        showLoading(true)

        // Get the note data from Firestore
        firebaseFirestore.collection(COLLECTION_NOTES)
            .document(noteId)
            .get()
            .addOnSuccessListener { document ->
                // Hide the loading indicator
                showLoading(false)

                // If the document exists, assign it to the note variable
                if (document.exists()) {
                    // Convert the document to a Note object
                    note = document.toObject(Note::class.java)

                    // Set the title, content, and security password fields
                    binding.inputTitle.setText(note?.title)
                    binding.inputContent.setText(note?.content)
                    binding.inputSecurityPassword.editText?.setText(note?.securityPassword.orEmpty())
                    setSecurityPasswordEnabled(note?.securityPassword.isNullOrBlank().not())
                } else {
                    // If the document does not exist, show an error message and finish the activity
                    showToast(getString(R.string.message_error_not_found, getString(R.string.note)))
                    finish()
                }
            }
            .addOnFailureListener { exception ->
                // Hide the loading indicator
                showLoading(false)
                // Log the error message
                Log.e(javaClass.simpleName, exception.message, exception)

                // Show an error message and finish the activity
                showToast(getString(R.string.message_error_please_try_again))
                finish()
            }
    }

    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        progressBar.isVisible = isShowLoading
        inputTitle.isEnabled = !isShowLoading
        inputContent.isEnabled = !isShowLoading
        inputSecurityPassword.isEnabled = !isShowLoading
    }

    private fun setSecurityPasswordEnabled(isEnabled: Boolean) = binding.apply {
        inputSecurityPassword.isVisible = isEnabled
        toolbar.menu.findItem(R.id.lock).setIcon(
            if (isEnabled) R.drawable.lock_open else R.drawable.baseline_lock_24
        )
    }

    private fun initView() = binding.apply {
        toolbar.setNavigationOnClickListener { finish() }
        // Set the toolbar menu click listener
        toolbar.setOnMenuItemClickListener(this@EditNoteActivity)
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        // Handle the menu item click
        when (item?.itemId) {

            R.id.lock -> {
                // Toggle the security password input field
                setSecurityPasswordEnabled(binding.inputSecurityPassword.isVisible.not())
                return true
            }

            R.id.action_save -> {
                // Validate the input fields
                validateInput()
                finish()
                return true
            }

            R.id.action_share -> {
                // Show the share note dialog
                note?.let {
                    val modal = ShareNoteDialog.newInstance(it)
                    supportFragmentManager.let { fragmentManager ->
                        modal.show(fragmentManager, ShareNoteDialog.TAG)
                    }
                }
                return true
            }

            R.id.action_delete -> {
                // Show the delete note dialog
                note?.let { showDeleteNoteDialog() }
                return true
            }
        }

        return false
    }

    private fun showDeleteNoteDialog() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.title_delete_note))
            .setMessage(getString(R.string.message_delete_note))
            .setPositiveButton(getString(R.string.action_delete)) { dialog, _ ->
                // Dismiss the dialog and delete the note
                dialog.dismiss()
                deleteNote()
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    private fun deleteNote() {
        // Show the loading indicator
        showLoading(true)

        // Delete the note from Firestore
        firebaseFirestore.collection(COLLECTION_NOTES)
            .document(noteId)
            .delete()
            .addOnSuccessListener {
                // Hide the loading indicator
                showLoading(false)

                // Show a success message and finish the activity
                showToast(getString(R.string.message_success_delete))
                finish()
            }
            .addOnFailureListener { exception ->
                // Hide the loading indicator
                showLoading(false)
                // Log the error message
                Log.e(javaClass.simpleName, exception.message, exception)

                // Show an error message
                showToast(getString(R.string.message_error_please_try_again))
            }
    }

     // Validates the input fields for the note. If the input is valid, it proceeds to update the note.

    private fun validateInput() = binding.apply {
        // Show the loading indicator
        showLoading(true)
        // Hide the input errors
        hideInputErrors()

        // Get the input values
        val title = inputTitle.text.toString().trim()
        val content = inputContent.text.toString()
        val securityPasswordEnabled = inputSecurityPassword.isVisible
        val securityPassword = inputSecurityPassword.editText?.text?.toString()?.trim()
        var isInputValid = true

        // Validate the input fields
        if (title.isBlank()) {
            // Show an error message if the title is empty
            inputTitle.error =
                getString(R.string.error_empty_field, getString(R.string.hint_title))
            isInputValid = false
        }

        // Show an error message if the content is empty
        if (content.isBlank()) {
            // Show an error message if the content is empty
            inputContent.error =
                getString(R.string.error_empty_field, getString(R.string.hint_content))
            isInputValid = false
        }
        if (securityPasswordEnabled && securityPassword?.isBlank() == true) {
            // Show an error message if the security password is empty
            inputSecurityPassword.error =
                getString(R.string.error_empty_field, getString(R.string.hint_security_password))
            isInputValid = false
        }


        // Update the note if the input is valid
        if (isInputValid) updateNote(
            title,
            content,
            if (securityPasswordEnabled) securityPassword else null
        ) else showLoading(
            false
        )
    }

    private fun updateNote(title: String, content: String, securityPassword: String? = null) {
        // Show the loading indicator
        showLoading(true)

        // Update the note in Firestore
        firebaseFirestore.collection(COLLECTION_NOTES)
            .document(noteId)
            .update(
                // Update the note with the new details
                mapOf(
                    "title" to title,
                    "content" to content,
                    "securityPassword" to securityPassword,
                    "updatedAt" to Date(),
                    "token" to generateToken()
                )
            )
            .addOnSuccessListener {
                // Hide the loading indicator
                showLoading(false)
                // Show a success message
                showToast(getString(R.string.message_success_update))
            }
            .addOnFailureListener { exception ->
                // Hide the loading indicator
                showLoading(false)
                // Log the error message
                Log.e(javaClass.simpleName, exception.message, exception)
                // Show an error message
                showToast(getString(R.string.message_error_please_try_again))
            }
    }

    private fun hideInputErrors() {
        binding.inputTitle.error = null
        binding.inputContent.error = null
        binding.inputSecurityPassword.error = null
    }

    private fun showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    companion object {

//         Key for passing the note ID as an argument.
        const val EXTRA_NOTE_ID = "extra_note_id"
    }
}