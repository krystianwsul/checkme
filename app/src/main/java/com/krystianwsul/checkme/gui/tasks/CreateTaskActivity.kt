package com.krystianwsul.checkme.gui.tasks

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
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

        fun getJoinIntent(joinTaskKeys: List<TaskKey>) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            check(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, ArrayList(joinTaskKeys))
        }

        fun getJoinIntent(joinTaskKeys: List<TaskKey>, parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            check(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, ArrayList(joinTaskKeys))
            putExtra(PARENT_TASK_KEY_HINT_KEY, parentTaskKeyHint as Parcelable)
        }

        fun getJoinIntent(joinTaskKeys: List<TaskKey>, scheduleHint: ScheduleHint) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            check(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, ArrayList(joinTaskKeys))
            putExtra(SCHEDULE_HINT_KEY, scheduleHint)
        }

        fun getEditIntent(taskKey: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }
    }

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener = this::finish

    private var taskKey: TaskKey? = null
    private var taskKeys: List<TaskKey>? = null

    private var scheduleHint: CreateTaskActivity.ScheduleHint? = null
    private var parentTaskKeyHint: TaskKey? = null
    private var nameHint: String? = null

    private var data: CreateTaskViewModel.Data? = null

    private var parent: CreateTaskViewModel.ParentTreeData? = null

    private lateinit var createTaskAdapter: CreateTaskAdapter

    private var hourMinutePickerPosition: Int? = null

    private lateinit var scheduleEntries: MutableList<ScheduleEntry>

    private var first = true

    private val parentFragmentListener = object : ParentPickerFragment.Listener {
        override fun onTaskSelected(parentTreeData: CreateTaskViewModel.ParentTreeData) {
            if (parentTreeData.parentKey.type == CreateTaskViewModel.ParentType.TASK)
                clearSchedules()

            parent = parentTreeData

            updateParentView()
        }

        override fun onTaskDeleted() {
            checkNotNull(parent)

            parent = null

            val view = scheduleRecycler.getChildAt(createTaskAdapter.elementsBeforeSchedules() - 1)!!

            val scheduleHolder = scheduleRecycler.getChildViewHolder(view) as CreateTaskAdapter.ScheduleHolder

            scheduleHolder.mScheduleText.text = null
        }
    }

    private val scheduleDialogListener = object : ScheduleDialogFragment.ScheduleDialogListener {
        override fun onScheduleDialogResult(scheduleDialogData: ScheduleDialogFragment.ScheduleDialogData) {
            checkNotNull(data)

            if (scheduleDialogData.scheduleType == ScheduleType.MONTHLY_DAY) {
                check(scheduleDialogData.monthlyDay)
            } else if (scheduleDialogData.scheduleType == ScheduleType.MONTHLY_WEEK) {
                check(!scheduleDialogData.monthlyDay)
            }

            if (hourMinutePickerPosition == null) {
                clearParent()

                createTaskAdapter.addScheduleEntry(ScheduleEntry.fromScheduleDialogData(scheduleDialogData))
            } else {
                hourMinutePickerPosition!!.let {
                    check(it >= createTaskAdapter.elementsBeforeSchedules())

                    scheduleEntries[it - createTaskAdapter.elementsBeforeSchedules()] = ScheduleEntry.fromScheduleDialogData(scheduleDialogData)

                    createTaskAdapter.notifyItemChanged(it)

                    hourMinutePickerPosition = null
                }
            }
        }

        override fun onScheduleDialogDelete() {
            hourMinutePickerPosition!!.let {
                check(it >= createTaskAdapter.elementsBeforeSchedules())
                checkNotNull(data)

                scheduleEntries.removeAt(it - createTaskAdapter.elementsBeforeSchedules())

                createTaskAdapter.notifyItemRemoved(it)

                hourMinutePickerPosition = null
            }
        }

        override fun onScheduleDialogCancel() {
            hourMinutePickerPosition?.let {
                check(it >= createTaskAdapter.elementsBeforeSchedules())

                hourMinutePickerPosition = null
            }
        }
    }

    private var note: String? = null

    private var noteHasFocus = false // keyboard hack

    private val onChildAttachStateChangeListener = object : RecyclerView.OnChildAttachStateChangeListener { // keyboard hack
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

    private val scheduleDatas get() = scheduleEntries.map { it.scheduleData }.apply { check(!isEmpty()) }

    private lateinit var createTaskViewModel: CreateTaskViewModel

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = data != null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        check(!hasValueParentTask() || !hasValueSchedule())

        when (item.itemId) {
            R.id.action_save -> {
                checkNotNull(data)
                checkNotNull(toolbarEditText)

                if (!updateError()) {

                    val name = toolbarEditText.text.toString().trim { it <= ' ' }
                    check(!TextUtils.isEmpty(name))

                    createTaskViewModel.stop()

                    if (hasValueSchedule()) {
                        check(!hasValueParentTask())

                        val projectId = if (hasValueParentInGeneral()) (parent!!.parentKey as CreateTaskViewModel.ParentKey.ProjectParentKey).projectId else null

                        if (taskKey != null) {
                            checkNotNull(data!!.taskData)
                            check(taskKeys == null)

                            val taskKey = DomainFactory.instance.updateScheduleTask(data!!.dataId, SaveService.Source.GUI, taskKey!!, name, scheduleDatas, note, projectId)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (taskKeys != null) {
                            check(data!!.taskData == null)
                            check(taskKeys!!.size > 1)

                            DomainFactory.instance.createScheduleJoinRootTask(ExactTimeStamp.now, data!!.dataId, SaveService.Source.GUI, name, scheduleDatas, taskKeys!!, note, projectId)

                            finish()
                        } else {
                            check(data!!.taskData == null)

                            DomainFactory.instance.createScheduleRootTask(data!!.dataId, SaveService.Source.GUI, name, scheduleDatas, note, projectId)

                            finish()
                        }
                    } else if (hasValueParentTask()) {
                        checkNotNull(parent)

                        val parentTaskKey = (parent!!.parentKey as CreateTaskViewModel.ParentKey.TaskParentKey).taskKey

                        if (taskKey != null) {
                            checkNotNull(data!!.taskData)
                            check(taskKeys == null)

                            val taskKey = DomainFactory.instance.updateChildTask(ExactTimeStamp.now, data!!.dataId, SaveService.Source.GUI, taskKey!!, name, parentTaskKey, note)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (taskKeys != null) {
                            check(data!!.taskData == null)
                            check(taskKeys!!.size > 1)

                            DomainFactory.instance.createJoinChildTask(data!!.dataId, SaveService.Source.GUI, parentTaskKey, name, taskKeys!!, note)

                            finish()
                        } else {
                            check(data!!.taskData == null)

                            DomainFactory.instance.createChildTask(data!!.dataId, SaveService.Source.GUI, parentTaskKey, name, note)

                            finish()
                        }
                    } else {  // no reminder
                        val projectId = if (hasValueParentInGeneral()) (parent!!.parentKey as CreateTaskViewModel.ParentKey.ProjectParentKey).projectId else null

                        if (taskKey != null) {
                            checkNotNull(data!!.taskData)
                            check(taskKeys == null)

                            val taskKey = DomainFactory.instance.updateRootTask(data!!.dataId, SaveService.Source.GUI, taskKey!!, name, note, projectId)

                            setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                            finish()
                        } else if (taskKeys != null) {
                            check(data!!.taskData == null)

                            DomainFactory.instance.createJoinRootTask(data!!.dataId, SaveService.Source.GUI, name, taskKeys!!, note, projectId)

                            finish()
                        } else {
                            check(data!!.taskData == null)

                            DomainFactory.instance.createRootTask(data!!.dataId, SaveService.Source.GUI, name, note, projectId)

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

        this.savedInstanceState = savedInstanceState

        scheduleRecycler.layoutManager = LinearLayoutManager(this)

        intent.run {
            if (hasExtra(TASK_KEY_KEY)) {
                check(!hasExtra(TASK_KEYS_KEY))
                check(!hasExtra(PARENT_TASK_KEY_HINT_KEY))
                check(!hasExtra(SCHEDULE_HINT_KEY))

                taskKey = getParcelableExtra(TASK_KEY_KEY)!!
            } else if (action == Intent.ACTION_SEND) {
                check(type == "text/plain")

                nameHint = getStringExtra(Intent.EXTRA_TEXT)!!
                check(!nameHint.isNullOrEmpty())
            } else {
                if (hasExtra(TASK_KEYS_KEY))
                    taskKeys = getParcelableArrayListExtra<TaskKey>(TASK_KEYS_KEY)!!.apply { check(size > 1) }

                if (hasExtra(PARENT_TASK_KEY_HINT_KEY)) {
                    check(!hasExtra(SCHEDULE_HINT_KEY))

                    parentTaskKeyHint = getParcelableExtra(PARENT_TASK_KEY_HINT_KEY)!!
                } else if (hasExtra(SCHEDULE_HINT_KEY)) {
                    scheduleHint = getParcelableExtra(SCHEDULE_HINT_KEY)!!
                }
            }
        }

        savedInstanceState?.run {
            if (containsKey(SCHEDULE_ENTRIES_KEY)) {
                scheduleEntries = getParcelableArrayList(SCHEDULE_ENTRIES_KEY)!!

                if (containsKey(HOUR_MINUTE_PICKER_POSITION_KEY))
                    hourMinutePickerPosition = getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -1).also { check(it > 0) }
            }
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener

        if (!noteHasFocus)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        createTaskViewModel = getViewModel<CreateTaskViewModel>().apply {
            start(taskKey, taskKeys)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (data != null) {
                putParcelableArrayList(SCHEDULE_ENTRIES_KEY, ArrayList(scheduleEntries))

                hourMinutePickerPosition?.let {
                    check(it >= createTaskAdapter.elementsBeforeSchedules())

                    putInt(HOUR_MINUTE_PICKER_POSITION_KEY, it)
                }

                if (parent != null) {
                    putParcelable(PARENT_KEY_KEY, parent!!.parentKey)
                }

                if (!note.isNullOrEmpty())
                    putString(NOTE_KEY, note)

                putBoolean(NOTE_HAS_FOCUS_KEY, noteHasFocus)
            }
        }
    }

    private fun onLoadFinished(data: CreateTaskViewModel.Data) {
        this.data = data

        toolbarLayout.run {
            visibility = View.VISIBLE
            isHintAnimationEnabled = true
        }

        toolbarEditText.run {
            if (savedInstanceState == null) {
                if (this@CreateTaskActivity.data!!.taskData != null) {
                    checkNotNull(taskKey)

                    setText(this@CreateTaskActivity.data!!.taskData!!.name)
                } else if (!TextUtils.isEmpty(nameHint)) {
                    check(taskKey == null)
                    check(taskKeys == null)
                    check(parentTaskKeyHint == null)
                    check(scheduleHint == null)

                    setText(nameHint)
                }
            }

            toolbarEditText.addTextChangedListener(object : TextWatcher {

                private var mSkip = savedInstanceState != null

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

        if (savedInstanceState?.containsKey(SCHEDULE_ENTRIES_KEY) == true) {
            savedInstanceState!!.run {
                if (containsKey(PARENT_KEY_KEY)) {
                    val parentKey = getParcelable<CreateTaskViewModel.ParentKey>(PARENT_KEY_KEY)!!

                    parent = findTaskData(parentKey)
                }

                if (containsKey(NOTE_KEY)) {
                    note = getString(NOTE_KEY)!!
                    check(!note.isNullOrEmpty())
                }

                check(containsKey(NOTE_HAS_FOCUS_KEY))

                noteHasFocus = getBoolean(NOTE_HAS_FOCUS_KEY)
            }
        } else {
            this.data!!.run {
                if (taskData?.taskParentKey != null) {
                    check(parentTaskKeyHint == null)
                    check(taskKeys == null)
                    checkNotNull(taskKey)

                    parent = findTaskData(taskData.taskParentKey)
                } else if (parentTaskKeyHint != null) {
                    check(taskKey == null)

                    parent = findTaskData(CreateTaskViewModel.ParentKey.TaskParentKey(parentTaskKeyHint!!))
                }

                taskData?.let { note = it.note }
            }
        }

        (supportFragmentManager.findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG) as? ParentPickerFragment)?.initialize(this.data!!.parentTreeDatas, parentFragmentListener)

        invalidateOptionsMenu()

        if (first && (savedInstanceState == null || !savedInstanceState!!.containsKey(SCHEDULE_ENTRIES_KEY))) {
            first = false

            scheduleEntries = ArrayList()

            this.data!!.run {
                if (taskData != null) {
                    if (taskData.scheduleDatas != null) {
                        check(!taskData.scheduleDatas.isEmpty())

                        scheduleEntries = taskData.scheduleDatas
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
                    if (parentTaskKeyHint == null)
                        scheduleEntries.add(firstScheduleEntry())
                }
            }
        }

        (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.initialize(this.data!!.customTimeDatas, scheduleDialogListener)

        createTaskAdapter = CreateTaskAdapter()
        scheduleRecycler.adapter = createTaskAdapter

        if (noteHasFocus) { // keyboard hack
            val notePosition = scheduleEntries.size + 1 + createTaskAdapter.elementsBeforeSchedules()

            scheduleRecycler.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)

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
                it.discardDialogListener = discardDialogListener
                it.show(supportFragmentManager, DISCARD_TAG)
            }

            false
        } else {
            true
        }
    }

    private fun updateError(): Boolean {
        checkNotNull(data)

        var hasError = false

        if (TextUtils.isEmpty(toolbarEditText.text)) {
            toolbarLayout.error = getString(R.string.nameError)

            hasError = true
        } else {
            toolbarLayout.error = null
        }

        for (scheduleEntry in scheduleEntries) {
            if (scheduleEntry.scheduleType != ScheduleType.SINGLE)
                continue

            val singleScheduleEntry = scheduleEntry as SingleScheduleEntry

            if (data!!.taskData != null && data!!.taskData!!.scheduleDatas != null && data!!.taskData!!.scheduleDatas!!.contains(scheduleEntry.scheduleData))
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

                data!!.customTimeDatas[timePair.customTimeKey]!!.hourMinutes[singleScheduleEntry.mDate.dayOfWeek]
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

        val index = scheduleEntries.indexOf(scheduleEntry)
        check(index >= 0)

        scheduleRecycler.getChildAt(index + createTaskAdapter.elementsBeforeSchedules())?.let {
            (scheduleRecycler.getChildViewHolder(it) as CreateTaskAdapter.ScheduleHolder).mScheduleLayout.error = scheduleEntry.error
        }
    }

    private fun dataChanged(): Boolean {
        if (data == null)
            return false

        check(!hasValueParentTask() || !hasValueSchedule())

        if (taskKey != null) {
            checkNotNull(data!!.taskData)
            check(taskKeys == null)
            check(parentTaskKeyHint == null)
            check(scheduleHint == null)

            if (toolbarEditText.text.toString() != data!!.taskData!!.name)
                return true

            if (!Utils.stringEquals(note, data!!.taskData!!.note))
                return true

            if (data!!.taskData!!.taskParentKey != null) {
                if (!hasValueParentInGeneral())
                    return true

                return parent!!.parentKey != data!!.taskData!!.taskParentKey
            } else if (data!!.taskData!!.scheduleDatas != null) {
                if (!hasValueSchedule())
                    return true

                return scheduleDataChanged()
            } else {
                return hasValueParentInGeneral() || hasValueSchedule()
            }
        } else {
            if (!TextUtils.isEmpty(toolbarEditText.text))
                return true

            if (!TextUtils.isEmpty(note))
                return true

            if (parentTaskKeyHint != null) {
                check(scheduleHint == null)

                if (!hasValueParentTask())
                    return true

                return parent == null || parent!!.parentKey != CreateTaskViewModel.ParentKey.TaskParentKey(parentTaskKeyHint!!)
            } else {
                if (!hasValueSchedule())
                    return true

                return scheduleDataChanged()
            }
        }
    }

    private fun findTaskData(parentKey: CreateTaskViewModel.ParentKey): CreateTaskViewModel.ParentTreeData {
        checkNotNull(data)

        return findTaskDataHelper(data!!.parentTreeDatas, parentKey).single()
    }

    private fun findTaskDataHelper(taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, parentKey: CreateTaskViewModel.ParentKey): Iterable<CreateTaskViewModel.ParentTreeData> {
        if (taskDatas.containsKey(parentKey))
            return listOf(taskDatas[parentKey]!!)

        return taskDatas.values
                .map { findTaskDataHelper(it.parentTreeDatas, parentKey) }
                .flatten()
    }

    private fun clearParent() {
        if (parent == null || parent!!.parentKey.type == CreateTaskViewModel.ParentType.PROJECT)
            return

        parent = null

        updateParentView()
    }

    private fun updateParentView() {
        val view = scheduleRecycler.getChildAt(createTaskAdapter.elementsBeforeSchedules() - 1)
                ?: return

        val scheduleHolder = scheduleRecycler.getChildViewHolder(view) as CreateTaskAdapter.ScheduleHolder

        scheduleHolder.mScheduleText.setText(if (parent != null) parent!!.name else null)
    }

    private fun hasValueParentInGeneral() = parent != null

    private fun hasValueParentTask() = parent?.parentKey?.type == CreateTaskViewModel.ParentType.TASK

    private fun hasValueSchedule() = !scheduleEntries.isEmpty()

    private fun firstScheduleEntry() = SingleScheduleEntry(scheduleHint)

    private fun scheduleDataChanged(): Boolean {
        if (data == null)
            return false

        val oldScheduleDatas = HashMultiset.create<CreateTaskViewModel.ScheduleData>(if (data!!.taskData != null) {
            data!!.taskData!!.scheduleDatas ?: listOf()
        } else {
            listOf(firstScheduleEntry().scheduleData)
        })

        val newScheduleDatas = HashMultiset.create<CreateTaskViewModel.ScheduleData>(scheduleEntries.map { it.scheduleData })

        return oldScheduleDatas != newScheduleDatas
    }

    private fun clearSchedules() {
        val scheduleCount = scheduleEntries.size

        scheduleEntries = ArrayList()
        createTaskAdapter.notifyItemRangeRemoved(createTaskAdapter.elementsBeforeSchedules(), scheduleCount)
    }

    private fun removeListenerHelper() { // keyboard hack
        checkNotNull(scheduleRecycler)

        scheduleRecycler.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener)
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
                note = s.toString()
            }
        }

        private val mProjectName: String?

        init {
            checkNotNull(data)

            mProjectName = if (data!!.taskData != null && !TextUtils.isEmpty(data!!.taskData!!.projectName)) {
                data!!.taskData!!.projectName
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
                        setText(this@CreateTaskActivity.parent?.name)

                        isEnabled = data!!.parentTreeDatas.isNotEmpty()

                        setOnClickListener {
                            ParentPickerFragment.newInstance(parent != null).let {
                                it.show(supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                                it.initialize(data!!.parentTreeDatas, parentFragmentListener)
                            }
                        }
                    }
                }
            } else if (position < elementsBeforeSchedules() + scheduleEntries.size) {
                (holder as ScheduleHolder).run {
                    val scheduleEntry = scheduleEntries[position - elementsBeforeSchedules()]

                    mScheduleMargin.visibility = View.GONE

                    mScheduleLayout.run {
                        hint = null
                        error = scheduleEntry.error
                        isHintAnimationEnabled = false
                    }

                    mScheduleText.run {
                        setText(scheduleEntry.getText(data!!.customTimeDatas, this@CreateTaskActivity))
                        isEnabled = true
                        setOnClickListener { onTextClick() }
                    }
                }
            } else if (position == elementsBeforeSchedules() + scheduleEntries.size) {
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
                            check(hourMinutePickerPosition == null)

                            ScheduleDialogFragment.newInstance(firstScheduleEntry().getScheduleDialogData(Date.today(), scheduleHint), false).let {
                                it.initialize(data!!.customTimeDatas, scheduleDialogListener)
                                it.show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                            }
                        }
                    }
                }
            } else {
                check(position == elementsBeforeSchedules() + scheduleEntries.size + 1)

                (holder as NoteHolder).run {
                    mNoteLayout.isHintAnimationEnabled = data != null

                    mNoteText.run {
                        setText(note)
                        removeTextChangedListener(mNameListener)
                        addTextChangedListener(mNameListener)
                        setOnFocusChangeListener { _, hasFocus -> noteHasFocus = hasFocus }
                    }
                }
            }
        }

        fun elementsBeforeSchedules() = if (TextUtils.isEmpty(mProjectName)) 1 else 2

        override fun getItemCount() = elementsBeforeSchedules() + scheduleEntries.size + 1 + 1

        override fun getItemViewType(position: Int) = if (position == 0) {
            TYPE_SCHEDULE
        } else if (position < elementsBeforeSchedules()) {
            TYPE_SCHEDULE
        } else if (position < elementsBeforeSchedules() + scheduleEntries.size) {
            TYPE_SCHEDULE
        } else if (position == elementsBeforeSchedules() + scheduleEntries.size) {
            TYPE_SCHEDULE
        } else {
            check(position == elementsBeforeSchedules() + scheduleEntries.size + 1)

            TYPE_NOTE
        }

        fun addScheduleEntry(scheduleEntry: ScheduleEntry) {
            val position = elementsBeforeSchedules() + scheduleEntries.size

            scheduleEntries.add(scheduleEntry)
            notifyItemInserted(position)
        }

        internal abstract inner class Holder(view: View) : RecyclerView.ViewHolder(view)

        internal inner class ScheduleHolder(scheduleRow: View) : Holder(scheduleRow) {

            val mScheduleMargin = itemView.schedule_margin!!
            val mScheduleLayout = itemView.schedule_layout!!
            val mScheduleText = itemView.schedule_text!!

            fun onTextClick() {
                checkNotNull(data)
                check(hourMinutePickerPosition == null)

                hourMinutePickerPosition = adapterPosition

                val scheduleEntry = scheduleEntries[hourMinutePickerPosition!! - createTaskAdapter.elementsBeforeSchedules()]

                ScheduleDialogFragment.newInstance(scheduleEntry.getScheduleDialogData(Date.today(), scheduleHint), true).let {
                    it.initialize(data!!.customTimeDatas, scheduleDialogListener)
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
