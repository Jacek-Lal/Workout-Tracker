package com.example.inz.models

data class MuscleLastWorked(
        val primaryMuscle: String,
        val lastWorked: Long = 0L
)
