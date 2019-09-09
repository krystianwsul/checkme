package com.krystianwsul.checkme.domainmodel

import com.krystianwsul.common.time.DayOfWeek
import com.krystianwsul.common.time.HourMinute

interface CustomTimeRecord {

    var name: String

    var sundayHour: Int
    var sundayMinute: Int

    var mondayHour: Int
    var mondayMinute: Int

    var tuesdayHour: Int
    var tuesdayMinute: Int

    var wednesdayHour: Int
    var wednesdayMinute: Int

    var thursdayHour: Int
    var thursdayMinute: Int

    var fridayHour: Int
    var fridayMinute: Int

    var saturdayHour: Int
    var saturdayMinute: Int


    fun setHourMinute(dayOfWeek: DayOfWeek, hourMinute: HourMinute) {
        when (dayOfWeek) {
            DayOfWeek.SUNDAY -> {
                sundayHour = hourMinute.hour
                sundayMinute = hourMinute.minute
            }
            DayOfWeek.MONDAY -> {
                mondayHour = hourMinute.hour
                mondayMinute = hourMinute.minute
            }
            DayOfWeek.TUESDAY -> {
                tuesdayHour = hourMinute.hour
                tuesdayMinute = hourMinute.minute
            }
            DayOfWeek.WEDNESDAY -> {
                wednesdayHour = hourMinute.hour
                wednesdayMinute = hourMinute.minute
            }
            DayOfWeek.THURSDAY -> {
                thursdayHour = hourMinute.hour
                thursdayMinute = hourMinute.minute
            }
            DayOfWeek.FRIDAY -> {
                fridayHour = hourMinute.hour
                fridayMinute = hourMinute.minute
            }
            DayOfWeek.SATURDAY -> {
                saturdayHour = hourMinute.hour
                saturdayMinute = hourMinute.minute
            }
        }
    }
}