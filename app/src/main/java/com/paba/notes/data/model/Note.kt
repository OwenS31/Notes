package com.paba.notes.data.model

import android.os.Parcelable
import com.google.firebase.firestore.DocumentId
import kotlinx.parcelize.Parcelize
import java.util.Date

@Parcelize
data class Note(
    @DocumentId
    val id: String? = null,
    val title: String? = null,
    val content: String? = null,
    val userIds: List<String>? = emptyList(),
    val token: String? = null,
    val createdAt: Date? = null,
    val updatedAt: Date? = null
) : Parcelable
