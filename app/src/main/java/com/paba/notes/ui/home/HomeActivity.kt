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

/**
 * HomeActivity is responsible for displaying the home screen where users can view, search, and manage their notes.
 * It interacts with Firebase to fetch and display notes, and provides navigation to other activities for note creation, editing, and security password verification.
 */
class HomeActivity : AppCompatActivity(), Toolbar.OnMenuItemClickListener {

    /**
     * View binding for the activity.
     */
    private lateinit var binding: ActivityHomeBinding

    /**
     * Firebase Authentication instance.
     */
    private lateinit var firebaseAuth: FirebaseAuth

    /**
     * Firebase Firestore instance.
     */
    private lateinit var firebaseFirestore: FirebaseFirestore

    /**
     * Adapter for displaying the list of notes.
     */
    private val noteListAdapter by lazy { NoteListAdapter() }

    /**
     * Adapter for displaying the search results.
     */
    private val noteSearchAdapter by lazy { NoteListAdapter() }

    /**
     * Called when the activity is starting. This is where most initialization should go.
     * It initializes view binding, Firebase instances, and sets up the window insets listener.
     *
     * @param savedInstanceState If the activity is being re-initialized after previously being shut down then this Bundle contains the data it most recently supplied in onSaveInstanceState(Bundle).
     */
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

        // Initialize Firebase instances
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        initView()
        observeData()
    }

    /**
     * Observes data changes in Firestore and updates the UI accordingly.
     */
    private fun observeData() {
        // Show loading progress bar
        showLoading(true)

        // Fetch notes from Firestore
        firebaseFirestore.collection(COLLECTION_NOTES)
            // Filter notes by the current user's ID
            .whereArrayContains("userIds", firebaseAuth.currentUser?.uid!!).get()
            // On successful fetch, update the UI with the notes
            .addOnSuccessListener { snapshot ->
                // Hide loading progress bar
                showLoading(false)

                // Convert the Firestore snapshot to a list of Note objects
                val notes = snapshot.toObjects(Note::class.java)
                // If the list of notes is empty, show the empty notes view
                binding.emptyNotes.isVisible = notes.isEmpty()
                // Submit the list of notes to the adapter
                noteListAdapter.submitList(notes)
            }.addOnFailureListener { exception ->
                // Log the error message and stack trace
                Log.e(this.javaClass.simpleName, exception.message.toString(), exception)
                showLoading(false)
            }
    }

    /**
     * Shows or hides the loading state.
     *
     * @param isShowLoading Boolean indicating whether to show the loading state.
     */
    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        progressBar.isVisible = isShowLoading
        rvNotes.isVisible = !isShowLoading
    }

    /**
     * Called when the activity is resumed. It re-observes data changes in Firestore.
     */
    override fun onResume() {
        super.onResume()
        observeData()
    }

    /**
     * Initializes the view and sets up click listeners for the UI elements.
     */
    private fun initView() = binding.apply {
        // Setup RecyclerView for displaying notes
        rvNotes.apply {
            // Set the layout manager to StaggeredGridLayoutManager with 2 columns
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            // Set the adapter to the noteListAdapter
            adapter = noteListAdapter
            // Set hasFixedSize to false to allow the RecyclerView to have a dynamic size
            setHasFixedSize(false)

            // Set the onItemClick listener for the noteListAdapter
            noteListAdapter.onItemClick = { note ->
                // If the note's ID is not null, navigate to the EditNoteActivity
                note.id?.let { noteId ->
                    navigateToEditNoteActivity(noteId)

                }
            }
        }

        // Setup RecyclerView for displaying search results
        searchResult.apply {
            // Set the layout manager to StaggeredGridLayoutManager with 2 columns
            layoutManager = StaggeredGridLayoutManager(2, StaggeredGridLayoutManager.VERTICAL)
            // Set the adapter to the noteSearchAdapter
            adapter = noteSearchAdapter
            // Set hasFixedSize to false to allow the RecyclerView to have a dynamic size
            setHasFixedSize(false)
        }

        // Set up the search view
        searchView.editText.addTextChangedListener(onTextChanged = { text, _, _, _ ->
            // If the text is null, return
            if (text == null) return@addTextChangedListener

            // Get the query from the search view
            val query = text.toString().trim()
            // Do a search for notes based on the query
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

    private fun searchNotes(query: String) {
        // Show the progress bar
        binding.searchProgressBar.isVisible = true
        // Convert the query to lowercase
        val lowercaseQuery = query.lowercase()

        // Fetch notes from Firestore
        firebaseFirestore.collection(COLLECTION_NOTES)
            // Filter notes by the current user's ID
            .whereArrayContains("userIds", firebaseAuth.currentUser?.uid!!).get()
            // On successful fetch, filter notes based on the query
            .addOnSuccessListener { snapshot ->
                // Hide the progress bar
                binding.searchProgressBar.isVisible = false
                // Convert the Firestore snapshot to a list of Note objects
                val notes = snapshot.toObjects(Note::class.java)
                // Filter notes based on the query
                val filteredNotes = notes.filter { note ->
                    note.title?.lowercase()
                        ?.contains(lowercaseQuery) == true || note.content?.lowercase()
                        ?.contains(lowercaseQuery) == true
                }

                // Update the UI based on the search results
                binding.searchResult.isVisible = filteredNotes.isNotEmpty()
                // Submit the list of filtered notes to the noteSearchAdapter
                noteSearchAdapter.submitList(filteredNotes)
            }.addOnFailureListener { exception ->
                // Log the error message and stack trace
                Log.e(this.javaClass.simpleName, exception.message.toString(), exception)
                // Hide the progress bar
                binding.searchProgressBar.isVisible = false

                // Show a snackbar with an error message
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


//  Called when the back button is pressed. It hides the search view if it is showing, otherwise it performs the default back action.
    @Suppress("DEPRECATION")
    @Override
    override fun onBackPressed() {
        // If the search view is showing, hide it
        if (binding.searchView.isShowing) {
            binding.searchView.hide()
        } else {
            super.onBackPressed()
        }
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        return when (item?.itemId) {
            // If the logout menu item is clicked, show the logout dialog
            R.id.action_logout -> {
                showLogoutDialog()
                true
            }

            // If the scan QR code menu item is clicked, show the QR code scanner
            R.id.action_scan_qr -> {
                showQRCodeScanner()
                true
            }

            else -> false
        }
    }

     // Shows the QR code scanner for importing notes.

    private fun showQRCodeScanner() {
        // Create a GmsBarcodeScannerOptions object with the QR code format
        val options = GmsBarcodeScannerOptions.Builder().setBarcodeFormats(
            Barcode.FORMAT_QR_CODE
        ).enableAutoZoom().build()

        // Create a GmsBarcodeScanning client with the options
        val scanner = GmsBarcodeScanning.getClient(this, options)
        // Start the scanner
        scanner.startScan().addOnSuccessListener { barcode ->
            // If the barcode is null, show a snackbar with an error message
            val data = barcode.rawValue?.split("  ")

            if (data.isNullOrEmpty() || data.size != 2) {
                showSnackbar(getString(R.string.message_invalid_qr_code))
                return@addOnSuccessListener
            }

            // Import the note using the data from the QR code
            importNote(data)
        }
    }

    private fun importNote(data: List<String>) {
        // Show a progress dialog
        val progressDialog = ProgressDialog(this)
        progressDialog.setMessage(getString(R.string.message_loading_importing_note))
        progressDialog.setCancelable(false)
        progressDialog.show()

        // Get the note ID and token from the data
        val noteId = data.first()
        val token = data.last()

        // Fetch the note from Firestore
        firebaseFirestore.collection(COLLECTION_NOTES)
            .document(noteId).get()
            // On successful fetch, check if the note is valid and import it
            .addOnSuccessListener { document ->
                // If the document is not null, convert it to a Note object
                if (document.exists()) {
                    // Convert the document to a Note object
                    val note = document.toObject(Note::class.java)

                    // If the note is not null and the token matches
                    if (note != null && note.token == token) {
                        // Dismiss the progress dialog
                        progressDialog.dismiss()

                        // Show an alert dialog to confirm the import
                        val alertDialog =
                            AlertDialog.Builder(this).setTitle(getString(R.string.action_add_note))
                                .setMessage(getString(R.string.message_import_note, note.title))
                                .setPositiveButton(getString(R.string.action_yes)) { _, _ ->
                                    // Show a loading progress bar
                                    showLoading(true)

                                    // Update the note with the current user's ID
                                    firebaseFirestore.collection(COLLECTION_NOTES).document(noteId)
                                        // Update the note with the current user's ID
                                        .update(
                                            "userIds",
                                            note.userIds?.plus(firebaseAuth.currentUser?.uid!!)
                                                ?.distinct()
                                        ).addOnSuccessListener {
                                            // Hide the loading progress bar
                                            showLoading(false)

                                            // Observe data changes in Firestore
                                            observeData()

                                            // Show a snackbar with a success message
                                            showSnackbar(getString(R.string.message_success_import_note))
                                        }.addOnFailureListener { exception ->
                                            // Hide the loading progress bar
                                            showLoading(false)
                                            Log.e(
                                                this.javaClass.simpleName,
                                                exception.message.toString(),
                                                exception
                                            )

                                            // Show a snackbar with an error message
                                            showSnackbar(getString(R.string.message_error_please_try_again))
                                        }
                                }
                                .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ -> dialog.dismiss() }
                                .create()
                        // Show the alert dialog
                        alertDialog.show()
                    }
                } else {
                    // Dismiss the progress dialog
                    progressDialog.dismiss()
                    // Show a snackbar with an error message
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
                // Perform the logout action
                logout()
            }
            .setNegativeButton(getString(R.string.action_cancel)) { dialog, _ ->
                // Dismiss the dialog
                dialog.dismiss()
            }
            .create()
        alertDialog.show()
    }

    private fun logout() {
        // Sign out the user
        firebaseAuth.signOut()

        // Navigate to the LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}