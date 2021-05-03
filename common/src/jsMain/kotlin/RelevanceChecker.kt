import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.JsDatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
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

            var userWrapperMapTmp: Map<String, UserWrapper>? = null
            var rootTaskMapTmp: Map<String, RootTaskJson>? = null
            var privateProjectMapTmp: Map<String, PrivateProjectJson>? = null
            var sharedProjectMapTmp: Map<String, JsonWrapper>? = null

            fun proceed() {
                val userWrapperMap = userWrapperMapTmp ?: return
                val rootTaskMap = rootTaskMapTmp ?: return
                val privateProjectMap = privateProjectMapTmp ?: return
                val sharedProjectMap = sharedProjectMapTmp ?: return

                val rootUserManager = JsRootUserManager(databaseWrapper, userWrapperMap)

                val rootUsers = rootUserManager.records.mapValues { RootUser(it.value) }

                val userCustomTimes = rootUsers.flatMap { it.value.customTimes.values }
                val userCustomTimeRelevances = userCustomTimes.associate { it.key to CustomTimeRelevance(it) }

                userCustomTimeRelevances.values
                        .filter { (it.customTime as Time.Custom.User).notDeleted(ExactTimeStamp.Local.now) }
                        .forEach { it.setRelevant() }

                val userCustomTimeProvider = object : JsonTime.UserCustomTimeProvider {

                    override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                        return rootUsers.getValue(userCustomTimeKey.userKey).getUserCustomTime(userCustomTimeKey)
                    }
                }

                val privateProjectManager = JsPrivateProjectManager(databaseWrapper, privateProjectMap)

                val sharedProjectManager = JsSharedProjectManager(databaseWrapper, sharedProjectMap)

                val privateProjects = privateProjectManager.value
                        .map { PrivateProject(it, userCustomTimeProvider) }
                        .associateBy { it.projectKey }

                val sharedProjects = sharedProjectManager.records
                        .values
                        .map { SharedProject(it, userCustomTimeProvider) }
                        .associateBy { it.projectKey }

                privateProjects.values.forEach { privateProject ->
                    response += "checking relevance for private project ${privateProject.projectKey}"

                    Irrelevant.setIrrelevant(
                            userCustomTimeRelevances,
                            privateProject,
                            ExactTimeStamp.Local.now,
                    )
                }

                val removedSharedProjects = sharedProjects.values.map { sharedProject ->
                    response += "checking relevance for shared project ${sharedProject.projectKey}: ${sharedProject.name}"

                    val removedSharedProjects = Irrelevant.setIrrelevant(
                            userCustomTimeRelevances,
                            sharedProject,
                            ExactTimeStamp.Local.now,
                    ).removedSharedProjects
                    check(removedSharedProjects.size < 2)

                    removedSharedProjects.singleOrNull()
                }
                        .filterNotNull()

                userCustomTimeRelevances.values
                        .filter { !it.relevant }
                        .forEach { (it.customTime as Time.Custom.User).delete() }

                val removedSharedProjectKeys = removedSharedProjects.map { it.projectKey }

                rootUsers.values.forEach { remoteUser ->
                    removedSharedProjectKeys.forEach {
                        remoteUser.removeProject(it)
                    }
                }

                val values = mutableMapOf<String, Any?>()

                privateProjectManager.save(values)
                sharedProjectManager.save(values)
                rootUserManager.save(values)
                // todo task relevance save tasks

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

            databaseWrapper.getUsers {
                userWrapperMapTmp = it

                proceed()
            }

            databaseWrapper.getRootTasks {
                rootTaskMapTmp = it

                proceed()
            }

            databaseWrapper.getPrivateProjects {
                privateProjectMapTmp = it

                proceed()
            }

            databaseWrapper.getSharedProjects {
                sharedProjectMapTmp = it

                proceed()
            }
        }
    }
}