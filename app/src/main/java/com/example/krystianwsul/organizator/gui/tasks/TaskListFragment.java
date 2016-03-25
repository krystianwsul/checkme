package com.example.krystianwsul.organizator.gui.tasks;

import android.app.Activity;
import android.content.Context;
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
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
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
        return inflater.inflate(R.layout.fragment_task_list, container, false);
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

        mTaskAdapter = new TaskAdapter(getActivity(), taskDatas, data.DataId, new TaskAdapter.OnCheckedChangedListener() {
            @Override
            public void OnCheckedChanged() {
                ArrayList<Integer> taskIds = mTaskAdapter.getSelected();
                if (taskIds.isEmpty()) {
                    if (mActionMode != null)
                        mActionMode.finish();
                } else {
                    if (mActionMode == null)
                        ((AppCompatActivity) getActivity()).startSupportActionMode(new TaskEditCallback());
                    else {
                        mActionMode.getMenu().findItem(R.id.action_task_join).setVisible((mTaskId == null) && (taskIds.size() > 1));
                    }
                }
            }
        });
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

    public static class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {
        private final Activity mActivity;
        private final ArrayList<TaskWrapper> mTaskWrappers;

        private final int mDataId;

        private final OnCheckedChangedListener mOnCheckedChangedListener;

        public TaskAdapter(Activity activity, ArrayList<Data> datas, int dataId, OnCheckedChangedListener onCheckedChangedListener) {
            Assert.assertTrue(activity != null);
            Assert.assertTrue(datas != null);
            Assert.assertTrue(onCheckedChangedListener != null);

            mActivity = activity;
            mDataId = dataId;
            mOnCheckedChangedListener = onCheckedChangedListener;

            mTaskWrappers = new ArrayList<>();
            for (Data data : datas)
                mTaskWrappers.add(new TaskWrapper(data));
        }

        @Override
        public int getItemCount() {
            return mTaskWrappers.size();
        }

        @Override
        public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mActivity);
            View showTaskRow = inflater.inflate(R.layout.row_task_list, parent, false);

            TextView taskRowName = (TextView) showTaskRow.findViewById(R.id.task_row_name);
            TextView taskRowDetails = (TextView) showTaskRow.findViewById(R.id.task_row_details);
            ImageView taskRowImage = (ImageView) showTaskRow.findViewById(R.id.task_row_img);
            CheckBox taskRowCheckBox = (CheckBox) showTaskRow.findViewById(R.id.task_row_checkbox);

            return new TaskHolder(showTaskRow, taskRowName, taskRowDetails, taskRowImage, taskRowCheckBox);
        }

        @Override
        public void onBindViewHolder(final TaskHolder taskHolder, int position) {
            TaskWrapper taskWrapper = mTaskWrappers.get(position);

            taskHolder.mTaskRowCheckBox.setChecked(taskWrapper.mSelected);
            taskHolder.mTaskRowCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                    taskHolder.onCheckedChanged(isChecked);
                    mOnCheckedChangedListener.OnCheckedChanged();
                }
            });

            if (!taskWrapper.mData.HasChildTasks)
                taskHolder.mTaskRowImg.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_label_outline_black_24dp));
            else
                taskHolder.mTaskRowImg.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_list_black_24dp));

            taskHolder.mTaskRowName.setText(taskWrapper.mData.Name);

            String scheduleText = taskWrapper.mData.ScheduleText;
            if (TextUtils.isEmpty(scheduleText))
                taskHolder.mTaskRowDetails.setVisibility(View.GONE);
            else
                taskHolder.mTaskRowDetails.setText(scheduleText);

            taskHolder.mShowTaskRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    taskHolder.onRowClick();
                }
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
                    taskIds.add(taskWrapper.mData.TaskId);
            return taskIds;
        }

        public void removeSelected() {
            ArrayList<TaskWrapper> selectedTaskWrappers = new ArrayList<>();
            for (TaskWrapper taskWrapper : mTaskWrappers)
                if (taskWrapper.mSelected)
                    selectedTaskWrappers.add(taskWrapper);

            ArrayList<Integer> taskIds = new ArrayList<>();
            for (TaskWrapper selectedTaskWrapper : selectedTaskWrappers) {
                taskIds.add(selectedTaskWrapper.mData.TaskId);

                int position = mTaskWrappers.indexOf(selectedTaskWrapper);
                mTaskWrappers.remove(position);
                notifyItemRemoved(position);
            }

            DomainFactory.getDomainFactory(mActivity).setTaskEndTimeStamps(mDataId, taskIds);

            mOnCheckedChangedListener.OnCheckedChanged();
        }

        private static class TaskWrapper {
            public final Data mData;
            public boolean mSelected;

            public TaskWrapper(Data data) {
                Assert.assertTrue(data != null);
                mData = data;
            }
        }

        public class TaskHolder extends RecyclerView.ViewHolder {
            public final View mShowTaskRow;
            public final TextView mTaskRowName;
            public final TextView mTaskRowDetails;
            public final ImageView mTaskRowImg;
            public final CheckBox mTaskRowCheckBox;

            public TaskHolder(View showTaskRow, TextView taskRowName, TextView taskRowDetails, ImageView taskRowImg, CheckBox taskRowCheckBox) {
                super(showTaskRow);

                Assert.assertTrue(taskRowName != null);
                Assert.assertTrue(taskRowDetails != null);
                Assert.assertTrue(taskRowImg != null);
                Assert.assertTrue(taskRowCheckBox != null);

                mShowTaskRow = showTaskRow;
                mTaskRowName = taskRowName;
                mTaskRowDetails = taskRowDetails;
                mTaskRowImg = taskRowImg;
                mTaskRowCheckBox = taskRowCheckBox;
            }

            public void onRowClick() {
                TaskWrapper taskWrapper = mTaskWrappers.get(getAdapterPosition());
                Assert.assertTrue(taskWrapper != null);

                mActivity.startActivity(ShowTaskActivity.getIntent(taskWrapper.mData.TaskId, mActivity));
            }

            public void onCheckedChanged(boolean checked) {
                int position = getAdapterPosition();

                TaskWrapper taskWrapper = mTaskWrappers.get(position);
                Assert.assertTrue(taskWrapper != null);

                taskWrapper.mSelected = checked;
            }
        }

        public static class Data {
            public final int TaskId;
            public final String Name;
            public final String ScheduleText;
            public final boolean HasChildTasks;

            public Data(int taskId, String name, String scheduleText, boolean hasChildTasks) {
                Assert.assertTrue(!TextUtils.isEmpty(name));

                TaskId = taskId;
                Name = name;
                ScheduleText = scheduleText;
                HasChildTasks = hasChildTasks;
            }
        }

        public interface OnCheckedChangedListener {
            void OnCheckedChanged();
        }
    }
}