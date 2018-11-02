package com.krystianwsul.checkme.domainmodel;

import android.annotation.SuppressLint;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.RemoteFriendFactory;
import com.krystianwsul.checkme.firebase.RemoteProject;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.Set;

@SuppressLint("UseSparseArrays")
public class DomainFactory {

    private final KotlinDomainFactory kotlinDomainFactory;

    private static void check(boolean value) {
        if (!value) throw new IllegalStateException();
    }

    DomainFactory(@NonNull KotlinDomainFactory kotlinDomainFactory) {
        this.kotlinDomainFactory = kotlinDomainFactory;
    }

    // misc

    // firebase

    // gets

    // sets

    @NonNull
    public synchronized TaskKey updateRootTask(int dataId, @NonNull SaveService.Source source, @NonNull TaskKey taskKey, @NonNull String name, @Nullable String note, @Nullable String projectId) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateRootTask");
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        check(!TextUtils.isEmpty(name));

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Task task = kotlinDomainFactory.getTaskForce(taskKey);
        check(task.current(now));

        task = task.updateProject(now, projectId);

        task.setName(name, note);

        TaskHierarchy taskHierarchy = kotlinDomainFactory.getParentTaskHierarchy(task, now);
        if (taskHierarchy != null)
            taskHierarchy.setEndExactTimeStamp(now);

        Stream.of(task.getCurrentSchedules(now))
                .forEach(schedule -> schedule.setEndExactTimeStamp(now));

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(task.getRemoteNullableProject());

        return task.getTaskKey();
    }

    @NonNull
    Irrelevant updateNotificationsTick(@NonNull ExactTimeStamp now, @NonNull SaveService.Source source, boolean silent) {
        kotlinDomainFactory.updateNotifications(silent, now, new ArrayList<>());

        Irrelevant irrelevant = kotlinDomainFactory.setIrrelevant(now);

        if (kotlinDomainFactory.getRemoteProjectFactory() != null)
            kotlinDomainFactory.localFactory.deleteInstanceShownRecords(kotlinDomainFactory.getRemoteProjectFactory().getTaskKeys());

        kotlinDomainFactory.save(0, source);

        return irrelevant;
    }

    public synchronized void updateNotificationsTick(@NonNull SaveService.Source source, boolean silent, @NonNull String sourceName) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateNotificationsTick source: " + sourceName);
        check(kotlinDomainFactory.getRemoteProjectFactory() == null || !kotlinDomainFactory.getRemoteProjectFactory().isSaved());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        updateNotificationsTick(now, source, silent);
    }

    public synchronized void removeFriends(@NonNull Set<String> keys) {
        MyCrashlytics.INSTANCE.log("DomainFactory.removeFriends");

        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(RemoteFriendFactory.Companion.hasFriends());
        check(!RemoteFriendFactory.Companion.isSaved());

        Stream.of(keys).forEach(friendId -> RemoteFriendFactory.Companion.removeFriend(kotlinDomainFactory.getUserInfo().getKey(), friendId));

        RemoteFriendFactory.Companion.save();
    }

    public synchronized void updateUserInfo(@NonNull SaveService.Source source, @NonNull UserInfo userInfo) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateUserInfo");
        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);

        if (kotlinDomainFactory.getUserInfo().equals(userInfo))
            return;

        kotlinDomainFactory.setUserInfo(userInfo);
        DatabaseWrapper.INSTANCE.setUserInfo(userInfo, kotlinDomainFactory.localFactory.getUuid());

        kotlinDomainFactory.getRemoteProjectFactory().updateUserInfo(userInfo);

        kotlinDomainFactory.save(0, source);
    }

    public synchronized void updateProject(int dataId, @NonNull SaveService.Source source, @NonNull String projectId, @NonNull String name, @NonNull Set<String> addedFriends, @NonNull Set<String> removedFriends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.updateProject");

        check(!TextUtils.isEmpty(projectId));
        check(!TextUtils.isEmpty(name));
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(RemoteFriendFactory.Companion.hasFriends());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().getRemoteProjectForce(projectId);

        remoteProject.setName(name);
        remoteProject.updateRecordOf(Stream.of(addedFriends).map(RemoteFriendFactory.Companion::getFriend)
                .collect(Collectors.toSet()), removedFriends);

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProject, removedFriends);
    }

    public synchronized void createProject(int dataId, @NonNull SaveService.Source source, @NonNull String name, @NonNull Set<String> friends) {
        MyCrashlytics.INSTANCE.log("DomainFactory.createProject");

        check(!TextUtils.isEmpty(name));
        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(kotlinDomainFactory.getRemoteRootUser() != null);

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<String> recordOf = new HashSet<>(friends);

        String key = kotlinDomainFactory.getUserInfo().getKey();
        check(!recordOf.contains(key));
        recordOf.add(key);

        RemoteProject remoteProject = kotlinDomainFactory.getRemoteProjectFactory().createRemoteProject(name, now, recordOf, kotlinDomainFactory.getRemoteRootUser());

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProject);
    }

    public synchronized void setProjectEndTimeStamps(int dataId, @NonNull SaveService.Source source, @NonNull Set<String> projectIds) {
        MyCrashlytics.INSTANCE.log("DomainFactory.setProjectEndTimeStamps");

        check(kotlinDomainFactory.getRemoteProjectFactory() != null);
        check(kotlinDomainFactory.getUserInfo() != null);
        check(!projectIds.isEmpty());

        ExactTimeStamp now = ExactTimeStamp.Companion.getNow();

        Set<RemoteProject> remoteProjects = Stream.of(projectIds).map(kotlinDomainFactory.getRemoteProjectFactory()::getRemoteProjectForce)
                .collect(Collectors.toSet());

        check(Stream.of(remoteProjects)
                .allMatch(remoteProject -> remoteProject.current(now)));

        Stream.of(remoteProjects)
                .forEach(remoteProject -> remoteProject.setEndExactTimeStamp(now));

        kotlinDomainFactory.updateNotifications(now);

        kotlinDomainFactory.save(dataId, source);

        kotlinDomainFactory.notifyCloud(remoteProjects);
    }

    // internal
}