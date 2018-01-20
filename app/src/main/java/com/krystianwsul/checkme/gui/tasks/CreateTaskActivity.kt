package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcel
import android.os.Parcelable
import android.support.v4.app.LoaderManager
import android.support.v4.content.Loader
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
import com.krystianwsul.checkme.loaders.CreateTaskLoader
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.ScheduleType
import com.krystianwsul.checkme.utils.TaskKey
import com.krystianwsul.checkme.utils.Utils
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import junit.framework.Assert
import kotlinx.android.synthetic.main.activity_create_task.*
import kotlinx.android.synthetic.main.row_note.view.*
import kotlinx.android.synthetic.main.row_schedule.view.*
import kotlinx.android.synthetic.main.toolbar_edit_text.*
import java.util.*

@Suppress("CascadeIf")
class CreateTaskActivity : AbstractActivity(), LoaderManager.LoaderCallbacks<CreateTaskLoader.Data> {

    companion object {

        private val DISCARD_TAG = "discard"

        private val TASK_KEY_KEY = "taskKey"
        private val TASK_KEYS_KEY = "taskKeys"

        private val PARENT_TASK_KEY_HINT_KEY = "parentTaskKeyHint"
        private val SCHEDULE_HINT_KEY = "scheduleHint"

        private val PARENT_KEY_KEY = "parentKey"
        private val PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment"

        private val HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition"
        private val SCHEDULE_ENTRIES_KEY = "scheduleEntries"
        private val NOTE_KEY = "note"
        private val NOTE_HAS_FOCUS_KEY = "noteHasFocus"

        private val SCHEDULE_DIALOG_TAG = "scheduleDialog"

        fun getCreateIntent(context: Context) = Intent(context, CreateTaskActivity::class.java)

        fun getCreateIntent(context: Context, scheduleHint: ScheduleHint) = Intent(context, CreateTaskActivity::class.java).apply { putExtra(SCHEDULE_HINT_KEY, scheduleHint) }

        fun getCreateIntent(parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply { putExtra(PARENT_TASK_KEY_HINT_KEY, parentTaskKeyHint as Parcelable) }

        fun getJoinIntent(joinTaskKeys: ArrayList<TaskKey>) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            Assert.assertTrue(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, joinTaskKeys)
        }

        fun getJoinIntent(joinTaskKeys: ArrayList<TaskKey>, parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            Assert.assertTrue(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, joinTaskKeys)
            putExtra(PARENT_TASK_KEY_HINT_KEY, parentTaskKeyHint as Parcelable)
        }

        fun getJoinIntent(joinTaskKeys: ArrayList<TaskKey>, scheduleHint: ScheduleHint) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            Assert.assertTrue(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, joinTaskKeys)
            putExtra(SCHEDULE_HINT_KEY, scheduleHint)
        }

        fun getEditIntent(taskKey: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private var mSavedInstanceState: Bundle? = null

    private val mDiscardDialogListener = DiscardDialogFragment.DiscardDialogListener { this@CreateTaskActivity.finish() }

    private var mTaskKey: TaskKey? = null
    private var mTaskKeys: List<TaskKey>? = null

    private var mScheduleHint: CreateTaskActivity.ScheduleHint? = null
    private var mParentTaskKeyHint: TaskKey? = null
    private var mNameHint: String? = null

    private var mData: CreateTaskLoader.Data? = null

    private var mParent: CreateTaskLoader.ParentTreeData? = null

    private lateinit var mCreateTaskAdapter: CreateTaskAdapter

    private var mHourMinutePickerPosition: Int? = null

    private lateinit var mScheduleEntries: MutableList<ScheduleEntry>

    private var mFirst = true

    private val mParentFragmentListener = object : ParentPickerFragment.Listener {
        override fun onTaskSelected(parentTreeData: CreateTaskLoader.ParentTreeData) {
            if (parentTreeData.parentKey.type == CreateTaskLoader.ParentType.TASK)
                clearSchedules()

            mParent = parentTreeData

            updateParentView()
        }

        override fun onTaskDeleted() {
            Assert.assertTrue(mParent != null)

            mParent = null

            val view = scheduleRecycler.getChildAt(mCreateTaskAdapter.elementsBeforeSchedules() - 1)!!

            val scheduleHolder = scheduleRecycler.getChildViewHolder(view) as CreateTaskAdapter.ScheduleHolder

            scheduleHolder.mScheduleText.text = null
        }
    }

    private val mScheduleDialogListener = object : ScheduleDialogFragment.ScheduleDialogListener {
        override fun onScheduleDialogResult(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
            Assert.assertTrue(mData != null)

            if (scheduleDialogData.mScheduleType == ScheduleType.MONTHLY_DAY) {
                Assert.assertTrue(scheduleDialogData.mMonthlyDay)
            } else if (scheduleDialogData.mScheduleType == ScheduleType.MONTHLY_WEEK) {
                Assert.assertTrue(!scheduleDialogData.mMonthlyDay)
            }

            if (mHourMinutePickerPosition == null) {
                clearParent()

                mCreateTaskAdapter.addScheduleEntry(ScheduleEntry.fromScheduleDialogData(scheduleDialogData))
            } else {
                mHourMinutePickerPosition!!.let {
                    Assert.assertTrue(it >= mCreateTaskAdapter.elementsBeforeSchedules())

                    mScheduleEntries[it - mCreateTaskAdapter.elementsBeforeSchedules()] = ScheduleEntry.fromScheduleDialogData(scheduleDialogData)

                    mCreateTaskAdapter.notifyItemChanged(it)

                    mHourMinutePickerPosition = null
                }
            }
        }

        override fun onScheduleDialogDelete() {
            mHourMinutePickerPosition!!.let {
                Assert.assertTrue(it >= mCreateTaskAdapter.elementsBeforeSchedules())
                Assert.assertTrue(mData != null)

                mScheduleEntries.removeAt(it - mCreateTaskAdapter.elementsBeforeSchedules())

                mCreateTaskAdapter.notifyItemRemoved(it)

                mHourMinutePickerPosition = null
            }
        }

        override fun onScheduleDialogCancel() {
            mHourMinutePickerPosition?.let {
                Assert.assertTrue(it >= mCreateTaskAdapter.elementsBeforeSchedules())

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

    private val scheduleDatas get() = mScheduleEntries.map { it.scheduleData }.apply { Assert.assertTrue(!isEmpty()) }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = mData != null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        Assert.assertTrue(!hasValueParentTask() || !hasValueSchedule())

        when (item.itemId) {
            R.id.action_save -> {
                Assert.assertTrue(mData != null)
                Assert.assertTrue(toolbarEditText != null)

                if (!updateError()) {

                    val name = toolbarEditText.text.toString().trim { it <= ' ' }
                    Assert.assertTrue(!TextUtils.isEmpty(name))

                    supportLoaderManager.destroyLoader(0)

                    if (hasValueSchedule()) {
                        Assert.assertTrue(!hasValueParentTask())

                        val projectId = if (hasValueParentInGeneral()) (mParent!!.parentKey as CreateTaskLoader.ParentKey.ProjectParentKey).projectId else null

                        if (mTaskKey != null) {
                            Assert.assertTrue(mData!!.taskData != null)
                            Assert.assertTrue(mTaskKeys == null)

                            val taskKey = DomainFactory.getDomainFactory().updateScheduleTask(this, mData!!.DataId, SaveService.Source.GUI, mTaskKey!!, name, scheduleDatas, mNote, projectId)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (mTaskKeys != null) {
                            Assert.assertTrue(mData!!.taskData == null)
                            Assert.assertTrue(mTaskKeys!!.size > 1)

                            DomainFactory.getDomainFactory().createScheduleJoinRootTask(this, ExactTimeStamp.getNow(), mData!!.DataId, SaveService.Source.GUI, name, scheduleDatas, mTaskKeys!!, mNote, projectId)

                            finish()
                        } else {
                            Assert.assertTrue(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createScheduleRootTask(this, mData!!.DataId, SaveService.Source.GUI, name, scheduleDatas, mNote, projectId)

                            finish()
                        }
                    } else if (hasValueParentTask()) {
                        Assert.assertTrue(mParent != null)

                        val parentTaskKey = (mParent!!.parentKey as CreateTaskLoader.ParentKey.TaskParentKey).taskKey

                        if (mTaskKey != null) {
                            Assert.assertTrue(mData!!.taskData != null)
                            Assert.assertTrue(mTaskKeys == null)

                            val taskKey = DomainFactory.getDomainFactory().updateChildTask(this, ExactTimeStamp.getNow(), mData!!.DataId, SaveService.Source.GUI, mTaskKey!!, name, parentTaskKey, mNote)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (mTaskKeys != null) {
                            Assert.assertTrue(mData!!.taskData == null)
                            Assert.assertTrue(mTaskKeys!!.size > 1)

                            DomainFactory.getDomainFactory().createJoinChildTask(this, mData!!.DataId, SaveService.Source.GUI, parentTaskKey, name, mTaskKeys!!, mNote)

                            finish()
                        } else {
                            Assert.assertTrue(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createChildTask(this, mData!!.DataId, SaveService.Source.GUI, parentTaskKey, name, mNote)

                            finish()
                        }
                    } else {  // no reminder
                        val projectId = if (hasValueParentInGeneral()) (mParent!!.parentKey as CreateTaskLoader.ParentKey.ProjectParentKey).projectId else null

                        if (mTaskKey != null) {
                            Assert.assertTrue(mData!!.taskData != null)
                            Assert.assertTrue(mTaskKeys == null)

                            val taskKey = DomainFactory.getDomainFactory().updateRootTask(this, mData!!.DataId, SaveService.Source.GUI, mTaskKey!!, name, mNote, projectId)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (mTaskKeys != null) {
                            Assert.assertTrue(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createJoinRootTask(this, mData!!.DataId, SaveService.Source.GUI, name, mTaskKeys!!, mNote, projectId)

                            finish()
                        } else {
                            Assert.assertTrue(mData!!.taskData == null)

                            DomainFactory.getDomainFactory().createRootTask(this, mData!!.DataId, SaveService.Source.GUI, name, mNote, projectId)

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
                Assert.assertTrue(!hasExtra(TASK_KEYS_KEY))
                Assert.assertTrue(!hasExtra(PARENT_TASK_KEY_HINT_KEY))
                Assert.assertTrue(!hasExtra(SCHEDULE_HINT_KEY))

                mTaskKey = getParcelableExtra(TASK_KEY_KEY)!!
            } else if (action == Intent.ACTION_SEND) {
                Assert.assertTrue(type == "text/plain")

                mNameHint = getStringExtra(Intent.EXTRA_TEXT)!!
                Assert.assertTrue(!mNameHint.isNullOrEmpty())
            } else {
                if (hasExtra(TASK_KEYS_KEY))
                    mTaskKeys = getParcelableArrayListExtra<TaskKey>(TASK_KEYS_KEY)!!.apply { Assert.assertTrue(size > 1) }

                if (hasExtra(PARENT_TASK_KEY_HINT_KEY)) {
                    Assert.assertTrue(!hasExtra(SCHEDULE_HINT_KEY))

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
                    mHourMinutePickerPosition = getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -1).also { Assert.assertTrue(it > 0) }
            }
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.setDiscardDialogListener(mDiscardDialogListener)

        if (!mNoteHasFocus)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        supportLoaderManager.initLoader<CreateTaskLoader.Data>(0, null, this)
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (mData != null) {
                putParcelableArrayList(SCHEDULE_ENTRIES_KEY, ArrayList(mScheduleEntries))

                mHourMinutePickerPosition?.let {
                    Assert.assertTrue(it >= mCreateTaskAdapter.elementsBeforeSchedules())

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

    override fun onCreateLoader(id: Int, args: Bundle?) = CreateTaskLoader(this, mTaskKey, mTaskKeys)

    override fun onLoadFinished(loader: Loader<CreateTaskLoader.Data>, data: CreateTaskLoader.Data) {
        mData = data

        toolbarLayout.run {
            visibility = View.VISIBLE
            isHintAnimationEnabled = true
        }

        toolbarEditText.run {
            if (mSavedInstanceState == null) {
                if (mData!!.taskData != null) {
                    Assert.assertTrue(mTaskKey != null)

                    setText(mData!!.taskData!!.name)
                } else if (!TextUtils.isEmpty(mNameHint)) {
                    Assert.assertTrue(mTaskKey == null)
                    Assert.assertTrue(mTaskKeys == null)
                    Assert.assertTrue(mParentTaskKeyHint == null)
                    Assert.assertTrue(mScheduleHint == null)

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
                    val parentKey = getParcelable<CreateTaskLoader.ParentKey>(PARENT_KEY_KEY)!!

                    mParent = findTaskData(parentKey)
                }

                if (containsKey(NOTE_KEY)) {
                    mNote = getString(NOTE_KEY)!!
                    Assert.assertTrue(!mNote.isNullOrEmpty())
                }

                Assert.assertTrue(containsKey(NOTE_HAS_FOCUS_KEY))

                mNoteHasFocus = getBoolean(NOTE_HAS_FOCUS_KEY)
            }
        } else {
            mData!!.run {
                if (taskData?.taskParentKey != null) {
                    Assert.assertTrue(mParentTaskKeyHint == null)
                    Assert.assertTrue(mTaskKeys == null)
                    Assert.assertTrue(mTaskKey != null)

                    mParent = findTaskData(taskData.taskParentKey)
                } else if (mParentTaskKeyHint != null) {
                    Assert.assertTrue(mTaskKey == null)

                    mParent = findTaskData(CreateTaskLoader.ParentKey.TaskParentKey(mParentTaskKeyHint!!))
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
                        Assert.assertTrue(!taskData.scheduleDatas.isEmpty())

                        mScheduleEntries = taskData.scheduleDatas
                                .map { scheduleData ->
                                    when (scheduleData) {
                                        is CreateTaskLoader.ScheduleData.SingleScheduleData -> SingleScheduleEntry(scheduleData)
                                        is CreateTaskLoader.ScheduleData.WeeklyScheduleData -> WeeklyScheduleEntry(scheduleData)
                                        is CreateTaskLoader.ScheduleData.MonthlyDayScheduleData -> MonthlyDayScheduleEntry(scheduleData)
                                        is CreateTaskLoader.ScheduleData.MonthlyWeekScheduleData -> MonthlyWeekScheduleEntry(scheduleData)
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

        Assert.assertTrue(!hasValueParentTask() || !hasValueSchedule())
    }

    override fun onLoaderReset(loader: Loader<CreateTaskLoader.Data>) = Unit

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        Assert.assertTrue(!hasValueParentTask() || !hasValueSchedule())

        return if (dataChanged()) {
            DiscardDialogFragment.newInstance().let {
                it.setDiscardDialogListener(mDiscardDialogListener)
                it.show(supportFragmentManager, DISCARD_TAG)
            }

            false
        } else {
            true
        }
    }

    private fun updateError(): Boolean {
        Assert.assertTrue(mData != null)

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
            val hourMinute = if (timePair.mCustomTimeKey != null) {
                Assert.assertTrue(timePair.mHourMinute == null)

                mData!!.customTimeDatas[timePair.mCustomTimeKey]!!.hourMinutes[singleScheduleEntry.mDate.dayOfWeek]
            } else {
                Assert.assertTrue(timePair.mHourMinute != null)

                timePair.mHourMinute
            }!!

            if (hourMinute <= HourMinute.getNow()) {
                setScheduleEntryError(scheduleEntry, R.string.error_time)

                hasError = true
            }
        }

        return hasError
    }

    private fun setScheduleEntryError(scheduleEntry: ScheduleEntry, stringId: Int) {
        scheduleEntry.mError = getString(stringId)
        Assert.assertTrue(!TextUtils.isEmpty(scheduleEntry.mError))

        val index = mScheduleEntries.indexOf(scheduleEntry)
        Assert.assertTrue(index >= 0)

        scheduleRecycler.getChildAt(index + mCreateTaskAdapter.elementsBeforeSchedules())?.let {
            (scheduleRecycler.getChildViewHolder(it) as CreateTaskAdapter.ScheduleHolder).mScheduleLayout.error = scheduleEntry.mError
        }
    }

    private fun dataChanged(): Boolean {
        if (mData == null)
            return false

        Assert.assertTrue(!hasValueParentTask() || !hasValueSchedule())

        if (mTaskKey != null) {
            Assert.assertTrue(mData!!.taskData != null)
            Assert.assertTrue(mTaskKeys == null)
            Assert.assertTrue(mParentTaskKeyHint == null)
            Assert.assertTrue(mScheduleHint == null)

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
                Assert.assertTrue(mScheduleHint == null)

                if (!hasValueParentTask())
                    return true

                return mParent == null || mParent!!.parentKey != CreateTaskLoader.ParentKey.TaskParentKey(mParentTaskKeyHint!!)
            } else {
                if (!hasValueSchedule())
                    return true

                return scheduleDataChanged()
            }
        }
    }

    private fun findTaskData(parentKey: CreateTaskLoader.ParentKey): CreateTaskLoader.ParentTreeData {
        Assert.assertTrue(mData != null)

        return findTaskDataHelper(mData!!.parentTreeDatas, parentKey).single()
    }

    private fun findTaskDataHelper(taskDatas: Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData>, parentKey: CreateTaskLoader.ParentKey): Iterable<CreateTaskLoader.ParentTreeData> {
        if (taskDatas.containsKey(parentKey))
            return listOf(taskDatas[parentKey]!!)

        return taskDatas.values
                .map { findTaskDataHelper(it.parentTreeDatas, parentKey) }
                .flatten()
    }

    private fun clearParent() {
        if (mParent == null || mParent!!.parentKey.type == CreateTaskLoader.ParentType.PROJECT)
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

    private fun hasValueParentTask() = mParent?.parentKey?.type == CreateTaskLoader.ParentType.TASK

    private fun hasValueSchedule() = !mScheduleEntries.isEmpty()

    private fun firstScheduleEntry() = SingleScheduleEntry(mScheduleHint)

    private fun scheduleDataChanged(): Boolean {
        if (mData == null)
            return false

        val oldScheduleDatas = HashMultiset.create<CreateTaskLoader.ScheduleData>(if (mData!!.taskData != null) {
            mData!!.taskData!!.scheduleDatas ?: listOf()
        } else {
            listOf(firstScheduleEntry().scheduleData)
        })

        val newScheduleDatas = HashMultiset.create<CreateTaskLoader.ScheduleData>(mScheduleEntries.map { it.scheduleData })

        return oldScheduleDatas != newScheduleDatas
    }

    private fun clearSchedules() {
        val scheduleCount = mScheduleEntries.size

        mScheduleEntries = ArrayList()
        mCreateTaskAdapter.notifyItemRangeRemoved(mCreateTaskAdapter.elementsBeforeSchedules(), scheduleCount)
    }

    private fun removeListenerHelper() { // keyboard hack
        Assert.assertTrue(scheduleRecycler != null)

        scheduleRecycler.removeOnChildAttachStateChangeListener(mOnChildAttachStateChangeListener)
    }

    class ScheduleHint : Parcelable {

        companion object {

            @JvmField
            val CREATOR: Parcelable.Creator<ScheduleHint> = object : Parcelable.Creator<ScheduleHint> {

                override fun createFromParcel(source: Parcel) = source.run {
                    val date = readParcelable<Date>(Date::class.java.classLoader)!!

                    val hasTimePair = readInt() == 1
                    val timePair = if (hasTimePair) readParcelable<TimePair>(HourMinute::class.java.classLoader)!! else null

                    ScheduleHint(date, timePair)
                }

                override fun newArray(size: Int) = arrayOfNulls<ScheduleHint>(size)
            }
        }

        val mDate: Date

        val mTimePair: TimePair?

        constructor(date: Date) { // root group list
            mDate = date
            mTimePair = null
        }

        constructor(date: Date, hourMinute: HourMinute) { // group list for group
            mDate = date
            mTimePair = TimePair(hourMinute)
        }

        constructor(date: Date, timePair: TimePair?) { // join instances, parcelable
            mDate = date
            mTimePair = timePair
        }

        override fun describeContents() = 0

        override fun writeToParcel(dest: Parcel, flags: Int) {
            dest.run {
                writeParcelable(mDate, 0)

                if (mTimePair == null) {
                    writeInt(0)
                } else {
                    writeInt(1)
                    writeParcelable(mTimePair, 0)
                }
            }
        }
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
            Assert.assertTrue(mData != null)

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
                Assert.assertTrue(!TextUtils.isEmpty(mProjectName))

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
                        error = scheduleEntry.mError
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
                            Assert.assertTrue(mHourMinutePickerPosition == null)

                            ScheduleDialogFragment.newInstance(firstScheduleEntry().getScheduleDialogData(Date.today(), mScheduleHint), false).let {
                                it.initialize(mData!!.customTimeDatas, mScheduleDialogListener)
                                it.show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                            }
                        }
                    }
                }
            } else {
                Assert.assertTrue(position == elementsBeforeSchedules() + mScheduleEntries.size + 1)

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
            Assert.assertTrue(position == elementsBeforeSchedules() + mScheduleEntries.size + 1)

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
                Assert.assertTrue(mData != null)
                Assert.assertTrue(mHourMinutePickerPosition == null)

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
