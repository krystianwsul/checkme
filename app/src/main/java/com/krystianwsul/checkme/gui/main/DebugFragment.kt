package com.krystianwsul.checkme.gui.main


import android.annotation.SuppressLint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.Button
import androidx.compose.material.Switch
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import arrow.core.Tuple5
import com.google.android.material.composethemeadapter.MdcTheme
import com.jakewharton.rxbinding4.view.clicks
import com.jakewharton.rxrelay3.BehaviorRelay
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.Preferences
import com.krystianwsul.checkme.databinding.FragmentDebugBinding
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.getGroupListData
import com.krystianwsul.checkme.domainmodel.extensions.setDebugMode
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.gui.base.AbstractFragment
import com.krystianwsul.checkme.gui.utils.ResettableProperty
import com.krystianwsul.checkme.ticks.Ticker
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.common.FeatureFlagManager
import com.krystianwsul.common.time.ExactTimeStamp
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.addTo
import io.reactivex.rxjava3.kotlin.plusAssign
import kotlinx.coroutines.MainScope
import kotlinx.coroutines.rx3.asObservable

class DebugFragment : AbstractFragment() {

    companion object {

        private val doneLog = mutableListOf<String>()

        fun newInstance() = DebugFragment()

        fun logDone(message: String) {
            if (FeatureFlagManager.getFlag(FeatureFlagManager.Flag.LOG_NOT_DONE_PERFORMANCE))
                doneLog += ExactTimeStamp.Local.now.hourMilli.toString() + " " + message
        }
    }

    private val bindingProperty = ResettableProperty<FragmentDebugBinding>()
    private var binding by bindingProperty

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?) =
        FragmentDebugBinding.inflate(inflater, container, false).also { binding = it }.root

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        binding.debugFeatureFlagContainer.setContent {
            MdcTheme {
                FeatureFlagSwitches()
            }
        }

        DomainFactory.instanceRelay
            .filterNotNull()
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe {
                binding.debugViewSwitch.apply {
                    isChecked = it.debugMode

                    setOnCheckedChangeListener { _, isChecked -> AndroidDomainUpdater.setDebugMode(isChecked).subscribe() }
                }
            }
            .addTo(viewCreatedDisposable)

        FeatureFlagManager.getFlow(FeatureFlagManager.Flag.LOG_NOT_DONE_PERFORMANCE)
            .asObservable(MainScope().coroutineContext)
            .subscribe { doneLog.clear() }
            .addTo(viewCreatedDisposable)

        binding.debugInstanceWarningSnooze.apply {
            isChecked = Preferences.instanceWarningSnoozeSet

            setOnCheckedChangeListener { _, isChecked -> Preferences.instanceWarningSnoozeSet = isChecked }
        }

        binding.debugTick
            .clicks()
            .switchMapCompletable { Ticker.tick("DebugFragment") }
            .subscribe()
            .addTo(viewCreatedDisposable)

        val loadStateClicks = PublishRelay.create<Unit>()

        binding.debugLoadState.setContent {
            MdcTheme {
                LoadStateButton { loadStateClicks.accept(Unit) }
            }
        }

        binding.debugClearPaper.setContent {
            MdcTheme {
                ClearPaperButton { AndroidDatabaseWrapper.onUpgrade() }
            }
        }

        loadStateClicks.toFlowable(BackpressureStrategy.DROP)
            .observeOnDomain()
            .subscribe { DomainFactory.instance.updateIsWaitingForTasks() }
            .addTo(viewCreatedDisposable)

        binding.debugLoad
            .clicks()
            .toFlowable(BackpressureStrategy.DROP)
            .observeOnDomain()
            .flatMapSingle(
                {
                    Single.fromCallable {
                        val domainFactory = DomainFactory.instance

                        val t1 = ExactTimeStamp.Local.now
                        DomainFactory.instance.getGroupListData(
                            ExactTimeStamp.Local.now,
                            0,
                            Preferences.TimeRange.DAY,
                            true,
                        )

                        val t2 = ExactTimeStamp.Local.now

                        val loadTime = t2.long - t1.long

                        val waitingOnDependencies = DomainFactory.instance.run {
                            val projectTaskRows = waitingProjectTasks().map { it.name }.toList()

                            val projectRows = waitingProjectDetails().map { (project, taskKeys) ->
                                listOf(project.name + " " + project.projectKey) + taskKeys.map { "\t" + it.toString() }
                            }.flatten()

                            val rootTaskRows = waitingRootTasks().map { it.name }.toList()

                            val waitingNames = projectTaskRows + projectRows + rootTaskRows

                            waitingNames.joinToString("\n")
                        }

                        val ordinalCount = DomainFactory.instance.run {
                            val user = myUserFactory.user

                            user.projectIds
                                .map {
                                    user.getOrdinalEntriesForProject(projectsFactory.sharedProjects.getValue(it))
                                        .values
                                        .count()
                                }
                                .sum()
                        }

                        Tuple5(
                            loadTime,
                            "isWaitingForTasks? ${DomainFactory.instance.isWaitingForTasks.value}\n$waitingOnDependencies",
                            ordinalCount,
                            domainFactory.taskCount,
                            domainFactory.instanceCount,
                        )
                    }
                },
                false,
                1,
            )
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe { (loadTime, waitingOnDependencies, ordinalCount, taskCount, instanceCount) ->
                binding.debugData.text = StringBuilder().apply {
                    val lastTick = Preferences.lastTick
                    val tickLog = Preferences.tickLog.log

                    val lastTickExactTimeStamp = ExactTimeStamp.Local(lastTick)

                    val domainFactory = DomainFactory.instance

                    val deviceDbInfo = domainFactory.deviceDbInfo

                    append("\ndevice info:")
                    append("\nuuid: " + deviceDbInfo.uuid)
                    append("\ntoken: " + deviceDbInfo.token + "\n")

                    append("\nload time: ")
                    append(domainFactory.remoteReadTimes.run { readMillis + instantiateMillis })
                    append("ms (")
                    append(domainFactory.remoteReadTimes.readMillis)
                    append(" + ")
                    append(domainFactory.remoteReadTimes.instantiateMillis)
                    append(")")
                    append("\ndependencies done: ")
                    append(domainFactory.finishedWaiting)
                    append("\nfirst ChangeType delay: ")
                    append(domainFactory.changeTypeDelay)

                    append("\ntoday: ")
                    append(loadTime)
                    append(" ms")

                    append("\n\ntasks: ")
                    append(taskCount)
                    append("\nall existing instances: ")
                    append(instanceCount)
                    append("\nfirst page root instances: existing ")
                    append(domainFactory.instanceInfo.first)
                    append(", virtual ")
                    append(domainFactory.instanceInfo.second)
                    append("\ncustom times: ")
                    append(domainFactory.customTimeCount)
                    append("\ninstance shown: ")
                    append(domainFactory.instanceShownCount)
                    append("\nproject ordinal entries: ")
                    append(ordinalCount)

                    append("\n\nwaiting on dependencies:\n")
                    append(waitingOnDependencies)

                    append("\n\ndone log:\n")
                    append(doneLog.joinToString("\n"))

                    append("\n\ntab log:\n")
                    append(Preferences.mainTabsLog.log)

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

        val showNotificationLog = BehaviorRelay.createDefault(false)

        viewCreatedDisposable += showNotificationLog.subscribe { show ->
            binding.debugNotificationSection.setContent {
                MdcTheme {
                    NotificationSection(
                        onClick = { showNotificationLog.accept(!show) },
                        text = Preferences.notificationLog
                            .log
                            .takeIf { show },
                    )
                }
            }
        }
    }

    override fun onDestroyView() {
        bindingProperty.reset()

        super.onDestroyView()
    }

    @Composable
    private fun FeatureFlagSwitches() {
        Column {
            FeatureFlagManager.getFlags().forEach { (flag, initialValue) ->
                val state = remember { mutableStateOf(initialValue) }

                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    Text(text = flag.toString(), modifier = Modifier.weight(1f))
                    Switch(checked = state.value, onCheckedChange = {
                        state.value = it
                        FeatureFlagManager.setFlag(flag, it)
                    })
                }
            }
        }
    }

    @Composable
    private fun LoadStateButton(onClick: () -> Unit) {
        Button(onClick = onClick) {
            Text("REFRESH LOAD STATE")
        }
    }

    @Composable
    private fun ClearPaperButton(onClick: () -> Unit) {
        Button(onClick = onClick) {
            Text("CLEAR PAPER FB CACHE")
        }
    }

    @Composable
    private fun NotificationSection(onClick: () -> Unit, text: String?) {
        Column {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = onClick,
            ) {
                Text((if (text == null) "SHOW" else "HIDE") + " NOTIFICATION LOG")
            }

            text?.let { Text(it, fontSize = 14.sp) }
        }
    }
}
