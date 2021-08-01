package com.mindorks.scheduler

enum class Priority {

    LOW,
    MEDIUM,
    DB, // we want database reads batched before model reads
    EDIT_SCREEN, // to get EditActivity launched from shortcut initialized before full DomainFactory load
    HIGH,
    IMMEDIATE
}