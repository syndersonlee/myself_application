package com.example.childreneducationapplication.entity

data class SelfEvaluation(
    val id: Int?,
    val user_id: Int?,
    val created_at: String?,
    val target_percent: Int?,
    val mission_impression: String?,
    val parents_impression: String?
)
