package com.krystianwsul.checkme.upload

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.ImageState
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.TaskKey
import java.io.FileInputStream

object Uploader {

    private val storage = FirebaseStorage.getInstance()
            .getReference("taskImages")
            .child(DatabaseWrapper.root)

    fun addUpload(taskKey: TaskKey, uuid: String, path: String) {
        Log.e("asdf", "image upload start")

        val task = DomainFactory.instance.getTaskForce(taskKey)

        check(task.image == ImageState.Local(uuid))

        val entry = Queue.addEntry(uuid, path)

        val stream = FileInputStream(path)

        storage.child(uuid)
                .putStream(stream)
                .addOnFailureListener { MyCrashlytics.logException(it) }
                .addOnSuccessListener {
                    Log.e("asdf", "image upload complete")

                    Queue.removeEntry(entry)

                    DomainFactory.addFirebaseListener {
                        it.setTaskImageUploaded(SaveService.Source.GUI, taskKey, uuid)
                        Log.e("asdf", "image upload written")
                    }
                }

        // todo image delete tmp files
    }

    fun getReference(imageData: ImageState.Remote) = storage.child(imageData.uuid)
    fun getPath(imageData: ImageState.Local) = Queue.getPath(imageData.uuid)
}