package com.krystianwsul.checkme.upload

import android.annotation.SuppressLint
import android.net.Uri
import androidx.core.net.toUri
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.google.firebase.storage.StorageTask
import com.google.firebase.storage.UploadTask
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.domainmodel.extensions.setTaskImageUploadedService
import com.krystianwsul.checkme.domainmodel.update.AndroidDomainUpdater
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.utils.filterNotNull
import com.krystianwsul.common.domain.DeviceDbInfo
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.rxjava3.android.schedulers.AndroidSchedulers
import io.reactivex.rxjava3.kotlin.Observables

object Uploader {

    private val storage = FirebaseStorage.getInstance()
            .getReference("taskImages")
            .child(AndroidDatabaseWrapper.root)

    fun addUpload(
            deviceDbInfo: DeviceDbInfo,
            taskKey: TaskKey,
            uuid: String,
            pair: Pair<String, Uri>,
    ) {
        val task = DomainFactory.instance.getTaskForce(taskKey)

        check(task.getImage(deviceDbInfo) == ImageState.Local(uuid))

        val entry = Queue.addEntry(taskKey, uuid, pair.first, pair.second)

        storage.child(uuid)
                .putFile(Uri.parse(entry.fileUri))
                .addOnProgressListener {
                    it.uploadSessionUri?.let {
                        if (entry.sessionUri == null) {
                            entry.sessionUri = it.toString()
                            Queue.write()
                        }
                    }
                }
                .addListeners(entry)
    }

    @SuppressLint("CheckResult")
    fun resume() {
        Observables.combineLatest(
                Queue.ready,
                DomainFactory.instanceRelay.filterNotNull()
        ).observeOn(AndroidSchedulers.mainThread())
                .subscribe { (_, domainFactory) ->
                    Queue.getEntries()
                            .toMutableList()
                            .forEach { entry ->
                                val task = domainFactory.getTaskIfPresent(entry.taskKey) ?: return@forEach

                                if (task.getImage(domainFactory.deviceDbInfo) != ImageState.Local(entry.uuid))
                                    return@forEach

                                try {
                                    storage.child(entry.uuid)
                                            .putFile(
                                                    entry.fileUri.toUri(),
                                                    StorageMetadata.Builder().build(),
                                                    entry.sessionUri?.toUri(),
                                            )
                                            .addListeners(entry)
                                } catch (throwable: Throwable) {
                                    MyCrashlytics.logException(UploadException("failed to read file", throwable))
                                }
                            }
                }
    }

    private fun StorageTask<UploadTask.TaskSnapshot>.addListeners(entry: Queue.Entry) {
        addOnFailureListener { MyCrashlytics.logException(UploadException("uri: ${entry.fileUri}", it)) }

        addOnSuccessListener {
            Queue.removeEntry(entry)

            AndroidDomainUpdater.setTaskImageUploadedService(entry.taskKey, entry.uuid).subscribe()
        }
    }

    fun getReference(uuid: String) = storage.child(uuid)
    fun getPath(imageData: ImageState.Local) = Queue.getPath(imageData.uuid)

    private class UploadException(message: String, cause: Throwable) : Exception(message, cause)
}