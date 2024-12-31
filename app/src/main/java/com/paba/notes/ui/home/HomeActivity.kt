package com.paba.notes.ui.home

import android.app.AlertDialog
import android.app.ProgressDialog
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.MenuItem
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.codescanner.GmsBarcodeScannerOptions
import com.google.mlkit.vision.codescanner.GmsBarcodeScanning
import com.paba.notes.R
import com.paba.notes.data.model.Note
import com.paba.notes.databinding.ActivityHomeBinding
import com.paba.notes.helper.COLLECTION_NOTES
import com.paba.notes.ui.login.LoginActivity
import com.paba.notes.ui.note.create.CreateNoteActivity
import com.paba.notes.ui.note.edit.EditNoteActivity
import com.paba.notes.ui.note.security_password.NoteSecurityPasswordActivity


class HomeActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    private lateinit var binding: ActivityHomeBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

    private val noteListAdapter = NoteListAdapter()

    private val noteSearchAdapter = NoteListAdapter()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityHomeBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }


        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        initView()
        observeData()
    }


    private fun observeData() {

        showLoading(true)


        firebaseFirestore.collection(COLLECTION_NOTES)

            .whereArrayContains("userIds", firebaseAuth.currentUser?.uid!!).get()

            .addOnSuccessListener { snapshot ->

                showLoading(false)

                val notes = snapshot.toObjects(Note::class.java)

                binding.emptyNotes.isVisible = notes.isEmpty()

                noteListAdapter.submitList(notes)
            }.addOnFailureListener { exception ->

                Log.e(this.javaClass.simpleName, exception.message.toString(), exception)
                showLoading(false)
            }
    }


    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        progressBar.isVisible = isShowLoading
        rvNotes.isVisible = !isShowLoading
    }


    override fun onResume() {
        super.onResume()
        observeData()
    }


    private fun initView() = binding.apply {

        rvNotes.apply {

            layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)

            adapter = noteListAdapter

            setHasFixedSize(false)

            noteListAdapter.onItemClick = { note ->
                note.id?.let { noteId ->

                    if (note.securityPassword.isNullOrBlank()) {

                        navigateToEditNoteActivity(noteId)
                    } else {
                        navigateToNoteSecurityPasswordActivity(noteId)
                    }
                }
            }
        }


        searchResult.apply {

            layoutManager = StaggeredGridLayoutManager(1, StaggeredGridLayoutManager.VERTICAL)

            adapter = noteSearchAdapter

            setHasFixedSize(false)
        }


        searchView.editText.addTextChangedListener(onTextChanged = { text, _, _, _ ->

            if (text == null) return@addTextChangedListener

            val query = text.toString().trim()

            searchNotes(query)
        })

        searchBar.setOnMenuItemClickListener(this@HomeActivity)
        fabCreateNote.setOnClickListener { navigateToCreateNoteActivity() }
    }

    private fun navigateToEditNoteActivity(id: String) {
        val intent = Intent(this, EditNoteActivity::class.java)
        intent.putExtra(EditNoteActivity.EXTRA_NOTE_ID, id)
        startActivity(intent)
    }

    private fun navigateToNoteSecurityPasswordActivity(id: String) {
        val intent = Intent(this, NoteSecurityPasswordActivity::class.java)
        intent.putExtra(NoteSecurityPasswordActivity.EXTRA_NOTE_ID, id)
        startActivity(intent)
    }

    private fun searchNotes(query: String) {

        binding.searchProgressBar.isVisible = true

        val lowercaseQuery = query.lowercase()


        firebaseFirestore.collection(COLLECTION_NOTES)

            .whereArrayContains("userIds", firebaseAuth.currentUser?.uid!!).get()

            .addOnSuccessListener { snapshot ->

                binding.searchProgressBar.isVisible = false

                val notes = snapshot.toObjects(Note::class.java)

                val filteredNotes = notes.filter { note ->
                    note.title?.lowercase()
                        ?.contains(lowercaseQuery) == true || note.content?.lowercase()
                        ?.contains(lowercaseQuery) == true
                }


                binding.searchResult.isVisible = filteredNotes.isNotEmpty()

                noteSearchAdapter.submitList(filteredNotes)
            }.addOnFailureListener { exception ->

                Log.e(this.javaClass.simpleName, exception.message.toString(), exception)

                binding.searchProgressBar.isVisible = false


                Snackbar.make(
                    binding.searchView,
                    getString(R.string.message_error_please_try_again),
                    Snackbar.LENGTH_SHORT
                ).show()
            }
    }

    private fun navigateToCreateNoteActivity() {
        val intent = Intent(this, CreateNoteActivity::class.java)
        startActivity(intent)
    }

    @Suppress("DEPRECATION")
    @Override
    override fun onBackPressed() {
        if (binding.searchView.isShowing) {
            binding.searchView.hide()
        } else {
            super.onBackPressed()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {

            R.id.action_logout -> {
                showLogoutDialog()
                true
            }

            R.id.action_scan_qr -> {
                showQRCodeScanner()
                true
            }

            else -> false
        }
    }


    private fun showQRCodeScanner() {

        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_QR_CODE
        ).enableAutoZoom().build()


        val scanner = GmsBarcodeScanning.getClient(this, options)

        scanner.startScan().addOnSuccessListener { barcode ->

            val data = barcode.rawValue?.split("  ")

            if (data.isNullOrEmpty() || data.size != 2) {
                showSnackbar(getString(R.string.message_invalid_qr_code))
                return@addOnSuccessListener
            }


            importNote(data)
        }
    }

    private fun importNote(data: List<String>) {

        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.message_loading_importing_note))
        progressDialog.setCancelable(false)
        progressDialog.show()


        val noteId = data.first()
        val token = data.last()


        firebaseFirestore.collection(COLLECTION_NOTES)
            .document(noteId).get()

            .addOnSuccessListener { document ->

                if (document.exists()) {

                    val note = document.toObject(Note::class.java)


                    if (note != null && note.token == token) {

                        progressDialog.dismiss()


                        val alertDialog =
                            AlertDialog.Builder(this).setTitle(getString(R.string.action_add_note))
                                .setMessage(getString(R.string.message_import_note, note.title))
                                .setPositiveButton(getString(R.string.action_yes)) { _, _ ->

                                    showLoading(true)


                                    firebaseFirestore.collection(COLLECTION_NOTES).document(noteId)

                                        .update(
                                            "userIds",
                                            note.userIds?.plus(firebaseAuth.currentUser?.uid!!)
                                                ?.distinct()
                                        ).addOnSuccessListener {

                                            showLoading(false)


                                            observeData()


                                            showSnackbar(getString(R.string.message_success_import_note))
                                        }.addOnFailureListener { exception ->

                                            showLoading(false)
                                            Log.e(
                                                this.javaClass.simpleName,
                                                exception.message.toString(),
                                                exception
                                            )

                                            showSnackbar(getString(R.string.message_error_please_try_again))
                                        }
                                }
                                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ -> dialog.dismiss() }
                                .create()

                        alertDialog.show()
                    }
                } else {

                    progressDialog.dismiss()

                    showSnackbar(getString(R.string.message_invalid_qr_code))
                }
            }
    }

    private fun showSnackbar(message: String) {
        Snackbar.make(binding.main, message, Snackbar.LENGTH_SHORT).show()
    }

    private fun showLogoutDialog() {
        val alertDialog = androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_logout))
            .setMessage(getString(R.string.message_logout))
            .setPositiveButton(getString(R.string.action_yes)) { _, _ ->

                logout()
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->

                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    private fun logout() {

        firebaseAuth.signOut()

        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}