package com.mindorks.scheduler;

import androidx.annotation.NonNull;

import com.mindorks.scheduler.internal.CustomPriorityScheduler;

import io.reactivex.rxjava3.core.Scheduler;

public final class RxPS {

    private static RxPS instance;
    private static Scheduler mockScheduler;

    private CustomPriorityScheduler priorityScheduler;

    private RxPS() {
        this.priorityScheduler = CustomPriorityScheduler.create();
    }

    private static RxPS getInstance() {
        if (instance == null) {
            synchronized (RxPS.class) {
                if (instance == null) {
                    instance = new RxPS();
                }
            }
        }
        return instance;
    }

    private CustomPriorityScheduler getPriorityScheduler() {
        return priorityScheduler;
    }

    public static Scheduler get(Priority priority) {
        if (mockScheduler != null) return mockScheduler;

        return getInstance().getPriorityScheduler().get(priority);
    }

    public static Scheduler low() {
        return get(Priority.LOW);
    }

    public static Scheduler medium() {
        return get(Priority.MEDIUM);
    }

    public static Scheduler high() {
        return get(Priority.HIGH);
    }

    public static Scheduler immediate() {
        return get(Priority.IMMEDIATE);
    }

    public static void setScheduler(@NonNull Scheduler scheduler) {
        mockScheduler = scheduler;
    }
}