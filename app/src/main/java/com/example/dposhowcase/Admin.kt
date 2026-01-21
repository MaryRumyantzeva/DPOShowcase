package com.example.dposhowcase

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class Admin(
    val id: String = "",
    val email: String = "",
    val name: String = ""
) : Parcelable