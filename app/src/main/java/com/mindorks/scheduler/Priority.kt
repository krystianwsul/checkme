package com.mindorks.scheduler

enum class Priority {

    LOW,
    MEDIUM,
    DB_NOTES, // we want database reads batched before model reads
    FIRST_READ, // to shove DomainListener into queue before DB for first load
    DB_NOTIFICATION_STORAGE,
    DB_TASKS, // we want database reads batched before model reads
    DB, // before rootTasks
    EDIT_SCREEN, // to get EditActivity launched from shortcut initialized before full DomainFactory load
    HIGH,
    IMMEDIATE
}