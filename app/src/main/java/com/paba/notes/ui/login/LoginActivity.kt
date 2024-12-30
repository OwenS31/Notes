package com.paba.notes.ui.login

import android.content.Intent
import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.Patterns
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.paba.notes.R
import com.paba.notes.data.model.User
import com.paba.notes.databinding.ActivityLoginBinding
import com.paba.notes.helper.COLLECTION_USERS
import com.paba.notes.ui.home.HomeActivity
import com.paba.notes.ui.register.RegisterActivity
import com.paba.notes.ui.security_password.SecurityPasswordActivity
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {

    private lateinit var binding: ActivityLoginBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityLoginBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(binding.main) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        // Initialize Firebase instances.
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()

        initView()
    }

// Initializes the view and sets up click listeners for the buttons.

    private fun initView() = binding.apply {
        // Hide the loading indicator.
        showLoading(false)

        // Set up click listeners for the buttons.
        btnLogin.setOnClickListener { validateInput() }

        // Set up click listener for the register button.
        btnRegister.apply {
            // Create a spannable string to change the color of the text.
            val spannableString = SpannableStringBuilder(getString(R.string.action_sign_up))
            spannableString.setSpan(
                ForegroundColorSpan(getColor(R.color.md_theme_primary)),
                18,
                spannableString.length,
                0
            )
            spannableString.setSpan(StyleSpan(Typeface.BOLD), 18, spannableString.length, 0)
            text = spannableString

            // Set up click listener for the register button.
            setOnClickListener { navigateToRegisterActivity() }
        }
    }

//      Validates the input fields for login. If the input is valid, it proceeds to log in the user.

    private fun validateInput() = binding.apply {
        // Show the loading indicator and hide the input errors.
        showLoading(true)
        // Hide the input errors.
        hideInputErrors()

        // Get the email and password from the input fields.
        val email = inputEmail.editText?.text?.toString().orEmpty().trim()
        val password = inputPassword.editText?.text?.toString().orEmpty().trim()
        var isInputValid = true

        // Validate the email and password.
        if (email.isBlank()) {
            // Show an error if the email is empty.
            inputEmail.error =
                getString(R.string.error_empty_field, getString(R.string.label_email))
            isInputValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Show an error if the email is invalid.
            inputEmail.error =
                getString(R.string.error_invalid_field, getString(R.string.label_email))
            isInputValid = false
        }

        // Show an error if the password is empty.
        if (password.isBlank()) {
            inputPassword.error =
                getString(R.string.error_empty_field, getString(R.string.label_password))
            isInputValid = false
        }

        // Log in the user if the input is valid.
        if (isInputValid) loginUser(email, password) else showLoading(false)
    }

       private fun loginUser(email: String, password: String) {
        // Show the loading indicator and log in the user.
        showLoading(true)

        lifecycleScope.launch {
            // Log in the user with the provided email and password.
            firebaseAuth.signInWithEmailAndPassword(email, password).addOnSuccessListener {
                // Get the current user from Firestore.
                firebaseAuth.currentUser?.uid?.let { userId ->
                    // Check if the user has a security password.
                    firebaseFirestore.collection(COLLECTION_USERS).document(userId).get()
                        .addOnSuccessListener { document ->
                            // Get the current user from the document.
                            val currentUser = document.toObject(User::class.java)

                            if (currentUser?.securityPassword.isNullOrBlank()) {
                                // Navigate to the home activity if the user has no security password.
                                navigateToHomeActivity()
                            } else {
                                // Navigate to the security password activity if the user has a security password.
                                navigateToSecurityPasswordActivity()
                            }
                        }.addOnFailureListener { exception ->
                            // Hide the loading indicator and log out the user.
                            showLoading(false)
                            firebaseAuth.signOut()
                            Log.e(
                                this.javaClass.simpleName, exception.localizedMessage, exception
                            )

                            // Show an error message if the user is not found.
                            showSnackBar(getString(R.string.message_error_user_not_found))
                        }
                }
            }.addOnFailureListener { exception ->
                // Hide the loading indicator and show an error message.
                showLoading(false)
                Log.e(this.javaClass.simpleName, exception.localizedMessage, exception)

                // Show an error message if the credentials are invalid.
                showSnackBar(getString(R.string.message_error_invalid_credentials))
            }
        }
    }

    private fun navigateToSecurityPasswordActivity() {
        val intent = Intent(this, SecurityPasswordActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun navigateToHomeActivity() {
        val intent = Intent(this, HomeActivity::class.java)
        startActivity(intent)
        finishAffinity()
    }

    private fun showSnackBar(message: String) =
        Snackbar.make(binding.main, message, Snackbar.LENGTH_SHORT).show()

    private fun hideInputErrors() = binding.apply {
        inputEmail.error = null
        inputPassword.error = null
    }

    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        progressBar.isVisible = isShowLoading
        inputEmail.isEnabled = !isShowLoading
        inputPassword.isEnabled = !isShowLoading
        btnLogin.isEnabled = !isShowLoading
        btnRegister.isEnabled = !isShowLoading
    }

    private fun navigateToRegisterActivity() {
        val intent = Intent(this, RegisterActivity::class.java)
        startActivity(intent)
    }
}