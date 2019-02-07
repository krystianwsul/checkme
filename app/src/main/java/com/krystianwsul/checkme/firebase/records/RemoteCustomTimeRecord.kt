package com.krystianwsul.checkme.firebase.records

import com.krystianwsul.checkme.domainmodel.CustomTimeRecord
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.json.CustomTimeJson


abstract class RemoteCustomTimeRecord : RemoteRecord, CustomTimeRecord {

    companion object {

        const val CUSTOM_TIMES = "customTimes"
    }

    val id: String

    protected abstract val remoteProjectRecord: RemoteProjectRecord

    protected abstract val customTimeJson: CustomTimeJson

    val localId get() = customTimeJson.localId

    override var name: String
        get() = customTimeJson.name
        set(value) {
            check(value.isNotEmpty())

            if (customTimeJson.name == value)
                return

            customTimeJson.name = value
            addValue("$key/name", value)
        }

    override var sundayHour: Int
        get() = customTimeJson.sundayHour
        set(value) {
            if (customTimeJson.sundayHour == value)
                return

            customTimeJson.sundayHour = value
            addValue("$key/sundayHour", value)
        }

    override var sundayMinute: Int
        get() = customTimeJson.sundayMinute
        set(value) {
            if (customTimeJson.sundayMinute == value)
                return

            customTimeJson.sundayMinute = value
            addValue("$key/sundayMinute", value)
        }

    override var mondayHour: Int
        get() = customTimeJson.mondayHour
        set(value) {
            if (customTimeJson.mondayHour == value)
                return

            customTimeJson.mondayHour = value
            addValue("$key/mondayHour", value)
        }

    override var mondayMinute: Int
        get() = customTimeJson.mondayMinute
        set(minute) {
            if (customTimeJson.mondayMinute == minute)
                return

            customTimeJson.mondayMinute = minute
            addValue("$key/mondayMinute", minute)
        }

    override var tuesdayHour: Int
        get() = customTimeJson.tuesdayHour
        set(hour) {
            if (customTimeJson.tuesdayHour == hour)
                return

            customTimeJson.tuesdayHour = hour
            addValue("$key/tuesdayHour", hour)
        }

    override var tuesdayMinute: Int
        get() = customTimeJson.tuesdayMinute
        set(minute) {
            if (customTimeJson.tuesdayMinute == minute)
                return

            customTimeJson.tuesdayMinute = minute
            addValue("$key/tuesdayMinute", minute)
        }

    override var wednesdayHour: Int
        get() = customTimeJson.wednesdayHour
        set(hour) {
            if (customTimeJson.wednesdayHour == hour)
                return

            customTimeJson.wednesdayHour = hour
            addValue("$key/wednesdayHour", hour)
        }

    override var wednesdayMinute: Int
        get() = customTimeJson.wednesdayMinute
        set(minute) {
            if (customTimeJson.wednesdayMinute == minute)
                return

            customTimeJson.wednesdayMinute = minute
            addValue("$key/wednesdayMinute", minute)
        }

    override var thursdayHour: Int
        get() = customTimeJson.thursdayHour
        set(hour) {
            if (customTimeJson.thursdayHour == hour)
                return

            customTimeJson.thursdayHour = hour
            addValue("$key/thursdayHour", hour)
        }

    override var thursdayMinute: Int
        get() = customTimeJson.thursdayMinute
        set(minute) {
            if (customTimeJson.thursdayMinute == minute)
                return

            customTimeJson.thursdayMinute = minute
            addValue("$key/thursdayMinute", minute)
        }

    override var fridayHour: Int
        get() = customTimeJson.fridayHour
        set(hour) {
            if (customTimeJson.fridayHour == hour)
                return

            customTimeJson.fridayHour = hour
            addValue("$key/fridayHour", hour)
        }

    override var fridayMinute: Int
        get() = customTimeJson.fridayMinute
        set(minute) {
            if (customTimeJson.fridayMinute == minute)
                return

            customTimeJson.fridayMinute = minute
            addValue("$key/fridayMinute", minute)
        }

    override var saturdayHour: Int
        get() = customTimeJson.saturdayHour
        set(hour) {
            if (customTimeJson.saturdayHour == hour)
                return

            customTimeJson.saturdayHour = hour
            addValue("$key/saturdayHour", hour)
        }

    override var saturdayMinute: Int
        get() = customTimeJson.saturdayMinute
        set(minute) {
            if (customTimeJson.sundayMinute == minute)
                return

            customTimeJson.saturdayMinute = minute
            addValue("$key/saturdayMinute", minute)
        }

    val projectId get() = remoteProjectRecord.id

    constructor(id: String) : super(false) {
        this.id = id
    }

    constructor(remoteProjectRecord: RemoteProjectRecord) : super(true) {
        id = remoteProjectRecord.getCustomTimeRecordId()
    }

    override val key get() = remoteProjectRecord.childKey + "/" + CUSTOM_TIMES + "/" + id

    abstract fun mine(domainFactory: DomainFactory): Boolean
}
