package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.json.CustomTimeJson
import junit.framework.Assert

class RemoteCustomTimeRecord : RemoteRecord {

    companion object {

        val CUSTOM_TIMES = "customTimes"
    }

    val id: String

    private val remoteProjectRecord: RemoteProjectRecord

    private val customTimeJson: CustomTimeJson

    val ownerId get() = customTimeJson.ownerId

    val localId get() = customTimeJson.localId

    var name: String
        get() = customTimeJson.name
        set(value) {
            Assert.assertTrue(value.isNotEmpty())

            if (customTimeJson.name == value)
                return

            customTimeJson.name = value
            addValue(key + "/name", value)
        }

    var sundayHour: Int
        get() = customTimeJson.sundayHour
        set(value) {
            if (customTimeJson.sundayHour == value)
                return

            customTimeJson.sundayHour = value
            addValue(key + "/sundayHour", value)
        }

    var sundayMinute: Int
        get() = customTimeJson.sundayMinute
        set(value) {
            if (customTimeJson.sundayMinute == value)
                return

            customTimeJson.sundayMinute = value
            addValue(key + "/sundayMinute", value)
        }

    var mondayHour: Int
        get() = customTimeJson.mondayHour
        set(value) {
            if (customTimeJson.mondayHour == value)
                return

            customTimeJson.mondayHour = value
            addValue(key + "/mondayHour", value)
        }

    var mondayMinute: Int
        get() = customTimeJson.mondayMinute
        set(minute) {
            if (customTimeJson.mondayMinute == minute)
                return

            customTimeJson.mondayMinute = minute
            addValue(key + "/mondayMinute", minute)
        }

    var tuesdayHour: Int
        get() = customTimeJson.tuesdayHour
        set(hour) {
            if (customTimeJson.tuesdayHour == hour)
                return

            customTimeJson.tuesdayHour = hour
            addValue(key + "/tuesdayHour", hour)
        }

    var tuesdayMinute: Int
        get() = customTimeJson.tuesdayMinute
        set(minute) {
            if (customTimeJson.tuesdayMinute == minute)
                return

            customTimeJson.tuesdayMinute = minute
            addValue(key + "/tuesdayMinute", minute)
        }

    var wednesdayHour: Int
        get() = customTimeJson.wednesdayHour
        set(hour) {
            if (customTimeJson.wednesdayHour == hour)
                return

            customTimeJson.wednesdayHour = hour
            addValue(key + "/wednesdayHour", hour)
        }

    var wednesdayMinute: Int
        get() = customTimeJson.wednesdayMinute
        set(minute) {
            if (customTimeJson.wednesdayMinute == minute)
                return

            customTimeJson.wednesdayMinute = minute
            addValue(key + "/wednesdayMinute", minute)
        }

    var thursdayHour: Int
        get() = customTimeJson.thursdayHour
        set(hour) {
            if (customTimeJson.thursdayHour == hour)
                return

            customTimeJson.thursdayHour = hour
            addValue(key + "/thursdayHour", hour)
        }

    var thursdayMinute: Int
        get() = customTimeJson.thursdayMinute
        set(minute) {
            if (customTimeJson.thursdayMinute == minute)
                return

            customTimeJson.thursdayMinute = minute
            addValue(key + "/thursdayMinute", minute)
        }

    var fridayHour: Int
        get() = customTimeJson.fridayHour
        set(hour) {
            if (customTimeJson.fridayHour == hour)
                return

            customTimeJson.fridayHour = hour
            addValue(key + "/fridayHour", hour)
        }

    var fridayMinute: Int
        get() = customTimeJson.fridayMinute
        set(minute) {
            if (customTimeJson.fridayMinute == minute)
                return

            customTimeJson.fridayMinute = minute
            addValue(key + "/fridayMinute", minute)
        }

    var saturdayHour: Int
        get() = customTimeJson.saturdayHour
        set(hour) {
            if (customTimeJson.saturdayHour == hour)
                return

            customTimeJson.saturdayHour = hour
            addValue(key + "/saturdayHour", hour)
        }

    var saturdayMinute: Int
        get() = customTimeJson.saturdayMinute
        set(minute) {
            if (customTimeJson.sundayMinute == minute)
                return

            customTimeJson.saturdayMinute = minute
            addValue(key + "/saturdayMinute", minute)
        }

    val projectId get() = remoteProjectRecord.id

    constructor(id: String, remoteProjectRecord: RemoteProjectRecord, customTimeJson: CustomTimeJson) : super(false) {
        this.id = id
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    constructor(remoteProjectRecord: RemoteProjectRecord, customTimeJson: CustomTimeJson) : super(true) {
        id = DatabaseWrapper.getCustomTimeRecordId(remoteProjectRecord.id)
        this.remoteProjectRecord = remoteProjectRecord
        this.customTimeJson = customTimeJson
    }

    public override fun getCreateObject() = customTimeJson

    override fun getKey() = remoteProjectRecord.key + "/" + RemoteProjectRecord.PROJECT_JSON + "/" + CUSTOM_TIMES + "/" + id
}
