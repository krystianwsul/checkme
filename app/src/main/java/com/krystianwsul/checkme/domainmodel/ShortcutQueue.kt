package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.jakewharton.rxrelay2.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.Task
import io.reactivex.BackpressureStrategy
import io.reactivex.schedulers.Schedulers

@SuppressLint("CheckResult")
object ShortcutQueue {

    private val relay = PublishRelay.create<List<ShortcutData>>()

    init {
        relay.toFlowable(BackpressureStrategy.BUFFER)
                .observeOn(Schedulers.io())
                .subscribe {
                    val shortcuts = it.map {
                        val icon = it.uuid
                                ?.let { ImageManager.getLargeIcon(it) }
                                ?.invoke()
                                ?.let { IconCompat.createWithAdaptiveBitmap(it) }
                                ?: IconCompat.createWithResource(MyApplication.instance, R.mipmap.launcher_add)

                        ShortcutInfoCompat.Builder(MyApplication.instance, it.taskKey.toShortcut())
                                .setShortLabel(MyApplication.instance.getString(R.string.addTo) + " " + it.name)
                                .setIcon(icon)
                                .setCategories(setOf("ADD_TO_LIST"))
                                .setIntent(EditActivity.getShortcutIntent(it.taskKey))
                                .build()
                    }

                    val existingShortcuts =
                            ShortcutManagerCompat.getDynamicShortcuts(MyApplication.context).map { it.id }

                    val addShortcuts = shortcuts.filter { it.id !in existingShortcuts }
                    val updateShortcuts = shortcuts.filter { it.id in existingShortcuts }

                    val shortcutIds = shortcuts.map { it.id }
                    val removeShortcuts = existingShortcuts.filter { it !in shortcutIds }

                    ShortcutManagerCompat.removeDynamicShortcuts(MyApplication.context, removeShortcuts)
                    ShortcutManagerCompat.addDynamicShortcuts(MyApplication.context, addShortcuts)
                    ShortcutManagerCompat.updateShortcuts(MyApplication.context, updateShortcuts)
                }
    }

    fun updateShortcuts(shortcutDatas: List<ShortcutData>) = relay.accept(shortcutDatas)

    class ShortcutData(deviceDbInfo: DeviceDbInfo, task: Task<*>) {

        val taskKey = task.taskKey
        val name = task.name
        val uuid = task.getImage(deviceDbInfo)?.uuid
    }
}