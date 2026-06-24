package com.dirtfy.srr.remote.model

import com.google.firebase.Timestamp

data class EvaluationRecord(
    val userId: String = "",
    val featureId: String = "",
    val orderedItemIds: List<String> = emptyList(),
    val submittedAt: Timestamp = Timestamp.now()
)
