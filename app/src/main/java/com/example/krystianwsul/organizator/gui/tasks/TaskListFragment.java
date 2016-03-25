package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.loaders.TaskListLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskListFragment extends Fragment implements LoaderManager.LoaderCallbacks<TaskListLoader.Data> {
    private static final String ALL_TASKS_KEY = "allTasks";
    private static final String TASK_ID_KEY = "taskId";

    private RecyclerView mTaskListFragmentRecycler;
    private FloatingActionButton mTaskListFragmentFab;

    private Integer mTaskId;

    private TaskAdapter mTaskAdapter;

    private ActionMode mActionMode;

    public static TaskListFragment getInstance() {
        TaskListFragment taskListFragment = new TaskListFragment();

        Bundle args = new Bundle();
        args.putBoolean(ALL_TASKS_KEY, true);
        taskListFragment.setArguments(args);

        return taskListFragment;
    }

    public static TaskListFragment getInstance(int taskId) {
        TaskListFragment taskListFragment = new TaskListFragment();

        Bundle args = new Bundle();
        args.putInt(TASK_ID_KEY, taskId);
        taskListFragment.setArguments(args);

        return taskListFragment;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Assert.assertTrue(context instanceof TaskListListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.task_list_fragment, container, false);
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

        mTaskListFragmentRecycler = (RecyclerView) view.findViewById(R.id.task_list_fragment_recycler);
        mTaskListFragmentRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mTaskListFragmentFab = (FloatingActionButton) getView().findViewById(R.id.task_list_fragment_fab);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onStop() {
        super.onStop();

        if (mActionMode != null)
            mActionMode.finish();
    }

    @Override
    public Loader<TaskListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new TaskListLoader(getActivity(), mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<TaskListLoader.Data> loader, TaskListLoader.Data data) {
        mTaskListFragmentFab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTaskId == null)
                    startActivity(CreateRootTaskActivity.getCreateIntent(getContext()));
                else
                    startActivity(CreateChildTaskActivity.getCreateIntent(getActivity(), mTaskId));
            }
        });

        ArrayList<TaskAdapter.Data> taskDatas = new ArrayList<>();
        for (TaskListLoader.TaskData taskData : data.taskDatas)
            taskDatas.add(new TaskAdapter.Data(taskData.TaskId, taskData.Name, taskData.ScheduleText, taskData.HasChildTasks));

        TaskAdapter.OnCheckedChangedListener listener = null;
        if (mTaskId == null) {
            listener = new TaskAdapter.OnCheckedChangedListener() {
                @Override
                public void OnCheckedChanged() {
                    TaskAdapter taskAdapter = (TaskAdapter) mTaskListFragmentRecycler.getAdapter();
                    Assert.assertTrue(taskAdapter != null);

                    ArrayList<Integer> taskIds = taskAdapter.getSelected();
                    if (taskIds.isEmpty()) {
                        if (mActionMode != null)
                            mActionMode.finish();
                    } else {
                        if (mActionMode == null)
                            ((AppCompatActivity) getActivity()).startSupportActionMode(new TaskEditCallback());
                    }
                }
            };
        }
        mTaskAdapter = new TaskAdapter(getActivity(), taskDatas, data.DataId, listener);
        mTaskListFragmentRecycler.setAdapter(mTaskAdapter);
    }

    @Override
    public void onLoaderReset(Loader<TaskListLoader.Data> loader) {
    }

    private class TaskEditCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(final ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;

            actionMode.getMenuInflater().inflate(R.menu.menu_edit_tasks, menu);
            actionMode.setTitle(getString(R.string.join));

            ((TaskListListener) getActivity()).onCreateTaskActionMode(actionMode);

            return true;
        }

        @Override
        public boolean onPrepareActionMode(ActionMode actionMode, Menu menu) {
            return false;
        }

        @Override
        public boolean onActionItemClicked(ActionMode actionMode, MenuItem menuItem) {
            ArrayList<Integer> taskIds = mTaskAdapter.getSelected();
            Assert.assertTrue(taskIds != null);
            Assert.assertTrue(!taskIds.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_task_join:
                    if (taskIds.size() == 1) {
                        MessageDialogFragment messageDialogFragment = MessageDialogFragment.newInstance(getString(R.string.two_tasks_message));
                        messageDialogFragment.show(getChildFragmentManager(), "two_tasks");
                    } else {
                        startActivity(CreateRootTaskActivity.getJoinIntent(getActivity(), taskIds));
                        actionMode.finish();
                    }

                    return true;
                case R.id.action_task_delete:
                    mTaskAdapter.removeSelected();
                default:
                    return false;
            }
        }

        @Override
        public void onDestroyActionMode(ActionMode actionMode) {
            Assert.assertTrue(mActionMode != null);
            mActionMode = null;

            mTaskAdapter.uncheck();

            ((TaskListListener) getActivity()).onDestroyTaskActionMode();
        }
    }

    public interface TaskListListener {
        void onCreateTaskActionMode(ActionMode actionMode);
        void onDestroyTaskActionMode();
    }
}