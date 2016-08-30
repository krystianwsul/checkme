package com.krystianwsul.checkme.gui.tasks;

import android.os.Bundle;
import android.support.v4.util.Pair;
import android.text.TextUtils;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.loaders.ScheduleLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.ScheduleType;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.TimePair;

import junit.framework.Assert;

import java.util.List;

public class WeeklyScheduleFragment extends RepeatingScheduleFragment implements ScheduleFragment {
    public static WeeklyScheduleFragment newInstance() {
        return new WeeklyScheduleFragment();
    }

    public static WeeklyScheduleFragment newInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        weeklyScheduleFragment.setArguments(args);

        return weeklyScheduleFragment;
    }

    public static WeeklyScheduleFragment newInstance(int rootTaskId) {
        WeeklyScheduleFragment weeklyScheduleFragment = new WeeklyScheduleFragment();

        Bundle args = new Bundle();
        args.putInt(ROOT_TASK_ID_KEY, rootTaskId);

        weeklyScheduleFragment.setArguments(args);
        return weeklyScheduleFragment;
    }

    @Override
    protected ScheduleEntry firstScheduleEntry(boolean showDelete) {
        if (mScheduleHint != null)
            return new WeeklyScheduleEntry(mScheduleHint.mDate.getDayOfWeek(), mScheduleHint.mTimePair, showDelete);
        else
            return new WeeklyScheduleEntry(DayOfWeek.today(), showDelete);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("WeeklyScheduleFragment.onResume");

        super.onResume();
    }

    private List<Pair<DayOfWeek, TimePair>> getDayOfWeekTimePairs() {
        Assert.assertTrue(!mScheduleEntries.isEmpty());

        return Stream.of(mScheduleEntries)
                .map(dayOfWeekTimeEntry -> new Pair<>(((WeeklyScheduleEntry) dayOfWeekTimeEntry).mDayOfWeek, ((WeeklyScheduleEntry) dayOfWeekTimeEntry).mTimePairPersist.getTimePair()))
                .collect(Collectors.toList());
    }

    @Override
    public boolean createRootTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        List<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createWeeklyScheduleRootTask(mData.DataId, name, dayOfWeekTimePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean updateRootTask(int rootTaskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (mData == null)
            return false;

        List<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).updateWeeklyScheduleTask(mData.DataId, rootTaskId, name, dayOfWeekTimePairs);

        TickService.startService(getActivity());

        return true;
    }

    @Override
    public boolean createRootJoinTask(String name, List<Integer> joinTaskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        if (mData == null)
            return false;

        List<Pair<DayOfWeek, TimePair>> dayOfWeekTimePairs = getDayOfWeekTimePairs();
        Assert.assertTrue(!dayOfWeekTimePairs.isEmpty());

        DomainFactory.getDomainFactory(getActivity()).createWeeklyScheduleJoinRootTask(mData.DataId, name, dayOfWeekTimePairs, joinTaskIds);

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
                .allMatch(scheduleData -> scheduleData.getScheduleType() == ScheduleType.WEEKLY)); // todo schedule hack

        List<Pair<DayOfWeek, TimePair>> oldDayOfWeekTimePairs = Stream.of(mData.ScheduleDatas)
                .map(scheduleData -> new Pair<>(((ScheduleLoader.WeeklyScheduleData) scheduleData).DayOfWeek, ((ScheduleLoader.WeeklyScheduleData) scheduleData).TimePair))
                .sortBy(Pair::hashCode)
                .collect(Collectors.toList());

        List<Pair<DayOfWeek, TimePair>> newDayOfWeekTimePairs = Stream.of(mScheduleEntries)
                .map(dayOfWeekTimeEntry -> new Pair<>(((WeeklyScheduleEntry) dayOfWeekTimeEntry).mDayOfWeek, ((WeeklyScheduleEntry) dayOfWeekTimeEntry).mTimePairPersist.getTimePair()))
                .sortBy(Pair::hashCode)
                .collect(Collectors.toList());

        if (!oldDayOfWeekTimePairs.equals(newDayOfWeekTimePairs))
            return true;

        return false;
    }
}
