package com.krystianwsul.checkme.gui.tasks;


import android.content.Intent;
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
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.checkme.utils.ScheduleType;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class SchedulePickerFragment extends Fragment implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data>, CreateTaskFragment {
    private static final String POSITION_KEY = "position";
    private static final String SCHEDULE_TYPE_CHANGED_KEY = "scheduleTypeChanged";

    private CreateTaskActivity.ScheduleHint mScheduleHint;

    private Spinner mCreateRootTaskSpinner;
    private Bundle mSavedInstanceState;

    private Integer mTaskId;
    private ArrayList<Integer> mTaskIds;

    private CreateTaskLoader.Data mData;

    private boolean mScheduleTypeChanged = false;

    public static SchedulePickerFragment getInstance() {
        return new SchedulePickerFragment();
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

        Intent intent = getActivity().getIntent();
        Assert.assertTrue(intent != null);

        if (intent.hasExtra(CreateTaskActivity.SCHEDULE_HINT_KEY)) {
            mScheduleHint = intent.getParcelableExtra(CreateTaskActivity.SCHEDULE_HINT_KEY);
            Assert.assertTrue(mScheduleHint != null);
        }

        if (intent.hasExtra(CreateTaskActivity.TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(CreateTaskActivity.TASK_IDS_KEY));

            mTaskId = intent.getIntExtra(CreateTaskActivity.TASK_ID_KEY, -1);
            Assert.assertTrue(mTaskId != -1);
        } else {
            if (intent.hasExtra(CreateTaskActivity.TASK_IDS_KEY)) {
                mTaskIds = intent.getIntegerArrayListExtra(CreateTaskActivity.TASK_IDS_KEY);
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

        getChildFragmentManager()
                .beginTransaction()
                .replace(R.id.schedule_picker_frame, fragment)
                .commitAllowingStateLoss();
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
    public Loader<CreateTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        List<Integer> excludedTaskIds = new ArrayList<>();

        if (mTaskId != null) {
            Assert.assertTrue(mTaskIds == null);

            excludedTaskIds.add(mTaskId);
        } else if (mTaskIds != null) {
            excludedTaskIds.addAll(mTaskIds);
        }

        return new CreateTaskLoader(getActivity(), mTaskId, excludedTaskIds);
    }

    @Override
    public void onLoadFinished(Loader<CreateTaskLoader.Data> loader, final CreateTaskLoader.Data data) {
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
        } else if (mData.TaskData != null && mData.TaskData.ScheduleType != null) {
            Assert.assertTrue(mTaskId != null);

            ScheduleType scheduleType = mData.TaskData.ScheduleType;

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
                Assert.assertTrue(position < 4);

                if (mCount > 0) {
                    mCount--;
                    return;
                }

                mScheduleTypeChanged = true;

                if (position < 3) {
                    loadFragment(position);
                } else {
                    Assert.assertTrue(position == 3);

                    Fragment fragment = getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
                    Assert.assertTrue(fragment != null);

                    getChildFragmentManager().beginTransaction()
                            .remove(fragment)
                            .commitAllowingStateLoss();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {

            }
        });
    }

    @Override
    public void onLoaderReset(Loader<CreateTaskLoader.Data> loader) {

    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        if (mData == null)
            return false;

        if (mData.TaskData == null || mData.TaskData.ScheduleType == null) {
            ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
            if (scheduleFragment == null)
                return true;

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
            if (scheduleFragment == null)
                return false;

            if (scheduleFragment.dataChanged())
                return true;

            return false;
        }
    }

    @Override
    public boolean updateTask(int rootTaskId, String name) {
        ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        if (scheduleFragment != null) {
            return scheduleFragment.updateRootTask(rootTaskId, name);
        } else {
            DomainFactory.getDomainFactory(getActivity()).updateRootTask(mData.DataId, rootTaskId, name);
            return true;
        }
    }

    @Override
    public boolean createJoinTask(String name, List<Integer> joinTaskIds) {
        ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        if (scheduleFragment != null) {
            return scheduleFragment.createRootJoinTask(name, joinTaskIds);
        } else {
            DomainFactory.getDomainFactory(getActivity()).createJoinRootTask(mData.DataId, name, joinTaskIds);
            return true;
        }
    }

    @Override
    public boolean createTask(String name) {
        ScheduleFragment scheduleFragment = (ScheduleFragment) getChildFragmentManager().findFragmentById(R.id.schedule_picker_frame);
        if (scheduleFragment != null) {
            return scheduleFragment.createRootTask(name);
        } else {
            DomainFactory.getDomainFactory(getActivity()).createRootTask(mData.DataId, name);
            return true;
        }
    }

    @Override
    public void updateError() {

    }

    public void clear() {
        mCreateRootTaskSpinner.setSelection(3);
    }
}
