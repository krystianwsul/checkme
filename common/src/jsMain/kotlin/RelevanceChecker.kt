import com.krystianwsul.common.ErrorLogger
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
        val roots = listOf("development", "production")

        val completed = roots.map {
                    listOf(
                            it to true,
                            it to false
                    )
                }
                .flatten()
                .associateWith { false }
                .toMutableMap()

        fun callback(root: String, private: Boolean) {
            val key = root to private
            check(!completed.getValue(key))

            completed[key] = true

            ErrorLogger.instance.log(completed.toString())

            if (completed.values.all { it }) {
                ErrorLogger.instance.log("calling onComplete")

                onComplete()
            }
        }

        roots.forEach { root ->
            val databaseWrapper = JsDatabaseWrapper(admin, root)

            databaseWrapper.getPrivateProjects {
                val privateProjectManager = JsPrivateProjectManager(databaseWrapper, it)

                val privateProjects = privateProjectManager.remotePrivateProjectRecords.map {
                    RemotePrivateProject(it)
                }

                val now = ExactTimeStamp.now

                privateProjects.forEach {
                    response += "checking relevance for private project ${it.id}"

                    Irrelevant.setIrrelevant(
                            object : RemoteProject.Parent {

                                override fun deleteProject(remoteProject: RemoteProject<*, *>) = throw UnsupportedOperationException()
                            },
                            it,
                            now
                    )
                }

                privateProjectManager.apply {
                    saveCallback = { callback(root, true) }

                    save(Unit)
                }
            }

            databaseWrapper.getSharedProjects {
                val sharedProjectManager = JsSharedProjectManager(databaseWrapper, it)

                val sharedProjects = sharedProjectManager.remoteProjectRecords
                        .entries
                        .associate { it.key to RemoteSharedProject(it.value.first) }
                        .toMutableMap()

                val now = ExactTimeStamp.now

                val parent = object : RemoteProject.Parent {

                    override fun deleteProject(remoteProject: RemoteProject<*, *>) {
                        check(sharedProjects.contains(remoteProject.id))

                        sharedProjects.remove(remoteProject.id)
                    }
                }

                val sharedProjectsRemoved = sharedProjects.values
                        .map {
                            response += "checking relevance for shared project ${it.id}: ${it.name}"

                            Irrelevant.setIrrelevant(parent, it, now).removedSharedProjects
                        }
                        .flatten()

                if (sharedProjectsRemoved.isNotEmpty()) {
                    // todo project remove from sharedProjectManager, remove keys from users
                }

                sharedProjectManager.apply {
                    saveCallback = { callback(root, false) }

                    save(Unit)
                }
            }
        }
    }
}