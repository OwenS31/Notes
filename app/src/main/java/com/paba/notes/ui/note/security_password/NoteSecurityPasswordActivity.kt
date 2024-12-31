package com.paba.notes.ui.note.security_password

import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paba.notes.databinding.ActivitySecurityPasswordBinding
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

    }
}