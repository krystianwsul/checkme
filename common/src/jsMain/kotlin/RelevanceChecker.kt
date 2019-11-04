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

            var privateProjects: List<RemotePrivateProject>? = null
            var sharedProjects: MutableMap<String, RemoteSharedProject>? = null

            fun callback() {
                @Suppress("NAME_SHADOWING")
                val privateProjects = privateProjects ?: return
                @Suppress("NAME_SHADOWING")
                val sharedProjects = sharedProjects ?: return

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

                sharedProjects.values.forEach { project -> Irrelevant.setIrrelevant(parent, project, now) }

                onComplete()
            }

            databaseWrapper.getPrivateProjects {
                val privateProjectManager = JsPrivateProjectManager(databaseWrapper, it)

                privateProjects = privateProjectManager.remotePrivateProjectRecords.map {
                    RemotePrivateProject(it)
                }

                callback()
            }

            databaseWrapper.getSharedProjects {
                val sharedProjectManager = JsSharedProjectManager(databaseWrapper, it)

                sharedProjects = sharedProjectManager.remoteProjectRecords
                        .entries
                        .associate { it.key to RemoteSharedProject(it.value) }
                        .toMutableMap()

                callback()
            }
        }
    }
}