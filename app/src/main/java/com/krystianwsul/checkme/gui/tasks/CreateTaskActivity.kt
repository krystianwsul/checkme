package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import com.google.common.collect.HashMultiset
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.gui.AbstractActivity
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_create_task.*
import kotlinx.android.synthetic.main.row_note.view.*
import kotlinx.android.synthetic.main.row_schedule.view.*
import kotlinx.android.synthetic.main.toolbar_edit_text.*
import java.util.*

@Suppress("CascadeIf")
class CreateTaskActivity : AbstractActivity() {

    companion object {

        private const val DISCARD_TAG = "discard"

        private const val TASK_KEY_KEY = "taskKey"
        private const val TASK_KEYS_KEY = "taskKeys"

        private const val PARENT_TASK_KEY_HINT_KEY = "parentTaskKeyHint"
        private const val SCHEDULE_HINT_KEY = "scheduleHint"

        private const val PARENT_KEY_KEY = "parentKey"
        private const val PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment"

        private const val HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition"
        private const val SCHEDULE_ENTRIES_KEY = "scheduleEntries"
        private const val NOTE_KEY = "note"
        private const val NOTE_HAS_FOCUS_KEY = "noteHasFocus"

        private const val SCHEDULE_DIALOG_TAG = "scheduleDialog"

        fun getCreateIntent(context: Context) = Intent(context, CreateTaskActivity::class.java)

        fun getCreateIntent(context: Context, scheduleHint: ScheduleHint) = Intent(context, CreateTaskActivity::class.java).apply { putExtra(SCHEDULE_HINT_KEY, scheduleHint) }

        fun getCreateIntent(parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply { putExtra(PARENT_TASK_KEY_HINT_KEY, parentTaskKeyHint as Parcelable) }

        fun getJoinIntent(joinTaskKeys: ArrayList<TaskKey>) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            check(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, joinTaskKeys)
        }

        fun getJoinIntent(joinTaskKeys: ArrayList<TaskKey>, parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            check(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, joinTaskKeys)
            putExtra(PARENT_TASK_KEY_HINT_KEY, parentTaskKeyHint as Parcelable)
        }

        fun getJoinIntent(joinTaskKeys: ArrayList<TaskKey>, scheduleHint: ScheduleHint) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            check(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, joinTaskKeys)
            putExtra(SCHEDULE_HINT_KEY, scheduleHint)
        }

        fun getEditIntent(taskKey: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private var mSavedInstanceState: Bundle? = null

    private val mDiscardDialogListener = this::finish

    private var mTaskKey: TaskKey? = null
    private var mTaskKeys: List<TaskKey>? = null

    private var mScheduleHint: CreateTaskActivity.ScheduleHint? = null
    private var mParentTaskKeyHint: TaskKey? = null
    private var mNameHint: String? = null

    private var mData: CreateTaskViewModel.Data? = null

    private var mParent: CreateTaskViewModel.ParentTreeData? = null

    private lateinit var mCreateTaskAdapter: CreateTaskAdapter

    private var mHourMinutePickerPosition: Int? = null

    private lateinit var mScheduleEntries: MutableList<ScheduleEntry>

    private var mFirst = true

    private val mParentFragmentListener = object : ParentPickerFragment.Listener {
        override fun onTaskSelected(parentTreeData: CreateTaskViewModel.ParentTreeData) {
            if (parentTreeData.parentKey.type == CreateTaskViewModel.ParentType.TASK)
                clearSchedules()

            mParent = parentTreeData

            updateParentView()
        }

        override fun onTaskDeleted() {
            checkNotNull(mParent)

            mParent = null

            val view = scheduleRecycler.getChildAt(mCreateTaskAdapter.elementsBeforeSchedules() - 1)!!

            val scheduleHolder = scheduleRecycler.getChildViewHolder(view) as CreateTaskAdapter.ScheduleHolder

            scheduleHolder.mScheduleText.text = null
        }
    }

    private val mScheduleDialogListener = object : ScheduleDialogFragment.ScheduleDialogListener {
        override fun onScheduleDialogResult(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
            checkNotNull(mData)

            if (scheduleDialogData.scheduleType == ScheduleType.MONTHLY_DAY) {
                check(scheduleDialogData.monthlyDay)
            } else if (scheduleDialogData.scheduleType == ScheduleType.MONTHLY_WEEK) {
                check(!scheduleDialogData.monthlyDay)
            }

            if (mHourMinutePickerPosition == null) {
                clearParent()

                mCreateTaskAdapter.addScheduleEntry(ScheduleEntry.fromScheduleDialogData(scheduleDialogData))
            } else {
                mHourMinutePickerPosition!!.let {
                    check(it >= mCreateTaskAdapter.elementsBeforeSchedules())

                    mScheduleEntries[it - mCreateTaskAdapter.elementsBeforeSchedules()] = ScheduleEntry.fromScheduleDialogData(scheduleDialogData)

                    mCreateTaskAdapter.notifyItemChanged(it)

                    mHourMinutePickerPosition = null
                }
            }
        }

        override fun onScheduleDialogDelete() {
            mHourMinutePickerPosition!!.let {
                check(it >= mCreateTaskAdapter.elementsBeforeSchedules())
                checkNotNull(mData)

                mScheduleEntries.removeAt(it - mCreateTaskAdapter.elementsBeforeSchedules())

                mCreateTaskAdapter.notifyItemRemoved(it)

                mHourMinutePickerPosition = null
            }
        }

        override fun onScheduleDialogCancel() {
            mHourMinutePickerPosition?.let {
                check(it >= mCreateTaskAdapter.elementsBeforeSchedules())

                mHourMinutePickerPosition = null
            }
        }
    }

    private var mNote: String? = null

    private var mNoteHasFocus = false // keyboard hack

    private val mOnChildAttachStateChangeListener = object : RecyclerView.OnChildAttachStateChangeListener { // keyboard hack
        override fun onChildViewAttachedToWindow(view: View) {
            view.noteText?.let {
                removeListenerHelper()

                it.requestFocus()

                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

                //InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
                //imm.showSoftInput(noteText, InputMethodManager.SHOW_FORCED);
            }
        }

        override fun onChildViewDetachedFromWindow(view: View) = Unit
    }

    private val scheduleDatas get() = mScheduleEntries.map { it.scheduleData }.apply { check(!isEmpty()) }

    private lateinit var createTaskViewModel: CreateTaskViewModel

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = mData != null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        check(!hasValueParentTask() || !hasValueSchedule())

        when (item.itemId) {
            R.id.action_save -> {
                checkNotNull(mData)
                checkNotNull(toolbarEditText)

                if (!updateError()) {

                    val name = toolbarEditText.text.toString().trim { it <= ' ' }
                    check(!TextUtils.isEmpty(name))

                    createTaskViewModel.stop()

                    if (hasValueSchedule()) {
                        check(!hasValueParentTask())

                        val projectId = if (hasValueParentInGeneral()) (mParent!!.parentKey as CreateTaskViewModel.ParentKey.ProjectParentKey).projectId else null

                        if (mTaskKey != null) {
                            checkNotNull(mData!!.taskData)
                            check(mTaskKeys == null)

                            val taskKey = DomainFactory.getDomainFactory().updateScheduleTask(this, mData!!.dataId, SaveService.Source.GUI, mTaskKey!!, name, scheduleDatas, mNote, projectId)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (mTaskKeys != null) {
                            check(mData!!.taskData == null)
                            check(mTaskKeys!!.size > 1)

                            DomainFactory.getDomainFactory().createScheduleJoinRootTask(this, ExactTimeStamp.now, mData!!.dataId, SaveService.Source.GUI, name, scheduleDatas, mTaskKeys!!, mNote, projectId)

                            finish()
                        } else {
                            check(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createScheduleRootTask(this, mData!!.dataId, SaveService.Source.GUI, name, scheduleDatas, mNote, projectId)

                            finish()
                        }
                    } else if (hasValueParentTask()) {
                        checkNotNull(mParent)

                        val parentTaskKey = (mParent!!.parentKey as CreateTaskViewModel.ParentKey.TaskParentKey).taskKey

                        if (mTaskKey != null) {
                            checkNotNull(mData!!.taskData)
                            check(mTaskKeys == null)

                            val taskKey = DomainFactory.getDomainFactory().updateChildTask(this, ExactTimeStamp.now, mData!!.dataId, SaveService.Source.GUI, mTaskKey!!, name, parentTaskKey, mNote)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (mTaskKeys != null) {
                            check(mData!!.taskData == null)
                            check(mTaskKeys!!.size > 1)

                            DomainFactory.getDomainFactory().createJoinChildTask(this, mData!!.dataId, SaveService.Source.GUI, parentTaskKey, name, mTaskKeys!!, mNote)

                            finish()
                        } else {
                            check(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createChildTask(this, mData!!.dataId, SaveService.Source.GUI, parentTaskKey, name, mNote)

                            finish()
                        }
                    } else {  // no reminder
                        val projectId = if (hasValueParentInGeneral()) (mParent!!.parentKey as CreateTaskViewModel.ParentKey.ProjectParentKey).projectId else null

                        if (mTaskKey != null) {
                            checkNotNull(mData!!.taskData)
                            check(mTaskKeys == null)

                            val taskKey = DomainFactory.getDomainFactory().updateRootTask(this, mData!!.dataId, SaveService.Source.GUI, mTaskKey!!, name, mNote, projectId)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (mTaskKeys != null) {
                            check(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createJoinRootTask(this, mData!!.dataId, SaveService.Source.GUI, name, mTaskKeys!!, mNote, projectId)

                            finish()
                        } else {
                            check(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createRootTask(this, mData!!.dataId, SaveService.Source.GUI, name, mNote, projectId)

                            finish()
                        }
                    }
                }
            }
            android.R.id.home -> {
                if (tryClose())
                    finish()
            }
            else -> throw UnsupportedOperationException()
        }
        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_create_task)

        setSupportActionBar(toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        mSavedInstanceState = savedInstanceState

        scheduleRecycler.layoutManager = LinearLayoutManager(this)

        intent.run {
            if (hasExtra(TASK_KEY_KEY)) {
                check(!hasExtra(TASK_KEYS_KEY))
                check(!hasExtra(PARENT_TASK_KEY_HINT_KEY))
                check(!hasExtra(SCHEDULE_HINT_KEY))

                mTaskKey = getParcelableExtra(TASK_KEY_KEY)!!
            } else if (action == Intent.ACTION_SEND) {
                check(type == "text/plain")

                mNameHint = getStringExtra(Intent.EXTRA_TEXT)!!
                check(!mNameHint.isNullOrEmpty())
            } else {
                if (hasExtra(TASK_KEYS_KEY))
                    mTaskKeys = getParcelableArrayListExtra<TaskKey>(TASK_KEYS_KEY)!!.apply { check(size > 1) }

                if (hasExtra(PARENT_TASK_KEY_HINT_KEY)) {
                    check(!hasExtra(SCHEDULE_HINT_KEY))

                    mParentTaskKeyHint = getParcelableExtra(PARENT_TASK_KEY_HINT_KEY)!!
                } else if (hasExtra(SCHEDULE_HINT_KEY)) {
                    mScheduleHint = getParcelableExtra(SCHEDULE_HINT_KEY)!!
                }
            }
        }

        savedInstanceState?.run {
            if (containsKey(SCHEDULE_ENTRIES_KEY)) {
                mScheduleEntries = getParcelableArrayList(SCHEDULE_ENTRIES_KEY)!!

                if (containsKey(HOUR_MINUTE_PICKER_POSITION_KEY))
                    mHourMinutePickerPosition = getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -1).also { check(it > 0) }
            }
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = mDiscardDialogListener

        if (!mNoteHasFocus)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        createTaskViewModel = getViewModel<CreateTaskViewModel>().apply {
            start(mTaskKey, mTaskKeys)

            createDisposable += data.subscribe { onLoadFinished(it.value!!) }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (mData != null) {
                putParcelableArrayList(SCHEDULE_ENTRIES_KEY, ArrayList(mScheduleEntries))

                mHourMinutePickerPosition?.let {
                    check(it >= mCreateTaskAdapter.elementsBeforeSchedules())

                    putInt(HOUR_MINUTE_PICKER_POSITION_KEY, it)
                }

                if (mParent != null) {
                    putParcelable(PARENT_KEY_KEY, mParent!!.parentKey)
                }

                if (!mNote.isNullOrEmpty())
                    putString(NOTE_KEY, mNote)

                putBoolean(NOTE_HAS_FOCUS_KEY, mNoteHasFocus)
            }
        }
    }

    private fun onLoadFinished(data: CreateTaskViewModel.Data) {
        mData = data

        toolbarLayout.run {
            visibility = View.VISIBLE
            isHintAnimationEnabled = true
        }

        toolbarEditText.run {
            if (mSavedInstanceState == null) {
                if (mData!!.taskData != null) {
                    checkNotNull(mTaskKey)

                    setText(mData!!.taskData!!.name)
                } else if (!TextUtils.isEmpty(mNameHint)) {
                    check(mTaskKey == null)
                    check(mTaskKeys == null)
                    check(mParentTaskKeyHint == null)
                    check(mScheduleHint == null)

                    setText(mNameHint)
                }
            }

            toolbarEditText.addTextChangedListener(object : TextWatcher {

                private var mSkip = mSavedInstanceState != null

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable) {
                    if (mSkip) {
                        mSkip = false
                        return
                    }

                    updateError()
                }
            })
        }

        if (mSavedInstanceState?.containsKey(SCHEDULE_ENTRIES_KEY) == true) {
            mSavedInstanceState!!.run {
                if (containsKey(PARENT_KEY_KEY)) {
                    val parentKey = getParcelable<CreateTaskViewModel.ParentKey>(PARENT_KEY_KEY)!!

                    mParent = findTaskData(parentKey)
                }

                if (containsKey(NOTE_KEY)) {
                    mNote = getString(NOTE_KEY)!!
                    check(!mNote.isNullOrEmpty())
                }

                check(containsKey(NOTE_HAS_FOCUS_KEY))

                mNoteHasFocus = getBoolean(NOTE_HAS_FOCUS_KEY)
            }
        } else {
            mData!!.run {
                if (taskData?.taskParentKey != null) {
                    check(mParentTaskKeyHint == null)
                    check(mTaskKeys == null)
                    checkNotNull(mTaskKey)

                    mParent = findTaskData(taskData.taskParentKey)
                } else if (mParentTaskKeyHint != null) {
                    check(mTaskKey == null)

                    mParent = findTaskData(CreateTaskViewModel.ParentKey.TaskParentKey(mParentTaskKeyHint!!))
                }

                taskData?.let { mNote = it.note }
            }
        }

        (supportFragmentManager.findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG) as? ParentPickerFragment)?.initialize(mData!!.parentTreeDatas, mParentFragmentListener)

        invalidateOptionsMenu()

        if (mFirst && (mSavedInstanceState == null || !mSavedInstanceState!!.containsKey(SCHEDULE_ENTRIES_KEY))) {
            mFirst = false

            mScheduleEntries = ArrayList()

            mData!!.run {
                if (taskData != null) {
                    if (taskData.scheduleDatas != null) {
                        check(!taskData.scheduleDatas.isEmpty())

                        mScheduleEntries = taskData.scheduleDatas
                                .asSequence()
                                .map { scheduleData ->
                                    when (scheduleData) {
                                        is CreateTaskViewModel.ScheduleData.SingleScheduleData -> SingleScheduleEntry(scheduleData)
                                        is CreateTaskViewModel.ScheduleData.WeeklyScheduleData -> WeeklyScheduleEntry(scheduleData)
                                        is CreateTaskViewModel.ScheduleData.MonthlyDayScheduleData -> MonthlyDayScheduleEntry(scheduleData)
                                        is CreateTaskViewModel.ScheduleData.MonthlyWeekScheduleData -> MonthlyWeekScheduleEntry(scheduleData)
                                    }
                                }
                                .toMutableList()
                    }
                } else {
                    if (mParentTaskKeyHint == null)
                        mScheduleEntries.add(firstScheduleEntry())
                }
            }
        }

        (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.initialize(mData!!.customTimeDatas, mScheduleDialogListener)

        mCreateTaskAdapter = CreateTaskAdapter()
        scheduleRecycler.adapter = mCreateTaskAdapter

        if (mNoteHasFocus) { // keyboard hack
            val notePosition = mScheduleEntries.size + 1 + mCreateTaskAdapter.elementsBeforeSchedules()

            scheduleRecycler.addOnChildAttachStateChangeListener(mOnChildAttachStateChangeListener)

            (scheduleRecycler.layoutManager as LinearLayoutManager).scrollToPosition(notePosition)
        }

        check(!hasValueParentTask() || !hasValueSchedule())
    }

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        check(!hasValueParentTask() || !hasValueSchedule())

        return if (dataChanged()) {
            DiscardDialogFragment.newInstance().let {
                it.discardDialogListener = mDiscardDialogListener
                it.show(supportFragmentManager, DISCARD_TAG)
            }

            false
        } else {
            true
        }
    }

    private fun updateError(): Boolean {
        checkNotNull(mData)

        var hasError = false

        if (TextUtils.isEmpty(toolbarEditText.text)) {
            toolbarLayout.error = getString(R.string.nameError)

            hasError = true
        } else {
            toolbarLayout.error = null
        }

        for (scheduleEntry in mScheduleEntries) {
            if (scheduleEntry.scheduleType != ScheduleType.SINGLE)
                continue

            val singleScheduleEntry = scheduleEntry as SingleScheduleEntry

            if (mData!!.taskData != null && mData!!.taskData!!.scheduleDatas != null && mData!!.taskData!!.scheduleDatas!!.contains(scheduleEntry.scheduleData))
                continue

            if (singleScheduleEntry.mDate > Date.today())
                continue

            if (singleScheduleEntry.mDate < Date.today()) {
                setScheduleEntryError(scheduleEntry, R.string.error_date)

                hasError = true
                continue
            }

            val timePair = singleScheduleEntry.mTimePair
            val hourMinute = if (timePair.customTimeKey != null) {
                check(timePair.hourMinute == null)

                mData!!.customTimeDatas[timePair.customTimeKey]!!.hourMinutes[singleScheduleEntry.mDate.dayOfWeek]
            } else {
                checkNotNull(timePair.hourMinute)

                timePair.hourMinute
            }!!

            if (hourMinute <= HourMinute.now) {
                setScheduleEntryError(scheduleEntry, R.string.error_time)

                hasError = true
            }
        }

        return hasError
    }

    private fun setScheduleEntryError(scheduleEntry: ScheduleEntry, stringId: Int) {
        scheduleEntry.error = getString(stringId)
        check(!TextUtils.isEmpty(scheduleEntry.error))

        val index = mScheduleEntries.indexOf(scheduleEntry)
        check(index >= 0)

        scheduleRecycler.getChildAt(index + mCreateTaskAdapter.elementsBeforeSchedules())?.let {
            (scheduleRecycler.getChildViewHolder(it) as CreateTaskAdapter.ScheduleHolder).mScheduleLayout.error = scheduleEntry.error
        }
    }

    private fun dataChanged(): Boolean {
        if (mData == null)
            return false

        check(!hasValueParentTask() || !hasValueSchedule())

        if (mTaskKey != null) {
            checkNotNull(mData!!.taskData)
            check(mTaskKeys == null)
            check(mParentTaskKeyHint == null)
            check(mScheduleHint == null)

            if (toolbarEditText.text.toString() != mData!!.taskData!!.name)
                return true

            if (!Utils.stringEquals(mNote, mData!!.taskData!!.note))
                return true

            if (mData!!.taskData!!.taskParentKey != null) {
                if (!hasValueParentInGeneral())
                    return true

                return mParent!!.parentKey != mData!!.taskData!!.taskParentKey
            } else if (mData!!.taskData!!.scheduleDatas != null) {
                if (!hasValueSchedule())
                    return true

                return scheduleDataChanged()
            } else {
                return hasValueParentInGeneral() || hasValueSchedule()
            }
        } else {
            if (!TextUtils.isEmpty(toolbarEditText.text))
                return true

            if (!TextUtils.isEmpty(mNote))
                return true

            if (mParentTaskKeyHint != null) {
                check(mScheduleHint == null)

                if (!hasValueParentTask())
                    return true

                return mParent == null || mParent!!.parentKey != CreateTaskViewModel.ParentKey.TaskParentKey(mParentTaskKeyHint!!)
            } else {
                if (!hasValueSchedule())
                    return true

                return scheduleDataChanged()
            }
        }
    }

    private fun findTaskData(parentKey: CreateTaskViewModel.ParentKey): CreateTaskViewModel.ParentTreeData {
        checkNotNull(mData)

        return findTaskDataHelper(mData!!.parentTreeDatas, parentKey).single()
    }

    private fun findTaskDataHelper(taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, parentKey: CreateTaskViewModel.ParentKey): Iterable<CreateTaskViewModel.ParentTreeData> {
        if (taskDatas.containsKey(parentKey))
            return listOf(taskDatas[parentKey]!!)

        return taskDatas.values
                .map { findTaskDataHelper(it.parentTreeDatas, parentKey) }
                .flatten()
    }

    private fun clearParent() {
        if (mParent == null || mParent!!.parentKey.type == CreateTaskViewModel.ParentType.PROJECT)
            return

        mParent = null

        updateParentView()
    }

    private fun updateParentView() {
        val view = scheduleRecycler.getChildAt(mCreateTaskAdapter.elementsBeforeSchedules() - 1) ?: return

        val scheduleHolder = scheduleRecycler.getChildViewHolder(view) as CreateTaskAdapter.ScheduleHolder

        scheduleHolder.mScheduleText.setText(if (mParent != null) mParent!!.name else null)
    }

    private fun hasValueParentInGeneral() = mParent != null

    private fun hasValueParentTask() = mParent?.parentKey?.type == CreateTaskViewModel.ParentType.TASK

    private fun hasValueSchedule() = !mScheduleEntries.isEmpty()

    private fun firstScheduleEntry() = SingleScheduleEntry(mScheduleHint)

    private fun scheduleDataChanged(): Boolean {
        if (mData == null)
            return false

        val oldScheduleDatas = HashMultiset.create<CreateTaskViewModel.ScheduleData>(if (mData!!.taskData != null) {
            mData!!.taskData!!.scheduleDatas ?: listOf()
        } else {
            listOf(firstScheduleEntry().scheduleData)
        })

        val newScheduleDatas = HashMultiset.create<CreateTaskViewModel.ScheduleData>(mScheduleEntries.map { it.scheduleData })

        return oldScheduleDatas != newScheduleDatas
    }

    private fun clearSchedules() {
        val scheduleCount = mScheduleEntries.size

        mScheduleEntries = ArrayList()
        mCreateTaskAdapter.notifyItemRangeRemoved(mCreateTaskAdapter.elementsBeforeSchedules(), scheduleCount)
    }

    private fun removeListenerHelper() { // keyboard hack
        checkNotNull(scheduleRecycler)

        scheduleRecycler.removeOnChildAttachStateChangeListener(mOnChildAttachStateChangeListener)
    }

    @Parcelize
    class ScheduleHint(val date: Date, val timePair: TimePair? = null) : Parcelable {

        constructor(date: Date, hourMinute: HourMinute) : this(date, TimePair(hourMinute))

    }

    @Suppress("PrivatePropertyName")
    private inner class CreateTaskAdapter : RecyclerView.Adapter<CreateTaskAdapter.Holder>() {

        private val TYPE_SCHEDULE = 0
        private val TYPE_NOTE = 1

        private val mNameListener = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable) {
                mNote = s.toString()
            }
        }

        private val mProjectName: String?

        init {
            checkNotNull(mData)

            mProjectName = if (mData!!.taskData != null && !TextUtils.isEmpty(mData!!.taskData!!.projectName)) {
                mData!!.taskData!!.projectName
            } else {
                null
            }
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            TYPE_SCHEDULE -> ScheduleHolder(layoutInflater.inflate(R.layout.row_schedule, parent, false)!!)
            TYPE_NOTE -> NoteHolder(layoutInflater.inflate(R.layout.row_note, parent, false)!!)
            else -> throw UnsupportedOperationException()
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            if (position == 0 && !TextUtils.isEmpty(mProjectName)) {
                check(!TextUtils.isEmpty(mProjectName))

                (holder as ScheduleHolder).run {
                    mScheduleMargin.visibility = View.VISIBLE

                    mScheduleText.setText(mProjectName)

                    mScheduleLayout.run {
                        hint = getString(R.string.parentProject)
                        error = null
                        isHintAnimationEnabled = false
                    }

                    mScheduleText.run {
                        isEnabled = false
                        setOnClickListener(null)
                    }
                }
            } else if (position < elementsBeforeSchedules()) {
                (holder as ScheduleHolder).run {
                    mScheduleMargin.visibility = if (position == 0) View.VISIBLE else View.GONE

                    mScheduleLayout.run {
                        hint = getString(R.string.parentTask)
                        error = null
                        isHintAnimationEnabled = false

                        addOneShotGlobalLayoutListener {
                            isHintAnimationEnabled = true
                        }
                    }

                    mScheduleText.run {
                        setText(mParent?.name)

                        isEnabled = true

                        setOnClickListener {
                            ParentPickerFragment.newInstance(mParent != null).let {
                                it.show(supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                                it.initialize(mData!!.parentTreeDatas, mParentFragmentListener)
                            }
                        }
                    }
                }
            } else if (position < elementsBeforeSchedules() + mScheduleEntries.size) {
                (holder as ScheduleHolder).run {
                    val scheduleEntry = mScheduleEntries[position - elementsBeforeSchedules()]

                    mScheduleMargin.visibility = View.GONE

                    mScheduleLayout.run {
                        hint = null
                        error = scheduleEntry.error
                        isHintAnimationEnabled = false
                    }

                    mScheduleText.run {
                        setText(scheduleEntry.getText(mData!!.customTimeDatas, this@CreateTaskActivity))
                        isEnabled = true
                        setOnClickListener { onTextClick() }
                    }
                }
            } else if (position == elementsBeforeSchedules() + mScheduleEntries.size) {
                (holder as ScheduleHolder).run {
                    mScheduleMargin.visibility = View.GONE

                    mScheduleLayout.run {
                        hint = getString(R.string.addReminder)
                        error = null
                        isHintAnimationEnabled = false
                    }

                    mScheduleText.run {
                        text = null
                        isEnabled = true
                        setOnClickListener {
                            check(mHourMinutePickerPosition == null)

                            ScheduleDialogFragment.newInstance(firstScheduleEntry().getScheduleDialogData(Date.today(), mScheduleHint), false).let {
                                it.initialize(mData!!.customTimeDatas, mScheduleDialogListener)
                                it.show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                            }
                        }
                    }
                }
            } else {
                check(position == elementsBeforeSchedules() + mScheduleEntries.size + 1)

                (holder as NoteHolder).run {
                    mNoteLayout.isHintAnimationEnabled = mData != null

                    mNoteText.run {
                        setText(mNote)
                        removeTextChangedListener(mNameListener)
                        addTextChangedListener(mNameListener)
                        setOnFocusChangeListener { _, hasFocus -> mNoteHasFocus = hasFocus }
                    }
                }
            }
        }

        fun elementsBeforeSchedules() = if (TextUtils.isEmpty(mProjectName)) 1 else 2

        override fun getItemCount() = elementsBeforeSchedules() + mScheduleEntries.size + 1 + 1

        override fun getItemViewType(position: Int) = if (position == 0) {
            TYPE_SCHEDULE
        } else if (position < elementsBeforeSchedules()) {
            TYPE_SCHEDULE
        } else if (position < elementsBeforeSchedules() + mScheduleEntries.size) {
            TYPE_SCHEDULE
        } else if (position == elementsBeforeSchedules() + mScheduleEntries.size) {
            TYPE_SCHEDULE
        } else {
            check(position == elementsBeforeSchedules() + mScheduleEntries.size + 1)

            TYPE_NOTE
        }

        fun addScheduleEntry(scheduleEntry: ScheduleEntry) {
            val position = elementsBeforeSchedules() + mScheduleEntries.size

            mScheduleEntries.add(scheduleEntry)
            notifyItemInserted(position)
        }

        internal abstract inner class Holder(view: View) : RecyclerView.ViewHolder(view)

        internal inner class ScheduleHolder(scheduleRow: View) : Holder(scheduleRow) {

            val mScheduleMargin = itemView.schedule_margin!!
            val mScheduleLayout = itemView.schedule_layout!!
            val mScheduleText = itemView.schedule_text!!

            fun onTextClick() {
                checkNotNull(mData)
                check(mHourMinutePickerPosition == null)

                mHourMinutePickerPosition = adapterPosition

                val scheduleEntry = mScheduleEntries[mHourMinutePickerPosition!! - mCreateTaskAdapter.elementsBeforeSchedules()]

                ScheduleDialogFragment.newInstance(scheduleEntry.getScheduleDialogData(Date.today(), mScheduleHint), true).let {
                    it.initialize(mData!!.customTimeDatas, mScheduleDialogListener)
                    it.show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                }
            }
        }

        internal inner class NoteHolder(scheduleRow: View) : Holder(scheduleRow) {

            val mNoteLayout = itemView.noteLayout!!
            val mNoteText = itemView.noteText!!
        }
    }
}
