import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.JsDatabaseWrapper
import com.krystianwsul.common.firebase.managers.JsPrivateProjectManager
import com.krystianwsul.common.firebase.managers.JsRootUserManager
import com.krystianwsul.common.firebase.managers.JsSharedProjectManager
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey

object RelevanceChecker {

    fun checkRelevance(
            admin: dynamic,
            response: MutableList<String>,
            updateDatabase: Boolean,
            onComplete: () -> Unit,
    ) {
        val roots = listOf("development", "production")

        val completed = roots.associateWith { false }.toMutableMap()

        fun callback(root: String) {
            check(!completed.getValue(root))

            completed[root] = true

            ErrorLogger.instance.log(completed.toString())

            if (completed.values.all { it }) {
                ErrorLogger.instance.log("calling onComplete")

                onComplete()
            }
        }

        roots.forEach { root ->
            val databaseWrapper = JsDatabaseWrapper(admin, root)

            var privateData: JsPrivateProjectManager? = null
            var sharedData: Pair<JsSharedProjectManager, List<SharedProject>>? = null

            fun projectCallback() {
                if (privateData == null)
                    return

                if (sharedData == null)
                    return

                fun saveProjects(rootUserManager: JsRootUserManager?) {
                    val values = mutableMapOf<String, Any?>()

                    privateData!!.save(values)
                    sharedData!!.first.save(values)
                    rootUserManager?.save(values)

                    ErrorLogger.instance.log("updateDatabase: $updateDatabase")
                    ErrorLogger.instance.log(
                            "all database values: ${
                                values.entries.joinToString(
                                        "<br>\n"
                                )
                            }"
                    )
                    if (updateDatabase) {
                        databaseWrapper.update(values) { message, _, exception ->
                            ErrorLogger.instance.apply {
                                log(message)
                                exception?.let { logException(it) }
                            }

                            callback(root)
                        }
                    } else {
                        callback(root)
                    }
                }

                if (sharedData!!.second.isEmpty()) {
                    saveProjects(null)
                } else {
                    databaseWrapper.getUsers {
                        val rootUserManager = JsRootUserManager(databaseWrapper, it)

                        val rootUsers = rootUserManager.records
                                .values
                                .map { RootUser(it.value) }

                        val removedSharedProjectKeys = sharedData!!.second.map { it.projectKey }

                        rootUsers.forEach { remoteUser ->
                            removedSharedProjectKeys.forEach {
                                remoteUser.removeProject(it)
                            }
                        }

                        saveProjects(rootUserManager)
                    }
                }
            }

            databaseWrapper.getPrivateProjects {
                val privateProjectManager = JsPrivateProjectManager(databaseWrapper, it)

                privateProjectManager.value.forEach { privateProjectRecord ->
                    val privateProject = PrivateProject(
                            privateProjectRecord,
                            object : JsonTime.UserCustomTimeProvider {

                                override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                                    TODO("todo customtimes fetch")
                                }
                            },
                    )

                    response += "checking relevance for private project ${privateProject.projectKey}"

                    Irrelevant.setIrrelevant(
                            object : Project.Parent {

                                override fun deleteProject(project: Project<*>) = throw UnsupportedOperationException()
                            },
                            privateProject,
                            ExactTimeStamp.Local.now
                    )

                    check(privateData == null)

                    privateData = privateProjectManager
                    projectCallback()
                }
            }

            databaseWrapper.getSharedProjects {
                val sharedProjectManager = JsSharedProjectManager(databaseWrapper, it)

                val sharedDataInner = sharedProjectManager.records
                        .values
                        .map { sharedProjectRecord ->
                            val sharedProject = SharedProject(
                                    sharedProjectRecord,
                                    object : JsonTime.UserCustomTimeProvider {

                                        override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                                            TODO("todo customtimes fetch")
                                        }
                                    },
                            )

                            response += "checking relevance for shared project ${sharedProject.projectKey}: ${sharedProject.name}"

                            val removedSharedProjects = Irrelevant.setIrrelevant(
                                    object : Project.Parent {

                                        override fun deleteProject(project: Project<*>) = Unit
                                    },
                                    sharedProject,
                                    ExactTimeStamp.Local.now
                            ).removedSharedProjects
                            check(removedSharedProjects.size < 2)

                            removedSharedProjects.singleOrNull()
                        }

                sharedData = Pair(
                        sharedProjectManager,
                        sharedDataInner.filterNotNull(),
                )

                projectCallback()
            }
        }
    }
}