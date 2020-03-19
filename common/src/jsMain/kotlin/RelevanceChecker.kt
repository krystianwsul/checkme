import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.ExactTimeStamp
import firebase.JsDatabaseWrapper
import firebase.managers.JsPrivateProjectManager
import firebase.managers.JsRootUserManager
import firebase.managers.JsSharedProjectManager

object RelevanceChecker {

    private enum class Branch {

        PRIVATE, SHARED, USER
    }

    fun checkRelevance(admin: dynamic, response: MutableList<String>, onComplete: () -> Unit) {
        val roots = listOf("development", "production")

        val completed = roots.map { root ->
                    Branch.values().map { root to it }
                }
                .flatten()
                .associateWith { false }
                .toMutableMap()

        fun callback(root: String, branch: Branch) {
            val key = root to branch
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
                    PrivateProject(it)
                }

                val now = ExactTimeStamp.now

                privateProjects.forEach {
                    response += "checking relevance for private project ${it.id}"

                    Irrelevant.setIrrelevant(
                            object : Project.Parent {

                                override fun deleteProject(project: Project<*>) = throw UnsupportedOperationException()
                            },
                            it,
                            now
                    )
                }

                privateProjectManager.apply {
                    saveCallback = { callback(root, Branch.PRIVATE) }

                    save(Unit)
                }
            }

            databaseWrapper.getSharedProjects {
                val sharedProjectManager = JsSharedProjectManager(databaseWrapper, it)

                val sharedProjects = sharedProjectManager.remoteProjectRecords
                        .entries
                        .associate { it.key to SharedProject(it.value.first) }
                        .toMutableMap()

                val now = ExactTimeStamp.now

                val parent = object : Project.Parent {

                    override fun deleteProject(project: Project<*>) {
                        check(sharedProjects.contains(project.id))

                        sharedProjects.remove(project.id)
                    }
                }

                val sharedProjectsRemoved = sharedProjects.values
                        .map {
                            response += "checking relevance for shared project ${it.id}: ${it.name}"

                            Irrelevant.setIrrelevant(parent, it, now).removedSharedProjects
                        }
                        .flatten()

                if (sharedProjectsRemoved.isNotEmpty()) {
                    databaseWrapper.getUsers {
                        val rootUserManager = JsRootUserManager(databaseWrapper, it)

                        val rootUsers = rootUserManager.remoteRootUserRecords
                                .values
                                .map { RootUser(it) }

                        val removedSharedProjectKeys = sharedProjectsRemoved.map { it.id }

                        rootUsers.forEach { remoteUser ->
                            removedSharedProjectKeys.forEach {
                                remoteUser.removeProject(it)
                            }
                        }

                        rootUserManager.apply {
                            saveCallback = { callback(root, Branch.USER) }
                            save()
                        }
                    }
                } else {
                    callback(root, Branch.USER)
                }

                sharedProjectManager.apply {
                    saveCallback = { callback(root, Branch.SHARED) }

                    save(Unit)
                }
            }
        }
    }
}