package com.krystianwsul.checkme.firebase.database

import com.mindorks.scheduler.Priority

enum class DatabaseReadPriority(val schedulerPriority: Priority) {

    NOTES(Priority.DB_NOTES),
    LATER_INSTANCES(Priority.DB_LATER_INSTANCES),
    TODAY_INSTANCES(Priority.DB_TASKS),
    NORMAL(Priority.DB)
}