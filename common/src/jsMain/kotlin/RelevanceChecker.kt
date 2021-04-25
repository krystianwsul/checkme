import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.JsDatabaseWrapper
import com.krystianwsul.common.firebase.managers.JsPrivateProjectManager
import com.krystianwsul.common.firebase.managers.JsRootUserManager
import com.krystianwsul.common.firebase.managers.JsSharedProjectManager
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.relevance.CustomTimeRelevance
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

            databaseWrapper.getUsers { userWrapperMap ->
                val rootUserManager = JsRootUserManager(databaseWrapper, userWrapperMap)

                val rootUsers = rootUserManager.records.mapValues { RootUser(it.value) }

                val userCustomTimes = rootUsers.flatMap { it.value.customTimes.values }
                val userCustomTimeRelevances = userCustomTimes.associate { it.key to CustomTimeRelevance(it) }

                userCustomTimeRelevances.values
                        .filter { (it.customTime as Time.Custom.User).notDeleted(ExactTimeStamp.Local.now) }
                        .forEach { it.setRelevant() }

                var privateData: JsPrivateProjectManager? = null
                var sharedData: Pair<JsSharedProjectManager, List<SharedProject>>? = null

                fun projectCallback() {
                    if (privateData == null) return

                    if (sharedData == null) return

                    fun saveProjects() {
                        val values = mutableMapOf<String, Any?>()

                        privateData!!.save(values)
                        sharedData!!.first.save(values)
                        rootUserManager.save(values)

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

                    userCustomTimeRelevances.values
                            .filter { !it.relevant }
                            .forEach { (it.customTime as Time.Custom.User).delete() }

                    if (sharedData!!.second.isEmpty()) {
                        saveProjects()
                    } else {
                        val removedSharedProjectKeys = sharedData!!.second.map { it.projectKey }

                        rootUsers.values.forEach { remoteUser ->
                            removedSharedProjectKeys.forEach {
                                remoteUser.removeProject(it)
                            }
                        }

                        saveProjects()
                    }
                }

                val userCustomTimeProvider = object : JsonTime.UserCustomTimeProvider {

                    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                        return rootUsers.getValue(userCustomTimeKey.userKey).getUserCustomTime(userCustomTimeKey)
                    }
                }

                databaseWrapper.getPrivateProjects {
                    val privateProjectManager = JsPrivateProjectManager(databaseWrapper, it)

                    privateProjectManager.value.forEach { privateProjectRecord ->
                        val privateProject = PrivateProject(privateProjectRecord, userCustomTimeProvider)

                        response += "checking relevance for private project ${privateProject.projectKey}"

                        Irrelevant.setIrrelevant(
                                userCustomTimeRelevances,
                                privateProject,
                                ExactTimeStamp.Local.now,
                        )
                    }

                    check(privateData == null)

                    privateData = privateProjectManager
                    projectCallback()
                }

                databaseWrapper.getSharedProjects {
                    val sharedProjectManager = JsSharedProjectManager(databaseWrapper, it)

                    val sharedDataInner = sharedProjectManager.records
                            .values
                            .map { sharedProjectRecord ->
                                val sharedProject = SharedProject(sharedProjectRecord, userCustomTimeProvider)

                                response += "checking relevance for shared project ${sharedProject.projectKey}: ${sharedProject.name}"

                                val removedSharedProjects = Irrelevant.setIrrelevant(
                                        userCustomTimeRelevances,
                                        sharedProject,
                                        ExactTimeStamp.Local.now,
                                ).removedSharedProjects
                                check(removedSharedProjects.size < 2)

                                removedSharedProjects.singleOrNull()
                            }

                    sharedData = Pair(sharedProjectManager, sharedDataInner.filterNotNull())

                    projectCallback()
                }
            }
        }
    }
}