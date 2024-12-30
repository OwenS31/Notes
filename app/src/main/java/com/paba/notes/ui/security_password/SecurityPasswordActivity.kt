package com.paba.notes.ui.security_password

import android.content.Intent
import android.os.Bundle
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paba.notes.R
import com.paba.notes.databinding.ActivitySecurityPasswordBinding
import com.paba.notes.helper.COLLECTION_USERS
import com.paba.notes.ui.home.HomeActivity
import com.paba.notes.ui.login.LoginActivity

class SecurityPasswordActivity : AppCompatActivity() {

    private lateinit var binding: ActivitySecurityPasswordBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

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

        initView()
    }

     // Initializes the view and sets up click listeners for the buttons.

    private fun initView() = binding.apply {
        btnLogout.setOnClickListener {
            showLogoutDialog()
        }
        btnVerify.setOnClickListener { validateInput() }
    }

    // Validates the input security password.
    // If the input is valid, it proceeds to validate the security password.

    private fun validateInput() = binding.apply {
        showLoading(true)
        inputSecurityPassword.error = null

        val securityPassword = inputSecurityPassword.editText?.text.toString().trim()

        // Validate the security password
        if (securityPassword.isBlank()) {
            // Show error if the security password is empty
            inputSecurityPassword.error =
                getString(R.string.error_empty_field, getString(R.string.label_security_password))
            showLoading(false)
        } else {
            // Proceed to validate the security password
            validateSecurityPassword()
        }
    }

    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        inputSecurityPassword.isEnabled = !isShowLoading
        btnLogout.isEnabled = !isShowLoading
        btnVerify.apply {
            isEnabled = !isShowLoading
            text = if (isShowLoading) {
                getString(R.string.action_verifying)
            } else {
                getString(R.string.action_login)
            }
        }
    }

    private fun validateSecurityPassword() {
        val securityPassword = binding.inputSecurityPassword.editText?.text.toString().trim()
        val user = firebaseAuth.currentUser

        // Check if the user is logged in
        if (user != null) {
            // Get the security password from Firestore
            firebaseFirestore.collection(COLLECTION_USERS).document(user.uid).get()
                .addOnSuccessListener { document ->
                    // Compare the security password
                    val securityPasswordDb = document["securityPassword"].toString()

                    // Navigate to HomeActivity if the security password is correct
                    if (securityPassword == securityPasswordDb) {
                        navigateToHomeActivity()
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
                .addOnFailureListener { // Show error if the security password is incorrect
                    showLoading(false)
                    binding.inputSecurityPassword.error =
                        getString(
                            R.string.error_invalid_field,
                            getString(R.string.label_security_password)
                        )
                }
        } else {
            // Logout if the user is not logged in
            logout()
        }
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun showLogoutDialog() {
        val alertDialog = AlertDialog.Builder(this)
            .setTitle(getString(R.string.action_logout))
            .setMessage(getString(R.string.message_logout))
            .setPositiveButton(getString(R.string.action_yes)) { _, _ ->
                // Logout the user
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

        // Navigate to LoginActivity
        val intent = Intent(this, LoginActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }
}