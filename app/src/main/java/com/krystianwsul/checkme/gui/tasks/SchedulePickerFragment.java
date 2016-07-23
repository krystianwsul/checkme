package com.krystianwsul.checkme.gui.tasks;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;

import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.SchedulePickerLoader;
import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class SchedulePickerFragment extends Fragment implements LoaderManager.LoaderCallbacks<SchedulePickerLoader.Data>, CreateTaskFragment {
    private static final String TASK_ID_KEY = "taskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String POSITION_KEY = "position";
    private static final String SCHEDULE_TYPE_CHANGED_KEY = "scheduleTypeChanged";

    private static final String SCHEDULE_HINT_KEY = "scheduleHint";

    private CreateTaskActivity.ScheduleHint mScheduleHint;

    private Spinner mCreateRootTaskSpinner;
    private Bundle mSavedInstanceState;

    private Integer mTaskId;
    private ArrayList<Integer> mTaskIds;

    private SchedulePickerLoader.Data mData;

    private boolean mScheduleTypeChanged = false;

    public static SchedulePickerFragment getCreateInstance() {
        return new SchedulePickerFragment();
    }

    public static SchedulePickerFragment getCreateInstance(CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(scheduleHint != null);

        SchedulePickerFragment schedulePickerFragment = new SchedulePickerFragment();

        Bundle args = new Bundle();
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        schedulePickerFragment.setArguments(args);

        return schedulePickerFragment;
    }

    public static SchedulePickerFragment getJoinInstance(ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        SchedulePickerFragment schedulePickerFragment = new SchedulePickerFragment();

        Bundle args = new Bundle();
        args.putIntegerArrayList(TASK_IDS_KEY, joinTaskIds);
        schedulePickerFragment.setArguments(args);

        return schedulePickerFragment;
    }

    public static SchedulePickerFragment getJoinInstance(ArrayList<Integer> joinTaskIds, CreateTaskActivity.ScheduleHint scheduleHint) {
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);
        Assert.assertTrue(scheduleHint != null);

        SchedulePickerFragment schedulePickerFragment = new SchedulePickerFragment();

        Bundle args = new Bundle();
        args.putIntegerArrayList(TASK_IDS_KEY, joinTaskIds);
        args.putParcelable(SCHEDULE_HINT_KEY, scheduleHint);
        schedulePickerFragment.setArguments(args);

        return schedulePickerFragment;
    }

    public static SchedulePickerFragment getEditInstance(int taskId) {
        SchedulePickerFragment schedulePickerFragment = new SchedulePickerFragment();

        Bundle args = new Bundle();
        args.putInt(TASK_ID_KEY, taskId);
        schedulePickerFragment.setArguments(args);

        return schedulePickerFragment;
    }

    public SchedulePickerFragment() {

    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_schedule_picker, container, false);

        mCreateRootTaskSpinner = (Spinner) view.findViewById(R.id.schedule_picker_spinner);
        Assert.assertTrue(mCreateRootTaskSpinner != null);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        ArrayAdapter<CharSequence> adapter = ArrayAdapter.createFromResource(getActivity(), R.array.schedule_spinner, android.R.layout.simple_spinner_item);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        mCreateRootTaskSpinner.setAdapter(adapter);

        mSavedInstanceState = savedInstanceState;

        Bundle args = getArguments();

        if (args != null && args.containsKey(SCHEDULE_HINT_KEY)) {
            mScheduleHint = args.getParcelable(SCHEDULE_HINT_KEY);
            Assert.assertTrue(mScheduleHint != null);
        }

        if (args != null && args.containsKey(TASK_ID_KEY)) {
            Assert.assertTrue(!args.containsKey(TASK_IDS_KEY));

            mTaskId = args.getInt(TASK_ID_KEY);
        } else {
            if (args != null && args.containsKey(TASK_IDS_KEY)) {
                mTaskIds = args.getIntegerArrayList(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("SchedulePickerFragment.onResume");

        super.onResume();
    }

    private void loadFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        Fragment fragment = createFragment(position);
        Assert.assertTrue(fragment != null);

        getChildFragmentManager().beginTransaction().replace(R.id.schedule_picker_frame, fragment).commitAllowingStateLoss();
    }

    private Fragment createFragment(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < 3);

        switch (position) {
            case 0:
                if (mScheduleHint != null) {
                    return SingleScheduleFragment.newInstance(mScheduleHint);
                } else {
                    return SingleScheduleFragment.newInstance();
                }
            case 1:
                if (mScheduleHint != null) {
                    return DailyScheduleFragment.newInstance(mScheduleHint);
                } else {
                    return DailyScheduleFragment.newInstance();
                }
            case 2:
                if (mScheduleHint != null) {
                    return WeeklyScheduleFragment.newInstance(mScheduleHint);
                } else {
                    return WeeklyScheduleFragment.newInstance();
                }
            default:
                return null;
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            outState.putInt(POSITION_KEY, mCreateRootTaskSpinner.getSelectedItemPosition());
            outState.putBoolean(SCHEDULE_TYPE_CHANGED_KEY, mScheduleTypeChanged);
        }
    }

    @Override
    public Loader<SchedulePickerLoader.Data> onCreateLoader(int id, Bundle args) {
        return new SchedulePickerLoader(getActivity(), mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<SchedulePickerLoader.Data> loader, final SchedulePickerLoader.Data data) {
        mData = data;

        mCreateRootTaskSpinner.setVisibility(View.VISIBLE);

        int spinnerPosition;
        int count = 0;
        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(POSITION_KEY)) {
            count++;

            spinnerPosition = mSavedInstanceState.getInt(POSITION_KEY, -1);
            Assert.assertTrue(spinnerPosition != -1);
            if (spinnerPosition > 0)
                count++;

            mScheduleTypeChanged = mSavedInstanceState.getBoolean(SCHEDULE_TYPE_CHANGED_KEY);
        } else if (mData.RootTaskData != null) {
            Assert.assertTrue(mTaskId != null);

            ScheduleType scheduleType = mData.RootTaskData.ScheduleType;

            Fragment fragment;
            if (scheduleType == ScheduleType.SINGLE) {
                fragment = SingleScheduleFragment.newInstance(mTaskId);
                spinnerPosition = 0;
            } else if (scheduleType == ScheduleType.DAILY) {
                count++;

                fragment = DailyScheduleFragment.newInstance(mTaskId);
                spinnerPosition = 1;
            } else if (scheduleType == ScheduleType.WEEKLY) {
                count++;

                fragment = WeeklyScheduleFragment.newInstance(mTaskId);
                spinnerPosition = 2;
            } else {
                throw new IndexOutOfBoundsException("unknown schedule type");
            }

            getChildFragmentManager().beginTransaction()
                    .replace(R.id.schedule_picker_frame, fragment)
                    .commitAllowingStateLoss();
        } else {
            spinnerPosition = 0;
            loadFragment(0);
        }
        final int finalCount = count;

        mCreateRootTaskSpinner.setSelection(spinnerPosition);

        mCreateRootTaskSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private int mCount = finalCount;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position < 3);

                if (mCount > 0) {
                    mCount--;
                    return;
                }

                mScheduleTypeChanged = true;

                loadFragment(position);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onLoaderReset(Loader<SchedulePickerLoader.Data> loader) {

    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        if (mData == null)
            return false;

        if (mData.RootTaskData == null) {
            ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
            Assert.assertTrue(scheduleFragment != null);

            if (!(scheduleFragment instanceof SingleScheduleFragment))
                return true;

            if (scheduleFragment.dataChanged())
                return true;

            return false;
        } else {
            Assert.assertTrue(mTaskId != null);

            if (mScheduleTypeChanged)
                return true;

            ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
            Assert.assertTrue(scheduleFragment != null);

            if (scheduleFragment.dataChanged())
                return true;

            return false;
        }
    }

    @Override
    public boolean updateTask(int rootTaskId, String name) {
        ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        Assert.assertTrue(scheduleFragment != null);

        return scheduleFragment.updateRootTask(rootTaskId, name);
    }

    @Override
    public boolean createJoinTask(String name, List<Integer> joinTaskIds) {
        ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        Assert.assertTrue(scheduleFragment != null);

        return scheduleFragment.createRootJoinTask(name, joinTaskIds);
    }

    @Override
    public boolean createTask(String name) {
        ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        Assert.assertTrue(scheduleFragment != null);

        return scheduleFragment.createRootTask(name);
    }

    @Override
    public void updateError() {

    }
}
