package com.krystianwsul.checkme.domainmodel

import android.annotation.SuppressLint
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.jakewharton.rxrelay3.PublishRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.R
import com.krystianwsul.checkme.domainmodel.notifications.ImageManager
import com.krystianwsul.checkme.gui.edit.EditActivity
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.task.Task
import io.reactivex.rxjava3.core.BackpressureStrategy
import io.reactivex.rxjava3.schedulers.Schedulers

@SuppressLint("CheckResult")
object ShortcutQueue {

    private const val CATEGORY = "ADD_TO_LIST"

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
                        .setCategories(setOf(CATEGORY))
                        .setIntent(EditActivity.getShortcutIntent(it.taskKey))
                        .setRank(10)
                        .build()
                }

                val existingShortcuts = ShortcutManagerCompat.getDynamicShortcuts(MyApplication.context)
                    .filter { it.categories.orEmpty().contains(CATEGORY) }
                    .map { it.id }

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

    class ShortcutData(deviceDbInfo: DeviceDbInfo, task: Task) {

        val taskKey = task.taskKey
        val name = task.name
        val uuid = task.getImage(deviceDbInfo)?.uuid
    }
}