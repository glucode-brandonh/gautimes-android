package com.glucode.gautimes.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey val id: Int = 0,
    val defaultFromId: String = "sandton",
    val defaultToId: String = "hatfield",
    val morningFromId: String? = null,
    val morningToId: String? = null,
    val afternoonFromId: String? = null,
    val afternoonToId: String? = null,
    val useSchedule: Boolean = false
)
