package com.paba.notes.ui.register

import android.graphics.Typeface
import android.os.Bundle
import android.text.SpannableStringBuilder
import android.text.style.ForegroundColorSpan
import android.text.style.StyleSpan
import android.util.Log
import android.util.Patterns
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseAuthUserCollisionException
import com.google.firebase.firestore.FirebaseFirestore
import com.paba.notes.R
import com.paba.notes.data.model.User
import com.paba.notes.databinding.ActivityRegisterBinding
import com.paba.notes.helper.COLLECTION_USERS
import kotlinx.coroutines.launch

class RegisterActivity : AppCompatActivity() {

    private lateinit var binding: ActivityRegisterBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityRegisterBinding.inflate(layoutInflater)
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
        showLoading(false)
        showSecurityPasswordInput(false)

        // Set up click listeners for the switch
        switchSecurityPassword.setOnCheckedChangeListener { _, isChecked ->
            showSecurityPasswordInput(isChecked)
        }

        // Set up click listeners for the buttons
        btnRegister.setOnClickListener { validateInput() }

        btnLogin.apply {
            // Set up the text with a clickable span
            val spannableString = SpannableStringBuilder(getString(R.string.action_sign_in))
            spannableString.setSpan(
                ForegroundColorSpan(getColor(R.color.md_theme_primary)),
                18,
                spannableString.length,
                0
            )
            spannableString.setSpan(StyleSpan(Typeface.BOLD), 18, spannableString.length, 0)

            text = spannableString
            setOnClickListener { finish() }
        }
    }

     // Validates the input fields for registration. If the input is valid, it proceeds to register the user.

    private fun validateInput() = binding.apply {
        showLoading(true)
        hideInputErrors()

        val name = inputFullName.editText?.text?.toString().orEmpty().trim()
        val email = inputEmail.editText?.text?.toString().orEmpty().trim()
        val password = inputPassword.editText?.text?.toString().orEmpty().trim()
        val securityPasswordEnabled = switchSecurityPassword.isChecked
        val securityPassword = inputSecurityPassword.editText?.text?.toString()?.trim()
        var isInputValid = true

        // Validate the input fields
        if (name.isBlank()) {
            // Show an error if the full name is empty
            inputFullName.error =
                getString(R.string.error_empty_field, getString(R.string.label_full_name))
            isInputValid = false
        }

        // Validate the email field
        if (email.isBlank()) {
            // Show an error if the email is empty
            inputEmail.error =
                getString(R.string.error_empty_field, getString(R.string.label_email))
            isInputValid = false
        } else if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
            // Show an error if the email is invalid
            inputEmail.error =
                getString(R.string.error_invalid_field, getString(R.string.label_email))
            isInputValid = false
        }

        // Validate the password field
        if (password.isBlank()) {
            // Show an error if the password is empty
            inputPassword.error =
                getString(R.string.error_empty_field, getString(R.string.label_password))
            isInputValid = false
        } else if (password.length < 6) {
            // Show an error if the password is less than 6 characters
            inputPassword.error = getString(R.string.error_password_length)
            isInputValid = false
        }

        // Validate the security password field
        if (securityPasswordEnabled && securityPassword?.isBlank() == true) {
            // Show an error if the security password is empty
            inputSecurityPassword.error =
                getString(R.string.error_empty_field, getString(R.string.label_security_password))
            isInputValid = false
        }

        // Register the user if the input is valid
        if (isInputValid) registerUser(
            name,
            email,
            password,
            if (securityPasswordEnabled) securityPassword else null
        ) else showLoading(
            false
        )
    }

    private fun registerUser(
        name: String,
        email: String,
        password: String,
        securityPassword: String? = null
    ) {
        showLoading(true)
        lifecycleScope.launch {
            // Create a new user with the provided details
            firebaseAuth.createUserWithEmailAndPassword(email, password).addOnSuccessListener {
                // Save the user details to Firestore if the user is successfully created
                firebaseAuth.currentUser?.let {
                    val user = User(
                        id = it.uid,
                        name = name,
                        email = email,
                        password = password,
                        securityPassword = securityPassword
                    )

                    // Save the user details to Firestore
                    firebaseFirestore.collection(COLLECTION_USERS).document(it.uid).set(user)
                        .addOnSuccessListener {
                            // Show a success message and finish the activity if the user details are saved successfully
                            firebaseAuth.signOut()
                            showToast(getString(R.string.message_register_success))
                            finish()
                        }.addOnFailureListener { exception ->
                            // Show an error message if the user details are not saved successfully
                            showLoading(false)
                            Log.e(this.javaClass.simpleName, exception.localizedMessage, exception)

                            firebaseAuth.currentUser?.delete()
                            showSnackBar(getString(R.string.message_error_please_try_again))
                        }
                }
            }.addOnFailureListener { exception ->
                // Show an error message if the user is not created successfully
                showLoading(false)
                Log.e(this.javaClass.simpleName, exception.localizedMessage, exception)

                // Handle the exception based on the type
                when (exception) {
                    is FirebaseAuthUserCollisionException -> {
                        // Show an error if the email is already registered
                        binding.inputEmail.error =
                            getString(R.string.error_email_already_registered)
                    }

                    // Show a generic error message for other exceptions
                    else -> showSnackBar(exception.localizedMessage.orEmpty())
                }
            }
        }
    }

    private fun hideInputErrors() = binding.apply {
        inputFullName.error = null
        inputEmail.error = null
        inputPassword.error = null
        inputSecurityPassword.error = null
    }

    private fun showToast(message: String) =
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()

    private fun showSnackBar(message: String) =
        Snackbar.make(binding.main, message, Snackbar.LENGTH_SHORT).show()

    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        progressBar.isVisible = isShowLoading
        inputFullName.isEnabled = !isShowLoading
        inputEmail.isEnabled = !isShowLoading
        inputPassword.isEnabled = !isShowLoading
        inputSecurityPassword.isEnabled = !isShowLoading
        switchSecurityPassword.isEnabled = !isShowLoading
        btnRegister.isEnabled = !isShowLoading
        btnLogin.isEnabled = !isShowLoading
    }
    
    private fun showSecurityPasswordInput(isShowSecurityPasswordInput: Boolean) {
        binding.labelSecurityPassword.isVisible = isShowSecurityPasswordInput
        binding.inputSecurityPassword.isVisible = isShowSecurityPasswordInput
    }
}