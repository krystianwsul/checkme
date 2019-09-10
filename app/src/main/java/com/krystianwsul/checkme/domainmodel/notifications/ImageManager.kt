@file:Suppress("DEPRECATION")

package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.Task
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.utils.circle
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
            "largeIcons",
            true)

    private val bigPictureDownloader = Downloader(
            Resources.getSystem()
                    .displayMetrics
                    .widthPixels,
            MyApplication.instance
                    .dpToPx(256)
                    .toInt(),
            "bigPictures",
            false)

    private val downloaders = listOf(largeIconDownloader, bigPictureDownloader)

    @Synchronized
    fun init() = downloaders.forEach { it.init() }

    @Synchronized
    fun prefetch(tasks: List<Task>, callback: () -> Unit) = downloaders.forEach { it.prefetch(tasks, callback) }

    @Synchronized
    fun getLargeIcon(uuid: String?) = largeIconDownloader.getImage(uuid)

    @Synchronized
    fun getBigPicture(uuid: String?) = bigPictureDownloader.getImage(uuid)

    private class Downloader(
            private val width: Int,
            private val height: Int,
            dirSuffix: String,
            private val circle: Boolean) {

        private val dir = File(MyApplication.instance.cacheDir.absolutePath, dirSuffix)

        private lateinit var imageStates: MutableMap<String, State>

        private fun getFile(uuid: String) = File(dir.absolutePath, uuid)

        fun init() {
            dir.mkdirs()

            Single.fromCallable {
                imageStates = (dir.listFiles()?.toList() ?: listOf()).map { it.name }
                        .associateWith { State.Downloaded }
                        .toMutableMap()
            }
                    .subscribeOn(Schedulers.io())
                    .subscribe()
        }

        fun prefetch(tasks: List<Task>, callback: () -> Unit) {
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
                        .toImageLoader()
                        .requestBuilder!!
                        .circle(circle)
                        .into(object : SimpleTarget<Bitmap>(width, height) {

                            @SuppressLint("CheckResult")
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                check(imageStates.getValue(uuid) is State.Downloading)

                                Single.fromCallable {
                                    check(imageStates.getValue(uuid) is State.Downloading)

                                    getFile(uuid).apply {
                                        createNewFile()

                                        outputStream().let {
                                            resource.compress(Bitmap.CompressFormat.PNG, 0, it)
                                            it.flush()
                                            it.close()
                                        }
                                    }
                                }
                                        .subscribeOn(Schedulers.io())
                                        .observeOn(AndroidSchedulers.mainThread())
                                        .subscribe { _ ->
                                            check(imageStates.getValue(uuid) is State.Downloading)

                                            imageStates[uuid] = State.Downloaded

                                            callback()
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

        fun getImage(uuid: String?) = uuid?.takeIf { (imageStates[it] is State.Downloaded) }?.let {
            { BitmapFactory.decodeFile(getFile(it).absolutePath) }
        }
    }

    private sealed class State {

        object Downloaded : State()
        class Downloading(val target: Target<Bitmap>) : State()
    }
}