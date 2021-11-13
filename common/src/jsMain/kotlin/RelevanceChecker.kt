import com.krystianwsul.common.ErrorLogger
import com.krystianwsul.common.firebase.JsDatabaseWrapper
import com.krystianwsul.common.firebase.json.JsonWrapper
import com.krystianwsul.common.firebase.json.UserWrapper
import com.krystianwsul.common.firebase.json.projects.PrivateProjectJson
import com.krystianwsul.common.firebase.json.tasks.RootTaskJson
import com.krystianwsul.common.firebase.json.tasks.TaskJson
import com.krystianwsul.common.firebase.managers.JsPrivateProjectManager
import com.krystianwsul.common.firebase.managers.JsRootTasksManager
import com.krystianwsul.common.firebase.managers.JsRootUserManager
import com.krystianwsul.common.firebase.managers.JsSharedProjectManager
import com.krystianwsul.common.firebase.models.RootUser
import com.krystianwsul.common.firebase.models.cache.RootModelChangeManager
import com.krystianwsul.common.firebase.models.project.PrivateProject
import com.krystianwsul.common.firebase.models.project.Project
import com.krystianwsul.common.firebase.models.project.SharedProject
import com.krystianwsul.common.firebase.models.task.RootTask
import com.krystianwsul.common.firebase.models.task.Task
import com.krystianwsul.common.firebase.models.taskhierarchy.TaskHierarchy
import com.krystianwsul.common.relevance.CustomTimeRelevance
import com.krystianwsul.common.relevance.Irrelevant
import com.krystianwsul.common.time.ExactTimeStamp
import com.krystianwsul.common.time.JsonTime
import com.krystianwsul.common.time.Time
import com.krystianwsul.common.utils.CustomTimeKey
import com.krystianwsul.common.utils.ProjectKey
import com.krystianwsul.common.utils.TaskKey

object RelevanceChecker {

    fun checkRelevance(
        admin: dynamic,
        response: MutableList<String>,
        updateDatabase: Boolean,
        onComplete: () -> Unit,
        onError: (throwable: Throwable) -> Unit,
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
                try {
                    val userWrapperMap = userWrapperMapTmp ?: return
                    val rootTaskMap = rootTaskMapTmp ?: return
                    val privateProjectMap = privateProjectMapTmp ?: return
                    val sharedProjectMap = sharedProjectMapTmp ?: return

                    val rootUserManager = JsRootUserManager(databaseWrapper, userWrapperMap)

                    val rootUsers = rootUserManager.records.mapValues { RootUser(it.value) }

                    val userCustomTimes = rootUsers.flatMap { it.value.customTimes.values }
                    val userCustomTimeRelevances = userCustomTimes.associate { it.key to CustomTimeRelevance(it) }

                    userCustomTimeRelevances.values
                        .filter { (it.customTime as Time.Custom.User).notDeleted }
                        .forEach { it.setRelevant() }

                    val userCustomTimeProvider = object : JsonTime.UserCustomTimeProvider {

                        override fun getUserCustomTime(userCustomTimeKey: CustomTimeKey.User): Time.Custom.User {
                            return rootUsers.getValue(userCustomTimeKey.userKey).getUserCustomTime(userCustomTimeKey)
                        }

                        override fun tryGetUserCustomTime(userCustomTimeKey: CustomTimeKey.User) =
                            getUserCustomTime(userCustomTimeKey)
                    }

                    val rootTaskManager = JsRootTasksManager(databaseWrapper, rootTaskMap)

                    lateinit var projectMap: Map<ProjectKey<*>, Project<*>>
                    lateinit var rootTasksByTaskKey: MutableMap<TaskKey.Root, RootTask>
                    lateinit var rootTasksByProjectId: MutableMap<String, MutableSet<RootTask>>

                    val rootTaskParent = object : RootTask.Parent {

                        override val rootModelChangeManager = RootModelChangeManager()

                        override fun createTask(
                            now: ExactTimeStamp.Local,
                            image: TaskJson.Image?,
                            name: String,
                            note: String?,
                            ordinal: Double?
                        ): RootTask {
                            throw UnsupportedOperationException()
                        }

                        override fun updateProjectRecord(
                            projectKey: ProjectKey<*>,
                            dependentRootTaskKeys: Set<TaskKey.Root>
                        ) {
                            // this is just for loading
                        }

                        override fun updateTaskRecord(taskKey: TaskKey.Root, dependentRootTaskKeys: Set<TaskKey.Root>) {
                            // this is just for loading
                        }

                        override fun getProject(projectId: String): Project<*> {
                            return projectMap.entries
                                .single { it.key.key == projectId }
                                .value
                        }

                        override fun getRootTask(rootTaskKey: TaskKey.Root) = rootTasksByTaskKey.getValue(rootTaskKey)

                        override fun tryGetRootTask(rootTaskKey: TaskKey.Root) = getRootTask(rootTaskKey)

                        override fun getTask(taskKey: TaskKey): Task {
                            return when (taskKey) {
                                is TaskKey.Root -> getRootTask(taskKey)
                                is TaskKey.Project -> projectMap.getValue(taskKey.projectKey)
                                    .getProjectTaskForce(taskKey)
                            }
                        }

                        override fun getRootTasksForProject(projectKey: ProjectKey<*>): Collection<RootTask> {
                            return rootTasksByProjectId[projectKey.key].orEmpty()
                        }

                        override fun getTaskHierarchiesByParentTaskKey(parentTaskKey: TaskKey): Set<TaskHierarchy> {
                            return rootTasksByTaskKey.flatMap { it.value.nestedParentTaskHierarchies.values }
                                .filter { it.parentTaskKey == parentTaskKey }
                                .toSet()
                        }

                        override fun deleteRootTask(task: RootTask) {
                            rootTasksByTaskKey.remove(task.taskKey)

                            rootTasksByProjectId.values.forEach { it.remove(task) }
                        }

                        override fun getAllExistingInstances() = projectMap.values
                            .asSequence()
                            .flatMap { it.getAllTasks() }
                            .flatMap { it.existingInstances.values }
                    }

                    rootTasksByTaskKey = rootTaskManager.records
                        .map { RootTask(it.value, rootTaskParent, userCustomTimeProvider) }
                        .associateBy { it.taskKey }
                        .toMutableMap()

                    rootTasksByProjectId = rootTasksByTaskKey.values
                        .groupBy { it.projectId }
                        .mapValues { it.value.toMutableSet() }
                        .toMutableMap()

                    val privateProjectManager = JsPrivateProjectManager(databaseWrapper, privateProjectMap)

                    val sharedProjectManager = JsSharedProjectManager(databaseWrapper, sharedProjectMap)

                    val privateProjects = privateProjectManager.value
                        .map {
                            PrivateProject(
                                it,
                                userCustomTimeProvider,
                                rootTaskParent,
                                rootTaskParent.rootModelChangeManager
                            )
                        }
                        .associateBy { it.projectKey }

                    val sharedProjects = sharedProjectManager.records
                        .values
                        .map {
                            SharedProject(
                                it,
                                userCustomTimeProvider,
                                rootTaskParent,
                                rootTaskParent.rootModelChangeManager
                            )
                        }
                        .associateBy { it.projectKey }

                    projectMap = privateProjects + sharedProjects

                    checkInconsistentRootTaskIds(rootTasksByTaskKey.values, projectMap.values)

                    val removedSharedProjects = Irrelevant.setIrrelevant(
                        { rootTasksByTaskKey },
                        userCustomTimeRelevances,
                        { projectMap },
                        rootTaskParent,
                        ExactTimeStamp.Local.now,
                    ).removedSharedProjects

                    userCustomTimeRelevances.values
                        .filter { !it.relevant }
                        .forEach { (it.customTime as Time.Custom.User).delete() }

                    val removedSharedProjectKeys = removedSharedProjects.map { it.projectKey }

                    rootUsers.values.forEach { removedSharedProjectKeys.forEach(it::removeProject) }

                    projectMap.values.forEach { project ->
                        project.projectRecord
                            .rootTaskParentDelegate
                            .rootTaskKeys
                            .forEach {
                                val task = rootTasksByTaskKey[it] ?: throw MissingTaskException(it, project.projectKey)

                                if (task.deleted) throw DeletedTaskException(it, project.projectKey)
                            }
                    }

                    rootTaskManager.records
                        .values
                        .filter { it.shouldDelete }
                        .map { it.taskKey }
                        .forEach { taskKey ->
                            projectMap.values.forEach { project ->
                                if (project.projectRecord.rootTaskParentDelegate.rootTaskKeys.contains(taskKey))
                                    throw DeletedRecordStillPresentException(taskKey, project.projectKey)
                            }
                        }

                    val values = mutableMapOf<String, Any?>()

                    privateProjectManager.save(values)
                    sharedProjectManager.save(values)
                    rootUserManager.save(values)
                    rootTaskManager.save(values)

                    ErrorLogger.instance.log("updateDatabase: $updateDatabase")
                    ErrorLogger.instance.log("all database values: ${values.entries.joinToString("<br>\n")}")

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
                } catch (throwable: Throwable) {
                    onError(throwable)
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

    private class InconsistentRootTaskIdsException(pairs: List<Triple<TaskKey.Root, ProjectKey<*>, Set<ProjectKey<*>>>>) :
        Exception(
            "rootTaskIds in wrong projects:\n" +
                    pairs.joinToString(";\n") { "${it.first} says it belongs in project ${it.second}, but was found in ${it.third}" }
        )

    private class MissingTaskException(taskKey: TaskKey.Root, projectKey: ProjectKey<*>) :
        Exception("rootTaskId $taskKey from $projectKey is missing from map")

    private class DeletedTaskException(taskKey: TaskKey.Root, projectKey: ProjectKey<*>) :
        Exception("rootTaskId $taskKey from $projectKey is deleted")

    private class DeletedRecordStillPresentException(taskKey: TaskKey.Root, projectKey: ProjectKey<*>) :
        Exception("deleted task $taskKey still present in $projectKey")
}