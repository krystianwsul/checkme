@file:Suppress("DEPRECATION")

package com.krystianwsul.checkme.domainmodel.notifications

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import com.bumptech.glide.request.target.SimpleTarget
import com.bumptech.glide.request.target.Target
import com.bumptech.glide.request.transition.Transition
import com.jakewharton.rxrelay3.BehaviorRelay
import com.krystianwsul.checkme.MyApplication
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.observeOnDomain
import com.krystianwsul.checkme.domainmodel.toImageLoader
import com.krystianwsul.checkme.utils.circle
import com.krystianwsul.checkme.utils.dpToPx
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.DomainThreadChecker
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.utils.TaskKey
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
            true,
        )
    }

    private val bigPictureDownloader by lazy {
        Downloader(
            BIG_PICTURE_WIDTH,
            BIG_PICTURE_HEIGHT,
            "bigPictures",
            false,
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

        private val tag = "ImageManager.$dirSuffix"

        private val dir = File(MyApplication.instance.cacheDir.absolutePath, dirSuffix)

        private lateinit var imageStates: MutableMap<String, State>

        private fun getFile(uuid: String) = File(dir.absolutePath, uuid)

        private val readyRelay = BehaviorRelay.create<Unit>()

        fun init() {
            check(!readyRelay.hasValue())

            dir.mkdirs()

            Single.fromCallable {
                check(!readyRelay.hasValue())

                imageStates = (dir.listFiles()?.toList() ?: listOf()).map { it.name }
                    .associateWith { State.Downloaded }
                    .toMutableMap()

                readyRelay.accept(Unit)
            }
                .subscribeOn(Schedulers.io())
                .subscribe()
        }

        fun prefetch(deviceDbInfo: DeviceDbInfo, tasks: List<Task>, callback: () -> Unit) {
            DomainThreadChecker.instance.requireDomainThread()

            readyRelay.observeOnDomain().subscribe {
                val tasksWithImages = tasks.map { it.taskKey to it.getImage(deviceDbInfo) as? ImageState.Displayable }
                    .filter { it.second != null }
                    .associate { (taskKey, imageState) -> imageState!!.uuid to Pair(taskKey, imageState) }

                val taskUuids = tasksWithImages.keys
                val presentUuids = imageStates.keys

                val imagesToDownload = taskUuids - presentUuids
                val imagesToRemove = presentUuids - taskUuids

                val statesToRemove = imagesToRemove.map { it to imageStates.getValue(it) }

                imagesToRemove.forEach {
                    imageStates.remove(it)
                    MyCrashlytics.log("$tag: removed $it")
                }

                statesToRemove.filter { it.second is State.Downloading }.forEach { (uuid, state) ->
                    (state as State.Downloading).target
                        .request!!
                        .clear()

                    imageStates.remove(uuid)

                    /*
                    todo: I think this also needs to cancel the RX inside the SimpleTarget.  Check logging to confirm.
                     */

                    MyCrashlytics.log("$tag: cleared $uuid")
                }

                Single.fromCallable {
                    statesToRemove.filter { it.second is State.Downloaded }.forEach {
                        getFile(it.first).delete()
                    }
                }
                    .subscribeOn(Schedulers.io())
                    .subscribe()

                val tasksToDownload = imagesToDownload.map { it to tasksWithImages.getValue(it) }

                imageStates += tasksToDownload.map { (uuid, pair) ->
                    val (taskKey, imageState) = pair

                    val requestBuilder = imageState.toImageLoader().requestBuilder

                    val state = if (requestBuilder == null) {
                        MyCrashlytics.logException(MissingImageException(taskKey, imageState))

                        State.Missing
                    } else {
                        val target = requestBuilder.circle(circle).into(object : SimpleTarget<Bitmap>(width, height) {

                            @SuppressLint("CheckResult")
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                check(imageStates.getValue(uuid) is State.Downloading)

                                MyCrashlytics.log("$tag: downloaded a $uuid")

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
                                        check(imageStates.containsKey(uuid)) { "$tag: can't find $uuid" }
                                        check(imageStates.getValue(uuid) is State.Downloading)

                                        imageStates[uuid] = State.Downloaded

                                        MyCrashlytics.log("$tag: downloaded b $uuid")

                                        callback()
                                    }
                            }

                            override fun onLoadFailed(errorDrawable: Drawable?) {
                                check(imageStates.getValue(uuid) is State.Downloading)

                                imageStates.remove(uuid)

                                MyCrashlytics.log("$tag: onLoadFailed $uuid")
                            }
                        })

                        State.Downloading(target)
                    }

                    uuid to state
                }.onEach {
                    MyCrashlytics.log("$tag: added " + it.first)
                }
            }
        }

        fun getImage(uuid: String?) = uuid?.takeIf { (imageStates[it] is State.Downloaded) }?.let {
            { BitmapFactory.decodeFile(getFile(it).absolutePath) }
        }
    }

    private sealed class State {

        object Missing : State()
        object Downloaded : State()
        class Downloading(val target: Target<Bitmap>) : State()
    }

    private class MissingImageException(taskKey: TaskKey, imageState: ImageState) :
        Exception("missing image for $taskKey: $imageState")
}