@file:Suppress("DEPRECATION")

package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import android.util.Log
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.Task
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

object ImageManager {

    private const val LARGE_ICON_SIZE = 256

    private val largeIconDir = File(MyApplication.instance.cacheDir.absolutePath, "largeIcons")

    private lateinit var largeIcons: MutableMap<String, State>

    private fun getFile(uuid: String) = File(largeIconDir.absolutePath, uuid)

    fun init() {
        Single.fromCallable {
            largeIcons = (largeIconDir.listFiles()?.toList() ?: listOf()).map { it.name }
                    .associateWith { State.Downloaded }
                    .toMutableMap()
        }
                .subscribeOn(Schedulers.io())
                .subscribe()
    }

    @Synchronized
    fun prefetch(tasks: List<Task>) {
        val tasksWithImages = tasks.filter { it.image?.uuid != null }.associateBy { it.image!!.uuid!! }

        val taskUuids = tasksWithImages.keys
        val presentUuids = largeIcons.keys

        val imagesToDownload = taskUuids - presentUuids
        val imagesToRemove = presentUuids - taskUuids

        val statesToRemove = imagesToRemove.map { it to largeIcons.getValue(it) }

        imagesToRemove.forEach { largeIcons.remove(it) }

        statesToRemove.filter { it.second is State.Downloading }.forEach { (uuid, state) ->
            (state as State.Downloading).target
                    .request!!
                    .clear()

            largeIcons.remove(uuid)
        }

        Single.fromCallable {
            statesToRemove.filter { it.second is State.Downloaded }.forEach {
                Log.e("asdf", "ImageManager deleting ${it.first}")
                getFile(it.first).delete()
            }
        }
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe()

        val tasksToDownload = imagesToDownload.map { it to tasksWithImages.getValue(it) }

        largeIcons.putAll(tasksToDownload.map { (uuid, task) ->
            val target = task.image!!
                    .requestBuilder!!
                    .into(object : SimpleTarget<File>(LARGE_ICON_SIZE, LARGE_ICON_SIZE) {

                        @SuppressLint("CheckResult")
                        override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                            check(largeIcons.getValue(uuid) is State.Downloading)

                            Single.fromCallable {
                                check(largeIcons.getValue(uuid) is State.Downloading)

                                resource.copyTo(getFile(uuid), false)
                                Log.e("asdf", "ImageManager copied to $uuid")
                            }
                                    .subscribeOn(Schedulers.io())
                                    .observeOn(AndroidSchedulers.mainThread())
                                    .subscribe { _ ->
                                        check(largeIcons.getValue(uuid) is State.Downloading)

                                        largeIcons[uuid] = State.Downloaded
                                    }
                        }

                        override fun onLoadFailed(errorDrawable: Drawable?) {
                            check(largeIcons.getValue(uuid) is State.Downloading)

                            largeIcons.remove(uuid)
                        }
                    })

            uuid to State.Downloading(target)
        })
    }

    @Synchronized
    fun getImage(task: Task) = task.image // todo async
            ?.uuid
            ?.takeIf { (largeIcons[it] is State.Downloaded) }
            ?.let { BitmapFactory.decodeFile(getFile(it).absolutePath) }

    private sealed class State {

        object Downloaded : State()
        class Downloading(val target: Target<File>) : State()
    }
}