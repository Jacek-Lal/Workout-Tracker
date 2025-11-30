package com.example.inz.models

data class Exercise(

    var id: Int = 0,
    val name: String,
    val primaryMuscle: String,
    val secondaryMuscle: String?,
    val equipment: String?,
    val type: String

)