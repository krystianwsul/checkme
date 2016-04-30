package com.example.krystianwsul.organizator.gui.tasks;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.TaskListLoader;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;

public class TaskListFragment extends Fragment implements LoaderManager.LoaderCallbacks<TaskListLoader.Data> {
    private static final String SELECTED_TASKS_KEY = "selectedTasks";

    private static final String ALL_TASKS_KEY = "allTasks";
    private static final String TASK_ID_KEY = "taskId";

    private RecyclerView mTaskListFragmentRecycler;
    private FloatingActionButton mTaskListFragmentFab;

    private Integer mTaskId;

    private TaskListLoader.Data mData;

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
        return inflater.inflate(R.layout.fragment_task_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);

        boolean allTasks = args.getBoolean(ALL_TASKS_KEY, false);
        int taskId = args.getInt(TASK_ID_KEY, -1);
        if (taskId != -1) {
            Assert.assertTrue(!allTasks);
            mTaskId = taskId;
        } else {
            Assert.assertTrue(allTasks);
            mTaskId = null;
        }

        View view = getView();
        Assert.assertTrue(view != null);

        mTaskListFragmentRecycler = (RecyclerView) view.findViewById(R.id.task_list_recycler);
        mTaskListFragmentRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mTaskListFragmentFab = (FloatingActionButton) getView().findViewById(R.id.task_list_fab);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<TaskListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new TaskListLoader(getActivity(), mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<TaskListLoader.Data> loader, TaskListLoader.Data data) {
        mData = data;

        mTaskListFragmentFab.setOnClickListener(v -> {
            if (mTaskId == null)
                startActivity(CreateRootTaskActivity.getCreateIntent(getContext()));
            else
                startActivity(CreateChildTaskActivity.getCreateIntent(getActivity(), mTaskId));
        });

        mTaskAdapter = new TaskAdapter(this, data);
        mTaskListFragmentRecycler.setAdapter(mTaskAdapter);
    }

    @Override
    public void onLoaderReset(Loader<TaskListLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        //asdf
    }

    public int getDataId() {
        Assert.assertTrue(mData != null);
        return mData.DataId;
    }

    private TaskEditCallback newTaskEditCallback() {
        return new TaskEditCallback();
    }

    public class TaskEditCallback implements ActionMode.Callback {
        @Override
        public boolean onCreateActionMode(final ActionMode actionMode, Menu menu) {
            mActionMode = actionMode;

            actionMode.getMenuInflater().inflate(R.menu.menu_edit_tasks, menu);

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
                    if (mTaskId == null)
                        startActivity(CreateRootTaskActivity.getJoinIntent(getActivity(), taskIds));
                    else
                        startActivity(CreateChildTaskActivity.getJoinIntent(getActivity(), mTaskId, taskIds));
                    break;
                case R.id.action_task_delete:
                    mTaskAdapter.removeSelected();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }

            actionMode.finish();
            return true;
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

    public static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {
        private final WeakReference<TaskListFragment> mTaskListFragmentReference;

        private final ArrayList<TaskWrapper> mTaskWrappers;

        private final TaskListLoader.Data mData;

        public TaskAdapter(TaskListFragment taskListFragment, TaskListLoader.Data data) {
            Assert.assertTrue(taskListFragment != null);
            Assert.assertTrue(data != null);

            mTaskListFragmentReference = new WeakReference<>(taskListFragment);
            mData = data;

            mTaskWrappers = new ArrayList<>();
            for (TaskListLoader.TaskData taskData : data.TaskDatas)
                mTaskWrappers.add(new TaskWrapper(taskData));
        }

        @Override
        public int getItemCount() {
            return mTaskWrappers.size();
        }

        @Override
        public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TaskListFragment taskListFragment = mTaskListFragmentReference.get();
            Assert.assertTrue(taskListFragment != null);

            LayoutInflater inflater = LayoutInflater.from(taskListFragment.getActivity());
            View showTaskRow = inflater.inflate(R.layout.row_task_list, parent, false);

            TextView taskRowName = (TextView) showTaskRow.findViewById(R.id.task_row_name);
            TextView taskRowDetails = (TextView) showTaskRow.findViewById(R.id.task_row_details);
            ImageView taskRowImage = (ImageView) showTaskRow.findViewById(R.id.task_row_img);

            return new TaskHolder(showTaskRow, taskRowName, taskRowDetails, taskRowImage);
        }

        @Override
        public void onBindViewHolder(final TaskHolder taskHolder, int position) {
            TaskListFragment taskListFragment = mTaskListFragmentReference.get();
            Assert.assertTrue(taskListFragment != null);

            TaskWrapper taskWrapper = mTaskWrappers.get(position);

            if (taskWrapper.mSelected)
                taskHolder.mShowTaskRow.setBackgroundColor(ContextCompat.getColor(taskListFragment.getActivity(), R.color.selected));
            else
                taskHolder.mShowTaskRow.setBackgroundColor(Color.TRANSPARENT);

            taskHolder.mShowTaskRow.setOnLongClickListener(v -> {
                taskHolder.onLongClick();
                return true;
            });

            if (taskWrapper.mTaskData.HasChildTasks)
                taskHolder.mTaskRowImg.setVisibility(View.VISIBLE);
            else
                taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);

            taskHolder.mTaskRowName.setText(taskWrapper.mTaskData.Name);

            String scheduleText = taskWrapper.mTaskData.ScheduleText;
            if (TextUtils.isEmpty(scheduleText))
                taskHolder.mTaskRowDetails.setVisibility(View.GONE);
            else
                taskHolder.mTaskRowDetails.setText(scheduleText);

            taskHolder.mShowTaskRow.setOnClickListener(v -> {
                if (taskListFragment.mActionMode != null)
                    taskHolder.onLongClick();
                else
                    taskHolder.onRowClick();
            });
        }

        public void uncheck() {
            for (TaskWrapper taskWrapper : mTaskWrappers) {
                if (taskWrapper.mSelected) {
                    taskWrapper.mSelected = false;
                    notifyItemChanged(mTaskWrappers.indexOf(taskWrapper));
                }
            }
        }

        public ArrayList<Integer> getSelected() {
            ArrayList<Integer> taskIds = new ArrayList<>();
            for (TaskWrapper taskWrapper : mTaskWrappers)
                if (taskWrapper.mSelected)
                    taskIds.add(taskWrapper.mTaskData.TaskId);
            return taskIds;
        }

        public void removeSelected() {
            TaskListFragment taskListFragment = mTaskListFragmentReference.get();
            Assert.assertTrue(taskListFragment != null);

            ArrayList<TaskWrapper> selectedTaskWrappers = new ArrayList<>();
            for (TaskWrapper taskWrapper : mTaskWrappers)
                if (taskWrapper.mSelected)
                    selectedTaskWrappers.add(taskWrapper);

            ArrayList<Integer> taskIds = new ArrayList<>();
            for (TaskWrapper selectedTaskWrapper : selectedTaskWrappers) {
                taskIds.add(selectedTaskWrapper.mTaskData.TaskId);

                int position = mTaskWrappers.indexOf(selectedTaskWrapper);
                mTaskWrappers.remove(position);
                notifyItemRemoved(position);
            }

            DomainFactory.getDomainFactory(taskListFragment.getActivity()).setTaskEndTimeStamps(mData.DataId, taskIds);
        }

        private static class TaskWrapper {
            public final TaskListLoader.TaskData mTaskData;
            public boolean mSelected;

            public TaskWrapper(TaskListLoader.TaskData taskData) {
                Assert.assertTrue(taskData != null);
                mTaskData = taskData;
            }
        }

        public class TaskHolder extends RecyclerView.ViewHolder {
            public final View mShowTaskRow;
            public final TextView mTaskRowName;
            public final TextView mTaskRowDetails;
            public final ImageView mTaskRowImg;

            public TaskHolder(View showTaskRow, TextView taskRowName, TextView taskRowDetails, ImageView taskRowImg) {
                super(showTaskRow);

                Assert.assertTrue(taskRowName != null);
                Assert.assertTrue(taskRowDetails != null);
                Assert.assertTrue(taskRowImg != null);

                mShowTaskRow = showTaskRow;
                mTaskRowName = taskRowName;
                mTaskRowDetails = taskRowDetails;
                mTaskRowImg = taskRowImg;
            }

            public void onRowClick() {
                TaskListFragment taskListFragment = mTaskListFragmentReference.get();
                Assert.assertTrue(taskListFragment != null);

                TaskWrapper taskWrapper = mTaskWrappers.get(getAdapterPosition());
                Assert.assertTrue(taskWrapper != null);

                taskListFragment.getActivity().startActivity(ShowTaskActivity.getIntent(taskWrapper.mTaskData.TaskId, taskListFragment.getActivity()));
            }

            public void onLongClick() {
                TaskListFragment taskListFragment = mTaskListFragmentReference.get();
                Assert.assertTrue(taskListFragment != null);

                int position = getAdapterPosition();

                TaskWrapper taskWrapper = mTaskWrappers.get(position);
                Assert.assertTrue(taskWrapper != null);

                taskWrapper.mSelected = !taskWrapper.mSelected;
                notifyItemChanged(position);

                ArrayList<Integer> taskIds = getSelected();
                if (taskIds.isEmpty()) {
                    if (taskListFragment.mActionMode != null)
                        taskListFragment.mActionMode.finish();
                } else {
                    if (taskListFragment.mActionMode == null)
                        ((AppCompatActivity) taskListFragment.getActivity()).startSupportActionMode(taskListFragment.newTaskEditCallback());
                    else
                        taskListFragment.mActionMode.getMenu().findItem(R.id.action_task_join).setVisible(taskIds.size() > 1);
                }
            }
        }
    }
}