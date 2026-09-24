package com.fathtube.app.innertube.models.body

import com.fathtube.app.innertube.models.Context
import kotlinx.serialization.Serializable

@Serializable
data class CreateCommentBody(
    val context: Context,
    val commentText: String,
    val createCommentParams: String,
)
