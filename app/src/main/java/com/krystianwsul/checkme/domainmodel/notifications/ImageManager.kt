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
import io.reactivex.rxjava3.disposables.Disposable
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
                val tasksWithImages = tasks.map {
                    val imageState = it.getImage(deviceDbInfo)

                    MyCrashlytics.log("$tag taskKey: ${it.taskKey}, imageState: $imageState, json: " + it.imageJson)

                    it.taskKey to imageState as? ImageState.Displayable
                }
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

                statesToRemove.filter { it.second is State.Stoppable }.forEach { (uuid, state) ->
                    (state as State.Stoppable).stop()

                    imageStates.remove(uuid)

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
                        /*
                        At the moment, it looks like there's a chance this is caused by a race condition in Uploader. First
                        the file is removed, then updating the Task is enqueued on the domain thread.  If that's the case,
                        then I see a few options:

                        1. Find a way to eliminate the inconsistency in ImageState
                        2. Try to cancel this operation during the inconsistency
                        3. Accept it, and ensure that this State.Missing gets overwritten on the next try
                            - Also, double-check that updating the Task from the Uploader does make this re-run
                         */
                        MyCrashlytics.logException(MissingImageException(taskKey, imageState))

                        State.Missing
                    } else {
                        val target = requestBuilder.circle(circle).into(object : SimpleTarget<Bitmap>(width, height) {

                            @SuppressLint("CheckResult")
                            override fun onResourceReady(resource: Bitmap, transition: Transition<in Bitmap>?) {
                                check(imageStates.getValue(uuid) is State.Downloading)

                                MyCrashlytics.log("$tag: downloaded a $uuid")

                                val disposable = Single.fromCallable {
                                    check(imageStates.getValue(uuid) is State.Reading)

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
                                        check(imageStates.getValue(uuid) is State.Reading)

                                        imageStates[uuid] = State.Downloaded

                                        MyCrashlytics.log("$tag: downloaded b $uuid")

                                        callback()
                                    }

                                imageStates[uuid] = State.Reading(disposable)
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
                    MyCrashlytics.log("$tag: added " + it.first + ", " + it.second)
                }
            }
        }

        fun getImage(uuid: String?) = uuid?.takeIf { (imageStates[it] is State.Downloaded) }?.let {
            { BitmapFactory.decodeFile(getFile(it).absolutePath) }
        }
    }

    private sealed interface State {

        object Missing : State

        sealed interface Stoppable : State {

            fun stop()
        }

        class Downloading(private val target: Target<Bitmap>) : Stoppable {

            override fun stop() = target.request!!.clear()
        }

        class Reading(private val disposable: Disposable) : Stoppable {

            override fun stop() = disposable.dispose()
        }

        object Downloaded : State
    }

    private class MissingImageException(taskKey: TaskKey, imageState: ImageState) :
        Exception("missing image for $taskKey: $imageState")
}