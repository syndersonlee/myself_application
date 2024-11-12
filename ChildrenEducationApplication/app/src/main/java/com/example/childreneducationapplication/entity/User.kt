package com.example.childreneducationapplication.entity

import java.time.LocalDate

data class User(
    val id: Int?,
    val name: String?,
    val created_at: String?,
    val mission_stamp: Int?,
    val accuracy_stamp: Int?,
    val last_stamp_update: String?,
    val today_mission_stamp: Int?,
    val today_accuracy_stamp: Int?,
)