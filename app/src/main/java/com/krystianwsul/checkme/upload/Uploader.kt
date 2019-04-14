package com.krystianwsul.checkme.upload

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.TaskKey

object Uploader {

    private val storage = FirebaseStorage.getInstance()
            .getReference("taskImages")
            .child(DatabaseWrapper.root)

    fun addUpload(taskKey: TaskKey, uuid: String, pair: Pair<String, Uri>) {
        Log.e("asdf", "image upload start")

        val task = DomainFactory.instance.getTaskForce(taskKey)

        check(task.image == ImageState.Local(uuid))

        val entry = Queue.addEntry(taskKey, uuid, pair.first, pair.second)

        storage.child(uuid)
                .putFile(pair.second)
                .addOnProgressListener {
                    it.uploadSessionUri?.let {
                        if (entry.sessionUri == null)
                            entry.sessionUri = it
                        Queue.write()
                    }
                }
                .addOnFailureListener {
                    Log.e("asdf", "image upload error", it)
                    MyCrashlytics.logException(it)
                }
                .addOnSuccessListener {
                    Log.e("asdf", "image upload complete")

                    Queue.removeEntry(entry)

                    DomainFactory.addFirebaseListener {
                        it.setTaskImageUploaded(SaveService.Source.GUI, taskKey, uuid)
                        Log.e("asdf", "image upload written")
                    }
                }
    }

    @SuppressLint("CheckResult")
    fun resume() {
        Queue.ready.subscribe {
            Queue.getEntries()
                    .toMutableList()
                    .forEach { entry ->
                        val task = DomainFactory.instance.getTaskIfPresent(entry.taskKey)
                                ?: return@forEach

                        if (task.image != ImageState.Local(entry.uuid))
                            return@forEach

                        Log.e("asdf", "image upload start")

                        storage.child(entry.uuid)
                                .putFile(entry.fileUri, StorageMetadata.Builder().build(), entry.sessionUri)
                                .addOnFailureListener {
                                    Log.e("asdf", "image upload error", it)
                                    MyCrashlytics.logException(it)
                                }
                                .addOnSuccessListener {
                                    Log.e("asdf", "image upload complete")

                                    Queue.removeEntry(entry)

                                    DomainFactory.addFirebaseListener {
                                        it.setTaskImageUploaded(SaveService.Source.GUI, entry.taskKey, entry.uuid)
                                        Log.e("asdf", "image upload written")
                                    }
                                }
                    }
        }
    }

    fun getReference(imageData: ImageState.Remote) = storage.child(imageData.uuid)
    fun getPath(imageData: ImageState.Local) = Queue.getPath(imageData.uuid)
}