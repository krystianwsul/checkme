package com.krystianwsul.checkme.upload

import android.annotation.SuppressLint
import android.net.Uri
import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageMetadata
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.AndroidDatabaseWrapper
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.common.firebase.models.ImageState
import com.krystianwsul.common.utils.TaskKey
import io.reactivex.android.schedulers.AndroidSchedulers
import io.reactivex.rxkotlin.Observables

object Uploader {

    private val storage = FirebaseStorage.getInstance()
            .getReference("taskImages")
            .child(AndroidDatabaseWrapper.root)

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
        Observables.combineLatest(
                Queue.ready,
                DomainFactory.instanceRelay
                        .filter { it.value != null }
                        .map { it.value!! }
        ).observeOn(AndroidSchedulers.mainThread())
                .subscribe { (_, domainFactory) ->
            Queue.getEntries()
                    .toMutableList()
                    .forEach { entry ->
                        val task = domainFactory.getTaskIfPresent(entry.taskKey)
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

    fun getReference(uuid: String) = storage.child(uuid)
    fun getPath(imageData: ImageState.Local) = Queue.getPath(imageData.uuid)
}