package com.mindorks.scheduler

enum class Priority {

    LOW,
    MEDIUM,
    DB_TASKS, // we want database reads batched before model reads
    DB, // before rootTasks
    FIRST_READ, // to shove DomainListener into queue before DB for first load
    EDIT_SCREEN, // to get EditActivity launched from shortcut initialized before full DomainFactory load
    HIGH,
    IMMEDIATE
}