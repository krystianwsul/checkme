package com.krystianwsul.checkme.gui.tasks;


import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.TextInputLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.loaders.ParentLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParentFragment extends Fragment implements LoaderManager.LoaderCallbacks<ParentLoader.Data>, CreateTaskFragment {
    private static final String PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment";

    private static final String PARENT_ID = "parentId";

    private static final String PARENT_TASK_ID_KEY = "parentTaskId";
    private static final String TASK_IDS_KEY = "taskIds";
    private static final String CHILD_TASK_ID_KEY = "childTaskId";

    private Bundle mSavedInstanceState;

    private TextInputLayout mFragmentParentLayout;
    private TextView mCreateChildTaskParent;

    private Integer mParentTaskId = null;
    private ArrayList<Integer> mTaskIds;
    private Integer mTaskId = null;

    private ParentLoader.TaskData mParent;

    private ParentLoader.Data mData;

    private final ParentPickerFragment.Listener mParentFragmentListener = taskData -> {
        Assert.assertTrue(taskData != null);

        mParent = taskData;
        mCreateChildTaskParent.setText(taskData.Name);

        updateError();
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

    public static ParentFragment getCreateInstance() {
        ParentFragment parentFragment = new ParentFragment();

        Bundle args = new Bundle();
        parentFragment.setArguments(args);

        return parentFragment;
    }

    public static ParentFragment getJoinInstance(ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        ParentFragment parentFragment = new ParentFragment();

        Bundle args = new Bundle();
        args.putIntegerArrayList(TASK_IDS_KEY, joinTaskIds);
        parentFragment.setArguments(args);

        return parentFragment;
    }

    public ParentFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_parent, container, false);

        mFragmentParentLayout = (TextInputLayout) view.findViewById(R.id.fragment_parent_layout);
        Assert.assertTrue(mFragmentParentLayout != null);

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

        if (args.containsKey(CHILD_TASK_ID_KEY)) {
            Assert.assertTrue(!args.containsKey(PARENT_TASK_ID_KEY));
            Assert.assertTrue(!args.containsKey(TASK_IDS_KEY));

            mTaskId = args.getInt(CHILD_TASK_ID_KEY, -1);
            Assert.assertTrue(mTaskId != -1);
        } else {
            if (args.containsKey(PARENT_TASK_ID_KEY)) {
                mParentTaskId = args.getInt(PARENT_TASK_ID_KEY);
            }

            if (args.containsKey(TASK_IDS_KEY)) {
                mTaskIds = args.getIntegerArrayList(TASK_IDS_KEY);
                Assert.assertTrue(mTaskIds != null);
                Assert.assertTrue(mTaskIds.size() > 1);
            }
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

        if (mData != null && mParent != null) {
            outState.putInt(PARENT_ID, mParent.TaskId);
        }
    }

    @Override
    public Loader<ParentLoader.Data> onCreateLoader(int id, Bundle args) {
        List<Integer> excludedTaskIds = new ArrayList<>();

        if (mTaskId != null) {
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mParentTaskId == null);

            excludedTaskIds.add(mTaskId);
        } else if (mTaskIds != null) {
            excludedTaskIds.addAll(mTaskIds);
        }

        return new ParentLoader(getActivity(), mTaskId, excludedTaskIds);
    }

    @Override
    public void onLoadFinished(Loader<ParentLoader.Data> loader, final ParentLoader.Data data) {
        mData = data;

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(PARENT_ID)) {
            int parentId = mSavedInstanceState.getInt(PARENT_ID);
            mParent = findTaskData(parentId);
            Assert.assertTrue(mParent != null);
        } else {
            if (mData.ChildTaskData != null) {
                Assert.assertTrue(mParentTaskId == null);
                Assert.assertTrue(mTaskIds == null);
                Assert.assertTrue(mTaskId != null);

                mParent = findTaskData(mData.ChildTaskData.ParentTaskId);
            } else if (mParentTaskId != null) {
                Assert.assertTrue(mTaskId == null);

                mParent = findTaskData(mParentTaskId);
            }
        }

        if (mParent != null)
            mCreateChildTaskParent.setText(mParent.Name);

        mFragmentParentLayout.setVisibility(View.VISIBLE);
        mFragmentParentLayout.setHintAnimationEnabled(true);

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
    @Override
    public boolean dataChanged() {
        if (mData == null)
            return false;

        if (mData.ChildTaskData != null) {
            Assert.assertTrue(mParentTaskId == null);
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mTaskId != null);

            if (mParent.TaskId != mData.ChildTaskData.ParentTaskId)
                return true;
        } else {
            if (mParentTaskId != null) {
                if (mParent == null || mParent.TaskId != mParentTaskId)
                    return true;
            } else {
                if (mParent != null)
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

    @Override
    public boolean updateTask(int taskId, String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (!isValidParent()) {
            updateError();
            return false;
        }

        DomainFactory.getDomainFactory(getActivity()).updateChildTask(mData.DataId, taskId, name, mParent.TaskId);

        return true;
    }

    @Override
    public boolean createJoinTask(String name, List<Integer> taskIds) {
        Assert.assertTrue(!TextUtils.isEmpty(name));
        Assert.assertTrue(taskIds != null);
        Assert.assertTrue(taskIds.size() > 1);

        if (!isValidParent()) {
            updateError();
            return false;
        }

        DomainFactory.getDomainFactory(getActivity()).createJoinChildTask(mData.DataId, mParent.TaskId, name, taskIds);

        return true;
    }

    @Override
    public boolean createTask(String name) {
        Assert.assertTrue(!TextUtils.isEmpty(name));

        if (!isValidParent()) {
            updateError();
            return false;
        }

        DomainFactory.getDomainFactory(getActivity()).createChildTask(mData.DataId, mParent.TaskId, name);

        return true;
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean isValidParent() {
        if (mData == null)
            return false;

        if (mParent == null)
            return false;

        return true;
    }

    @Override
    public void updateError() {
        Assert.assertTrue(mFragmentParentLayout != null);

        if (isValidParent()) {
            mFragmentParentLayout.setError(null);
        } else {
            mFragmentParentLayout.setError(getString(R.string.error_parent));
        }
    }
}
