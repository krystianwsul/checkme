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

    private lateinit var parameters: CreateTaskParameters

    private lateinit var createTaskAdapter: CreateTaskAdapter

    private var data: CreateTaskViewModel.Data? = null
    private lateinit var delegate: Delegate

    private val discardDialogListener = this::finish

    private val parentFragmentListener = object : ParentPickerFragment.Listener {

        override fun onTaskSelected(parentTreeData: CreateTaskViewModel.ParentTreeData) {
            delegate.stateData.parent = parentTreeData
        }

        override fun onTaskDeleted() = removeParent()

        override fun onNewParent(nameHint: String?) = startActivityForResult(
                getCreateIntent(
                        null,
                        delegate.stateData.run {
                            ParentScheduleState(
                                    parent?.parentKey,
                                    schedules.map { ScheduleEntry(it.scheduleDataWrapper) }.toMutableList()
                            )
                        },
                        nameHint
                ),
                REQUEST_CREATE_PARENT
        )
    }

    private fun removeParent() {
        checkNotNull(delegate.stateData.parent)

        delegate.stateData.parent = null
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

                val taskKey = delegate.createTask(createParameters, delegate.stateData) // todo create move into delegate

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

        (supportFragmentManager.findFragmentByTag(DISCARD_TAG) as? DiscardDialogFragment)?.discardDialogListener = discardDialogListener

        if (!noteHasFocus)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        createTaskViewModel = getViewModel<CreateTaskViewModel>().apply {
            parameters.startViewModel(this)

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
                                delegate.stateData.addSchedule(result.scheduleDialogData.toScheduleEntry())
                            } else {
                                check(result.position >= 1)

                                val position = result.position - 1

                                val oldId = if (position < delegate.stateData.schedules.size) {
                                    delegate.stateData
                                            .schedules[position]
                                            .id
                                } else {
                                    null
                                }

                                delegate.stateData.setSchedule(position, result.scheduleDialogData.toScheduleEntry(oldId))
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

        delegate.stateData.removeSchedule(position - 1)
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
                delegate.stateData.saveState(this)
                putParcelable(KEY_INITIAL_STATE, delegate.initialState)

                if (!note.isNullOrEmpty())
                    putString(NOTE_KEY, note)

                putBoolean(NOTE_HAS_FOCUS_KEY, noteHasFocus)
            }

            putSerializable(IMAGE_URL_KEY, imageUrl.value!!)
        }
    }

    private val loadFinishedDisposable = CompositeDisposable().also { createDisposable += it }

    private fun onLoadFinished(data: CreateTaskViewModel.Data) {
        loadFinishedDisposable.clear()

        this.data = data

        if (!this::delegate.isInitialized) {
            delegate = Delegate.fromParameters(
                    parameters,
                    data,
                    savedInstanceState?.takeIf { it.containsKey(KEY_INITIAL_STATE) }?.run {
                        Pair<ParentScheduleState, ParentScheduleState>(
                                getParcelable(KEY_INITIAL_STATE)!!,
                                getParcelable(KEY_STATE)!!
                        )
                    }
            )
        } else {
            delegate.data = data
        }

        delegate.stateData
                .parentObservable
                .subscribe {
                    createTaskRecycler.getChildAt(0)?.let { view ->
                        val scheduleHolder = createTaskRecycler.getChildViewHolder(view) as ScheduleHolder // todo create use payloads
                        updateParentView(scheduleHolder)
                    }
                }
                .addTo(loadFinishedDisposable)

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

        if (savedInstanceState?.containsKey(KEY_STATE) == true) {
            savedInstanceState!!.run {
                if (containsKey(NOTE_KEY)) {
                    note = getString(NOTE_KEY)!!
                    check(!note.isNullOrEmpty())
                }

                check(containsKey(NOTE_HAS_FOCUS_KEY))

                noteHasFocus = getBoolean(NOTE_HAS_FOCUS_KEY)
            }
        }

        (supportFragmentManager.findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG) as? ParentPickerFragment)?.initialize(data.parentTreeDatas, parentFragmentListener)

        invalidateOptionsMenu()

        (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.initialize(data.customTimeDatas)

        createTaskAdapter = CreateTaskAdapter()
        createTaskRecycler.adapter = createTaskAdapter
        createTaskRecycler.itemAnimator = CustomItemAnimator()

        delegate.stateData
                .scheduleObservable
                .subscribe { createTaskAdapter.setSchedules(it) }
                .addTo(loadFinishedDisposable)

        if (noteHasFocus) { // keyboard hack
            val notePosition = createTaskAdapter.items.indexOf(Item.Note)

            createTaskRecycler.addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)

            (createTaskRecycler.layoutManager as LinearLayoutManager).scrollToPosition(notePosition)
        }
    }

    override fun onBackPressed() {
        if (tryClose())
            super.onBackPressed()
    }

    private fun tryClose(): Boolean {
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

        for (scheduleEntry in delegate.stateData.schedules) {
            if (scheduleEntry.scheduleDataWrapper !is CreateTaskViewModel.ScheduleDataWrapper.Single)
                continue

            if (delegate.skipScheduleCheck(scheduleEntry, delegate.stateData.parent?.parentKey)) // todo create move into delegate
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

        val index = delegate.stateData
                .schedules
                .indexOf(scheduleEntry)
        check(index >= 0)

        createTaskRecycler.getChildAt(index + 1)?.let {
            (createTaskRecycler.getChildViewHolder(it) as ScheduleHolder).scheduleLayout.error = scheduleEntry.error // todo create use payloads
        }
    }

    private fun dataChanged(): Boolean {
        if (data == null)
            return false

        if (!delegate.stateData.equalTo(delegate.initialState)) // todo create move into delegate
            return true

        return delegate.checkDataChanged(toolbarEditText.text.toString(), note)
    }

    private fun updateParentView(scheduleHolder: ScheduleHolder) {
        scheduleHolder.apply {
            scheduleLayout.endIconMode = if (delegate.stateData.parent != null)
                TextInputLayout.END_ICON_CLEAR_TEXT
            else
                TextInputLayout.END_ICON_DROPDOWN_MENU

            scheduleText.run {
                setText(delegate.stateData.parent?.name)

                setFixedOnClickListener {
                    ParentPickerFragment.newInstance(delegate.stateData.parent != null).let {
                        it.show(supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                        it.initialize(data!!.parentTreeDatas, parentFragmentListener)
                    }
                }
            }

            if (delegate.stateData.parent != null)
                scheduleLayout.setEndIconOnClickListener { removeParent() }
        }
    }

    private fun removeListenerHelper() { // keyboard hack
        checkNotNull(createTaskRecycler)

        createTaskRecycler.removeOnChildAttachStateChangeListener(onChildAttachStateChangeListener)
    }

    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CREATE_PARENT) {
            if (resultCode == Activity.RESULT_OK) {
                val taskKey = data!!.getParcelableExtra<TaskKey>(ShowTaskActivity.TASK_KEY_KEY)!!
                delegate.stateData.parent = delegate.findTaskData(CreateTaskViewModel.ParentKey.Task(taskKey))
            }
        }
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
    private inner class CreateTaskAdapter : RecyclerView.Adapter<Holder>() {

        private fun getItems(scheduleEntries: List<ScheduleEntry>) = listOf(Item.Parent) +
                scheduleEntries.map { Item.Schedule(it) } +
                Item.NewSchedule +
                Item.Note +
                Item.Image

        var items by observable(getItems(listOf())) { _, oldItems, newItems ->
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

        fun setSchedules(scheduleEntries: List<ScheduleEntry>) {
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
            val schedules: MutableList<ScheduleEntry>,
            @Suppress("unused")
            val bogus: Unit
    ) : Parcelable {

        constructor(
                parentKey: CreateTaskViewModel.ParentKey?,
                schedules: List<ScheduleEntry>? = null
        ) : this(parentKey, schedules.orEmpty().toMutableList(), Unit)

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

    private class ParentScheduleData(
            private val state: ParentScheduleState,
            initialParent: CreateTaskViewModel.ParentTreeData?
    ) { // todo create move into delegate

        private val parentRelay = BehaviorRelay.createDefault(NullableWrapper(initialParent))

        val parentObservable: Observable<NullableWrapper<CreateTaskViewModel.ParentTreeData>> = parentRelay

        var parent
            get() = parentRelay.value!!.value
            set(newValue) {
                state.parentKey = newValue?.parentKey

                if (newValue?.parentKey is CreateTaskViewModel.ParentKey.Task) {
                    state.schedules.clear()

                    scheduleUpdates.accept(Unit)
                }

                parentRelay.accept(NullableWrapper(newValue))
            }

        private val scheduleUpdates = BehaviorRelay.createDefault(Unit)

        val schedules: List<ScheduleEntry> get() = state.schedules

        val scheduleObservable = scheduleUpdates.map { schedules }!!

        fun setSchedule(position: Int, scheduleEntry: ScheduleEntry) {
            state.schedules[position] = scheduleEntry

            scheduleUpdates.accept(Unit)
        }

        fun removeSchedule(position: Int) {
            state.schedules.removeAt(position)

            scheduleUpdates.accept(Unit)
        }

        fun addSchedule(scheduleEntry: ScheduleEntry) {
            if (parent?.parentKey is CreateTaskViewModel.ParentKey.Task)
                parent = null

            state.schedules += scheduleEntry

            scheduleUpdates.accept(Unit)
        }

        fun equalTo(parentScheduleState: ParentScheduleState) = state == parentScheduleState

        fun saveState(outState: Bundle) = outState.putParcelable(KEY_STATE, state) // todo create unify in delegate
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
                                            scheduleEntry.scheduleDataWrapper.getScheduleDialogData(
                                                    Date.today(),
                                                    activity.delegate.scheduleHint
                                            ),
                                            true
                                    )

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
                                    activity.delegate
                                            .firstScheduleEntry
                                            .scheduleDataWrapper
                                            .getScheduleDialogData(
                                                    Date.today(),
                                                    activity.delegate.scheduleHint
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

            fun fromParameters(
                    parameters: CreateTaskParameters,
                    data: CreateTaskViewModel.Data,
                    savedStates: Pair<ParentScheduleState, ParentScheduleState>?
            ): Delegate {
                return when (parameters) {
                    is CreateTaskParameters.Copy -> CopyDelegate(parameters, data, savedStates)
                    is CreateTaskParameters.Edit -> EditDelegate(parameters, data, savedStates)
                    is CreateTaskParameters.Join -> JoinDelegate(parameters, data, savedStates)
                    is CreateTaskParameters.Create,
                    is CreateTaskParameters.Share,
                    is CreateTaskParameters.Shortcut,
                    CreateTaskParameters.None -> CreateDelegate(parameters, data, savedStates)
                }
            }
        }

        abstract var data: CreateTaskViewModel.Data

        open val initialName: String? = null
        open val scheduleHint: Hint.Schedule? = null

        protected fun TaskKey.toParentKey() = CreateTaskViewModel.ParentKey.Task(this)
        protected fun Hint.toParentKey() = (this as? Hint.Task)?.taskKey?.toParentKey()
        protected fun Hint.toScheduleHint() = this as? Hint.Schedule

        val firstScheduleEntry by lazy {
            val (date, timePair) = scheduleHint?.let { Pair(it.date, it.timePair) }
                    ?: HourMinute.nextHour.let { Pair(it.first, TimePair(it.second)) }

            ScheduleEntry(CreateTaskViewModel.ScheduleDataWrapper.Single(ScheduleData.Single(date, timePair)))
        }

        abstract val initialState: ParentScheduleState
        abstract val stateData: ParentScheduleData

        protected fun getStateData(savedState: ParentScheduleState?): ParentScheduleData {
            val parentScheduleState = savedState ?: initialState
            val initialParent = parentScheduleState.parentKey?.let { findTaskData(it) }
            return ParentScheduleData(parentScheduleState, initialParent)
        }

        open fun checkDataChanged(name: String, note: String?) = name.isNotEmpty() || !note.isNullOrEmpty()

        protected fun checkDataChanged(
                taskData: CreateTaskViewModel.TaskData,
                name: String,
                note: String?
        ) = name != taskData.name || note != taskData.note

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

        fun createTask(createParameters: CreateParameters, stateData: ParentScheduleData): TaskKey {
            val projectId = (stateData.parent?.parentKey as? CreateTaskViewModel.ParentKey.Project)?.projectId

            return when {
                stateData.schedules.isNotEmpty() -> createTaskWithSchedule(
                        createParameters,
                        stateData.schedules.map { it.scheduleDataWrapper.scheduleData },
                        projectId
                )
                stateData.parent?.parentKey is CreateTaskViewModel.ParentKey.Task -> {
                    check(projectId == null)

                    val parentTaskKey = (stateData.parent!!.parentKey as CreateTaskViewModel.ParentKey.Task).taskKey

                    createTaskWithParent(createParameters, parentTaskKey)
                }
                else -> createTaskWithoutReminder(createParameters, projectId)
            }
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
            override var data: CreateTaskViewModel.Data,
            savedStates: Pair<ParentScheduleState, ParentScheduleState>?
    ) : Delegate() {

        private val taskData get() = data.taskData!!

        override val initialName get() = taskData.name

        override val initialState = savedStates?.first ?: ParentScheduleState(
                taskData.parentKey,
                taskData.scheduleDataWrappers
                        ?.map { ScheduleEntry(it) }
                        ?.toList()
        )

        override val stateData = getStateData(savedStates?.second)

        override fun checkDataChanged(name: String, note: String?) = checkDataChanged(taskData, name, note)

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
            override var data: CreateTaskViewModel.Data,
            savedStates: Pair<ParentScheduleState, ParentScheduleState>?
    ) : Delegate() {

        private val taskData get() = data.taskData!!

        override val initialName get() = taskData.name

        override val initialState = savedStates?.first ?: ParentScheduleState(
                taskData.parentKey,
                taskData.scheduleDataWrappers
                        ?.map { ScheduleEntry(it) }
                        ?.toMutableList()
                        ?: mutableListOf()
        )

        override val stateData = getStateData(savedStates?.second)

        override fun checkDataChanged(name: String, note: String?) = checkDataChanged(taskData, name, note)

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
            override var data: CreateTaskViewModel.Data,
            savedStates: Pair<ParentScheduleState, ParentScheduleState>?
    ) : Delegate() {

        override val scheduleHint = parameters.hint?.toScheduleHint()

        override val initialState: ParentScheduleState

        init {
            val (initialParentKey, schedule) = parameters.run {
                if (hint is Hint.Task) {
                    Pair(hint.toParentKey(), null)
                } else {
                    Pair(
                            taskKeys.map { it.projectKey }
                                    .distinct()
                                    .singleOrNull()
                                    ?.let {
                                        (it as? ProjectKey.Shared)?.let { CreateTaskViewModel.ParentKey.Project(it) }
                                    },
                            firstScheduleEntry.takeIf { data.defaultReminder }
                    )
                }
            }

            initialState = savedStates?.first ?: ParentScheduleState(
                    initialParentKey,
                    listOfNotNull(schedule)
            )
        }

        override val stateData = getStateData(savedStates?.second)

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
            override var data: CreateTaskViewModel.Data,
            savedStates: Pair<ParentScheduleState, ParentScheduleState>?
    ) : Delegate() {

        override val initialName: String?
        override val scheduleHint: Hint.Schedule?
        override val initialState: ParentScheduleState

        init {
            when (parameters) {
                is CreateTaskParameters.Create -> {
                    initialName = parameters.nameHint
                    scheduleHint = parameters.hint?.toScheduleHint()

                    val initialParentKey = parameters.hint?.toParentKey()
                    initialState = savedStates?.first ?: parameters.parentScheduleState
                            ?: ParentScheduleState(
                            initialParentKey,
                            listOfNotNull(firstScheduleEntry.takeIf { initialParentKey !is CreateTaskViewModel.ParentKey.Task && data.defaultReminder })
                    )
                }
                is CreateTaskParameters.Share -> {
                    initialName = parameters.nameHint
                    scheduleHint = null

                    val initialParentKey = parameters.parentTaskKeyHint?.toParentKey()
                    initialState = savedStates?.first ?: ParentScheduleState(
                            initialParentKey,
                            listOfNotNull(firstScheduleEntry.takeIf { initialParentKey == null && data.defaultReminder })
                    )
                }
                is CreateTaskParameters.Shortcut -> {
                    initialName = null
                    scheduleHint = null

                    val initialParentKey = parameters.parentTaskKeyHint.toParentKey()
                    initialState = savedStates?.first ?: ParentScheduleState(initialParentKey)
                }
                CreateTaskParameters.None -> {
                    initialName = null
                    scheduleHint = null

                    initialState = savedStates?.first ?: ParentScheduleState(
                            null,
                            listOfNotNull(firstScheduleEntry.takeIf { data.defaultReminder })
                    )
                }
                else -> throw IllegalArgumentException()
            }
        }

        override val stateData = getStateData(savedStates?.second)

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
            if (parameters is CreateTaskParameters.Share)
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
