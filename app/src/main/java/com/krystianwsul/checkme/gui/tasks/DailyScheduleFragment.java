package com.krystianwsul.checkme.gui.tasks;

import android.os.Bundle;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.List;

public class DailyScheduleFragment extends RepeatingScheduleFragment implements ScheduleFragment {
    public static DailyScheduleFragment newInstance() {
        return new DailyScheduleFragment();
    }

    public static DailyScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);

        dailyScheduleFragment.setArguments(args);
        return dailyScheduleFragment;
    }

    public static DailyScheduleFragment newInstance(int rootTaskId) {
        DailyScheduleFragment dailyScheduleFragment = new DailyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        dailyScheduleFragment.setArguments(args);
        return dailyScheduleFragment;
    }

    @Override
    protected ScheduleEntry firstScheduleEntry(boolean showDelete) {
        if (mScheduleHint != null)
            return new DailyScheduleEntry(mScheduleHint.mTimePair, showDelete);
        else
            return new DailyScheduleEntry(showDelete);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("DailyScheduleFragment.onResume");

        super.onResume();
    }

    private List<TimePair> getTimePairs() {
        Assert.assertTrue(mScheduleEntries != null);
        Assert.assertTrue(!mScheduleEntries.isEmpty());

        return Stream.of(mScheduleEntries)
                .map(timeEntry -> ((DailyScheduleEntry) timeEntry).mTimePairPersist.getTimePair())
                .collect(Collectors.toList());
    }

    @Override
    public boolean createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        Assert.assertTrue(mRootTaskId == null);

        if (mData == null)
            return false;

        List<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createDailyScheduleRootTask(mData.DataId, name, timePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        List<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateDailyScheduleTask(mData.DataId, rootTaskId, name, timePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean createRootJoinTask(String name, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Assert.assertTrue(mRootTaskId == null);

        if (mData == null)
            return false;

        List<TimePair> timePairs = getTimePairs();
        Assert.assertTrue(!timePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createDailyScheduleJoinRootTask(mData.DataId, name, timePairs, joinTaskIds);

        TickService.startService(getActivity());

        return true;
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        Assert.assertTrue(mRootTaskId != null);

        if (mData == null)
            return false;

        Assert.assertTrue(mScheduleAdapter != null);

        Assert.assertTrue(mData.ScheduleDatas != null);
        Assert.assertTrue(Stream.of(mData.ScheduleDatas)
                .allMatch(scheduleData -> scheduleData.getScheduleType() == ScheduleType.DAILY)); // todo schedule hack

        List<TimePair> oldTimePairs = Stream.of(mData.ScheduleDatas)
                .map(scheduleData -> ((ScheduleLoader.DailyScheduleData) scheduleData).TimePair)
                .sortBy(TimePair::hashCode)
                .collect(Collectors.toList());

        List<TimePair> newTimePairs = Stream.of(mScheduleEntries)
                .map(timeEntry -> ((DailyScheduleEntry) timeEntry).mTimePairPersist.getTimePair())
                .sortBy(TimePair::hashCode)
                .collect(Collectors.toList());

        if (!oldTimePairs.equals(newTimePairs))
            return true;

        return false;
    }
}
