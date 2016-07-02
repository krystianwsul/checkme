package com.krystianwsul.checkme.gui.tasks;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.loaders.ParentLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParentFragment extends Fragment implements LoaderManager.LoaderCallbacks<ParentLoader.Data> {
    private static final String PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment";

    private static final String PARENT_ID = "parentId";

    private static final String PARENT_TASK_ID_KEY = "parentTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String CHILD_TASK_ID_KEY = "childTaskId";

    private Bundle mSavedInstanceState;

    private TextView mCreateChildTaskParent;

    private Integer mParentTaskId = null;
    private ArrayList<Integer> mTaskIds;
    private Integer mChildTaskId = null;

    private ParentLoader.TaskData mParent;

    private ParentLoader.Data mData;

    private final ParentPickerFragment.Listener mParentFragmentListener = taskData -> {
        Assert.assertTrue(taskData != null);

        mParent = taskData;
        mCreateChildTaskParent.setText(taskData.Name);
    };

    public static ParentFragment getCreateInstance(int parentTaskId) {
        ParentFragment parentFragment = new ParentFragment();

        Bundle args = new Bundle();
        args.putInt(PARENT_TASK_ID_KEY, parentTaskId);
        parentFragment.setArguments(args);

        return parentFragment;
    }

    public static ParentFragment getJoinInstance(int parentTaskId, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ParentFragment parentFragment = new ParentFragment();

        Bundle args = new Bundle();
        args.putInt(PARENT_TASK_ID_KEY, parentTaskId);
        args.putIntegerArrayList(TASK_IDS_KEY, joinTaskIds);
        parentFragment.setArguments(args);

        return parentFragment;
    }

    public static ParentFragment getEditInstance(int childTaskId) {
        ParentFragment parentFragment = new ParentFragment();

        Bundle args = new Bundle();
        args.putInt(CHILD_TASK_ID_KEY, childTaskId);
        parentFragment.setArguments(args);

        return parentFragment;
    }

    public ParentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent, container, false);

        mCreateChildTaskParent = (TextView) view.findViewById(R.id.create_child_task_parent);
        Assert.assertTrue(mCreateChildTaskParent != null);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mSavedInstanceState = savedInstanceState;

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        if (args.containsKey(PARENT_TASK_ID_KEY)) {
            mParentTaskId = args.getInt(PARENT_TASK_ID_KEY);

            if (args.containsKey(TASK_IDS_KEY)) {
                mTaskIds = args.getIntegerArrayList(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }
        } else {
            Assert.assertTrue(args.containsKey(CHILD_TASK_ID_KEY));
            Assert.assertTrue(!args.containsKey(TASK_IDS_KEY));

            mChildTaskId = args.getInt(CHILD_TASK_ID_KEY, -1);
            Assert.assertTrue(mChildTaskId != -1);
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("ParentFragment.onResume");

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mData != null) {
            Assert.assertTrue(mParent != null);
            outState.putInt(PARENT_ID, mParent.TaskId);
        }
    }

    @Override
    public Loader<ParentLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ParentLoader(getActivity(), mChildTaskId);
    }

    @Override
    public void onLoadFinished(Loader<ParentLoader.Data> loader, final ParentLoader.Data data) {
        mData = data;

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(PARENT_ID)) {
            int parentId = mSavedInstanceState.getInt(PARENT_ID);
            mParent = findTaskData(parentId);
            Assert.assertTrue(mParent != null);
        } else {
            if (mParentTaskId != null) {
                Assert.assertTrue(mChildTaskId == null);
                Assert.assertTrue(mData.ChildTaskData == null);

                mParent = findTaskData(mParentTaskId);
            } else {
                Assert.assertTrue(mChildTaskId != null);
                Assert.assertTrue(mData.ChildTaskData != null);

                mParent = findTaskData(mData.ChildTaskData.ParentTaskId);
            }
        }

        Assert.assertTrue(mParent != null);
        mCreateChildTaskParent.setText(mParent.Name);

        mCreateChildTaskParent.setVisibility(View.VISIBLE);

        mCreateChildTaskParent.setOnClickListener(v -> {
            ParentPickerFragment parentPickerFragment = ParentPickerFragment.newInstance();
            parentPickerFragment.show(getChildFragmentManager(), PARENT_PICKER_FRAGMENT_TAG);
            parentPickerFragment.initialize(mData.TaskDatas, mParentFragmentListener);
        });

        ParentPickerFragment parentPickerFragment = (ParentPickerFragment) getChildFragmentManager().findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG);
        if (parentPickerFragment != null)
            parentPickerFragment.initialize(mData.TaskDatas, mParentFragmentListener);
    }

    @Override
    public void onLoaderReset(Loader<ParentLoader.Data> loader) {
    }

    @SuppressWarnings("RedundantIfStatement")
    public boolean dataChanged() {
        if (mData == null)
            return false;

        Assert.assertTrue(mParent != null);

        if (mChildTaskId != null) {
            Assert.assertTrue(mParentTaskId == null);
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mData.ChildTaskData != null);

            if (mParent.TaskId != mData.ChildTaskData.ParentTaskId)
                return true;
        } else {
            Assert.assertTrue(mParentTaskId != null);

            if (mParent.TaskId != mParentTaskId) {
                return true;
            }
        }

        return false;
    }

    private ParentLoader.TaskData findTaskData(int taskId) {
        Assert.assertTrue(mData != null);

        List<ParentLoader.TaskData> taskDatas = findTaskDataHelper(mData.TaskDatas, taskId)
                .collect(Collectors.toList());

        Assert.assertTrue(taskDatas.size() == 1);
        return taskDatas.get(0);
    }

    private Stream<ParentLoader.TaskData> findTaskDataHelper(Map<Integer, ParentLoader.TaskData> taskDatas, int taskId) {
        Assert.assertTrue(taskDatas != null);

        if (taskDatas.containsKey(taskId)) {
            List<ParentLoader.TaskData> ret = new ArrayList<>();
            ret.add(taskDatas.get(taskId));
            return Stream.of(ret);
        }

        return Stream.of(taskDatas.values())
                .map(taskData -> findTaskDataHelper(taskData.TaskDatas, taskId))
                .flatMap(stream -> stream);
    }

    public int getParentTaskId() {
        return mParent.TaskId;
    }
}
