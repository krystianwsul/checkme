import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.managers.RootInstanceManager
import com.krystianwsul.common.firebase.models.PrivateProject
import com.krystianwsul.common.firebase.models.Project
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.SharedProject
import com.krystianwsul.common.firebase.records.ProjectRecord
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.utils.ProjectType
import com.krystianwsul.common.utils.TaskKey
import firebase.JsDatabaseWrapper
import firebase.managers.JsPrivateProjectManager
import firebase.managers.JsRootInstanceManager
import firebase.managers.JsRootUserManager
import firebase.managers.JsSharedProjectManager

object RelevanceChecker {

    fun checkRelevance(admin: dynamic, response: MutableList<String>, onComplete: () -> Unit) {
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

            fun <T : ProjectType> getInstances(
                    projectRecord: ProjectRecord<T>,
                    callback: (Map<TaskKey, JsRootInstanceManager<T>>) -> Unit
            ) {
                val results = projectRecord.taskRecords
                        .values
                        .associate { it.taskKey to null as JsRootInstanceManager<T>? }
                        .toMutableMap()

                projectRecord.taskRecords
                        .values
                        .forEach { taskRecord ->
                            databaseWrapper.getInstances(taskRecord.rootInstanceKey) {
                                check(results[taskRecord.taskKey] == null)

                                results[taskRecord.taskKey] = JsRootInstanceManager(
                                        taskRecord,
                                        it.values.flatMap { it.values },
                                        databaseWrapper
                                )

                                if (results.values.none { it == null }) {
                                    callback(results.mapValues { it.value!! })
                                }
                            }
                        }
            }

            var privateData: Pair<JsPrivateProjectManager, Collection<RootInstanceManager<ProjectType.Private>>>? = null
            var sharedData: Triple<JsSharedProjectManager, List<SharedProject>, Collection<RootInstanceManager<ProjectType.Shared>>>? = null

            fun projectCallback() {
                if (privateData == null)
                    return

                if (sharedData == null)
                    return

                fun saveProjects(rootUserManager: JsRootUserManager?) {
                    val values = mutableMapOf<String, Any?>()

                    privateData!!.first.save(values)
                    privateData!!.second.forEach { it.save(values) }
                    sharedData!!.first.save(values)
                    sharedData!!.third.forEach { it.save(values) }
                    rootUserManager?.save(values)

                    databaseWrapper.update(values) { message, _, exception ->
                        ErrorLogger.instance.apply {
                            log(message)
                            exception?.let { logException(it) }
                        }

                        callback(root)
                    }
                }

                if (sharedData!!.second.isEmpty()) {
                    saveProjects(null)
                } else {
                    databaseWrapper.getUsers {
                        val rootUserManager = JsRootUserManager(databaseWrapper, it)

                        val rootUsers = rootUserManager.remoteRootUserRecords
                                .values
                                .map { RootUser(it) }

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

                val privateRootInstanceManagers = privateProjectManager.privateProjectRecords
                        .associate { it.projectKey to null as Collection<RootInstanceManager<ProjectType.Private>>? }
                        .toMutableMap()

                privateProjectManager.privateProjectRecords.forEach { privateProjectRecord ->
                    getInstances(privateProjectRecord) { rootInstanceManagers ->
                        val privateProject = PrivateProject(privateProjectRecord, rootInstanceManagers) {
                            throw UnsupportedOperationException()
                        }

                        response += "checking relevance for private project ${privateProject.projectKey}"

                        Irrelevant.setIrrelevant(
                                object : Project.Parent {

                                    override fun deleteProject(project: Project<*>) = throw UnsupportedOperationException()
                                },
                                privateProject,
                                ExactTimeStamp.now
                        )

                        check(privateRootInstanceManagers.containsKey(privateProject.projectKey))
                        check(privateRootInstanceManagers[privateProject.projectKey] == null)

                        privateRootInstanceManagers[privateProject.projectKey] = rootInstanceManagers.values

                        if (privateRootInstanceManagers.none { it.value == null }) {
                            check(privateData == null)

                            privateData = Pair(
                                    privateProjectManager,
                                    privateRootInstanceManagers.values
                                            .map { it!! }
                                            .flatten()
                            )

                            projectCallback()
                        }
                    }
                }
            }

            databaseWrapper.getSharedProjects {
                val sharedProjectManager = JsSharedProjectManager(databaseWrapper, it)

                val sharedDataInner = sharedProjectManager.sharedProjectRecords
                        .values
                        .associate { it.first.projectKey to null as Pair<SharedProject?, Collection<RootInstanceManager<ProjectType.Shared>>>? }
                        .toMutableMap()

                sharedProjectManager.sharedProjectRecords
                        .values
                        .forEach { (sharedProjectRecord, _) ->
                            getInstances(sharedProjectRecord) { rootInstanceManagers ->
                                val sharedProject = SharedProject(sharedProjectRecord, rootInstanceManagers) {
                                    throw UnsupportedOperationException()
                                }

                                response += "checking relevance for shared project ${sharedProject.projectKey}: ${sharedProject.name}"

                                val removedSharedProjects = Irrelevant.setIrrelevant(
                                        object : Project.Parent {

                                            override fun deleteProject(project: Project<*>) = Unit
                                        },
                                        sharedProject,
                                        ExactTimeStamp.now
                                ).removedSharedProjects
                                check(removedSharedProjects.size < 2)

                                check(sharedDataInner.containsKey(sharedProjectRecord.projectKey))
                                check(sharedDataInner[sharedProjectRecord.projectKey] == null)

                                sharedDataInner[sharedProjectRecord.projectKey] = Pair(
                                        removedSharedProjects.singleOrNull(),
                                        rootInstanceManagers.values
                                )

                                if (sharedDataInner.values.none { it == null }) {
                                    sharedData = Triple(
                                            sharedProjectManager,
                                            sharedDataInner.values.mapNotNull { it!!.first },
                                            sharedDataInner.values
                                                    .map { it!!.second }
                                                    .flatten()
                                    )

                                    projectCallback()
                                }
                            }
                        }
            }
        }
    }
}