package com.krystianwsul.checkme.gui.tasks

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.util.Log
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.widget.ImageView
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.jakewharton.rxrelay2.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.gui.NavBarActivity
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.*
import com.krystianwsul.checkme.utils.time.Date
import com.krystianwsul.checkme.utils.time.ExactTimeStamp
import com.krystianwsul.checkme.utils.time.HourMinute
import com.krystianwsul.checkme.utils.time.TimePair
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.miguelbcr.ui.rx_paparazzo2.entities.FileData
import com.miguelbcr.ui.rx_paparazzo2.entities.Response
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_create_task.*
import kotlinx.android.synthetic.main.row_image.view.*
import kotlinx.android.synthetic.main.row_note.view.*
import kotlinx.android.synthetic.main.row_schedule.view.*
import kotlinx.android.synthetic.main.toolbar_edit_text.*
import java.io.Serializable
import kotlin.properties.Delegates.observable


class CreateTaskActivity : NavBarActivity() {

    companion object {

        private const val DISCARD_TAG = "discard"

        private const val TASK_KEY_KEY = "taskKey"
        private const val TASK_KEYS_KEY = "taskKeys"

        private const val KEY_HINT = "hint"
        private const val KEY_REMOVE_INSTANCE_KEYS = "removeInstanceKeys"
        private const val KEY_PARENT_PROJECT = "parentProject"
        private const val KEY_PARENT_TASK = "parentTask"
        private const val KEY_SHORTCUT_ID = "android.intent.extra.shortcut.ID"

        private const val PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment"

        private const val HOUR_MINUTE_PICKER_POSITION_KEY = "hourMinutePickerPosition"
        private const val KEY_INITIAL_STATE = "initialState"
        private const val KEY_STATE = "state"
        private const val NOTE_KEY = "note"
        private const val NOTE_HAS_FOCUS_KEY = "noteHasFocus"
        private const val IMAGE_URL_KEY = "imageUrl"

        private const val SCHEDULE_DIALOG_TAG = "scheduleDialog"
        private const val TAG_CAMERA_GALLERY = "cameraGallery"

        private const val REQUEST_CREATE_PARENT = 982

        fun getCreateIntent(hint: Hint? = null, parentScheduleState: ParentScheduleState? = null) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            hint?.let { putExtra(KEY_HINT, hint) }
            parentScheduleState?.let { putExtra(KEY_INITIAL_STATE, parentScheduleState) }
        }

        fun getJoinIntent(
                joinTaskKeys: List<TaskKey>,
                hint: Hint? = null,
                removeInstanceKeys: List<InstanceKey> = listOf()) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            check(joinTaskKeys.size > 1)

            putParcelableArrayListExtra(TASK_KEYS_KEY, ArrayList(joinTaskKeys))
            hint?.let { putExtra(KEY_HINT, hint) }
            putParcelableArrayListExtra(KEY_REMOVE_INSTANCE_KEYS, ArrayList(removeInstanceKeys))
        }

        fun getEditIntent(taskKey: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply { putExtra(TASK_KEY_KEY, taskKey as Parcelable) }

        fun getShortcutIntent(parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            action = Intent.ACTION_DEFAULT
            putExtra(KEY_PARENT_PROJECT, parentTaskKeyHint.remoteProjectId)
            putExtra(KEY_PARENT_TASK, parentTaskKeyHint.remoteTaskId)
        }

        var createdTaskKey: TaskKey? = null
    }

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener = this::finish

    private var taskKey: TaskKey? = null
    private var taskKeys: List<TaskKey>? = null

    private var hint: Hint? = null
    private var nameHint: String? = null

    private var data: CreateTaskViewModel.Data? = null

    private lateinit var createTaskAdapter: CreateTaskAdapter

    private var hourMinutePickerPosition: Int? = null

    private lateinit var initialState: ParentScheduleState
    private var tmpState: ParentScheduleState? = null
    private lateinit var stateData: ParentScheduleData

    private val parentFragmentListener = object : ParentPickerFragment.Listener {

        override fun onTaskSelected(parentTreeData: CreateTaskViewModel.ParentTreeData) {
            stateData.parent = parentTreeData

            updateParentView()
        }

        override fun onTaskDeleted() {
            checkNotNull(stateData.parent)

            stateData.parent = null

            val view = createTaskRecycler.getChildAt(createTaskAdapter.elementsBeforeSchedules() - 1)!!

            val scheduleHolder = createTaskRecycler.getChildViewHolder(view) as CreateTaskAdapter.ScheduleHolder

            scheduleHolder.scheduleText.text = null
        }

        override fun onNewParent() = startActivityForResult(getCreateIntent(hint, ParentScheduleState(stateData.state.parentKey, stateData.state.schedules.map { ScheduleEntry(it.scheduleData) }.toMutableList())), REQUEST_CREATE_PARENT)
    }

    private fun setupParent(view: View) {
        if (view !is EditText) {
            view.setOnTouchListener { _, _ ->
                hideSoftKeyboard()
                false
            }
        }

        if (view is ViewGroup) {
            for (i in 0 until view.childCount) {
                setupParent(view.getChildAt(i))
            }
        }
    }

    private fun hideSoftKeyboard() {
        val inputMethodManager = getSystemService(Activity.INPUT_METHOD_SERVICE) as InputMethodManager
        inputMethodManager.hideSoftInputFromWindow(createTaskRoot.windowToken, 0)
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
                clearParentTask()

                createTaskAdapter.addScheduleEntry(scheduleDialogData.toScheduleEntry())
            } else {
                hourMinutePickerPosition!!.let {
                    check(it >= createTaskAdapter.elementsBeforeSchedules())

                    stateData.state.schedules[it - createTaskAdapter.elementsBeforeSchedules()] = scheduleDialogData.toScheduleEntry()

                    createTaskAdapter.notifyItemChanged(it)

                    hourMinutePickerPosition = null
                }
            }
        }

        override fun onScheduleDialogDelete() {
            hourMinutePickerPosition!!.let {
                check(it >= createTaskAdapter.elementsBeforeSchedules())
                checkNotNull(data)

                stateData.state
                        .schedules
                        .removeAt(it - createTaskAdapter.elementsBeforeSchedules())

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

    private val scheduleDatas
        get() = stateData.state
                .schedules
                .map { it.scheduleData }
                .apply { check(!isEmpty()) }

    private lateinit var createTaskViewModel: CreateTaskViewModel

    val imageUrl = BehaviorRelay.createDefault<ImageState>(ImageState.None)

    private lateinit var removeInstanceKeys: List<InstanceKey>

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

                    val writeImagePath = imageUrl.value!!.writeImagePath

                    val taskKey: TaskKey = when {
                        hasValueSchedule() -> {
                            check(!hasValueParentTask())

                            val projectId = if (hasValueParentInGeneral()) (stateData.parent!!.parentKey as CreateTaskViewModel.ParentKey.Project).projectId else null

                            when {
                                taskKey != null -> {
                                    checkNotNull(data!!.taskData)
                                    check(taskKeys == null)
                                    check(removeInstanceKeys.isEmpty())

                                    DomainFactory.instance.updateScheduleTask(
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            taskKey!!,
                                            name,
                                            scheduleDatas,
                                            note,
                                            projectId,
                                            writeImagePath)
                                }
                                taskKeys != null -> {
                                    check(data!!.taskData == null)
                                    check(taskKeys!!.size > 1)

                                    DomainFactory.instance.createScheduleJoinRootTask(
                                            ExactTimeStamp.now,
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            name,
                                            scheduleDatas,
                                            taskKeys!!,
                                            note,
                                            projectId,
                                            writeImagePath?.value,
                                            removeInstanceKeys).also { createdTaskKey = it }
                                }
                                else -> {
                                    check(data!!.taskData == null)
                                    check(removeInstanceKeys.isEmpty())

                                    DomainFactory.instance.createScheduleRootTask(
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            name,
                                            scheduleDatas,
                                            note,
                                            projectId,
                                            writeImagePath?.value).also { createdTaskKey = it }
                                }
                            }
                        }
                        hasValueParentTask() -> {
                            checkNotNull(stateData.parent)

                            val parentTaskKey = (stateData.parent!!.parentKey as CreateTaskViewModel.ParentKey.Task).taskKey

                            if (intent.action == Intent.ACTION_SEND)
                                ShortcutManager.addShortcut(parentTaskKey)

                            when {
                                taskKey != null -> {
                                    checkNotNull(data!!.taskData)
                                    check(taskKeys == null)
                                    check(removeInstanceKeys.isEmpty())

                                    DomainFactory.instance.updateChildTask(
                                            ExactTimeStamp.now,
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            taskKey!!,
                                            name,
                                            parentTaskKey,
                                            note,
                                            writeImagePath)
                                }
                                taskKeys != null -> {
                                    check(data!!.taskData == null)
                                    check(taskKeys!!.size > 1)

                                    DomainFactory.instance.createJoinChildTask(
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            parentTaskKey,
                                            name,
                                            taskKeys!!,
                                            note,
                                            writeImagePath?.value,
                                            removeInstanceKeys).also { createdTaskKey = it }
                                }
                                else -> {
                                    check(data!!.taskData == null)
                                    check(removeInstanceKeys.isEmpty())

                                    DomainFactory.instance.createChildTask(
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            parentTaskKey,
                                            name,
                                            note,
                                            writeImagePath?.value).also { createdTaskKey = it }
                                }
                            }
                        }
                        else -> {  // no reminder
                            val projectId = if (hasValueParentInGeneral()) (stateData.parent!!.parentKey as CreateTaskViewModel.ParentKey.Project).projectId else null

                            when {
                                taskKey != null -> {
                                    checkNotNull(data!!.taskData)
                                    check(taskKeys == null)
                                    check(removeInstanceKeys.isEmpty())

                                    DomainFactory.instance.updateRootTask(
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            taskKey!!,
                                            name,
                                            note,
                                            projectId,
                                            writeImagePath)
                                }
                                taskKeys != null -> {
                                    check(data!!.taskData == null)

                                    DomainFactory.instance.createJoinRootTask(
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            name,
                                            taskKeys!!,
                                            note,
                                            projectId,
                                            writeImagePath?.value,
                                            removeInstanceKeys).also { createdTaskKey = it }
                                }
                                else -> {
                                    check(data!!.taskData == null)
                                    check(removeInstanceKeys.isEmpty())

                                    DomainFactory.instance.createRootTask(
                                            data!!.dataId,
                                            SaveService.Source.GUI,
                                            name,
                                            note,
                                            projectId,
                                            writeImagePath?.value).also { createdTaskKey = it }
                                }
                            }
                        }
                    }

                    setResult(Activity.RESULT_OK, Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) })

                    finish()
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

        createTaskFocus.requestFocus()

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        this.savedInstanceState = savedInstanceState

        savedInstanceState?.run {
            imageUrl.accept(getSerializable(IMAGE_URL_KEY) as ImageState)
        }

        createTaskRecycler.layoutManager = LinearLayoutManager(this)

        intent.run {
            if (hasExtra(TASK_KEY_KEY)) {
                Log.e("asdf", "shortcut a")
                check(!hasExtra(TASK_KEYS_KEY))
                check(!hasExtra(KEY_HINT))

                taskKey = getParcelableExtra(TASK_KEY_KEY)!!
            } else {
                Log.e("asdf", "shortcut b")
                if (action == Intent.ACTION_SEND) {
                    Log.e("asdf", "shortcut c")
                    check(type == "text/plain")

                    nameHint = getStringExtra(Intent.EXTRA_TEXT)!!
                    check(!nameHint.isNullOrEmpty())
                }

                Log.e("asdf", "shortcut keys: " + extras?.keySet()?.joinToString(","))

                if (hasExtra(TASK_KEYS_KEY))
                    taskKeys = getParcelableArrayListExtra<TaskKey>(TASK_KEYS_KEY)!!.apply { check(size > 1) }

                hint = when {
                    hasExtra(KEY_HINT) -> {
                        check(!hasExtra(KEY_SHORTCUT_ID))

                        getSerializableExtra(KEY_HINT) as Hint
                    }
                    hasExtra(KEY_SHORTCUT_ID) -> Hint.Task(TaskKey.fromShortcut(getStringExtra(KEY_SHORTCUT_ID)!!))
                    hasExtra(KEY_PARENT_PROJECT) -> {
                        Log.e("asdf", "shortcut e")
                        check(hasExtra(KEY_PARENT_TASK))
                        check(!hasExtra(KEY_SHORTCUT_ID))

                        Hint.Task(TaskKey(getStringExtra(KEY_PARENT_PROJECT)!!, getStringExtra(KEY_PARENT_TASK)!!))
                    }
                    else -> null
                }
            }

            removeInstanceKeys = getParcelableArrayListExtra(KEY_REMOVE_INSTANCE_KEYS) ?: listOf()
        }

        if (savedInstanceState != null) {
            savedInstanceState.run {
                @Suppress("UNCHECKED_CAST")
                if (containsKey(KEY_INITIAL_STATE)) {
                    initialState = getParcelable(KEY_INITIAL_STATE)!!
                    tmpState = getParcelable(KEY_STATE)!!

                    if (containsKey(HOUR_MINUTE_PICKER_POSITION_KEY))
                        hourMinutePickerPosition = getInt(HOUR_MINUTE_PICKER_POSITION_KEY, -1).also { check(it > 0) }
                }
            }
        } else {
            if (intent.hasExtra(KEY_INITIAL_STATE)) {
                tmpState = intent.getParcelableExtra(KEY_INITIAL_STATE)!!
                initialState = ParentScheduleState(tmpState!!.parentKey, ArrayList(tmpState!!.schedules))
            }
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener

        if (!noteHasFocus)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        createTaskViewModel = getViewModel<CreateTaskViewModel>().apply {
            start(taskKey, taskKeys, (hint as? Hint.Task)?.taskKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        setupParent(createTaskRoot)
    }

    @SuppressLint("CheckResult")
    fun getImage(single: Observable<Response<CreateTaskActivity, FileData>>) {
        single.observeOn(AndroidSchedulers.mainThread()).subscribe {
            if (it.resultCode() == Activity.RESULT_OK) {
                val file = it.data().file
                it.targetUI()
                        .imageUrl
                        .accept(ImageState.Selected(file.absolutePath, file.toURI().toString()))
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (data != null) {
                putParcelable(KEY_STATE, stateData.state)
                putParcelable(KEY_INITIAL_STATE, initialState)

                hourMinutePickerPosition?.let {
                    check(it >= createTaskAdapter.elementsBeforeSchedules())

                    putInt(HOUR_MINUTE_PICKER_POSITION_KEY, it)
                }

                if (!note.isNullOrEmpty())
                    putString(NOTE_KEY, note)

                putBoolean(NOTE_HAS_FOCUS_KEY, noteHasFocus)
            }

            putSerializable(IMAGE_URL_KEY, imageUrl.value!!)
        }
    }

    private fun onLoadFinished(data: CreateTaskViewModel.Data) {
        this.data = data

        data.taskData
                ?.imageState
                ?.takeUnless { imageUrl.value!!.dontOverwrite }
                ?.let { imageUrl.accept(ImageState.Existing(it)) }

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

                    setText(nameHint)
                }
            }

            addTextChangedListener(object : TextWatcher {

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

        val parentHint = (hint as? Hint.Task)?.taskKey
        var parentKey: CreateTaskViewModel.ParentKey? = null

        if (savedInstanceState?.containsKey(KEY_STATE) == true) {
            savedInstanceState!!.run {
                if (containsKey(NOTE_KEY)) {
                    note = getString(NOTE_KEY)!!
                    check(!note.isNullOrEmpty())
                }

                check(containsKey(NOTE_HAS_FOCUS_KEY))

                noteHasFocus = getBoolean(NOTE_HAS_FOCUS_KEY)
            }
        } else {
            this.data!!.run {
                if (taskData?.parentKey != null) {
                    check(parentHint == null)
                    check(taskKeys == null)
                    checkNotNull(taskKey)

                    parentKey = taskData.parentKey
                } else if (parentHint != null) {
                    check(taskKey == null)

                    MyCrashlytics.log("CreateTaskActivity.parentTaskKeyHint: $parentHint")
                    parentKey = CreateTaskViewModel.ParentKey.Task(parentHint)
                }

                taskData?.let { note = it.note }
            }
        }

        (supportFragmentManager.findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG) as? ParentPickerFragment)?.initialize(this.data!!.parentTreeDatas, parentFragmentListener)

        invalidateOptionsMenu()

        if (!this::initialState.isInitialized) {
            check(!this::stateData.isInitialized)

            val schedules = mutableListOf<ScheduleEntry>()

            data.run {
                if (taskData != null) {
                    if (taskData.scheduleDatas != null) {
                        check(taskData.scheduleDatas.isNotEmpty())

                        schedules.addAll(taskData.scheduleDatas
                                .asSequence()
                                .map { ScheduleEntry(it) }
                                .toMutableList())
                    }
                } else {
                    if (parentHint == null && defaultReminder)
                        schedules.add(firstScheduleEntry())
                }
            }

            tmpState = ParentScheduleState(parentKey, schedules)
            initialState = ParentScheduleState(parentKey, ArrayList(schedules))
        }

        stateData = ParentScheduleData(tmpState!!)

        (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.initialize(this.data!!.customTimeDatas, scheduleDialogListener)

        createTaskAdapter = CreateTaskAdapter()
        createTaskRecycler.adapter = createTaskAdapter

        if (noteHasFocus) { // keyboard hack
            val notePosition = stateData.state
                    .schedules
                    .size + 1 + createTaskAdapter.elementsBeforeSchedules()

            createTaskRecycler.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)

            (createTaskRecycler.layoutManager as LinearLayoutManager).scrollToPosition(notePosition)
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

        for (scheduleEntry in stateData.state.schedules) {
            if (scheduleEntry.scheduleData !is CreateTaskViewModel.ScheduleData.Single)
                continue

            if (data!!.taskData != null && data!!.taskData!!.scheduleDatas != null && data!!.taskData!!.scheduleDatas!!.contains(scheduleEntry.scheduleData))
                continue

            if (scheduleEntry.scheduleData.date > Date.today())
                continue

            if (scheduleEntry.scheduleData.date < Date.today()) {
                setScheduleEntryError(scheduleEntry, R.string.error_date)

                hasError = true
                continue
            }

            val timePair = scheduleEntry.scheduleData.timePair
            val hourMinute = if (timePair.customTimeKey != null) {
                check(timePair.hourMinute == null)

                data!!.customTimeDatas
                        .getValue(timePair.customTimeKey)
                        .hourMinutes[scheduleEntry.scheduleData.date.dayOfWeek]
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

        val index = stateData.state
                .schedules
                .indexOf(scheduleEntry)
        check(index >= 0)

        createTaskRecycler.getChildAt(index + createTaskAdapter.elementsBeforeSchedules())?.let {
            (createTaskRecycler.getChildViewHolder(it) as CreateTaskAdapter.ScheduleHolder).scheduleLayout.error = scheduleEntry.error
        }
    }

    private fun dataChanged(): Boolean {
        if (data == null)
            return false

        check(!hasValueParentTask() || !hasValueSchedule())

        if (initialState != stateData.state)
            return true

        if (taskKey != null) {
            checkNotNull(data!!.taskData)
            check(taskKeys == null)
            check(hint == null)

            if (toolbarEditText.text.toString() != data!!.taskData!!.name)
                return true

            if (note != data!!.taskData!!.note)
                return true
        } else {
            if (!TextUtils.isEmpty(toolbarEditText.text))
                return true

            if (!TextUtils.isEmpty(note))
                return true
        }

        return false
    }

    private fun findTaskData(parentKey: CreateTaskViewModel.ParentKey): CreateTaskViewModel.ParentTreeData {
        checkNotNull(data)

        return findTaskDataHelper(data!!.parentTreeDatas, parentKey).single()
    }

    private fun findTaskDataHelper(taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>, parentKey: CreateTaskViewModel.ParentKey): Iterable<CreateTaskViewModel.ParentTreeData> {
        if (taskDatas.containsKey(parentKey))
            return listOf(taskDatas.getValue(parentKey))

        return taskDatas.values
                .map { findTaskDataHelper(it.parentTreeDatas, parentKey) }
                .flatten()
    }

    private fun clearParentTask() {
        if (stateData.parent == null || stateData.parent!!.parentKey is CreateTaskViewModel.ParentKey.Project)
            return

        stateData.parent = null

        updateParentView()
    }

    private fun updateParentView() {
        val view = createTaskRecycler.getChildAt(createTaskAdapter.elementsBeforeSchedules() - 1)
                ?: return

        val scheduleHolder = createTaskRecycler.getChildViewHolder(view) as CreateTaskAdapter.ScheduleHolder

        scheduleHolder.scheduleText.setText(if (stateData.parent != null) stateData.parent!!.name else null)
    }

    private fun hasValueParentInGeneral() = stateData.parent != null

    private fun hasValueParentTask() = stateData.parent?.parentKey is CreateTaskViewModel.ParentKey.Task

    private fun hasValueSchedule() = stateData.state
            .schedules
            .isNotEmpty()

    private fun firstScheduleEntry() = hintToSchedule(hint as? Hint.Schedule)

    private fun removeListenerHelper() { // keyboard hack
        checkNotNull(createTaskRecycler)

        createTaskRecycler.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CREATE_PARENT) {
            if (resultCode == Activity.RESULT_OK) {
                val taskKey = data!!.getParcelableExtra<TaskKey>(ShowTaskActivity.TASK_KEY_KEY)!!
                stateData.parent = findTaskData(CreateTaskViewModel.ParentKey.Task(taskKey))
            }
        }
    }

    private fun hintToSchedule(scheduleHint: Hint.Schedule?): ScheduleEntry {
        val (date, timePair) = scheduleHint?.let { Pair(it.date, it.timePair) }
                ?: HourMinute.nextHour.let { Pair(it.first, TimePair(it.second)) }

        return ScheduleEntry(CreateTaskViewModel.ScheduleData.Single(date, timePair))
    }

    sealed class Hint : Serializable {

        class Schedule(val date: Date, val timePair: TimePair) : Hint() {

            constructor(date: Date, pair: Pair<Date, HourMinute> = HourMinute.getNextHour(date)) : this(pair.first, TimePair(pair.second))

            constructor(date: Date, hourMinute: HourMinute) : this(date, TimePair(hourMinute))
        }

        class Task(val taskKey: TaskKey) : Hint()
    }

    @Suppress("PrivatePropertyName")
    private inner class CreateTaskAdapter : RecyclerView.Adapter<CreateTaskAdapter.Holder>() {

        private val TYPE_SCHEDULE = 0
        private val TYPE_NOTE = 1
        private val TYPE_IMAGE = 2

        private val mNameListener = object : TextWatcher {

            override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

            override fun afterTextChanged(s: Editable) {
                note = s.toString()
            }
        }

        init {
            checkNotNull(data)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = when (viewType) {
            TYPE_SCHEDULE -> ScheduleHolder(layoutInflater.inflate(R.layout.row_schedule, parent, false)!!)
            TYPE_NOTE -> NoteHolder(layoutInflater.inflate(R.layout.row_note, parent, false)!!)
            TYPE_IMAGE -> ImageHolder(layoutInflater.inflate(R.layout.row_image, parent, false)!!)
            else -> throw UnsupportedOperationException()
        }

        override fun onBindViewHolder(holder: Holder, position: Int) {
            val elementsBeforeSchedules = elementsBeforeSchedules()

            when (position) {
                in (0 until elementsBeforeSchedules) -> (holder as ScheduleHolder).run {
                    scheduleMargin.visibility = if (position == 0) View.VISIBLE else View.GONE

                    scheduleLayout.run {
                        hint = getString(R.string.parentTask)
                        error = null
                        isHintAnimationEnabled = false

                        addOneShotGlobalLayoutListener {
                            isHintAnimationEnabled = true
                        }
                    }

                    scheduleText.run {
                        setText(this@CreateTaskActivity.stateData.parent?.name)

                        setOnClickListener {
                            ParentPickerFragment.newInstance(this@CreateTaskActivity.stateData.parent != null).let {
                                it.show(supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                                it.initialize(data!!.parentTreeDatas, parentFragmentListener)
                            }
                        }

                        fixClicks()
                    }
                }
                in (elementsBeforeSchedules until (elementsBeforeSchedules + stateData.state.schedules.size)) -> (holder as ScheduleHolder).run {
                    val scheduleEntry = stateData.state.schedules[position - elementsBeforeSchedules()]

                    scheduleMargin.visibility = View.GONE

                    scheduleLayout.run {
                        hint = null
                        error = scheduleEntry.error
                        isHintAnimationEnabled = false
                    }

                    scheduleText.run {
                        setText(scheduleEntry.scheduleData.getText(data!!.customTimeDatas, this@CreateTaskActivity))

                        setOnClickListener { onTextClick() }

                        fixClicks()
                    }
                }
                elementsBeforeSchedules + stateData.state.schedules.size -> (holder as ScheduleHolder).run {
                    scheduleMargin.visibility = View.GONE

                    scheduleLayout.run {
                        hint = getString(R.string.addReminder)
                        error = null
                        isHintAnimationEnabled = false
                    }

                    scheduleText.run {
                        text = null

                        setOnClickListener {
                            check(hourMinutePickerPosition == null)

                            ScheduleDialogFragment.newInstance(firstScheduleEntry().scheduleData.getScheduleDialogData(Date.today(), (this@CreateTaskActivity.hint as? Hint.Schedule)), false).let {
                                it.initialize(data!!.customTimeDatas, scheduleDialogListener)
                                it.show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                            }
                        }

                        fixClicks()
                    }
                }
                elementsBeforeSchedules + stateData.state.schedules.size + 1 -> {
                    (holder as NoteHolder).run {
                        mNoteLayout.isHintAnimationEnabled = data != null

                        mNoteText.run {
                            setText(note)
                            removeTextChangedListener(mNameListener)
                            addTextChangedListener(mNameListener)
                            setOnFocusChangeListener { _, hasFocus ->
                                noteHasFocus = hasFocus

                                if (hasFocus)
                                    editToolbarAppBar.setExpanded(false)
                            }
                        }
                    }
                }
                elementsBeforeSchedules + stateData.state.schedules.size + 2 -> {
                    (holder as ImageHolder).run {
                        listOf(imageLayoutText, imageImage, imageEdit).forEach {
                            it.setOnClickListener {
                                CameraGalleryFragment.newInstance(imageUrl.value!!.loader != null).show(supportFragmentManager, TAG_CAMERA_GALLERY)
                            }
                        }

                        imageLayoutText.fixClicks()
                    }
                }
                else -> throw IllegalArgumentException()
            }
        }

        override fun onViewAttachedToWindow(holder: Holder) {
            super.onViewAttachedToWindow(holder)

            (holder as? ImageHolder)?.run {
                compositeDisposable += imageUrl.subscribe {
                    if (it.loader != null) {
                        imageProgress.visibility = View.VISIBLE
                        imageImage.visibility = View.VISIBLE
                        imageLayout.visibility = View.GONE
                        imageEdit.visibility = View.VISIBLE

                        it.loader!!(imageImage)
                    } else {
                        imageProgress.visibility = View.GONE
                        imageImage.visibility = View.GONE
                        imageLayout.visibility = View.VISIBLE
                        imageEdit.visibility = View.GONE
                    }
                }
            }
        }

        override fun onViewDetachedFromWindow(holder: Holder) {
            holder.compositeDisposable.clear()

            super.onViewDetachedFromWindow(holder)
        }

        fun elementsBeforeSchedules() = 1

        override fun getItemCount() = elementsBeforeSchedules() + stateData.state.schedules.size + 3

        override fun getItemViewType(position: Int): Int {
            val elementsBeforeSchedules = elementsBeforeSchedules()

            return when (position) {
                0 -> TYPE_SCHEDULE
                in (1 until elementsBeforeSchedules) -> TYPE_SCHEDULE
                in (elementsBeforeSchedules until elementsBeforeSchedules + stateData.state.schedules.size) -> TYPE_SCHEDULE
                elementsBeforeSchedules + stateData.state.schedules.size -> TYPE_SCHEDULE
                elementsBeforeSchedules + stateData.state.schedules.size + 1 -> TYPE_NOTE
                elementsBeforeSchedules + stateData.state.schedules.size + 2 -> TYPE_IMAGE
                else -> throw IllegalArgumentException()
            }
        }

        fun addScheduleEntry(scheduleEntry: ScheduleEntry) {
            val position = elementsBeforeSchedules() + stateData.state.schedules.size

            stateData.state.schedules.add(scheduleEntry)
            notifyItemInserted(position)
        }

        abstract inner class Holder(view: View) : RecyclerView.ViewHolder(view) {

            val compositeDisposable = CompositeDisposable()
        }

        inner class ScheduleHolder(scheduleRow: View) : Holder(scheduleRow) {

            val scheduleMargin = itemView.scheduleMargin!!
            val scheduleLayout = itemView.scheduleLayout!!
            val scheduleText = itemView.scheduleText!!

            fun onTextClick() {
                checkNotNull(data)
                check(hourMinutePickerPosition == null)

                hourMinutePickerPosition = adapterPosition

                val scheduleEntry = stateData.state.schedules[hourMinutePickerPosition!! - createTaskAdapter.elementsBeforeSchedules()]

                ScheduleDialogFragment.newInstance(scheduleEntry.scheduleData.getScheduleDialogData(Date.today(), hint as? Hint.Schedule), true).let {
                    it.initialize(data!!.customTimeDatas, scheduleDialogListener)
                    it.show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                }
            }
        }

        inner class NoteHolder(scheduleRow: View) : Holder(scheduleRow) {

            val mNoteLayout = itemView.noteLayout!!
            val mNoteText = itemView.noteText!!
        }

        inner class ImageHolder(itemView: View) : Holder(itemView) {

            val imageImage = itemView.imageImage!!
            val imageProgress = itemView.imageProgress!!
            val imageLayout = itemView.imageLayout!!
            val imageLayoutText = itemView.imageLayoutText!!
            val imageEdit = itemView.imageEdit!!
        }
    }

    @Parcelize
    class ParentScheduleState(
            var parentKey: CreateTaskViewModel.ParentKey?,
            val schedules: MutableList<ScheduleEntry> = mutableListOf()) : Parcelable {

        override fun hashCode() = (parentKey?.hashCode() ?: 0) * 32 + schedules.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other === this)
                return true

            if (other !is ParentScheduleState)
                return false

            fun ParentScheduleState.scheduleDatas() = schedules.map { it.scheduleData }

            return (parentKey == other.parentKey && scheduleDatas() == other.scheduleDatas())
        }
    }

    private inner class ParentScheduleData(val state: ParentScheduleState) {

        var parent by observable(state.parentKey?.let { findTaskData(it) }) { _, _, newValue ->
            state.parentKey = newValue?.parentKey

            if (newValue?.parentKey is CreateTaskViewModel.ParentKey.Task) {
                val scheduleCount = state.schedules.size

                state.schedules.clear()
                createTaskAdapter.notifyItemRangeRemoved(createTaskAdapter.elementsBeforeSchedules(), scheduleCount)
            }
        }
    }

    sealed class ImageState : Serializable {

        open val dontOverwrite = false

        open val loader: ((ImageView) -> Any)? = null

        open val writeImagePath: NullableWrapper<Pair<String, Uri>>? = null

        object None : ImageState()

        data class Existing(val imageState: com.krystianwsul.checkme.firebase.ImageState) : ImageState() {

            override val loader get() = imageState::load
        }

        object Removed : ImageState() {

            override val dontOverwrite = true

            override val writeImagePath = NullableWrapper<Pair<String, Uri>>(null)
        }

        data class Selected(val path: String, val uri: String) : ImageState() {

            override val dontOverwrite = true

            override val loader
                get() = { imageView: ImageView ->
                    Glide.with(imageView)
                            .load(path)
                            .into(imageView)
                }

            override val writeImagePath get() = NullableWrapper(Pair(path, Uri.parse(uri)))
        }
    }
}
