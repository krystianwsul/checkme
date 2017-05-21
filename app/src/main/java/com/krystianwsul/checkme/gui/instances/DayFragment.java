package com.krystianwsul.checkme.gui.instances;


import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.TabLayout;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.FabUser;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment;
import com.krystianwsul.checkme.loaders.DayLoader;
import com.krystianwsul.checkme.utils.time.Date;

import junit.framework.Assert;

import java.text.DateFormatSymbols;
import java.util.Calendar;

public class DayFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<DayLoader.Data>, FabUser {
    private static final String POSITION_KEY = "position";
    private static final String TIME_RANGE_KEY = "timeRange";

    private int mPosition;
    private MainActivity.TimeRange mTimeRange;

    private TabLayout mDayTabLayout;
    private GroupListFragment mGroupListFragment;

    @Nullable
    private FloatingActionButton mFloatingActionButton;

    @NonNull
    public static DayFragment newInstance(@NonNull MainActivity.TimeRange timeRange, int day) {
        Assert.assertTrue(day >= 0);

        DayFragment dayFragment = new DayFragment();

        Bundle args = new Bundle();
        args.putInt(POSITION_KEY, day);
        args.putSerializable(TIME_RANGE_KEY, timeRange);

        dayFragment.setArguments(args);
        return dayFragment;
    }

    public DayFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day, container, false);
        Assert.assertTrue(view != null);

        mDayTabLayout = (TabLayout) view.findViewById(R.id.day_tab_layout);
        Assert.assertTrue(mDayTabLayout != null);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        Assert.assertTrue(args.containsKey(POSITION_KEY));
        mPosition = args.getInt(POSITION_KEY);
        Assert.assertTrue(mPosition >= 0);

        Assert.assertTrue(args.containsKey(TIME_RANGE_KEY));
        mTimeRange = (MainActivity.TimeRange) args.getSerializable(TIME_RANGE_KEY);
        Assert.assertTrue(mTimeRange != null);

        String title;
        if (mTimeRange == MainActivity.TimeRange.DAY) {
            switch (mPosition) {
                case 0:
                    title = getActivity().getString(R.string.today);
                    break;
                case 1:
                    title = getActivity().getString(R.string.tomorrow);
                    break;
                default:
                    Calendar calendar = Calendar.getInstance();

                    calendar.add(Calendar.DATE, mPosition);
                    Date date = new Date(calendar);
                    title = date.getDayOfWeek().toString() + ", " + date.toString();
            }
        } else {
            if (mTimeRange == MainActivity.TimeRange.WEEK) {
                Calendar start = Calendar.getInstance();

                if (mPosition > 0) {
                    start.add(Calendar.WEEK_OF_YEAR, mPosition);
                    start.set(Calendar.DAY_OF_WEEK, start.getFirstDayOfWeek());
                }

                Calendar end = Calendar.getInstance();

                end.add(Calendar.WEEK_OF_YEAR, mPosition + 1);
                end.set(Calendar.DAY_OF_WEEK, end.getFirstDayOfWeek());
                end.add(Calendar.DATE, -1);

                Date startDate = new Date(start);
                Date endDate = new Date(end);

                title = startDate.toString() + " - " + endDate.toString();
            } else {
                Assert.assertTrue(mTimeRange == MainActivity.TimeRange.MONTH);

                Calendar calendar = Calendar.getInstance();
                calendar.add(Calendar.MONTH, mPosition);

                int month = calendar.get(Calendar.MONTH);

                title = DateFormatSymbols.getInstance().getMonths()[month];
            }
        }

        mDayTabLayout.addTab(mDayTabLayout.newTab().setText(title));

        mGroupListFragment = (GroupListFragment) getChildFragmentManager().findFragmentById(R.id.day_frame);
        Assert.assertTrue(mGroupListFragment != null);

        if (mFloatingActionButton != null)
            mGroupListFragment.setFab(mFloatingActionButton);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<DayLoader.Data> onCreateLoader(int id, Bundle args) {
        return new DayLoader(getActivity(), mPosition, mTimeRange);
    }

    @Override
    public void onLoadFinished(Loader<DayLoader.Data> loader, DayLoader.Data data) {
        Assert.assertTrue(data != null);

        mGroupListFragment.setAll(mTimeRange, mPosition, data.DataId, data.mDataWrapper);
    }

    @Override
    public void onLoaderReset(Loader<DayLoader.Data> loader) {

    }

    public void selectAll() {
        mGroupListFragment.selectAll();
    }

    @Override
    public void setFab(@NonNull FloatingActionButton floatingActionButton) {
        if (mFloatingActionButton == floatingActionButton)
            return;

        mFloatingActionButton = floatingActionButton;

        if (mGroupListFragment != null)
            mGroupListFragment.setFab(floatingActionButton);
    }

    @Override
    public void clearFab() {
        mFloatingActionButton = null;

        mGroupListFragment.clearFab();
    }
}
