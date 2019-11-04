import firebase.JsDatabaseWrapper

object RelevanceChecker {

    fun checkRelevance(admin: dynamic) {
        listOf("development", "production").forEach { root ->
            val databaseWrapper = JsDatabaseWrapper(admin, root)


        }
    }
}