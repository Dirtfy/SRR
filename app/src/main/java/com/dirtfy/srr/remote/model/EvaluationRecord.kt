package com.dirtfy.srr.remote.model

import com.google.firebase.firestore.ServerTimestamp
import java.util.Date

data class EvaluationRecord(
    val userId: String = "",
    val featureId: String = "",
    val orderedItemIds: List<String> = emptyList(),
    @ServerTimestamp val submittedAt: Date? = null
)
