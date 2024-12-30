package com.paba.notes.ui.note.share

import android.app.Dialog
import android.graphics.Bitmap
import android.graphics.Color
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.os.bundleOf
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetBehavior
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.paba.notes.R
import com.paba.notes.data.model.Note
import com.paba.notes.databinding.ItemBottomsheetBinding
import com.paba.notes.helper.COLLECTION_NOTES
import com.paba.notes.helper.generateToken
import kotlinx.coroutines.launch

class ShareNoteDialog : BottomSheetDialogFragment() {

    private lateinit var binding: ItemBottomsheetBinding

    private lateinit var firebaseAuth: FirebaseAuth

    private lateinit var firebaseFirestore: FirebaseFirestore

    private var note: Note? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        binding = ItemBottomsheetBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // Initialize Firebase instances and get the note from the arguments
        firebaseAuth = FirebaseAuth.getInstance()
        firebaseFirestore = FirebaseFirestore.getInstance()
        note = requireArguments().getParcelable<Note>(EXTRA_NOTE) as Note

        showLoading(true)
        generateQrCode()
    }


     // Generates a QR code for the note and updates the note's token in Firestore.

    private fun generateQrCode() = lifecycleScope.launch {
        showLoading(true)
        val newToken = generateToken() // Generate a new token for the note

        // Update the note's token in Firestore
        firebaseFirestore.collection(COLLECTION_NOTES)
            .document(note?.id!!)
            .update("token", newToken)
            .addOnSuccessListener {
                // Generate the QR code
                val content = "${note?.id}  $newToken"
                val writer = QRCodeWriter()
                val bitMatrix = writer.encode(content, BarcodeFormat.QR_CODE, 512, 512)
                val width = bitMatrix.width
                val height = bitMatrix.height
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.RGB_565)
                for (x in 0 until width) {
                    for (y in 0 until height) {
                        bitmap.setPixel(x, y, if (bitMatrix.get(x, y)) Color.BLACK else Color.WHITE)
                    }
                }

                // Load the QR code into the ImageView
                Glide.with(binding.root)
                    .load(bitmap)
                    .into(binding.ivQrCode)

                // Hide the loading indicator
                showLoading(false)
            }
            .addOnFailureListener { exception ->
                // Log the error and show a toast message
                Log.e(javaClass.simpleName, exception.message, exception)
                showLoading(false)

                Toast.makeText(
                    requireContext(),
                    getString(R.string.message_error_please_try_again),
                    Toast.LENGTH_SHORT
                ).show()
                dismiss()
            }
    }

    private fun showLoading(isShowLoading: Boolean) = binding.apply {
        progressBar.isVisible = isShowLoading
        ivQrCode.isVisible = !isShowLoading
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        dialog?.setOnShowListener { it ->
            val d = it as BottomSheetDialog
            val bottomSheet =
                d.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
            bottomSheet?.let {
                val behavior = BottomSheetBehavior.from(it)
                behavior.state = BottomSheetBehavior.STATE_EXPANDED
            }
        }
        return super.onCreateDialog(savedInstanceState)
    }

    companion object {

        const val TAG = "ShareNoteDialog"

         // Key for passing the note as an argument.
        const val EXTRA_NOTE = "extra_note"

        fun newInstance(note: Note) = ShareNoteDialog().apply {
            arguments = bundleOf(
                EXTRA_NOTE to note
            )
        }
    }
}