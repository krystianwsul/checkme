@file:Suppress("DEPRECATION")

package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.utils.dpToPx
import io.reactivex.Single
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.schedulers.Schedulers
import java.io.File

object ImageManager {

    private const val LARGE_ICON_SIZE = 256

    private val largeIconDownloader = Downloader(
            LARGE_ICON_SIZE,
            LARGE_ICON_SIZE,
            "largeIcons")

    private val bigPictureDownloader = Downloader(
            Resources.getSystem()
                    .displayMetrics
                    .widthPixels,
            MyApplication.instance
                    .dpToPx(256)
                    .toInt(),
            "bigPictures")

    private val downloaders = listOf(largeIconDownloader, bigPictureDownloader)

    @Synchronized
    fun init() = downloaders.forEach { it.init() }

    @Synchronized
    fun prefetch(tasks: List<Task>) = downloaders.forEach { it.prefetch(tasks) }

    @Synchronized
    fun getLargeIcon(task: Task) = largeIconDownloader.getImage(task)

    @Synchronized
    fun getBigPicture(task: Task) = bigPictureDownloader.getImage(task)

    private class Downloader(private val width: Int, private val height: Int, dirSuffix: String) {

        private val dir = File(MyApplication.instance.cacheDir.absolutePath, dirSuffix)

        private lateinit var imageStates: MutableMap<String, State>

        private fun getFile(uuid: String) = File(dir.absolutePath, uuid)

        fun init() {
            Single.fromCallable {
                imageStates = (dir.listFiles()?.toList() ?: listOf()).map { it.name }
                        .associateWith { State.Downloaded }
                        .toMutableMap()
            }
                    .subscribeOn(Schedulers.io())
                    .subscribe()
        }

        fun prefetch(tasks: List<Task>) {
            val tasksWithImages = tasks.filter { it.image?.uuid != null }.associateBy { it.image!!.uuid!! }

            val taskUuids = tasksWithImages.keys
            val presentUuids = imageStates.keys

            val imagesToDownload = taskUuids - presentUuids
            val imagesToRemove = presentUuids - taskUuids

            val statesToRemove = imagesToRemove.map { it to imageStates.getValue(it) }

            imagesToRemove.forEach { imageStates.remove(it) }

            statesToRemove.filter { it.second is State.Downloading }.forEach { (uuid, state) ->
                (state as State.Downloading).target
                        .request!!
                        .clear()

                imageStates.remove(uuid)
            }

            Single.fromCallable {
                statesToRemove.filter { it.second is State.Downloaded }.forEach {
                    getFile(it.first).delete()
                }
            }
                    .subscribeOn(Schedulers.io())
                    .observeOn(AndroidSchedulers.mainThread())
                    .subscribe()

            val tasksToDownload = imagesToDownload.map { it to tasksWithImages.getValue(it) }

            imageStates.putAll(tasksToDownload.map { (uuid, task) ->
                val target = task.image!!
                        .requestBuilder!!
                        .into(object : SimpleTarget<File>(width, height) {

                            @SuppressLint("CheckResult")
                            override fun onResourceReady(resource: File, transition: Transition<in File>?) {
                                check(imageStates.getValue(uuid) is State.Downloading)

                                Single.fromCallable {
                                    check(imageStates.getValue(uuid) is State.Downloading)

                                    resource.copyTo(getFile(uuid), false)
                                }
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { _ ->
                                            check(imageStates.getValue(uuid) is State.Downloading)

                                            imageStates[uuid] = State.Downloaded
                                        }
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                check(imageStates.getValue(uuid) is State.Downloading)

                                imageStates.remove(uuid)
                            }
                        })

                uuid to State.Downloading(target)
            })
        }

        fun getImage(task: Task) = task.image // todo async
                ?.uuid
                ?.takeIf { (imageStates[it] is State.Downloaded) }
                ?.let { BitmapFactory.decodeFile(getFile(it).absolutePath) }
    }

    private sealed class State {

        object Downloaded : State()
        class Downloading(val target: Target<File>) : State()
    }
}