package com.krystianwsul.checkme.gui.tasks.create

import android.annotation.SuppressLint
import android.app.Activity
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
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
import com.krystianwsul.checkme.gui.DiscardDialogFragment
import com.krystianwsul.checkme.gui.NavBarActivity
import com.krystianwsul.checkme.gui.tasks.*
import com.krystianwsul.checkme.gui.tasks.create.delegates.CreateTaskDelegate
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.setFixedOnClickListener
import com.krystianwsul.checkme.utils.startTicks
import com.krystianwsul.checkme.viewmodels.CreateTaskViewModel
import com.krystianwsul.checkme.viewmodels.getViewModel
import com.krystianwsul.common.time.Date
import com.krystianwsul.common.time.HourMinute
import com.krystianwsul.common.time.TimePair
import com.krystianwsul.common.utils.InstanceKey
import com.krystianwsul.common.utils.ScheduleType
import com.krystianwsul.common.utils.TaskKey
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

        const val KEY_PARENT_PROJECT_TYPE = "parentProjectType"
        const val KEY_PARENT_PROJECT_KEY = "parentProjectKey"
        const val KEY_PARENT_TASK = "parentTask"

        private const val PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment"

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
    private lateinit var delegate: CreateTaskDelegate

    private val discardDialogListener = this::finish

    private val parentFragmentListener = object : ParentPickerFragment.Listener {

        override fun onTaskSelected(parentTreeData: CreateTaskViewModel.ParentTreeData) {
            delegate.parentScheduleManager.parent = parentTreeData
        }

        override fun onTaskDeleted() = removeParent()

        override fun onNewParent(nameHint: String?) = startActivityForResult(
                getCreateIntent(
                        null,
                        delegate.parentScheduleManager.run {
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
        checkNotNull(delegate.parentScheduleManager.parent)

        delegate.parentScheduleManager.parent = null
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

    private val timeRelay = BehaviorRelay.createDefault(Unit)

    private val timeReceiver = object : BroadcastReceiver() {

        override fun onReceive(context: Context?, intent: Intent?) = timeRelay.accept(Unit)
    }

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
            checkNotNull(toolbarEditText)

            if (!updateError()) {
                val name = toolbarEditText.text.toString().trim { it <= ' ' }
                check(!TextUtils.isEmpty(name))

                createTaskViewModel.stop()

                val writeImagePath = imageUrl.value!!.writeImagePath

                val createParameters = CreateTaskDelegate.CreateParameters(
                        name,
                        note,
                        writeImagePath
                )

                val taskKey = delegate.createTask(createParameters)

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
                                initialize(delegate.data.customTimeDatas)
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
                            if (result.scheduleDialogData.scheduleType == ScheduleType.MONTHLY_DAY) {
                                check(result.scheduleDialogData.monthlyDay)
                            } else if (result.scheduleDialogData.scheduleType == ScheduleType.MONTHLY_WEEK) {
                                check(!result.scheduleDialogData.monthlyDay)
                            }

                            if (result.position == null) {
                                delegate.parentScheduleManager.addSchedule(result.scheduleDialogData.toScheduleEntry())
                            } else {
                                check(result.position >= 1)

                                val position = result.position - 1

                                val oldId = if (position < delegate.parentScheduleManager.schedules.size) {
                                    delegate.parentScheduleManager
                                            .schedules[position]
                                            .id
                                } else {
                                    null
                                }

                                delegate.parentScheduleManager.setSchedule(position, result.scheduleDialogData.toScheduleEntry(oldId))
                            }
                        }
                        is ScheduleDialogFragment.Result.Delete -> removeSchedule(result.position)
                        is ScheduleDialogFragment.Result.Cancel -> Unit
                    }
                }
                .addTo(createDisposable)

        startTicks(timeReceiver)
    }

    private fun removeSchedule(position: Int) {
        check(position >= 1)

        delegate.parentScheduleManager.removeSchedule(position - 1)
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
                delegate.saveState(this)

                if (!note.isNullOrEmpty())
                    putString(NOTE_KEY, note)

                putBoolean(NOTE_HAS_FOCUS_KEY, noteHasFocus)
            }

            putSerializable(IMAGE_URL_KEY, imageUrl.value!!)
        }
    }

    private val loadFinishedDisposable = CompositeDisposable().also { createDisposable += it }

    private val dataLoaded get() = this::delegate.isInitialized

    private fun onLoadFinished(data: CreateTaskViewModel.Data) {
        loadFinishedDisposable.clear()

        this.data = data

        if (dataLoaded) {
            delegate.data = data
        } else {
            delegate = CreateTaskDelegate.fromParameters(
                    parameters,
                    data,
                    savedInstanceState
            )
        }

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

        if (savedInstanceState?.containsKey(NOTE_HAS_FOCUS_KEY) == true) {
            savedInstanceState!!.run {
                if (containsKey(NOTE_KEY)) {
                    note = getString(NOTE_KEY)!!
                    check(!note.isNullOrEmpty())
                }

                noteHasFocus = getBoolean(NOTE_HAS_FOCUS_KEY)
            }
        }

        (supportFragmentManager.findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG) as? ParentPickerFragment)?.initialize(data.parentTreeDatas, parentFragmentListener)

        invalidateOptionsMenu()

        (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.initialize(data.customTimeDatas)

        createTaskAdapter = CreateTaskAdapter()
        createTaskRecycler.adapter = createTaskAdapter
        createTaskRecycler.itemAnimator = CustomItemAnimator()

        delegate.parentScheduleManager
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
        var hasError = false

        if (TextUtils.isEmpty(toolbarEditText.text)) {
            toolbarLayout.error = getString(R.string.nameError)

            hasError = true
        } else {
            toolbarLayout.error = null
        }

        for (scheduleEntry in delegate.parentScheduleManager.schedules) {
            if (delegate.getError(scheduleEntry) != null)
                hasError = true
        }

        return hasError
    }

    override fun onDestroy() {
        unregisterReceiver(timeReceiver)

        super.onDestroy()
    }

    private fun dataChanged(): Boolean {
        if (data == null)
            return false

        return delegate.checkDataChanged(toolbarEditText.text.toString(), note)
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
                delegate.parentScheduleManager.parent = delegate.findTaskData(CreateTaskViewModel.ParentKey.Task(taskKey))
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

            fun getItem() = holder.adapterPosition
                    .takeIf { it >= 0 }
                    ?.let { items[it] }

            holder.compositeDisposable += imageUrl.subscribe { getItem()?.onNewImageState(it, holder) }

            delegate.parentScheduleManager
                    .parentObservable
                    .subscribe { getItem()?.onNewParent(this@CreateTaskActivity, holder) }
                    .addTo(holder.compositeDisposable)

            holder.compositeDisposable += timeRelay.subscribe { getItem()?.onTimeChanged(this@CreateTaskActivity, holder) }
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

    private sealed class Item {

        abstract val holderType: HolderType

        abstract fun bind(activity: CreateTaskActivity, holder: Holder)

        open fun onNewImageState(imageState: CreateTaskImageState, holder: Holder) = Unit

        open fun onNewParent(activity: CreateTaskActivity, holder: Holder) = Unit

        open fun onTimeChanged(activity: CreateTaskActivity, holder: Holder) = Unit

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

                        onTimeChanged(activity, holder)

                        addOneShotGlobalLayoutListener {
                            isHintAnimationEnabled = true
                        }
                    }

                    onNewParent(activity, this)
                }
            }

            override fun onNewParent(activity: CreateTaskActivity, holder: Holder) {
                val parent = activity.delegate
                        .parentScheduleManager
                        .parent

                (holder as ScheduleHolder).apply {
                    scheduleLayout.endIconMode = if (parent != null)
                        TextInputLayout.END_ICON_CLEAR_TEXT
                    else
                        TextInputLayout.END_ICON_DROPDOWN_MENU

                    scheduleText.run {
                        setText(parent?.name)

                        setFixedOnClickListener {
                            ParentPickerFragment.newInstance(parent != null).let {
                                it.show(activity.supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                                it.initialize(activity.delegate.data.parentTreeDatas, activity.parentFragmentListener)
                            }
                        }
                    }

                    if (parent != null)
                        scheduleLayout.setEndIconOnClickListener { activity.removeParent() }
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
                        isHintAnimationEnabled = false
                        endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
                    }

                    scheduleText.run {
                        setText(scheduleEntry.scheduleDataWrapper.getText(activity.delegate.data.customTimeDatas, activity))

                        setFixedOnClickListener(
                                {
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

            override fun onTimeChanged(activity: CreateTaskActivity, holder: Holder) {
                activity.delegate
                        .getError(scheduleEntry)
                        ?.let { (holder as ScheduleHolder).scheduleLayout.error = activity.getString(it.resource) }
            }

            fun same(other: ScheduleEntry): Boolean {
                if (scheduleEntry.id == other.id)
                    return true

                return scheduleEntry.scheduleDataWrapper === other.scheduleDataWrapper
            }

            override fun same(other: Item): Boolean {
                if (other !is Schedule)
                    return false

                return same(other.scheduleEntry)
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

            override fun onNewImageState(imageState: CreateTaskImageState, holder: Holder) {
                (holder as ImageHolder).apply {
                    if (imageState.loader != null) {
                        imageProgress.visibility = View.VISIBLE
                        imageImage.visibility = View.VISIBLE
                        imageLayout.visibility = View.GONE
                        imageEdit.visibility = View.VISIBLE

                        imageState.loader!!(imageImage)
                    } else {
                        imageProgress.visibility = View.GONE
                        imageImage.visibility = View.GONE
                        imageLayout.visibility = View.VISIBLE
                        imageEdit.visibility = View.GONE
                    }
                }
            }
        }
    }
}
