package com.krystianwsul.checkme.gui.main


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import com.jakewharton.rxbinding4.view.clicks
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.databinding.FragmentDebugBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo

class DebugFragment : AbstractFragment() {

    companion object {

        fun newInstance() = DebugFragment()
    }

    private val bindingProperty = ResettableProperty<FragmentDebugBinding>()
    private var binding by bindingProperty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) = FragmentDebugBinding.inflate(inflater, container, false).also { binding = it }.root

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        DomainFactory.instanceRelay
                .filterNotNull()
                .subscribe {
                    binding.debugViewSwitch.apply {
                        isChecked = it.debugMode

                        setOnCheckedChangeListener { _, isChecked ->
                            it.debugMode = isChecked
                        }
                    }
                }
                .addTo(viewCreatedDisposable)

        binding.debugTick
                .clicks()
                .switchMapCompletable { Ticker.tick("DebugFragment") }
                .subscribe()
                .addTo(viewCreatedDisposable)

        binding.debugLoad
                .clicks()
                .toFlowable(BackpressureStrategy.DROP)
                .observeOnDomain()
                .flatMapSingle(
                        {
                            Single.fromCallable {
                                val t1 = ExactTimeStamp.Local.now
                                DomainFactory.instance.getGroupListData(
                                        ExactTimeStamp.Local.now,
                                        0,
                                        Preferences.TimeRange.DAY,
                                )

                                val t2 = ExactTimeStamp.Local.now

                                t2.long - t1.long
                            }
                        },
                        false,
                        1,
                )
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe { loadTime ->
                    binding.debugData.text = StringBuilder().apply {
                        val lastTick = Preferences.lastTick
                        val tickLog = Preferences.tickLog.log

                        val lastTickExactTimeStamp = ExactTimeStamp.Local(lastTick)

                        val domainFactory = DomainFactory.instance

                        append("\nload time: ")
                        append(domainFactory.remoteReadTimes.run { readMillis + instantiateMillis })
                        append("ms (")
                        append(domainFactory.remoteReadTimes.readMillis)
                        append(" + ")
                        append(domainFactory.remoteReadTimes.instantiateMillis)
                        append(")")

                        append("\n\ntasks: ")
                        append(domainFactory.taskCount)
                        append("\nall existing instances: ")
                        append(domainFactory.instanceCount)
                        append("\nfirst page root instances: existing ")
                        append(domainFactory.instanceInfo.first)
                        append(", virtual ")
                        append(domainFactory.instanceInfo.second)
                        append("\ncustom times: ")
                        append(domainFactory.customTimeCount)
                        append("\ninstance shown: ")
                        append(domainFactory.instanceShownCount)

                        append("\n\n")
                        append(Preferences.mainTabsLog.log)

                        append("\n\ntoday: ")
                        append(loadTime)
                        append(" ms")

                        append("\ncrashlytics enabled: ")
                        append(MyCrashlytics.enabled)

                        append("\n\ntemporary notification log:\n\n")
                        append(Preferences.temporaryNotificationLog.log)

                        append("\n\nlast beeping tick: ")
                        append(lastTickExactTimeStamp.toString())
                        append("\n\ntick log:\n\n")
                        append(tickLog)
                    }
                }
                .addTo(viewCreatedDisposable)
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }
}
