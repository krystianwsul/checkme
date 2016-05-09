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
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.annimon.stream.Stream;
import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.SelectionCallback;
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

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mTaskAdapter.uncheck();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            ArrayList<Integer> taskIds = mTaskAdapter.getSelected();
            Assert.assertTrue(taskIds != null);
            Assert.assertTrue(!taskIds.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_task_edit:
                    Assert.assertTrue(taskIds.size() == 1);

                    TaskListLoader.TaskData taskData = Stream.of(mTaskAdapter.mTaskWrappers)
                            .filter(taskWrapper -> taskWrapper.mSelected)
                            .findFirst()
                            .get()
                            .mTaskData;

                    if (taskData.IsRootTask)
                        startActivity(CreateRootTaskActivity.getEditIntent(getActivity(), taskData.TaskId));
                    else
                        startActivity(CreateChildTaskActivity.getEditIntent(getActivity(), taskData.TaskId));
                    break;
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
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mActionMode.getMenuInflater().inflate(R.menu.menu_edit_tasks, mActionMode.getMenu());

            mTaskListFragmentFab.setVisibility(View.GONE);

            ((TaskListListener) getActivity()).onCreateTaskActionMode(mActionMode);
        }

        @Override
        protected void onSecondAdded() {
            mActionMode.getMenu().findItem(R.id.action_task_join).setVisible(true);
            mActionMode.getMenu().findItem(R.id.action_task_edit).setVisible(false);
        }

        @Override
        protected void onOtherAdded() {

        }

        @Override
        protected void onLastRemoved() {
            mTaskListFragmentFab.setVisibility(View.VISIBLE);

            ((TaskListListener) getActivity()).onDestroyTaskActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {
            mActionMode.getMenu().findItem(R.id.action_task_join).setVisible(false);
            mActionMode.getMenu().findItem(R.id.action_task_edit).setVisible(true);
        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    private ArrayList<Integer> mSelectedTasks;

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

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_TASKS_KEY)) {
            mSelectedTasks = savedInstanceState.getIntegerArrayList(SELECTED_TASKS_KEY);
            Assert.assertTrue(mSelectedTasks != null);
            Assert.assertTrue(!mSelectedTasks.isEmpty());
        }

        View view = getView();
        Assert.assertTrue(view != null);

        mTaskListFragmentRecycler = (RecyclerView) view.findViewById(R.id.task_list_recycler);
        mTaskListFragmentRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mTaskListFragmentFab = (FloatingActionButton) view.findViewById(R.id.task_list_fab);
        Assert.assertTrue(mTaskListFragmentFab != null);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<TaskListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new TaskListLoader(getActivity(), mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<TaskListLoader.Data> loader, TaskListLoader.Data data) {
        mData = data;

        if (mTaskAdapter != null) {
            ArrayList<Integer> selected = mTaskAdapter.getSelected();

            if (selected.isEmpty()) {
                Assert.assertTrue(!mSelectionCallback.hasActionMode());
                mSelectedTasks = null;
            } else {
                Assert.assertTrue(mSelectionCallback.hasActionMode());
                mSelectedTasks = selected;
            }
        }

        mTaskListFragmentFab.setOnClickListener(v -> {
            if (mTaskId == null)
                startActivity(CreateRootTaskActivity.getCreateIntent(getContext()));
            else
                startActivity(CreateChildTaskActivity.getCreateIntent(getActivity(), mTaskId));
        });

        mTaskAdapter = new TaskAdapter(this, data, mSelectedTasks);
        mTaskListFragmentRecycler.setAdapter(mTaskAdapter);

        mSelectionCallback.setSelected(mTaskAdapter.getSelected().size());
    }

    @Override
    public void onLoaderReset(Loader<TaskListLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTaskAdapter != null) {
            ArrayList<Integer> selected = mTaskAdapter.getSelected();
            if (!selected.isEmpty()) {
                Assert.assertTrue(mSelectionCallback.hasActionMode());
                outState.putIntegerArrayList(SELECTED_TASKS_KEY, mTaskAdapter.getSelected());
            }
        }
    }

    public int getDataId() {
        Assert.assertTrue(mData != null);
        return mData.DataId;
    }

    public interface TaskListListener {
        void onCreateTaskActionMode(ActionMode actionMode);
        void onDestroyTaskActionMode();
    }

    public static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {
        private final WeakReference<TaskListFragment> mTaskListFragmentReference;

        private final ArrayList<TaskWrapper> mTaskWrappers;

        private final TaskListLoader.Data mData;

        public TaskAdapter(TaskListFragment taskListFragment, TaskListLoader.Data data, ArrayList<Integer> selectedTasks) {
            Assert.assertTrue(taskListFragment != null);
            Assert.assertTrue(data != null);

            mTaskListFragmentReference = new WeakReference<>(taskListFragment);
            mData = data;

            mTaskWrappers = new ArrayList<>();
            for (TaskListLoader.TaskData taskData : data.TaskDatas)
                mTaskWrappers.add(new TaskWrapper(taskData, selectedTasks));
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
            TextView taskRowChildren = (TextView) showTaskRow.findViewById(R.id.task_row_children);
            ImageView taskRowImage = (ImageView) showTaskRow.findViewById(R.id.task_row_img);

            return new TaskHolder(showTaskRow, taskRowName, taskRowDetails, taskRowChildren, taskRowImage);
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

            if (TextUtils.isEmpty(taskWrapper.mTaskData.Children))
                taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);
            else
                taskHolder.mTaskRowImg.setVisibility(View.VISIBLE);

            taskHolder.mTaskRowName.setText(taskWrapper.mTaskData.Name);

            if (TextUtils.isEmpty(taskWrapper.mTaskData.ScheduleText)) {
                taskHolder.mTaskRowDetails.setVisibility(View.GONE);
            } else {
                taskHolder.mTaskRowDetails.setVisibility(View.VISIBLE);
                taskHolder.mTaskRowDetails.setText(taskWrapper.mTaskData.ScheduleText);
            }

            if (TextUtils.isEmpty(taskWrapper.mTaskData.Children)) {
                taskHolder.mTaskRowChildren.setVisibility(View.GONE);
            } else {
                taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);
                taskHolder.mTaskRowChildren.setText(taskWrapper.mTaskData.Children);
            }

            taskHolder.mShowTaskRow.setOnClickListener(v -> {
                if (taskListFragment.mSelectionCallback.hasActionMode())
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

            public TaskWrapper(TaskListLoader.TaskData taskData, ArrayList<Integer> selectedTasks) {
                Assert.assertTrue(taskData != null);
                mTaskData = taskData;

                if (selectedTasks != null) {
                    Assert.assertTrue(!selectedTasks.isEmpty());
                    mSelected = selectedTasks.contains(mTaskData.TaskId);
                }
            }
        }

        public class TaskHolder extends RecyclerView.ViewHolder {
            public final View mShowTaskRow;
            public final TextView mTaskRowName;
            public final TextView mTaskRowDetails;
            public final TextView mTaskRowChildren;
            public final ImageView mTaskRowImg;

            public TaskHolder(View showTaskRow, TextView taskRowName, TextView taskRowDetails, TextView taskRowChildren, ImageView taskRowImg) {
                super(showTaskRow);

                Assert.assertTrue(taskRowName != null);
                Assert.assertTrue(taskRowDetails != null);
                Assert.assertTrue(taskRowChildren != null);
                Assert.assertTrue(taskRowImg != null);

                mShowTaskRow = showTaskRow;
                mTaskRowName = taskRowName;
                mTaskRowDetails = taskRowDetails;
                mTaskRowChildren = taskRowChildren;
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

                if (taskWrapper.mSelected) {
                    taskListFragment.mSelectionCallback.incrementSelected();
                } else {
                    taskListFragment.mSelectionCallback.decrementSelected();
                }

                notifyItemChanged(position);
            }
        }
    }
}