package com.krystianwsul.checkme.gui.tasks.create

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextUtils
import android.text.TextWatcher
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.core.view.isVisible
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputLayout
import com.jakewharton.rxrelay2.BehaviorRelay
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.ShortcutManager
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.gui.NavBarActivity
import com.krystianwsul.checkme.gui.tasks.*
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.setFixedOnClickListener
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.checkme.viewmodels.NullableWrapper
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.*
import com.miguelbcr.ui.rx_paparazzo2.entities.FileData
import com.miguelbcr.ui.rx_paparazzo2.entities.Response
import io.reactivex.BackpressureStrategy
import io.reactivex.Observable
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.disposables.CompositeDisposable
import io.reactivex.rxkotlin.addTo
import io.reactivex.rxkotlin.merge
import io.reactivex.rxkotlin.plusAssign
import kotlinx.android.parcel.Parcelize
import kotlinx.android.synthetic.main.activity_create_task.*
import kotlinx.android.synthetic.main.row_image.view.*
import kotlinx.android.synthetic.main.row_note.view.*
import kotlinx.android.synthetic.main.row_schedule.view.*
import kotlinx.android.synthetic.main.toolbar_edit_text.*
import kotlin.properties.Delegates.observable


class CreateTaskActivity : NavBarActivity() {

    companion object {

        private const val DISCARD_TAG = "discard"

        const val KEY_PARAMETERS = "parameters"
        private const val KEY_INITIAL_STATE = "initialState"

        const val KEY_PARENT_PROJECT_TYPE = "parentProjectType"
        const val KEY_PARENT_PROJECT_KEY = "parentProjectKey"
        const val KEY_PARENT_TASK = "parentTask"

        private const val PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment"

        private const val KEY_STATE = "state"
        private const val NOTE_KEY = "note"
        private const val NOTE_HAS_FOCUS_KEY = "noteHasFocus"
        private const val IMAGE_URL_KEY = "imageUrl"

        private const val SCHEDULE_DIALOG_TAG = "scheduleDialog"
        private const val TAG_CAMERA_GALLERY = "cameraGallery"

        private const val REQUEST_CREATE_PARENT = 982

        fun getCreateIntent(
                hint: Hint? = null,
                parentScheduleState: ParentScheduleState? = null,
                nameHint: String? = null
        ) = getParametersIntent(CreateTaskParameters.Create(hint, parentScheduleState, nameHint))

        fun getJoinIntent(
                joinTaskKeys: List<TaskKey>,
                hint: Hint? = null,
                removeInstanceKeys: List<InstanceKey> = listOf()
        ) = getParametersIntent(CreateTaskParameters.Join(joinTaskKeys, hint, removeInstanceKeys))

        fun getEditIntent(taskKey: TaskKey) = getParametersIntent(CreateTaskParameters.Edit(taskKey))

        fun getCopyIntent(taskKey: TaskKey) = getParametersIntent(CreateTaskParameters.Copy(taskKey))

        private fun getParametersIntent(createTaskParameters: CreateTaskParameters) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            putExtra(KEY_PARAMETERS, createTaskParameters)
        }

        fun getShortcutIntent(parentTaskKeyHint: TaskKey) = Intent(MyApplication.instance, CreateTaskActivity::class.java).apply {
            action = Intent.ACTION_DEFAULT

            putExtra(KEY_PARENT_PROJECT_KEY, parentTaskKeyHint.projectKey.key)
            putExtra(KEY_PARENT_PROJECT_TYPE, parentTaskKeyHint.projectKey.type.ordinal)
            putExtra(KEY_PARENT_TASK, parentTaskKeyHint.taskId)
        }

        var createdTaskKey: TaskKey? = null
    }

    private var savedInstanceState: Bundle? = null

    private val discardDialogListener = this::finish

    private lateinit var parameters: CreateTaskParameters
    private var taskKeys: List<TaskKey>? = null
    private lateinit var removeInstanceKeys: List<InstanceKey>
    private var taskKey: TaskKey? = null
    private var hint: Hint? = null
    private var tmpState: ParentScheduleState? = null

    private lateinit var delegate: Delegate

    private var data: CreateTaskViewModel.Data? = null

    private lateinit var createTaskAdapter: CreateTaskAdapter

    private lateinit var initialState: ParentScheduleState
    private lateinit var stateData: ParentScheduleData

    private val parentFragmentListener = object : ParentPickerFragment.Listener {

        override fun onTaskSelected(parentTreeData: CreateTaskViewModel.ParentTreeData) {
            stateData.parent = parentTreeData

            updateParentView()
        }

        override fun onTaskDeleted() = removeParent()

        override fun onNewParent(nameHint: String?) = startActivityForResult(
                getCreateIntent(
                        hint,
                        stateData.state.run {
                            ParentScheduleState(
                                    parentKey,
                                    schedules.map { ScheduleEntry(it.scheduleDataWrapper) }.toMutableList()
                            )
                        },
                        nameHint
                ),
                REQUEST_CREATE_PARENT
        )
    }

    private fun removeParent() {
        checkNotNull(stateData.parent)

        stateData.parent = null

        updateParentView()
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

    private var note: String? = null

    private var noteHasFocus = false // keyboard hack

    private val onChildAttachStateChangeListener = object : RecyclerView.OnChildAttachStateChangeListener { // keyboard hack

        override fun onChildViewAttachedToWindow(view: View) {
            view.noteText?.let {
                removeListenerHelper()

                it.requestFocus()

                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }
        }

        override fun onChildViewDetachedFromWindow(view: View) = Unit
    }

    private val scheduleDataWrappers
        get() = stateData.state
                .schedules
                .map { it.scheduleDataWrapper }
                .apply { check(!isEmpty()) }

    private val scheduleDatas get() = scheduleDataWrappers.map { it.scheduleData }

    private lateinit var createTaskViewModel: CreateTaskViewModel

    val imageUrl = BehaviorRelay.createDefault<CreateTaskImageState>(CreateTaskImageState.None)

    private val parametersRelay = PublishRelay.create<ScheduleDialogFragment.Parameters>()

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = data != null
        menu.findItem(R.id.action_save_and_open).isVisible = data != null && data!!.taskData == null

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        check(!hasValueParentTask() || !hasValueSchedule())

        fun save(andOpen: Boolean) {
            checkNotNull(data)
            checkNotNull(toolbarEditText)

            if (!updateError()) {
                val name = toolbarEditText.text.toString().trim { it <= ' ' }
                check(!TextUtils.isEmpty(name))

                createTaskViewModel.stop()

                val writeImagePath = imageUrl.value!!.writeImagePath

                val createParameters = Delegate.CreateParameters(
                        data!!.dataId,
                        name,
                        note,
                        writeImagePath
                )

                val projectId = (stateData.parent?.parentKey as? CreateTaskViewModel.ParentKey.Project)?.projectId

                val taskKey: TaskKey = when {
                    hasValueSchedule() -> {
                        check(!hasValueParentTask())

                        delegate.createTaskWithSchedule(createParameters, scheduleDatas, projectId)
                    }
                    hasValueParentTask() -> {
                        checkNotNull(stateData.parent)
                        check(projectId == null)
                        check(scheduleDatas.isEmpty())

                        val parentTaskKey = (stateData.parent!!.parentKey as CreateTaskViewModel.ParentKey.Task).taskKey

                        delegate.createTaskWithParent(createParameters, parentTaskKey)
                    }
                    else -> {  // no reminder
                        check(scheduleDatas.isEmpty())

                        delegate.createTaskWithoutReminder(createParameters, projectId)
                    }
                }

                if (andOpen)
                    startActivity(ShowTaskActivity.newIntent(taskKey))

                setResult(
                        Activity.RESULT_OK,
                        Intent().apply { putExtra(ShowTaskActivity.TASK_KEY_KEY, taskKey as Parcelable) }
                )

                finish()
            }
        }

        when (item.itemId) {
            R.id.action_save -> save(false)
            R.id.action_save_and_open -> save(true)
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

        savedInstanceState?.run {
            imageUrl.accept(getSerializable(IMAGE_URL_KEY) as CreateTaskImageState)
        }

        createTaskRecycler.layoutManager = LinearLayoutManager(this)

        parameters = CreateTaskParameters.fromIntent(intent)

        taskKeys = parameters.taskKeys // join
        removeInstanceKeys = parameters.removeInstanceKeys

        taskKey = parameters.taskKey // edit, copy

        hint = parameters.hint // create, join, shortcut

        if (savedInstanceState != null) {
            savedInstanceState.run {
                @Suppress("UNCHECKED_CAST")
                if (containsKey(KEY_INITIAL_STATE)) {
                    initialState = getParcelable(KEY_INITIAL_STATE)!!
                    tmpState = getParcelable(KEY_STATE)!!
                }
            }
        } else {
            tmpState = parameters.parentScheduleState // create
            if (tmpState != null)
                initialState = ParentScheduleState(tmpState!!.parentKey, ArrayList(tmpState!!.schedules))
        }

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener

        if (!noteHasFocus)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        createTaskViewModel = getViewModel<CreateTaskViewModel>().apply {
            start(taskKey, taskKeys, (hint as? Hint.Task)?.taskKey)

            createDisposable += data.subscribe { onLoadFinished(it) }
        }

        setupParent(createTaskRoot)

        listOfNotNull(
                parametersRelay.toFlowable(BackpressureStrategy.DROP).flatMapSingle(
                        {
                            ScheduleDialogFragment.newInstance(it).run {
                                initialize(data!!.customTimeDatas)
                                show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                                result.firstOrError()
                            }
                        },
                        false,
                        1
                ).toObservable(),
                (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.let { Observable.just(it) }?.flatMapSingle { it.result.firstOrError() }
        ).merge()
                .subscribe { result ->
                    when (result) {
                        is ScheduleDialogFragment.Result.Change -> {
                            checkNotNull(data)

                            if (result.scheduleDialogData.scheduleType == ScheduleType.MONTHLY_DAY) {
                                check(result.scheduleDialogData.monthlyDay)
                            } else if (result.scheduleDialogData.scheduleType == ScheduleType.MONTHLY_WEEK) {
                                check(!result.scheduleDialogData.monthlyDay)
                            }

                            if (result.position == null) {
                                clearParentTask()

                                createTaskAdapter.addScheduleEntry(result.scheduleDialogData.toScheduleEntry())
                            } else {
                                check(result.position >= 1)

                                val position = result.position - 1

                                val oldId = if (position < stateData.state.schedules.size) {
                                    stateData.state
                                            .schedules[position]
                                            .id
                                } else {
                                    null
                                }

                                stateData.state.schedules[position] = result.scheduleDialogData.toScheduleEntry(oldId)

                                createTaskAdapter.updateSchedules()
                            }
                        }
                        is ScheduleDialogFragment.Result.Delete -> removeSchedule(result.position)
                        is ScheduleDialogFragment.Result.Cancel -> Unit
                    }
                }
                .addTo(createDisposable)
    }

    private fun removeSchedule(position: Int) {
        check(position >= 1)
        checkNotNull(data)

        stateData.state
                .schedules
                .removeAt(position - 1)

        createTaskAdapter.updateSchedules()
    }

    @SuppressLint("CheckResult")
    fun getImage(single: Observable<Response<CreateTaskActivity, FileData>>) {
        single.observeOn(AndroidSchedulers.mainThread()).subscribe {
            if (it.resultCode() == Activity.RESULT_OK) {
                val file = it.data().file
                it.targetUI()
                        .imageUrl
                        .accept(CreateTaskImageState.Selected(file.absolutePath, file.toURI().toString()))
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (data != null) {
                putParcelable(KEY_STATE, stateData.state)
                putParcelable(KEY_INITIAL_STATE, initialState)

                if (!note.isNullOrEmpty())
                    putString(NOTE_KEY, note)

                putBoolean(NOTE_HAS_FOCUS_KEY, noteHasFocus)
            }

            putSerializable(IMAGE_URL_KEY, imageUrl.value!!)
        }
    }

    private fun onLoadFinished(data: CreateTaskViewModel.Data) {
        this.data = data

        if (!this::delegate.isInitialized)
            delegate = Delegate.fromParameters(parameters, data)
        else
            delegate.data = data

        data.taskData
                ?.imageState
                ?.takeUnless { imageUrl.value!!.dontOverwrite }
                ?.let { imageUrl.accept(CreateTaskImageState.Existing(it)) }

        toolbarLayout.run {
            visibility = View.VISIBLE
            isHintAnimationEnabled = true
        }

        toolbarEditText.run {
            if (savedInstanceState == null)
                setText(delegate.initialName)

            addTextChangedListener(object : TextWatcher {

                private var skip = savedInstanceState != null

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable) {
                    if (skip) {
                        skip = false
                        return
                    }

                    updateError()
                }
            })
        }

        val parentHint = (hint as? Hint.Task)?.taskKey
        val parentKey: CreateTaskViewModel.ParentKey?

        if (savedInstanceState?.containsKey(KEY_STATE) == true) {
            savedInstanceState!!.run {
                if (containsKey(NOTE_KEY)) {
                    note = getString(NOTE_KEY)!!
                    check(!note.isNullOrEmpty())
                }

                check(containsKey(NOTE_HAS_FOCUS_KEY))

                noteHasFocus = getBoolean(NOTE_HAS_FOCUS_KEY)
            }

            parentKey = null
        } else {
            data.run {
                parentKey = when {
                    taskData?.parentKey != null -> {
                        check(parentHint == null)
                        check(taskKeys == null)
                        checkNotNull(taskKey)

                        taskData.parentKey
                    }
                    parentHint != null -> {
                        check(taskKey == null)

                        CreateTaskViewModel.ParentKey.Task(parentHint)
                    }
                    else -> {
                        taskKeys?.map { it.projectKey }
                                ?.distinct()
                                ?.singleOrNull()
                                ?.let {
                                    (it as? ProjectKey.Shared)?.let { CreateTaskViewModel.ParentKey.Project(it) }
                                }
                    }
                }

                taskData?.let { note = it.note }
            }
        }

        (supportFragmentManager.findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG) as? ParentPickerFragment)?.initialize(data.parentTreeDatas, parentFragmentListener)

        invalidateOptionsMenu()

        if (!this::initialState.isInitialized) {
            check(!this::stateData.isInitialized)

            val schedules = mutableListOf<ScheduleEntry>()

            data.run {
                if (taskData != null) {
                    if (taskData.scheduleDataWrappers != null) {
                        check(taskData.scheduleDataWrappers.isNotEmpty())

                        schedules.addAll(taskData.scheduleDataWrappers
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

        (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.initialize(data.customTimeDatas)

        createTaskAdapter = CreateTaskAdapter(stateData.state.schedules)
        createTaskRecycler.adapter = createTaskAdapter
        createTaskRecycler.itemAnimator = CustomItemAnimator()

        if (noteHasFocus) { // keyboard hack
            val notePosition = createTaskAdapter.items.indexOf(Item.Note)

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
        val data = delegate.data

        var hasError = false

        if (TextUtils.isEmpty(toolbarEditText.text)) {
            toolbarLayout.error = getString(R.string.nameError)

            hasError = true
        } else {
            toolbarLayout.error = null
        }

        for (scheduleEntry in stateData.state.schedules) {
            if (scheduleEntry.scheduleDataWrapper !is CreateTaskViewModel.ScheduleDataWrapper.Single)
                continue

            if (delegate.skipScheduleCheck(scheduleEntry, stateData.state.parentKey))
                continue

            val date = scheduleEntry.scheduleDataWrapper
                    .scheduleData
                    .date

            if (date > Date.today())
                continue

            if (date < Date.today()) {
                setScheduleEntryError(scheduleEntry, R.string.error_date)

                hasError = true
                continue
            }

            val hourMinute = scheduleEntry.scheduleDataWrapper
                    .timePair
                    .run {
                        customTimeKey?.let { data.customTimeDatas.getValue(it) }
                                ?.hourMinutes
                                ?.getValue(date.dayOfWeek)
                                ?: hourMinute!!
                    }

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

        createTaskRecycler.getChildAt(index + 1)?.let {
            (createTaskRecycler.getChildViewHolder(it) as ScheduleHolder).scheduleLayout.error = scheduleEntry.error
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

    private fun clearParentTask() {
        if (stateData.parent?.parentKey !is CreateTaskViewModel.ParentKey.Task)
            return

        stateData.parent = null

        updateParentView()
    }

    private fun updateParentView() {
        val view = createTaskRecycler.getChildAt(0) ?: return

        val scheduleHolder = createTaskRecycler.getChildViewHolder(view) as ScheduleHolder
        updateParentView(scheduleHolder)
    }

    private fun updateParentView(scheduleHolder: ScheduleHolder) {
        scheduleHolder.apply {
            scheduleLayout.endIconMode = if (stateData.parent != null)
                TextInputLayout.END_ICON_CLEAR_TEXT
            else
                TextInputLayout.END_ICON_DROPDOWN_MENU

            scheduleText.run {
                setText(stateData.parent?.name)

                setFixedOnClickListener {
                    ParentPickerFragment.newInstance(stateData.parent != null).let {
                        it.show(supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                        it.initialize(data!!.parentTreeDatas, parentFragmentListener)
                    }
                }
            }

            if (stateData.parent != null)
                scheduleLayout.setEndIconOnClickListener { removeParent() }
        }
    }

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
                stateData.parent = delegate.findTaskData(CreateTaskViewModel.ParentKey.Task(taskKey))
            }
        }
    }

    private fun hintToSchedule(scheduleHint: Hint.Schedule?): ScheduleEntry {
        val (date, timePair) = scheduleHint?.let { Pair(it.date, it.timePair) }
                ?: HourMinute.nextHour.let { Pair(it.first, TimePair(it.second)) }

        return ScheduleEntry(CreateTaskViewModel.ScheduleDataWrapper.Single(ScheduleData.Single(date, timePair)))
    }

    sealed class Hint : Parcelable {

        @Parcelize
        class Schedule(val date: Date, val timePair: TimePair) : Hint() {

            constructor(
                    date: Date,
                    pair: Pair<Date, HourMinute> = HourMinute.getNextHour(date)
            ) : this(pair.first, TimePair(pair.second))
        }

        @Parcelize
        class Task(val taskKey: TaskKey) : Hint()
    }

    private enum class HolderType {

        SCHEDULE {

            override val layout = R.layout.row_schedule

            override fun newHolder(view: View) = ScheduleHolder(view)
        },

        NOTE {

            override val layout = R.layout.row_note

            override fun newHolder(view: View) = NoteHolder(view)
        },

        IMAGE {

            override val layout = R.layout.row_image

            override fun newHolder(view: View) = ImageHolder(view)
        };

        abstract val layout: Int

        abstract fun newHolder(view: View): Holder
    }

    @Suppress("PrivatePropertyName")
    private inner class CreateTaskAdapter(scheduleEntries: List<ScheduleEntry>) : RecyclerView.Adapter<Holder>() {

        private fun getItems(scheduleEntries: List<ScheduleEntry>) = listOf(Item.Parent) +
                scheduleEntries.map { Item.Schedule(it) } +
                Item.NewSchedule +
                Item.Note +
                Item.Image

        var items by observable(getItems(scheduleEntries)) { _, oldItems, newItems ->
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {

                override fun getOldListSize() = oldItems.size

                override fun getNewListSize() = newItems.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        oldItems[oldItemPosition].same(newItems[newItemPosition])

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                        oldItems[oldItemPosition] == newItems[newItemPosition]
            }).dispatchUpdatesTo(this)
        }
            private set

        init {
            checkNotNull(data)
        }

        private fun setSchedules(scheduleEntries: List<ScheduleEntry>) {
            items = getItems(scheduleEntries)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) = HolderType.values()[viewType].run {
            newHolder(layoutInflater.inflate(layout, parent, false))
        }

        override fun onBindViewHolder(holder: Holder, position: Int) =
                items[position].bind(this@CreateTaskActivity, holder)

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

        override fun getItemCount() = items.size

        override fun getItemViewType(position: Int) = items[position].holderType.ordinal

        fun addScheduleEntry(scheduleEntry: ScheduleEntry) {
            stateData.state.schedules += scheduleEntry
            updateSchedules()
        }

        fun updateSchedules() = setSchedules(stateData.state.schedules)
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        val compositeDisposable = CompositeDisposable()
    }

    class ScheduleHolder(scheduleRow: View) : Holder(scheduleRow) {

        val scheduleMargin = itemView.scheduleMargin!!
        val scheduleLayout = itemView.scheduleLayout!!
        val scheduleText = itemView.scheduleText!!
    }

    class NoteHolder(scheduleRow: View) : Holder(scheduleRow) {

        val noteLayout = itemView.noteLayout!!
        val noteText = itemView.noteText!!
    }

    class ImageHolder(itemView: View) : Holder(itemView) {

        val imageImage = itemView.imageImage!!
        val imageProgress = itemView.imageProgress!!
        val imageLayout = itemView.imageLayout!!
        val imageLayoutText = itemView.imageLayoutText!!
        val imageEdit = itemView.imageEdit!!
    }

    @Parcelize
    class ParentScheduleState(
            var parentKey: CreateTaskViewModel.ParentKey?,
            val schedules: MutableList<ScheduleEntry> = mutableListOf()
    ) : Parcelable {

        override fun hashCode() = (parentKey?.hashCode() ?: 0) * 32 + schedules.hashCode()

        override fun equals(other: Any?): Boolean {
            if (other === this)
                return true

            if (other !is ParentScheduleState)
                return false

            fun ParentScheduleState.scheduleDatas() = schedules.map { it.scheduleDataWrapper }

            return (parentKey == other.parentKey && scheduleDatas() == other.scheduleDatas())
        }
    }

    private inner class ParentScheduleData(val state: ParentScheduleState) {

        var parent by observable(state.parentKey?.let { delegate.findTaskData(it) }) { _, _, newValue ->
            state.parentKey = newValue?.parentKey

            if (newValue?.parentKey is CreateTaskViewModel.ParentKey.Task) {
                state.schedules.clear()
                createTaskAdapter.updateSchedules()
            }
        }
    }

    private sealed class Item {

        abstract val holderType: HolderType

        abstract fun bind(activity: CreateTaskActivity, holder: Holder)

        open fun same(other: Item) = other == this

        object Parent : Item() {

            override val holderType get() = HolderType.SCHEDULE

            override fun bind(activity: CreateTaskActivity, holder: Holder) {
                (holder as ScheduleHolder).apply {
                    scheduleMargin.isVisible = true

                    scheduleLayout.run {
                        hint = activity.getString(R.string.parentTask)
                        error = null
                        isHintAnimationEnabled = false

                        addOneShotGlobalLayoutListener {
                            isHintAnimationEnabled = true
                        }
                    }

                    activity.updateParentView(this)
                }
            }
        }

        data class Schedule(private val scheduleEntry: ScheduleEntry) : Item() {

            override val holderType = HolderType.SCHEDULE

            override fun bind(activity: CreateTaskActivity, holder: Holder) {
                (holder as ScheduleHolder).apply {
                    scheduleMargin.visibility = View.GONE

                    scheduleLayout.run {
                        hint = null
                        error = scheduleEntry.error
                        isHintAnimationEnabled = false
                        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    }

                    scheduleText.run {
                        setText(scheduleEntry.scheduleDataWrapper.getText(activity.data!!.customTimeDatas, activity))

                        setFixedOnClickListener(
                                {
                                    checkNotNull(activity.data)

                                    val parameters = ScheduleDialogFragment.Parameters(
                                            adapterPosition,
                                            scheduleEntry.scheduleDataWrapper.getScheduleDialogData(Date.today(), activity.hint as? Hint.Schedule),
                                            true)

                                    activity.parametersRelay.accept(parameters)
                                },
                                { activity.removeSchedule(adapterPosition) }
                        )
                    }
                }
            }

            override fun same(other: Item): Boolean {
                if (other !is Schedule)
                    return false

                if (scheduleEntry.id == other.scheduleEntry.id)
                    return true

                return scheduleEntry.scheduleDataWrapper === other.scheduleEntry.scheduleDataWrapper
            }
        }

        object NewSchedule : Item() {

            override val holderType = HolderType.SCHEDULE

            override fun bind(activity: CreateTaskActivity, holder: Holder) {
                (holder as ScheduleHolder).apply {
                    scheduleMargin.visibility = View.GONE

                    scheduleLayout.run {
                        hint = activity.getString(R.string.addReminder)
                        error = null
                        isHintAnimationEnabled = false
                        endIconMode = TextInputLayout.END_ICON_DROPDOWN_MENU
                    }

                    scheduleText.run {
                        text = null

                        setFixedOnClickListener {
                            val parameters = ScheduleDialogFragment.Parameters(
                                    null,
                                    activity.firstScheduleEntry()
                                            .scheduleDataWrapper
                                            .getScheduleDialogData(
                                                    Date.today(),
                                                    activity.hint as? Hint.Schedule
                                            ),
                                    false
                            )

                            activity.parametersRelay.accept(parameters)
                        }
                    }
                }
            }
        }

        object Note : Item() {

            override val holderType = HolderType.NOTE

            override fun bind(activity: CreateTaskActivity, holder: Holder) {
                (holder as NoteHolder).apply {
                    noteLayout.isHintAnimationEnabled = activity.data != null

                    val nameListener = object : TextWatcher {

                        override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                        override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                        override fun afterTextChanged(s: Editable) {
                            activity.note = s.toString()
                        }
                    }

                    noteText.run {
                        setText(activity.note)
                        removeTextChangedListener(nameListener)
                        addTextChangedListener(nameListener)
                        setOnFocusChangeListener { _, hasFocus ->
                            activity.noteHasFocus = hasFocus

                            if (hasFocus)
                                activity.editToolbarAppBar.setExpanded(false)
                        }
                    }
                }
            }
        }

        object Image : Item() {

            override val holderType = HolderType.IMAGE

            override fun bind(activity: CreateTaskActivity, holder: Holder) {
                (holder as ImageHolder).apply {
                    fun listener() = CameraGalleryFragment.newInstance(activity.imageUrl.value!!.loader != null).show(activity.supportFragmentManager, TAG_CAMERA_GALLERY)

                    imageImage.setOnClickListener { listener() }
                    imageEdit.setOnClickListener { listener() }
                    imageLayoutText.setFixedOnClickListener(::listener)
                }
            }
        }
    }

    private abstract class Delegate {

        companion object {

            fun fromParameters(parameters: CreateTaskParameters, data: CreateTaskViewModel.Data): Delegate {
                return when (parameters) {
                    is CreateTaskParameters.Copy -> CopyDelegate(parameters, data)
                    is CreateTaskParameters.Edit -> EditDelegate(parameters, data)
                    is CreateTaskParameters.Join -> JoinDelegate(parameters, data)
                    is CreateTaskParameters.Create,
                    is CreateTaskParameters.Share,
                    is CreateTaskParameters.Shortcut,
                    CreateTaskParameters.None -> CreateDelegate(parameters, data)
                }
            }
        }

        abstract var data: CreateTaskViewModel.Data

        open val initialName: String? = null

        open fun skipScheduleCheck(
                scheduleEntry: ScheduleEntry,
                parentKey: CreateTaskViewModel.ParentKey?
        ): Boolean = false

        fun findTaskData(parentKey: CreateTaskViewModel.ParentKey) =
                findTaskDataHelper(data.parentTreeDatas, parentKey).single()

        private fun findTaskDataHelper(
                taskDatas: Map<CreateTaskViewModel.ParentKey, CreateTaskViewModel.ParentTreeData>,
                parentKey: CreateTaskViewModel.ParentKey
        ): Iterable<CreateTaskViewModel.ParentTreeData> {
            if (taskDatas.containsKey(parentKey))
                return listOf(taskDatas.getValue(parentKey))

            return taskDatas.values
                    .map { findTaskDataHelper(it.parentTreeDatas, parentKey) }
                    .flatten()
        }

        abstract fun createTaskWithSchedule(
                createParameters: CreateParameters,
                scheduleDatas: List<ScheduleData>,
                projectKey: ProjectKey.Shared?
        ): TaskKey

        abstract fun createTaskWithParent(
                createParameters: CreateParameters,
                parentTaskKey: TaskKey
        ): TaskKey

        abstract fun createTaskWithoutReminder(
                createParameters: CreateParameters,
                projectKey: ProjectKey.Shared?
        ): TaskKey

        class CreateParameters(
                val dataId: Int,
                val name: String,
                val note: String?,
                val writeImagePath: NullableWrapper<Pair<String, Uri>>?
        )
    }

    private class CopyDelegate(
            private val parameters: CreateTaskParameters.Copy,
            override var data: CreateTaskViewModel.Data
    ) : Delegate() {

        private val taskData get() = data.taskData!!

        override val initialName get() = taskData.name

        override fun createTaskWithSchedule(
                createParameters: CreateParameters,
                scheduleDatas: List<ScheduleData>,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance
                    .createScheduleRootTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            createParameters.name,
                            scheduleDatas,
                            createParameters.note,
                            projectKey,
                            createParameters.writeImagePath?.value,
                            parameters.taskKey
                    )
                    .also { createdTaskKey = it }
        }

        override fun createTaskWithParent(
                createParameters: CreateParameters,
                parentTaskKey: TaskKey
        ): TaskKey {
            return DomainFactory.instance
                    .createChildTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            parentTaskKey,
                            createParameters.name,
                            createParameters.note,
                            createParameters.writeImagePath?.value,
                            parameters.taskKey
                    )
                    .also { createdTaskKey = it }
        }

        override fun createTaskWithoutReminder(
                createParameters: CreateParameters,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance
                    .createRootTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            createParameters.name,
                            createParameters.note,
                            projectKey,
                            createParameters.writeImagePath?.value,
                            parameters.taskKey
                    )
                    .also { createdTaskKey = it }
        }
    }

    private class EditDelegate(
            private val parameters: CreateTaskParameters.Edit,
            override var data: CreateTaskViewModel.Data
    ) : Delegate() {

        private val taskData get() = data.taskData!!

        override val initialName get() = taskData.name

        override fun skipScheduleCheck(scheduleEntry: ScheduleEntry, parentKey: CreateTaskViewModel.ParentKey?): Boolean {
            if (taskData.scheduleDataWrappers?.contains(scheduleEntry.scheduleDataWrapper) == true) {
                if (taskData.parentKey == parentKey) {
                    return true
                } else {
                    fun CreateTaskViewModel.ParentKey.getProjectId() = when (this) {
                        is CreateTaskViewModel.ParentKey.Project -> projectId
                        is CreateTaskViewModel.ParentKey.Task -> findTaskData(this).projectId
                    }

                    val initialProject = taskData.parentKey?.getProjectId()

                    val finalProject = parentKey?.getProjectId()

                    if (initialProject == finalProject)
                        return true
                }
            }

            return false
        }

        override fun createTaskWithSchedule(
                createParameters: CreateParameters,
                scheduleDatas: List<ScheduleData>,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance.updateScheduleTask(
                    createParameters.dataId,
                    SaveService.Source.GUI,
                    parameters.taskKey,
                    createParameters.name,
                    scheduleDatas,
                    createParameters.note,
                    projectKey,
                    createParameters.writeImagePath
            )
        }

        override fun createTaskWithParent(
                createParameters: CreateParameters,
                parentTaskKey: TaskKey
        ): TaskKey {
            return DomainFactory.instance.updateChildTask(
                    ExactTimeStamp.now,
                    createParameters.dataId,
                    SaveService.Source.GUI,
                    parameters.taskKey,
                    createParameters.name,
                    parentTaskKey,
                    createParameters.note,
                    createParameters.writeImagePath
            )
        }

        override fun createTaskWithoutReminder(
                createParameters: CreateParameters,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance.updateRootTask(
                    createParameters.dataId,
                    SaveService.Source.GUI,
                    parameters.taskKey,
                    createParameters.name,
                    createParameters.note,
                    projectKey,
                    createParameters.writeImagePath
            )
        }
    }

    private class JoinDelegate(
            private val parameters: CreateTaskParameters.Join,
            override var data: CreateTaskViewModel.Data
    ) : Delegate() {

        override fun createTaskWithSchedule(
                createParameters: CreateParameters,
                scheduleDatas: List<ScheduleData>,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance
                    .createScheduleJoinRootTask(
                            ExactTimeStamp.now,
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            createParameters.name,
                            scheduleDatas,
                            parameters.taskKeys,
                            createParameters.note,
                            projectKey,
                            createParameters.writeImagePath?.value,
                            parameters.removeInstanceKeys
                    )
                    .also { createdTaskKey = it }
        }

        override fun createTaskWithParent(
                createParameters: CreateParameters,
                parentTaskKey: TaskKey
        ): TaskKey {
            return DomainFactory.instance
                    .createJoinChildTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            parentTaskKey,
                            createParameters.name,
                            parameters.taskKeys,
                            createParameters.note,
                            createParameters.writeImagePath?.value,
                            parameters.removeInstanceKeys
                    )
                    .also { createdTaskKey = it }
        }

        override fun createTaskWithoutReminder(
                createParameters: CreateParameters,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance
                    .createJoinRootTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            createParameters.name,
                            parameters.taskKeys,
                            createParameters.note,
                            projectKey,
                            createParameters.writeImagePath?.value,
                            parameters.removeInstanceKeys
                    )
                    .also { createdTaskKey = it }
        }
    }

    private class CreateDelegate(
            private val parameters: CreateTaskParameters,
            override var data: CreateTaskViewModel.Data
    ) : Delegate() {

        override val initialName: String?

        init {
            when (parameters) {
                is CreateTaskParameters.Create -> {
                    initialName = parameters.nameHint
                }
                is CreateTaskParameters.Share -> {
                    initialName = parameters.nameHint
                }
                is CreateTaskParameters.Shortcut -> {
                    initialName = null
                }
                CreateTaskParameters.None -> {
                    initialName = null
                }
                else -> throw IllegalArgumentException()
            }
        }

        override fun createTaskWithSchedule(
                createParameters: CreateParameters,
                scheduleDatas: List<ScheduleData>,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance
                    .createScheduleRootTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            createParameters.name,
                            scheduleDatas,
                            createParameters.note,
                            projectKey,
                            createParameters.writeImagePath?.value
                    )
                    .also { createdTaskKey = it }
        }

        override fun createTaskWithParent(
                createParameters: CreateParameters,
                parentTaskKey: TaskKey
        ): TaskKey {
            if (parameters.fromSendIntent)
                ShortcutManager.addShortcut(parentTaskKey)

            return DomainFactory.instance
                    .createChildTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            parentTaskKey,
                            createParameters.name,
                            createParameters.note,
                            createParameters.writeImagePath?.value
                    )
                    .also { createdTaskKey = it }
        }

        override fun createTaskWithoutReminder(
                createParameters: CreateParameters,
                projectKey: ProjectKey.Shared?
        ): TaskKey {
            return DomainFactory.instance
                    .createRootTask(
                            createParameters.dataId,
                            SaveService.Source.GUI,
                            createParameters.name,
                            createParameters.note,
                            projectKey,
                            createParameters.writeImagePath?.value
                    )
                    .also { createdTaskKey = it }
        }
    }
}
