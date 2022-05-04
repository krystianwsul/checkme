package com.krystianwsul.checkme.gui.instances


import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.StringRes
import androidx.appcompat.widget.LinearLayoutCompat
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.databinding.FragmentDayBinding
import com.krystianwsul.checkme.gui.main.MainActivity
import com.krystianwsul.checkme.gui.utils.BottomFabMenuDelegate
import com.krystianwsul.checkme.utils.partition
import com.krystianwsul.checkme.utils.time.toDateTimeTz
import com.krystianwsul.checkme.viewmodels.DayViewModel
import com.krystianwsul.common.time.Date
import io.reactivex.rxjava3.core.Maybe
import io.reactivex.rxjava3.core.Observable
import io.reactivex.rxjava3.disposables.CompositeDisposable
import io.reactivex.rxjava3.kotlin.Observables
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import java.util.*


class DayFragment @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : LinearLayoutCompat(context, attrs, defStyleAttr) {
    // todo consider subclass

    companion object {

        fun getTitle(position: Int): String {
            fun getString(@StringRes stringId: Int) = MyApplication.instance.getString(stringId)

            return when (position) {
                0 -> getString(R.string.today)
                1 -> getString(R.string.tomorrow)
                else -> {
                    Date(
                        Calendar.getInstance()
                            .apply { add(Calendar.DATE, position) }
                            .toDateTimeTz()
                    ).let { it.dayOfWeek.toString() + ", " + it.toString() }
                }
            }
        }
    }

    private val key = BehaviorRelay.create<Int>()

    private var fabDelegate: BottomFabMenuDelegate.FabDelegate? = null

    private val activity = context as MainActivity

    private val dayViewModel = activity.dayViewModel
    private var entry: DayViewModel.Entry? = null

    private val attachedToWindowDisposable = CompositeDisposable()

    private val compositeDisposable = CompositeDisposable()

    private val binding: FragmentDayBinding

    init {
        check(context is Host)

        layoutParams = ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT)
        orientation = VERTICAL

        binding = FragmentDayBinding.inflate(LayoutInflater.from(context), this)

        binding.groupListFragment.apply {
            listener = (context as MainActivity).daysGroupListListener
            forceSaveStateListener = { saveState() }
        }
    }

    fun saveState() = activity.setState(key.value!!, binding.groupListFragment.onSaveInstanceState())

    fun setPosition(position: Int) {
        entry?.stop()
        compositeDisposable.clear()

        key.value?.let { saveState() }

        key.accept(position)

        activity.getState(key.value!!)?.let {
            binding.groupListFragment.onRestoreInstanceState(it)
        }

        // this seems redundant/obsolete, but I'll leave it for now
        fabDelegate?.let(binding.groupListFragment::setVisible)

        entry = dayViewModel.getEntry(position)
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()

        val hostEvents = key.switchMap { key -> (context as Host).hostEvents.map { Pair(key, it) } }

        val (startEvents, stopEvents) =
            hostEvents.partition { (key, event) -> event is Event.PageVisible && event.position == key }

        compositeDisposable += startEvents.subscribe { (_, event) ->
            check(event is Event.PageVisible)

            setFab(event.fabDelegate)

            activity.selectAllRelay
                .subscribe {
                    binding.groupListFragment
                        .treeViewAdapter
                        .selectAll()
                }
                .addTo(compositeDisposable)
        }

        Observables.combineLatest(startEvents, Preferences.showAssignedObservable)
            .subscribe { (_, showAssigned) -> entry!!.start(showAssigned) }
            .addTo(compositeDisposable)

        compositeDisposable += stopEvents.subscribe {
            entry!!.stop()

            clearFab()
            saveState()
        }

        key.switchMap { key -> entry!!.data.map { Triple(key, it, entry!!.dataId) } }
            .subscribe { (key, data, dataId) ->
                binding.groupListFragment.setAll(
                    key,
                    dataId,
                    data.immediate,
                    data.groupListDataWrapper,
                )
            }
            .addTo(compositeDisposable)

        hostEvents.switchMap { (key, event) ->
            if (event is Event.PageVisible && event.position == key)
                activity.started
            else
                Observable.never()
        }
            .filter { it }
            .subscribe { binding.groupListFragment.checkCreatedTaskKey() }
            .addTo(compositeDisposable)

        // this should be inside the GroupListFragment
        activity.dateChangeRelay
            .switchMapMaybe {
                binding.groupListFragment
                    .treeViewAdapterNullable
                    ?.listUpdates
                    ?.firstElement()
                    ?: Maybe.empty()
            }
            .subscribe { binding.groupListFragment.scrollToTop() }
            .addTo(attachedToWindowDisposable)
    }

    override fun onDetachedFromWindow() {
        compositeDisposable.clear()
        attachedToWindowDisposable.clear()

        super.onDetachedFromWindow()
    }

    private fun setFab(fabDelegate: BottomFabMenuDelegate.FabDelegate) {
        this.fabDelegate = fabDelegate

        binding.groupListFragment.setVisible(fabDelegate)
    }

    private fun clearFab() {
        fabDelegate = null

        binding.groupListFragment.clearFab()
    }

    interface Host {

        val hostEvents: Observable<Event>
    }

    sealed class Event {

        data class PageVisible(val position: Int, val fabDelegate: BottomFabMenuDelegate.FabDelegate) : Event()

        object Invisible : Event()
    }
}
