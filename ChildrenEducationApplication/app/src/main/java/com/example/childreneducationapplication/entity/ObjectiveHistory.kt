package com.example.childreneducationapplication.entity

data class ObjectiveHistory (
    val id: Int?,
    val user_id: Int?,
    val title: String?,
    val created_at: String?,
    val content: String?,
    val pass: Boolean?,
    var parents_approve: Boolean?,
)