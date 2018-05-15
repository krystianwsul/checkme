package com.krystianwsul.checkme.domainmodel.local

import android.content.Context
import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.*
import com.krystianwsul.checkme.firebase.RemoteProject
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.persistencemodel.TaskRecord
import com.krystianwsul.checkme.utils.ScheduleKey
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import junit.framework.Assert
import java.util.*

class LocalTask(domainFactory: DomainFactory, private val taskRecord: TaskRecord) : Task(domainFactory) {

    private val mSchedules = ArrayList<Schedule>()

    override val name get() = taskRecord.name

    val id get() = taskRecord.id

    override val startExactTimeStamp get() = ExactTimeStamp(taskRecord.startTime)

    override val note get() = taskRecord.note

    override val taskKey get() = TaskKey(taskRecord.id)

    override val schedules get() = mSchedules

    override val existingInstances get() = HashMap<ScheduleKey, Instance>(domainFactory.localFactory.getExistingInstances(taskKey))

    override val remoteNullableProject: RemoteProject? = null

    override val remoteNonNullProject get() = throw UnsupportedOperationException()

    fun addSchedules(schedules: List<Schedule>) = mSchedules.addAll(schedules)

    override fun setName(name: String, note: String?) {
        Assert.assertTrue(!TextUtils.isEmpty(name))

        taskRecord.name = name
        taskRecord.note = note
    }

    override fun getEndExactTimeStamp() = taskRecord.endTime?.let { ExactTimeStamp(it) }

    override fun setMyEndExactTimeStamp(now: ExactTimeStamp) {
        taskRecord.endTime = now.long
    }

    override fun getOldestVisible(): Date? {
        return if (taskRecord.oldestVisibleYear != null) {
            Assert.assertTrue(taskRecord.oldestVisibleMonth != null)
            Assert.assertTrue(taskRecord.oldestVisibleDay != null)

            Date(taskRecord.oldestVisibleYear!!, taskRecord.oldestVisibleMonth!!, taskRecord.oldestVisibleDay!!)
        } else {
            Assert.assertTrue(taskRecord.oldestVisibleMonth == null)
            Assert.assertTrue(taskRecord.oldestVisibleDay == null)

            null
        }
    }

    override fun setOldestVisible(date: Date) {
        taskRecord.oldestVisibleYear = date.year
        taskRecord.oldestVisibleMonth = date.month
        taskRecord.oldestVisibleDay = date.day
    }

    override fun delete() {
        val taskKey = taskKey

        ArrayList(domainFactory.localFactory.getTaskHierarchiesByChildTaskKey(taskKey)).forEach { it.delete() }

        ArrayList(schedules).forEach { it.delete() }

        domainFactory.localFactory.deleteTask(this)
        taskRecord.delete()
    }

    override fun createChildTask(now: ExactTimeStamp, name: String, note: String?) = domainFactory.localFactory.createChildTask(domainFactory, now, this, name, note)

    override fun addSchedules(scheduleDatas: List<CreateTaskLoader.ScheduleData>, now: ExactTimeStamp) {
        Assert.assertTrue(!scheduleDatas.isEmpty())

        val schedules = domainFactory.localFactory.createSchedules(domainFactory, this, scheduleDatas, now)
        Assert.assertTrue(!schedules.isEmpty())

        addSchedules(schedules)
    }

    override fun addChild(childTask: Task, now: ExactTimeStamp) {
        Assert.assertTrue(childTask is LocalTask)

        domainFactory.localFactory.createTaskHierarchy(domainFactory, this, childTask as LocalTask, now)
    }

    override fun deleteSchedule(schedule: Schedule) {
        Assert.assertTrue(mSchedules.contains(schedule))

        mSchedules.remove(schedule)
    }

    override fun getTaskHierarchiesByChildTaskKey(childTaskKey: TaskKey): Set<TaskHierarchy> = domainFactory.localFactory.getTaskHierarchiesByChildTaskKey(childTaskKey)

    override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> = domainFactory.localFactory.getTaskHierarchiesByParentTaskKey(parentTaskKey)

    override fun belongsToRemoteProject() = false

    override fun updateProject(context: Context, now: ExactTimeStamp, projectId: String?): Task {
        return if (TextUtils.isEmpty(projectId)) {
            this
        } else {
            domainFactory.convertLocalToRemote(context, now, this, projectId!!)
        }
    }
}
