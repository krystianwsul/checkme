package com.krystianwsul.checkme.gui.tasks;


import android.content.Intent;
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
import com.krystianwsul.checkme.loaders.CreateTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParentFragment extends Fragment implements LoaderManager.LoaderCallbacks<CreateTaskLoader.Data>, CreateTaskFragment {
    private static final String PARENT_PICKER_FRAGMENT_TAG = "parentPickerFragment";

    private static final String PARENT_ID = "parentId";

    private Bundle mSavedInstanceState;

    private TextInputLayout mFragmentParentLayout;
    private TextView mCreateChildTaskParent;

    private Integer mParentTaskId = null;
    private ArrayList<Integer> mTaskIds;
    private Integer mTaskId = null;

    private CreateTaskLoader.TaskTreeData mParent;

    private CreateTaskLoader.Data mData;

    private final ParentPickerFragment.Listener mParentFragmentListener = taskData -> {
        Assert.assertTrue(taskData != null);

        mParent = taskData;
        mCreateChildTaskParent.setText(taskData.Name);

        updateError();
    };

    public static ParentFragment getInstance() {
        return new ParentFragment();
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

        Intent intent = getActivity().getIntent();
        Assert.assertTrue(intent != null);

        if (intent.hasExtra(CreateTaskActivity.TASK_ID_KEY)) {
            Assert.assertTrue(!intent.hasExtra(CreateTaskActivity.PARENT_TASK_ID_HINT_KEY));
            Assert.assertTrue(!intent.hasExtra(CreateTaskActivity.TASK_IDS_KEY));

            mTaskId = intent.getIntExtra(CreateTaskActivity.TASK_ID_KEY, -1);
            Assert.assertTrue(mTaskId != -1);
        } else {
            if (intent.hasExtra(CreateTaskActivity.PARENT_TASK_ID_HINT_KEY)) {
                mParentTaskId = intent.getIntExtra(CreateTaskActivity.PARENT_TASK_ID_HINT_KEY, -1);
                Assert.assertTrue(mParentTaskId != -1);
            }

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
    public Loader<CreateTaskLoader.Data> onCreateLoader(int id, Bundle args) {
        List<Integer> excludedTaskIds = new ArrayList<>();

        if (mTaskId != null) {
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mParentTaskId == null);

            excludedTaskIds.add(mTaskId);
        } else if (mTaskIds != null) {
            excludedTaskIds.addAll(mTaskIds);
        }

        return new CreateTaskLoader(getActivity(), mTaskId, excludedTaskIds);
    }

    @Override
    public void onLoadFinished(Loader<CreateTaskLoader.Data> loader, final CreateTaskLoader.Data data) {
        mData = data;

        if (mSavedInstanceState != null && mSavedInstanceState.containsKey(PARENT_ID)) {
            int parentId = mSavedInstanceState.getInt(PARENT_ID);
            mParent = findTaskData(parentId);
            Assert.assertTrue(mParent != null);
        } else {
            if (mData.TaskData != null && mData.TaskData.ParentTaskId != null) {
                Assert.assertTrue(mParentTaskId == null);
                Assert.assertTrue(mTaskIds == null);
                Assert.assertTrue(mTaskId != null);

                mParent = findTaskData(mData.TaskData.ParentTaskId);
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
            parentPickerFragment.initialize(mData.TaskTreeDatas, mParentFragmentListener);
        });

        ParentPickerFragment parentPickerFragment = (ParentPickerFragment) getChildFragmentManager().findFragmentByTag(PARENT_PICKER_FRAGMENT_TAG);
        if (parentPickerFragment != null)
            parentPickerFragment.initialize(mData.TaskTreeDatas, mParentFragmentListener);
    }

    @Override
    public void onLoaderReset(Loader<CreateTaskLoader.Data> loader) {
    }

    @SuppressWarnings("RedundantIfStatement")
    @Override
    public boolean dataChanged() {
        if (mData == null)
            return false;

        if (mData.TaskData != null && mData.TaskData.ParentTaskId != null) {
            Assert.assertTrue(mParentTaskId == null);
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mTaskId != null);

            if (mParent.TaskId != mData.TaskData.ParentTaskId)
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

    private CreateTaskLoader.TaskTreeData findTaskData(int taskId) {
        Assert.assertTrue(mData != null);

        List<CreateTaskLoader.TaskTreeData> taskTreeDatas = findTaskDataHelper(mData.TaskTreeDatas, taskId)
                .collect(Collectors.toList());

        Assert.assertTrue(taskTreeDatas.size() == 1);
        return taskTreeDatas.get(0);
    }

    private Stream<CreateTaskLoader.TaskTreeData> findTaskDataHelper(Map<Integer, CreateTaskLoader.TaskTreeData> taskDatas, int taskId) {
        Assert.assertTrue(taskDatas != null);

        if (taskDatas.containsKey(taskId)) {
            List<CreateTaskLoader.TaskTreeData> ret = new ArrayList<>();
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

    public void clear() {
        mParent = null;
        mCreateChildTaskParent.setText(null);

        mFragmentParentLayout.setError(null);
    }
}
