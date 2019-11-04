import com.krystianwsul.common.firebase.models.RemotePrivateProject
import com.krystianwsul.common.firebase.models.RemoteProject
import com.krystianwsul.common.firebase.models.RemoteSharedProject
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.ExactTimeStamp
import firebase.JsDatabaseWrapper
import firebase.managers.JsPrivateProjectManager
import firebase.managers.JsSharedProjectManager

object RelevanceChecker {

    fun checkRelevance(admin: dynamic, response: MutableList<String>, onComplete: () -> Unit) {
        listOf("development"/*, "production"*/).forEach { root ->
            // todo production
            val databaseWrapper = JsDatabaseWrapper(admin, root)

            var privateProjectManager: JsPrivateProjectManager? = null
            var sharedProjectManager: JsSharedProjectManager? = null

            fun callback() {
                if (privateProjectManager == null || sharedProjectManager == null)
                    return

                val privateProjects = privateProjectManager!!.remotePrivateProjectRecords.map {
                    RemotePrivateProject(it)
                }

                val sharedProjects = sharedProjectManager!!.remoteProjectRecords
                        .entries
                        .associate { it.key to RemoteSharedProject(it.value) }
                        .toMutableMap()

                val now = ExactTimeStamp.now

                privateProjects.forEach {
                    response += "checking relevance for private project ${it.id}"

                    Irrelevant.setIrrelevant(
                            object : RemoteProject.Parent {

                                override fun deleteProject(remoteProject: RemoteProject<*>) = throw UnsupportedOperationException()
                            },
                            it,
                            now
                    )
                }

                val parent = object : RemoteProject.Parent {

                    override fun deleteProject(remoteProject: RemoteProject<*>) {
                        check(sharedProjects.contains(remoteProject.id))

                        sharedProjects.remove(remoteProject.id)
                    }
                }

                sharedProjects.values.forEach {
                    response += "checking relevance for shared project ${it.id}: ${it.name}"

                    Irrelevant.setIrrelevant(parent, it, now)
                }

                var privateSaved = false
                var sharedSaved = false

                privateProjectManager!!.apply {
                    saveCallback = {
                        privateSaved = true
                        if (sharedSaved)
                            onComplete()
                    }

                    save()
                }

                sharedProjectManager!!.apply {
                    saveCallback = {
                        sharedSaved = true
                        if (privateSaved)
                            onComplete()
                    }

                    save()
                }
            }

            databaseWrapper.getPrivateProjects {
                privateProjectManager = JsPrivateProjectManager(databaseWrapper, it)

                callback()
            }

            databaseWrapper.getSharedProjects {
                sharedProjectManager = JsSharedProjectManager(databaseWrapper, it)

                callback()
            }
        }
    }
}