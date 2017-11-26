package com.krystianwsul.checkme.loaders

import android.content.Context
import android.os.Parcel
import android.os.Parcelable
import android.text.TextUtils
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.utils.CustomTimeKey
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.time.*
import com.krystianwsul.checkme.utils.time.Date
import junit.framework.Assert
import java.util.*

class CreateTaskLoader(context: Context, private val taskKey: TaskKey?, private val joinTaskKeys: List<TaskKey>?) : DomainLoader<CreateTaskLoader.Data>(context, needsFirebase(taskKey)) {

    companion object {

        private fun needsFirebase(taskKey: TaskKey?) = if (taskKey?.type == TaskKey.Type.REMOTE) {
            DomainLoader.FirebaseLevel.NEED
        } else {
            DomainLoader.FirebaseLevel.WANT
        }
    }

    internal override fun getName() = "CreateTaskLoader, taskKey: $taskKey, excludedTaskKeys: $joinTaskKeys"

    public override fun loadDomain(domainFactory: DomainFactory) = domainFactory.getCreateTaskData(taskKey, context, joinTaskKeys)

    sealed class ScheduleData {

        abstract val scheduleType: ScheduleType

        data class SingleScheduleData(val date: Date, val timePair: TimePair) : ScheduleData() {

            override val scheduleType = ScheduleType.SINGLE
        }

        data class DailyScheduleData(val timePair: TimePair) : ScheduleData() {

            override val scheduleType = ScheduleType.DAILY
        }

        data class WeeklyScheduleData(val dayOfWeek: DayOfWeek, val timePair: TimePair) : ScheduleData() {

            override val scheduleType = ScheduleType.WEEKLY
        }

        data class MonthlyDayScheduleData(val dayOfMonth: Int, val beginningOfMonth: Boolean, val timePair: TimePair) : ScheduleData() {

            override val scheduleType = ScheduleType.MONTHLY_DAY
        }

        data class MonthlyWeekScheduleData(val dayOfMonth: Int, val dayOfWeek: DayOfWeek, val beginningOfMonth: Boolean, val TimePair: TimePair) : ScheduleData() {

            override val scheduleType = ScheduleType.MONTHLY_WEEK
        }
    }

    data class Data(val taskData: TaskData?, val parentTreeDatas: Map<ParentKey, ParentTreeData>, val customTimeDatas: Map<CustomTimeKey, CustomTimeData>) : DomainLoader.Data()

    data class CustomTimeData(val customTimeKey: CustomTimeKey, val name: String, val hourMinutes: TreeMap<DayOfWeek, HourMinute>)

    data class TaskData(val name: String, val taskParentKey: ParentKey.TaskParentKey?, val scheduleDatas: List<ScheduleData>?, val note: String?, val projectName: String?)

    data class ParentTreeData(val name: String, val parentTreeDatas: Map<ParentKey, ParentTreeData>, val parentKey: ParentKey, val scheduleText: String?, val note: String?, val sortKey: SortKey)

    sealed class ParentKey : Parcelable {

        abstract val type: ParentType

        data class ProjectParentKey(val projectId: String) : ParentKey() {

            companion object {

                val CREATOR: Parcelable.Creator<ProjectParentKey> = object : Parcelable.Creator<ProjectParentKey> {

                    override fun createFromParcel(parcel: Parcel): ProjectParentKey {
                        val projectId = parcel.readString()!!
                        Assert.assertTrue(!TextUtils.isEmpty(projectId))

                        return ProjectParentKey(projectId)
                    }

                    override fun newArray(size: Int) = arrayOfNulls<ProjectParentKey>(size)
                }
            }

            override val type = ParentType.PROJECT

            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {
                dest.writeString(projectId)
            }
        }

        data class TaskParentKey(val taskKey: TaskKey) : ParentKey() {

            companion object {

                val CREATOR: Parcelable.Creator<TaskParentKey> = object : Parcelable.Creator<TaskParentKey> {

                    override fun createFromParcel(parcel: Parcel): TaskParentKey {
                        val taskKey = parcel.readParcelable<TaskKey>(TaskKey::class.java.classLoader)!!

                        return TaskParentKey(taskKey)
                    }

                    override fun newArray(size: Int) = arrayOfNulls<TaskParentKey>(size)
                }
            }

            override val type = ParentType.TASK

            override fun describeContents() = 0

            override fun writeToParcel(dest: Parcel, flags: Int) {
                dest.writeParcelable(taskKey, 0)
            }
        }
    }

    enum class ParentType { PROJECT, TASK }

    sealed class SortKey : Comparable<SortKey> {

        data class ProjectSortKey(private val projectId: String) : SortKey() {

            init {
                Assert.assertTrue(!TextUtils.isEmpty(projectId))
            }

            override fun compareTo(other: SortKey): Int {
                if (other is TaskSortKey)
                    return 1

                val projectSortKey = other as ProjectSortKey

                return projectId.compareTo(projectSortKey.projectId)
            }
        }

        data class TaskSortKey(private val startExactTimeStamp: ExactTimeStamp) : SortKey() {

            override fun compareTo(other: SortKey): Int {
                if (other is ProjectSortKey)
                    return -1

                val taskSortKey = other as TaskSortKey

                return startExactTimeStamp.compareTo(taskSortKey.startExactTimeStamp)
            }
        }
    }
}