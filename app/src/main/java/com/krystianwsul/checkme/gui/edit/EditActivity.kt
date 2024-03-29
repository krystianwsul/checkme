package com.krystianwsul.checkme.gui.edit

import android.annotation.SuppressLint
import android.app.Activity
import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.text.Editable
import android.text.TextWatcher
import android.view.*
import androidx.activity.viewModels
import androidx.core.view.isVisible
import androidx.core.view.updatePadding
import androidx.recyclerview.widget.CustomItemAnimator
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.textfield.TextInputEditText
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.*
import com.krystianwsul.checkme.gui.base.NavBarActivity
import com.krystianwsul.checkme.gui.dialogs.ConfirmDialogFragment
import com.krystianwsul.checkme.gui.dialogs.TwoChoicesCancelDialogFragment
import com.krystianwsul.checkme.gui.edit.delegates.EditDelegate
import com.krystianwsul.checkme.gui.edit.dialogs.AssignToDialogFragment
import com.krystianwsul.checkme.gui.edit.dialogs.CameraGalleryFragment
import com.krystianwsul.checkme.gui.edit.dialogs.JoinAllRemindersDialogFragment
import com.krystianwsul.checkme.gui.edit.dialogs.parentpicker.ParentPickerFragment
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogFragment
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogParameters
import com.krystianwsul.checkme.gui.edit.dialogs.schedule.ScheduleDialogResult
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity
import com.krystianwsul.checkme.utils.addOneShotGlobalLayoutListener
import com.krystianwsul.checkme.utils.hideKeyboardOnClickOutside
import com.krystianwsul.checkme.utils.loadPhoto
import com.krystianwsul.checkme.utils.tryGetFragment
import com.krystianwsul.common.criteria.SearchCriteria
import com.krystianwsul.common.utils.TaskKey
import com.krystianwsul.common.utils.UserKey
import com.krystianwsul.treeadapter.getCurrentValue
import com.miguelbcr.ui.rx_paparazzo2.entities.FileData
import com.miguelbcr.ui.rx_paparazzo2.entities.Response
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.merge
import io.reactivex.rxjava3.kotlin.plusAssign
import io.reactivex.rxjava3.kotlin.subscribeBy
import kotlin.properties.Delegates.observable


class EditActivity : NavBarActivity() {

    companion object {

        private const val DISCARD_TAG = "discard"

        const val KEY_PARAMETERS = "parameters"

        const val KEY_PARENT_PROJECT_TYPE = "parentProjectType"
        const val KEY_PARENT_PROJECT_KEY = "parentProjectKey"
        const val KEY_PARENT_TASK = "parentTask"

        private const val PARENT_PICKER_FRAGMENT_TAG = "parentTaskPickerFragment"

        private const val NOTE_KEY = "note"
        private const val NOTE_HAS_FOCUS_KEY = "noteHasFocus"

        private const val SCHEDULE_DIALOG_TAG = "scheduleDialog"
        private const val TAG_CAMERA_GALLERY = "cameraGallery"
        private const val TAG_JOIN_ALL_REMINDERS = "joinAllReminders"
        private const val TAG_ADD_TO_ALL_REMINDERS = "addToAllReminders"
        private const val TAG_ASSIGN_TO = "assignTo"

        private const val REQUEST_CREATE_PARENT = 982

        fun getParametersIntent(editParameters: EditParameters) =
            Intent(MyApplication.instance, EditActivity::class.java).apply {
                putExtra(KEY_PARAMETERS, editParameters)
            }

        fun getShortcutIntent(parentTaskKeyHint: TaskKey?) = Intent(MyApplication.instance, EditActivity::class.java).apply {
            action = Intent.ACTION_DEFAULT

            if (parentTaskKeyHint != null) {

                if (parentTaskKeyHint is TaskKey.Project) {
                    putExtra(KEY_PARENT_PROJECT_KEY, parentTaskKeyHint.projectKey.key)
                    putExtra(KEY_PARENT_PROJECT_TYPE, parentTaskKeyHint.projectKey.type.ordinal)
                }

                putExtra(KEY_PARENT_TASK, parentTaskKeyHint.taskId)

            }
        }

        var createdTaskKey: TaskKey? = null
    }

    private var savedInstanceState: Bundle? = null

    private lateinit var parameters: EditParameters

    private lateinit var createTaskAdapter: CreateTaskAdapter

    private val discardDialogListener: (Parcelable?) -> Unit = { finish() }

    private var note: String? = null

    private val noteHasFocusRelay = BehaviorRelay.createDefault(false) // keyboard hack

    private val onChildAttachStateChangeListener = object : RecyclerView.OnChildAttachStateChangeListener { // keyboard hack

        override fun onChildViewAttachedToWindow(view: View) {
            view.findViewById<TextInputEditText>(R.id.noteText)?.let {
                binding.editRecycler.removeOnChildAttachStateChangeListener(this)

                it.requestFocus()

                window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
            }
        }

        override fun onChildViewDetachedFromWindow(view: View) = Unit
    }

    val editViewModel by viewModels<EditViewModel>()

    private val parametersRelay = PublishRelay.create<ScheduleDialogParameters>()

    private val joinAllRemindersListener = { allReminders: Boolean ->
        save(false, EditDelegate.DialogResult.JoinAllInstances(allReminders))
    }

    private val addToAllRemindersListener = { allReminders: Boolean,
                                              payload: TwoChoicesCancelDialogFragment.Parameters.BooleanPayload ->
        save(payload.value, EditDelegate.DialogResult.AddToAllInstances(allReminders))
    }

    override val rootView get() = binding.root

    private val noteChanges = PublishRelay.create<Unit>()

    private val imageHeightRelay = BehaviorRelay.create<Int>()

    private lateinit var binding: ActivityEditBinding

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_save, menu)
        return true
    }

    override fun onPrepareOptionsMenu(menu: Menu): Boolean {
        menu.findItem(R.id.action_save).isVisible = editViewModel.hasDelegate
        menu.findItem(R.id.action_save_and_open).isVisible = editViewModel.hasDelegate &&
                editViewModel.delegate.showSaveAndOpen

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        fun trySave(andOpen: Boolean) {
            if (updateError()) return

            when (editViewModel.delegate.showDialog()) {
                EditDelegate.ShowDialog.JOIN -> {
                    check(!andOpen)

                    JoinAllRemindersDialogFragment.newInstance()
                        .apply { listener = joinAllRemindersListener }
                        .show(supportFragmentManager, TAG_JOIN_ALL_REMINDERS)
                }
                EditDelegate.ShowDialog.ADD -> TwoChoicesCancelDialogFragment.newInstance(
                    TwoChoicesCancelDialogFragment.Parameters.newAddToAllReminders(andOpen)
                )
                    .apply { listener = addToAllRemindersListener }
                    .show(supportFragmentManager, TAG_ADD_TO_ALL_REMINDERS)
                EditDelegate.ShowDialog.NONE -> save(andOpen, EditDelegate.DialogResult.None)
            }
        }

        when (item.itemId) {
            R.id.action_save -> trySave(false)
            R.id.action_save_and_open -> trySave(true)
            android.R.id.home -> if (tryClose()) finish()
            else -> throw UnsupportedOperationException()
        }

        return true
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityEditBinding.inflate(layoutInflater)
        setContentView(binding.root)

        setSupportActionBar(binding.editToolbarEditTextInclude.toolbar)

        supportActionBar!!.run {
            setDisplayHomeAsUpEnabled(true)
            setHomeAsUpIndicator(R.drawable.ic_close_white_24dp)
        }

        this.savedInstanceState = savedInstanceState

        binding.editRecycler.layoutManager = LinearLayoutManager(this)

        parameters = EditParameters.fromIntent(intent)

        supportFragmentManager.run {
            tryGetFragment<ConfirmDialogFragment>(DISCARD_TAG)?.listener = discardDialogListener
            tryGetFragment<JoinAllRemindersDialogFragment>(TAG_JOIN_ALL_REMINDERS)?.listener = joinAllRemindersListener

            tryGetFragment<TwoChoicesCancelDialogFragment<TwoChoicesCancelDialogFragment.Parameters.BooleanPayload>>(
                TAG_ADD_TO_ALL_REMINDERS
            )?.listener =
                addToAllRemindersListener
        }

        if (!noteHasFocusRelay.value!!)// keyboard hack
            window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)

        parentPickerDelegateRelay.switchMap { delegate ->
            delegate.startedRelay.map<Pair<ParentPickerDelegate?, Boolean>> { delegate to it }
        }
            .startWithItem(null to false)
            .switchMap { (delegate, started) ->
                if (started) {
                    delegate!!.queryObservable.map(SearchCriteria.Search::Query)
                } else {
                    /*
                    I think this stream should be used not just if the delegate isn't started, but also if its query hasn't
                    been modified yet.  But it works as-is for now.
                     */
                    editViewModel.delegateRelay.switchMap {
                        it.parentScheduleManager
                            .parentObservable
                            .map {
                                val taskKey = it.value
                                    ?.parentKey
                                    ?.let { it as? EditViewModel.ParentKey.Task }
                                    ?.let { SearchCriteria.Search.TaskKey(it.taskKey) }

                                taskKey ?: SearchCriteria.Search.Query()
                            }
                    }
                }
            }
            .subscribe(editViewModel.searchRelay)
            .addTo(createDisposable)

        editViewModel.apply {
            start(parameters, this@EditActivity)

            createDisposable += mainData.subscribe { onLoadFinished() }
        }

        hideKeyboardOnClickOutside(binding.root)

        // to handle different backpressure
        val programmaticParametersRelay = PublishRelay.create<ScheduleDialogParameters>()

        listOfNotNull(
            listOf(
                parametersRelay.toFlowable(BackpressureStrategy.DROP),
                programmaticParametersRelay.toFlowable(BackpressureStrategy.BUFFER),
            ).merge()
                .flatMapSingle(
                    {
                        ScheduleDialogFragment.newInstance(it).run {
                            initialize(editViewModel.delegate.customTimeDatas)
                            show(supportFragmentManager, SCHEDULE_DIALOG_TAG)
                            result.firstOrError()
                        }
                    },
                    false,
                    1,
                )
                .toObservable(),
            (supportFragmentManager.findFragmentByTag(SCHEDULE_DIALOG_TAG) as? ScheduleDialogFragment)?.let {
                Observable.just(it)
            }?.flatMapSingle { it.result.firstOrError() },
        ).merge()
            .subscribe { result ->
                when (result) {
                    is ScheduleDialogResult.Change -> {
                        editViewModel.delegate.run {
                            if (result.position == null) {
                                parentScheduleManager.addSchedule(result.scheduleDialogData.toScheduleEntry())
                            } else {
                                setSchedule(result.position, result.scheduleDialogData)
                            }
                        }
                    }
                    is ScheduleDialogResult.Delete -> removeSchedule(result.position)
                    is ScheduleDialogResult.Cancel -> Unit
                    is ScheduleDialogResult.Copy -> programmaticParametersRelay.accept(
                        ScheduleDialogParameters(
                            result.scheduleDialogData,
                            parameters.excludedTaskKeys,
                        )
                    )
                }
            }
            .addTo(createDisposable)

        Observable.combineLatest(
            keyboardInsetRelay,
            noteHasFocusRelay,
            noteChanges,
            imageHeightRelay,
        ) { keyboardInset, noteHasFocus, _, imageHeight ->
            if (noteHasFocus) {
                binding.editToolbarEditTextInclude
                    .editToolbarAppBar
                    .setExpanded(false)

                val padding = (keyboardInset - imageHeight).coerceAtLeast(0)

                binding.editRecycler.apply {
                    updatePadding(bottom = padding)
                    smoothScrollBy(0, padding)
                }
            } else {
                binding.editRecycler.updatePadding(bottom = 0)
            }
        }
            .subscribe()
            .addTo(createDisposable)

        tryGetFragment<AssignToDialogFragment>(TAG_ASSIGN_TO)?.listener = ::assignTo
    }

    private fun removeSchedule(position: Int) {
        check(position >= 0)

        editViewModel.delegate.removeSchedule(position)
    }

    @SuppressLint("CheckResult")
    fun getImage(single: Observable<Response<EditActivity, FileData>>) {
        single.observeOn(AndroidSchedulers.mainThread()).subscribe {
            if (it.resultCode() == Activity.RESULT_OK) {
                it.targetUI()
                    .editViewModel
                    .setEditImageState(EditImageState.Selected(it.data().file))
            }
        }
    }

    public override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)

        outState.run {
            if (editViewModel.hasDelegate) {
                putString(NOTE_KEY, note)
                putBoolean(NOTE_HAS_FOCUS_KEY, noteHasFocusRelay.value!!)
            }
        }
    }

    private var first = true

    private fun onLoadFinished() {
        if (!first) return
        first = false

        binding.editToolbarEditTextInclude
            .toolbarLayout
            .run {
                visibility = View.VISIBLE
                isHintAnimationEnabled = true
            }

        binding.editToolbarEditTextInclude
            .toolbarEditText
            .run {
                if (savedInstanceState == null) setText(editViewModel.delegate.initialName)

                addTextChangedListener(object : TextWatcher {

                    private var skip = savedInstanceState != null

                    override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                    override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                    override fun afterTextChanged(s: Editable) {
                        if (skip) skip = false else updateNameError()
                    }
                })
            }

        if (savedInstanceState?.containsKey(NOTE_HAS_FOCUS_KEY) == true) {
            savedInstanceState!!.run {
                check(containsKey(NOTE_KEY))

                note = getString(NOTE_KEY)
                noteHasFocusRelay.accept(getBoolean(NOTE_HAS_FOCUS_KEY))
            }
        } else {
            note = editViewModel.delegate.initialNote
        }

        tryGetFragment<ParentPickerFragment>(PARENT_PICKER_FRAGMENT_TAG)?.initialize(newParentPickerDelegate())

        invalidateOptionsMenu()

        tryGetFragment<ScheduleDialogFragment>(SCHEDULE_DIALOG_TAG)?.initialize(editViewModel.delegate.customTimeDatas)

        createTaskAdapter = CreateTaskAdapter()

        binding.editRecycler.apply {
            adapter = createTaskAdapter
            itemAnimator = CustomItemAnimator()

            if (noteHasFocusRelay.value!!) { // keyboard hack
                addOnChildAttachStateChangeListener(onChildAttachStateChangeListener)

                (layoutManager as LinearLayoutManager).scrollToPosition(createTaskAdapter.notePosition)
            }
        }
    }

    override fun onBackPressed() {
        if (tryClose()) super.onBackPressed()
    }

    private fun tryClose(): Boolean {
        return if (dataChanged()) {
            ConfirmDialogFragment.newInstance(ConfirmDialogFragment.Parameters.Discard).let {
                it.listener = discardDialogListener
                it.show(supportFragmentManager, DISCARD_TAG)
            }

            false
        } else {
            true
        }
    }

    private fun updateNameError(): Boolean {
        val error = getString(R.string.titleError).takeIf {
            binding.editToolbarEditTextInclude
                .toolbarEditText
                .text
                .isNullOrEmpty()
        }

        binding.editToolbarEditTextInclude
            .toolbarLayout
            .error = error

        return error != null
    }

    private fun updateError() = updateNameError()

    private fun dataChanged(): Boolean {
        if (!editViewModel.hasDelegate) return false

        return editViewModel.delegate.checkDataChanged(
            editViewModel.editImageState,
            binding.editToolbarEditTextInclude
                .toolbarEditText
                .text
                .toString(),
            note,
        )
    }

    @Suppress("OVERRIDE_DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        @Suppress("DEPRECATION")
        super.onActivityResult(requestCode, resultCode, data)

        if (requestCode == REQUEST_CREATE_PARENT) {
            if (resultCode == Activity.RESULT_OK) {
                val taskKey = data!!.getParcelableExtra<TaskKey>(ShowTaskActivity.TASK_KEY_KEY)!!

                editViewModel.delegate.setParentTask(taskKey)
            }
        }
    }

    private fun save(andOpen: Boolean, dialogResult: EditDelegate.DialogResult) {
        val name = binding.editToolbarEditTextInclude
            .toolbarEditText
            .text
            .toString()
            .trim { it <= ' ' }

        check(name.isNotEmpty())

        editViewModel.stop()

        val createParameters = EditDelegate.CreateParameters(
            name,
            note,
            editViewModel.editImageState
                .writeImagePath
                ?.value,
        )

        editViewModel.delegate
            .createTask(createParameters, dialogResult)
            .subscribeBy {
                if (andOpen) startActivity(it.intent)

                setResult(
                    Activity.RESULT_OK,
                    Intent().putExtra(ShowTaskActivity.TASK_KEY_KEY, it.taskKey as Parcelable)
                )

                finish()
            }
            .addTo(createDisposable)
    }

    private fun assignTo(userKeys: Set<UserKey>) {
        val allUserKeys = editViewModel.delegate
            .parentScheduleManager
            .parent!!
            .projectUsers
            .values
            .map { it.key }
            .toSet()

        val finalUserKeys = userKeys.takeIf { it != allUserKeys } ?: setOf()

        editViewModel.delegate
            .parentScheduleManager
            .assignedTo = finalUserKeys
    }

    private val parentPickerDelegateRelay = PublishRelay.create<ParentPickerDelegate>()
    private fun newParentPickerDelegate() = ParentPickerDelegate().also(parentPickerDelegateRelay::accept)

    enum class HolderType {

        SCHEDULE {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                ScheduleHolder(RowScheduleBinding.inflate(layoutInflater, parent, false))
        },

        NOTE {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                NoteHolder(RowNoteBinding.inflate(layoutInflater, parent, false))
        },

        ASSIGNED {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                AssignedHolder(RowAssignedBinding.inflate(layoutInflater, parent, false))
        },

        IMAGE {

            override fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup) =
                ImageHolder(RowImageBinding.inflate(layoutInflater, parent, false))
        };

        abstract fun newHolder(layoutInflater: LayoutInflater, parent: ViewGroup): Holder
    }

    @Suppress("PrivatePropertyName")
    private inner class CreateTaskAdapter : RecyclerView.Adapter<Holder>() {

        private var items: List<Item> by observable(
            editViewModel.delegate
                .adapterItemObservable
                .getCurrentValue()
        ) { _, oldItems, newItems ->
            DiffUtil.calculateDiff(object : DiffUtil.Callback() {

                override fun getOldListSize() = oldItems.size

                override fun getNewListSize() = newItems.size

                override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldItems[oldItemPosition].same(newItems[newItemPosition])

                override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int) =
                    oldItems[oldItemPosition] == newItems[newItemPosition]
            }).dispatchUpdatesTo(this)
        }

        init {
            editViewModel.delegate
                .adapterItemObservable
                .subscribe { items = it }
                .addTo(createDisposable)
        }

        val notePosition get() = items.indexOf(Item.Note)

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int) =
            HolderType.values()[viewType].run { newHolder(layoutInflater, parent) }

        override fun onBindViewHolder(holder: Holder, position: Int) =
            items[position].bind(this@EditActivity, holder)

        override fun onViewAttachedToWindow(holder: Holder) {
            super.onViewAttachedToWindow(holder)

            fun getItem() = holder.adapterPosition
                .takeIf { it >= 0 }
                ?.let { items[it] }

            editViewModel.editImageStateObservable
                .subscribe { getItem()?.onNewImageState(it, holder) }
                .addTo(holder.compositeDisposable)

            editViewModel.delegate
                .parentScheduleManager
                .parentObservable
                .subscribe { getItem()?.onNewParent(this@EditActivity, holder) }
                .addTo(holder.compositeDisposable)

            editViewModel.delegate
                .parentScheduleManager
                .assignedToObservable
                .subscribe { getItem()?.onNewAssignedTo(this@EditActivity, holder) }
                .addTo(holder.compositeDisposable)

            createDisposable += holder.compositeDisposable
        }

        override fun onViewDetachedFromWindow(holder: Holder) {
            holder.compositeDisposable.clear()

            createDisposable.remove(holder.compositeDisposable)

            super.onViewDetachedFromWindow(holder)
        }

        override fun getItemCount() = items.size

        override fun getItemViewType(position: Int) = items[position].holderType.ordinal
    }

    abstract class Holder(view: View) : RecyclerView.ViewHolder(view) {

        val compositeDisposable = CompositeDisposable()
    }

    class ScheduleHolder(val rowScheduleBinding: RowScheduleBinding) : Holder(rowScheduleBinding.root)

    class NoteHolder(val rowNoteBinding: RowNoteBinding) : Holder(rowNoteBinding.root)

    class AssignedHolder(val rowAssignedBinding: RowAssignedBinding) : Holder(rowAssignedBinding.root)

    class ImageHolder(val rowImageBinding: RowImageBinding) : Holder(rowImageBinding.root)

    sealed class Item {

        abstract val holderType: HolderType

        abstract fun bind(activity: EditActivity, holder: Holder)

        open fun onNewImageState(imageState: EditImageState, holder: Holder) = Unit

        open fun onNewParent(activity: EditActivity, holder: Holder) = Unit

        open fun onNewAssignedTo(activity: EditActivity, holder: Holder) = Unit

        open fun same(other: Item) = other == this

        object Parent : Item() {

            override val holderType get() = HolderType.SCHEDULE

            override fun bind(activity: EditActivity, holder: Holder) {
                (holder as ScheduleHolder).apply {
                    rowScheduleBinding.apply {
                        scheduleMargin.isVisible = true

                        scheduleLayout.run {
                            hint = activity.getString(R.string.parentTask)
                            error = null
                            isHintAnimationEnabled = false

                            addOneShotGlobalLayoutListener { isHintAnimationEnabled = true }
                        }
                    }

                    onNewParent(activity, this)
                }
            }

            override fun onNewParent(activity: EditActivity, holder: Holder) {
                val parent = activity.editViewModel
                    .delegate
                    .parentScheduleManager
                    .parent

                (holder as ScheduleHolder).rowScheduleBinding.apply {
                    scheduleLayout.apply {
                        fun clickListener() = ParentPickerFragment.newInstance(parent != null, true).let {
                            it.show(activity.supportFragmentManager, PARENT_PICKER_FRAGMENT_TAG)
                            it.initialize(activity.newParentPickerDelegate())
                        }

                        if (parent != null) {
                            setClose(::clickListener) {
                                activity.editViewModel
                                    .delegate
                                    .parentScheduleManager
                                    .clearParentAndReplaceSchedules()
                            }
                        } else {
                            setDropdown(::clickListener)
                        }
                    }

                    scheduleText.setText(parent?.name)
                }
            }
        }

        data class Schedule(private val scheduleEntry: ScheduleEntry) : Item() {

            override val holderType = HolderType.SCHEDULE

            override fun bind(activity: EditActivity, holder: Holder) {
                (holder as ScheduleHolder).rowScheduleBinding.apply {
                    scheduleMargin.isVisible = false

                    scheduleLayout.run {
                        hint = null
                        isHintAnimationEnabled = false

                        setClose(
                            {
                                val parameters = ScheduleDialogParameters(
                                    scheduleEntry.scheduleDataWrapper.getScheduleDialogData(
                                        activity.editViewModel
                                            .delegate
                                            .getDefaultScheduleDateTimePair()
                                            .date
                                    ),
                                    activity.parameters.excludedTaskKeys,
                                    holder.adapterPosition,
                                )

                                activity.parametersRelay.accept(parameters)
                            },
                            { activity.removeSchedule(holder.adapterPosition) }
                        )
                    }

                    scheduleText.setText(
                        scheduleEntry.scheduleDataWrapper.getText(
                            activity.editViewModel
                                .delegate
                                .customTimeDatas,
                            activity,
                        )
                    )
                }
            }

            private fun same(other: ScheduleEntry): Boolean {
                if (scheduleEntry.id == other.id) return true

                return scheduleEntry.scheduleDataWrapper === other.scheduleDataWrapper
            }

            override fun same(other: Item): Boolean {
                if (other !is Schedule) return false

                return same(other.scheduleEntry)
            }
        }

        object NewSchedule : Item() {

            override val holderType = HolderType.SCHEDULE

            override fun bind(activity: EditActivity, holder: Holder) {
                (holder as ScheduleHolder).rowScheduleBinding.apply {
                    scheduleMargin.isVisible = false

                    scheduleLayout.run {
                        hint = activity.getString(R.string.addReminder)
                        error = null
                        isHintAnimationEnabled = false

                        setDropdown {
                            val parameters = activity.editViewModel
                                .delegate
                                .getDefaultScheduleDialogData()
                                .let { ScheduleDialogParameters(it, activity.parameters.excludedTaskKeys) }

                            activity.parametersRelay.accept(parameters)
                        }
                    }

                    scheduleText.text = null
                }
            }
        }

        object Note : Item() {

            private var activity: EditActivity? = null

            private val textListener = object : TextWatcher {

                override fun beforeTextChanged(s: CharSequence, start: Int, count: Int, after: Int) = Unit

                override fun onTextChanged(s: CharSequence, start: Int, before: Int, count: Int) = Unit

                override fun afterTextChanged(s: Editable) {
                    activity?.note = s.toString()
                }
            }

            private val layoutChangeListener = View.OnLayoutChangeListener { _, _, _, _, _, _, _, _, _ ->
                activity?.noteChanges?.accept(Unit)
            }

            override val holderType = HolderType.NOTE

            override fun bind(activity: EditActivity, holder: Holder) {
                this.activity = activity

                (holder as NoteHolder).rowNoteBinding.apply {
                    noteText.run {
                        setText(activity.note)

                        removeTextChangedListener(textListener)
                        addTextChangedListener(textListener)

                        removeOnLayoutChangeListener(layoutChangeListener)
                        addOnLayoutChangeListener(layoutChangeListener)

                        setOnFocusChangeListener { _, hasFocus -> activity.noteHasFocusRelay.accept(hasFocus) }
                    }

                    noteLayout.isHintAnimationEnabled = true
                }
            }
        }

        object Image : Item() {

            private var activity: EditActivity? = null

            private val layoutChangeListener = View.OnLayoutChangeListener { _, _, top, _, bottom, _, _, _, _ ->
                activity?.imageHeightRelay?.accept(bottom - top)
            }

            override val holderType = HolderType.IMAGE

            override fun bind(activity: EditActivity, holder: Holder) {
                this.activity = activity

                (holder as ImageHolder).rowImageBinding.apply {
                    fun listener() = CameraGalleryFragment.newInstance(
                        activity.editViewModel
                            .editImageState
                            .loader != null
                    ).show(activity.supportFragmentManager, TAG_CAMERA_GALLERY)

                    imageImage.setOnClickListener { listener() }
                    imageEdit.setOnClickListener { listener() }

                    imageLayout.setDropdown(::listener)

                    root.removeOnLayoutChangeListener(layoutChangeListener)
                    root.addOnLayoutChangeListener(layoutChangeListener)

                    root.updatePadding(bottom = activity.bottomInset)
                }
            }

            override fun onNewImageState(imageState: EditImageState, holder: Holder) {
                (holder as ImageHolder).rowImageBinding.apply {
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

        object AssignTo : Item() {

            override val holderType = HolderType.ASSIGNED

            private fun openDialog(editActivity: EditActivity) {
                val projectUsers = editActivity.editViewModel
                    .delegate
                    .parentScheduleManager
                    .parent!!
                    .projectUsers
                    .values
                    .toList()

                AssignToDialogFragment.newInstance(
                    projectUsers,
                    editActivity.editViewModel
                        .delegate
                        .parentScheduleManager
                        .assignedTo
                        .toList()
                        .takeIf { it.isNotEmpty() }
                        ?: projectUsers.map { it.key },
                )
                    .apply { listener = editActivity::assignTo }
                    .show(editActivity.supportFragmentManager, TAG_ASSIGN_TO)
            }

            override fun bind(activity: EditActivity, holder: Holder) {
                viewsMap.clear()

                (holder as AssignedHolder).rowAssignedBinding
                    .assignedLayout
                    .setDropdown { openDialog(activity) }

                onNewAssignedTo(activity, holder)
            }

            private val viewsMap = mutableMapOf<UserKey, View>()

            override fun onNewAssignedTo(activity: EditActivity, holder: Holder) {
                (holder as AssignedHolder).rowAssignedBinding.apply {
                    val users = activity.editViewModel
                        .delegate
                        .parentScheduleManager
                        .assignedToUsers

                    if (users.isEmpty()) {
                        assignedLayout.isVisible = true
                        assignedChipGroup.isVisible = false
                    } else {
                        assignedLayout.isVisible = false
                        assignedChipGroup.isVisible = true

                        val removeUserKeys = viewsMap.keys - users.keys
                        val addUserKeys = users.keys - viewsMap.keys

                        removeUserKeys.forEach {
                            assignedChipGroup.removeView(viewsMap.getValue(it))
                            viewsMap.remove(it)
                        }

                        addUserKeys.forEach { userKey ->
                            val user = users.getValue(userKey)

                            viewsMap[userKey] = RowAssignedChipBinding.inflate(
                                LayoutInflater.from(root.context),
                                assignedChipGroup,
                                true
                            )
                                .root
                                .apply {
                                    text = user.name
                                    loadPhoto(user.photoUrl)

                                    setOnClickListener { openDialog(activity) }

                                    setOnCloseIconClickListener {
                                        activity.editViewModel
                                            .delegate
                                            .parentScheduleManager
                                            .removeAssignedTo(user.key)
                                    }
                                }
                        }
                    }
                }
            }
        }
    }

    private inner class ParentPickerDelegate : ParentPickerFragment.Delegate {

        override val startedRelay = BehaviorRelay.createDefault(false)

        override val adapterDataObservable by lazy {
            editViewModel.parentPickerData.map { ParentPickerFragment.AdapterData(it.parentTreeDatas) }
        }

        private val queryRelay = BehaviorRelay.create<String>()

        val queryObservable = queryRelay.hide()

        override val initialScrollMatcher by lazy {
            editViewModel.delegate
                .parentScheduleManager
                .parent
                ?.let { parent ->
                    { entryData: ParentPickerFragment.EntryData ->
                        (entryData as EditViewModel.ParentEntryData).entryKey == parent.parentKey
                    }
                }
        }

        override fun onEntrySelected(entryData: ParentPickerFragment.EntryData) {
            editViewModel.delegate
                .parentScheduleManager
                .setNewParent((entryData as EditViewModel.ParentEntryData).toParent())
        }

        override fun onEntryDeleted() {
            editViewModel.delegate
                .parentScheduleManager
                .clearParentAndReplaceSchedules()
        }

        @Suppress("DEPRECATION")
        override fun onNewEntry(nameHint: String?) = startActivityForResult(
            getParametersIntent(
                editViewModel.delegate
                    .parentScheduleManager
                    .run {
                        EditParameters.Create(
                            parent?.parentKey?.let {
                                when (it) { // there's probably a helper for this somewhere
                                    is EditViewModel.ParentKey.Project -> EditParentHint.Project(it.projectId)
                                    is EditViewModel.ParentKey.Task ->
                                        parameters.getReplacementHintForNewTask(it.taskKey)
                                            ?: EditParentHint.Task(it.taskKey)
                                }
                            },
                            ParentScheduleState(
                                schedules.map { ScheduleEntry(it.scheduleDataWrapper) }.toMutableList(),
                                assignedTo,
                            ),
                            nameHint,
                        )
                    },
            ),
            REQUEST_CREATE_PARENT,
        )

        override fun onSearch(query: String) = queryRelay.accept(query)

        override fun onPaddingShown() = throw IllegalStateException()
    }
}
