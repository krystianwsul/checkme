package com.krystianwsul.checkme.upload

import android.util.Log
import com.google.firebase.storage.FirebaseStorage
import com.krystianwsul.checkme.MyCrashlytics
import com.krystianwsul.checkme.domainmodel.DomainFactory
import com.krystianwsul.checkme.firebase.DatabaseWrapper
import com.krystianwsul.checkme.firebase.ImageData
import com.krystianwsul.checkme.gui.instances.tree.ImageNode
import com.krystianwsul.checkme.persistencemodel.SaveService
import com.krystianwsul.checkme.utils.TaskKey
import java.io.FileInputStream

object Uploader {

    private val storage = FirebaseStorage.getInstance()
            .getReference("taskImages")
            .child(DatabaseWrapper.root)

    fun addUpload(taskKey: TaskKey, uuid: String, path: String) {
        val task = DomainFactory.instance.getTaskForce(taskKey)

        val imageData = ImageData(uuid, true)
        check(task.image == imageData)

        // todo add to queue

        val stream = FileInputStream(path)

        storage.child(uuid)
                .putStream(stream)
                .addOnFailureListener { MyCrashlytics.logException(it) }
                .addOnSuccessListener {
                    Log.e("asdf", "upload complete")
                    DomainFactory.addFirebaseListener {
                        it.setTaskImageUploaded(SaveService.Source.GUI, taskKey, imageData)
                    }
                }

        // todo delete tmp files
    }

    fun getReference(imageData: ImageNode.Data.Remote) = storage.child(imageData.uuid)
}