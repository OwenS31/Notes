package com.paba.notes.data.model

import com.google.firebase.firestore.DocumentId

data class User(
    @DocumentId
    val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val password: String? = null,
    val securityPassword: String? = null
)
