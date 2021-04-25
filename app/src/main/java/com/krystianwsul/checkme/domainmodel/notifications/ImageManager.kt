@file:Suppress("DEPRECATION")

package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.utils.circle
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.task.Task
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.core.Single
import io.reactivex.rxjava3.kotlin.subscribeBy
import io.reactivex.rxjava3.schedulers.Schedulers
import java.io.File
import kotlin.math.roundToInt

object ImageManager {

    private val LARGE_ICON_SIZE by lazy { MyApplication.context.dpToPx(64).roundToInt() }

    private val BIG_PICTURE_WIDTH by lazy { MyApplication.context.dpToPx(450).roundToInt() }
    private val BIG_PICTURE_HEIGHT by lazy { BIG_PICTURE_WIDTH / 2 }

    private val largeIconDownloader by lazy {
        Downloader(
                LARGE_ICON_SIZE,
                LARGE_ICON_SIZE,
                "largeIcons",
                true
        )
    }

    private val bigPictureDownloader by lazy {
        Downloader(
                BIG_PICTURE_WIDTH,
                BIG_PICTURE_HEIGHT,
                "bigPictures",
                false
        )
    }

    private val downloaders by lazy { listOf(largeIconDownloader, bigPictureDownloader) }

    @Synchronized
    fun init() = downloaders.forEach { it.init() }

    @Synchronized
    fun getLargeIcon(uuid: String?) = largeIconDownloader.getImage(uuid)

    @Synchronized
    fun getBigPicture(uuid: String?) = bigPictureDownloader.getImage(uuid)

    @Synchronized
    fun prefetch(deviceDbInfo: DeviceDbInfo, tasks: List<Task>, callback: () -> Unit) {
        largeIconDownloader.prefetch(deviceDbInfo, tasks, callback)
        bigPictureDownloader.prefetch(deviceDbInfo, tasks, callback)
    }

    private class Downloader(
            private val width: Int,
            private val height: Int,
            dirSuffix: String,
            private val circle: Boolean,
    ) {

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

        fun prefetch(deviceDbInfo: DeviceDbInfo, tasks: List<Task>, callback: () -> Unit) {
            val tasksWithImages = tasks.map { it to it.getImage(deviceDbInfo)?.uuid }
                    .filter { it.second != null }
                    .associate { it.second!! to it.first }

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
                val target = task.getImage(deviceDbInfo)!!
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
                                        .observeOnDomain()
                                        .subscribeBy {
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