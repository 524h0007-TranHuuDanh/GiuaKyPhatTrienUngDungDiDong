package com.example.coffeestock.util

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

object Clock {

    fun nowUtc(): String {
        val formatter = SimpleDateFormat(
            "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'",
            Locale.US
        )

        formatter.timeZone = TimeZone.getTimeZone("UTC")

        return formatter.format(Date())
    }
}