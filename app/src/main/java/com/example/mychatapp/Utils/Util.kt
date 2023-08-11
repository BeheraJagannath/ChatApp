package com.example.mychatapp.Utils

import java.text.SimpleDateFormat
import java.util.*

object Util {
    private val yyyy_MM_dd_HH_mm: SimpleDateFormat = SimpleDateFormat(
        "dd/MM/yyyy HH:mm", Locale.getDefault()
    )
    private val HHmm: SimpleDateFormat = SimpleDateFormat(
        "HH:mm", Locale.US
    ) /*from w  w  w .  j a  v  a  2s .  co m*/
    private val MM_dd_HHmm: SimpleDateFormat = SimpleDateFormat(
        "dd/MM/yyyy HH:mm", Locale.US
    )

    fun date2DayTime(oldTime: Date): String {
        val newTime = Date()
        try {
            val cal: Calendar = Calendar.getInstance()
            cal.setTime(newTime)
            val oldCal: Calendar = Calendar.getInstance()
            oldCal.setTime(oldTime)
            val oldYear: Int = oldCal.get(Calendar.YEAR)
            val year: Int = cal.get(Calendar.YEAR)
            val oldDay: Int = oldCal.get(Calendar.DAY_OF_YEAR)
            val day: Int = cal.get(Calendar.DAY_OF_YEAR)
            if (oldYear == year) {
                val value = oldDay - day
                return if (value == -1) {
                    "Yesterday, " + HHmm.format(oldTime)
                } else if (value == 0) {
                    "Today, " + HHmm.format(oldTime)
                } else if (value == 1) {
                    "Tomorrow, " + HHmm.format(oldTime)
                } else {
                    MM_dd_HHmm.format(oldTime)
                }
            }
        } catch (e: Exception) {
        }
        return yyyy_MM_dd_HH_mm.format(oldTime)
    }



    private val HH: SimpleDateFormat = SimpleDateFormat(
        "HH:mm", Locale.US
    ) /*from w  w  w .  j a  v  a  2s .  co m*/
    private val MM_dd: SimpleDateFormat = SimpleDateFormat(
        "dd/MM/yyyy", Locale.getDefault()
    )

    fun dateDayTime(oldTime: Date): String {
        val newTime = Date()
        try {
            val cal: Calendar = Calendar.getInstance()
            cal.setTime(newTime)
            val oldCal: Calendar = Calendar.getInstance()
            oldCal.setTime(oldTime)
            val oldYear: Int = oldCal.get(Calendar.YEAR)
            val year: Int = cal.get(Calendar.YEAR)
            val oldDay: Int = oldCal.get(Calendar.DAY_OF_YEAR)
            val day: Int = cal.get(Calendar.DAY_OF_YEAR)
            if (oldYear == year) {
                val value = oldDay - day
                return if (value == -1) {
                    "Yesterday"
                } else if (value == 0) {
                    HH.format(oldTime)
                } else if (value == 1) {
                    "Tomorrow"
                } else {
                    MM_dd.format(oldTime)
                }
            }
        } catch (e: Exception) {
        }
        return yyyy_MM_dd_HH_mm.format(oldTime)
    }

}