package com.example.krystianwsul.organizator.gui.tasks;

import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.TaskListLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class ShowTaskFragment extends Fragment implements LoaderManager.LoaderCallbacks<TaskListLoader.Data> {
    private static final String ALL_TASKS_KEY = "allTasks";
    private static final String TASK_ID_KEY = "taskId";

    private RecyclerView mTaskListFragmentRecycler;
    private FloatingActionButton mTaskListFragmentFab;

    private Integer mTaskId;

    public static ShowTaskFragment getInstance() {
        ShowTaskFragment taskListFragment = new ShowTaskFragment();

        Bundle args = new Bundle();
        args.putBoolean(ALL_TASKS_KEY, true);
        taskListFragment.setArguments(args);

        return taskListFragment;
    }

    public static ShowTaskFragment getInstance(int taskId) {
        ShowTaskFragment taskListFragment = new ShowTaskFragment();

        Bundle args = new Bundle();
        args.putInt(TASK_ID_KEY, taskId);
        taskListFragment.setArguments(args);

        return taskListFragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_show_task, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        boolean allTasks = getArguments().getBoolean(ALL_TASKS_KEY, false);
        int taskId = getArguments().getInt(TASK_ID_KEY, -1);
        if (taskId != -1) {
            Assert.assertTrue(!allTasks);
            mTaskId = taskId;
        } else {
            Assert.assertTrue(allTasks);
            mTaskId = null;
        }

        View view = getView();
        Assert.assertTrue(view != null);

        mTaskListFragmentRecycler = (RecyclerView) view.findViewById(R.id.fragment_show_task_recycler);
        mTaskListFragmentRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mTaskListFragmentFab = (FloatingActionButton) getView().findViewById(R.id.fragment_show_task_fab);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<TaskListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new TaskListLoader(getActivity(), mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<TaskListLoader.Data> loader, final TaskListLoader.Data data) {
        mTaskListFragmentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(CreateChildTaskActivity.getCreateIntent(getActivity(), mTaskId));
            }
        });

        ArrayList<TaskAdapter.Data> taskDatas = new ArrayList<>();
        for (TaskListLoader.TaskData taskData : data.taskDatas)
            taskDatas.add(new TaskAdapter.Data(taskData.TaskId, taskData.Name, null, taskData.HasChildTasks));

        mTaskListFragmentRecycler.setAdapter(new TaskAdapter(getActivity(), taskDatas, data.DataId, null));
    }

    @Override
    public void onLoaderReset(Loader<TaskListLoader.Data> loader) {
    }

    public void removeSelected() {
        TaskAdapter taskAdapter = (TaskAdapter) mTaskListFragmentRecycler.getAdapter();
        Assert.assertTrue(taskAdapter != null);

        taskAdapter.removeSelected();
    }
}