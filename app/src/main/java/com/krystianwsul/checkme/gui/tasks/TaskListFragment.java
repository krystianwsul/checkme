package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
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

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.tree.ModelNode;
import com.krystianwsul.checkme.gui.tree.NodeContainer;
import com.krystianwsul.checkme.gui.tree.TreeModelAdapter;
import com.krystianwsul.checkme.gui.tree.TreeNode;
import com.krystianwsul.checkme.gui.tree.TreeNodeCollection;
import com.krystianwsul.checkme.gui.tree.TreeViewAdapter;
import com.krystianwsul.checkme.loaders.TaskListLoader;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment implements LoaderManager.LoaderCallbacks<TaskListLoader.Data> {
    private static final String SELECTED_TASKS_KEY = "selectedTasks";

    private static final String ALL_TASKS_KEY = "allTasks";
    private static final String TASK_ID_KEY = "taskId";

    private RecyclerView mTaskListFragmentRecycler;
    private FloatingActionButton mTaskListFragmentFab;
    private TextView mEmptyText;

    private Integer mTaskId;

    private TaskListLoader.Data mData;

    private TreeViewAdapter mTreeViewAdapter;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mTreeViewAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            List<TreeNode> selected = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(selected != null);
            Assert.assertTrue(!selected.isEmpty());

            ArrayList<Integer> taskIds = Stream.of(selected)
                    .map(treeNode -> ((TaskAdapter.TaskWrapper) treeNode.getModelNode()).mTaskData.TaskId)
                    .collect(Collectors.toCollection(ArrayList::new));
            Assert.assertTrue(taskIds != null);
            Assert.assertTrue(!taskIds.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_task_edit:
                    Assert.assertTrue(selected.size() == 1);

                    TaskListLoader.TaskData taskData = ((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mTaskData;

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
                    do {
                        TreeNode treeNode = selected.get(0);
                        Assert.assertTrue(treeNode != null);

                        TaskAdapter.TaskWrapper taskWrapper = (TaskAdapter.TaskWrapper) treeNode.getModelNode();
                        Assert.assertTrue(taskWrapper != null);

                        taskWrapper.removeFromParent();

                        decrementSelected();
                    } while (!(selected = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(mData.DataId, taskIds);

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

    private List<Integer> mSelectedTaskIds;

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
            mSelectedTaskIds = savedInstanceState.getIntegerArrayList(SELECTED_TASKS_KEY);
            Assert.assertTrue(mSelectedTaskIds != null);
            Assert.assertTrue(!mSelectedTaskIds.isEmpty());
        }

        View view = getView();
        Assert.assertTrue(view != null);

        mTaskListFragmentRecycler = (RecyclerView) view.findViewById(R.id.task_list_recycler);
        mTaskListFragmentRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mTaskListFragmentFab = (FloatingActionButton) view.findViewById(R.id.task_list_fab);
        Assert.assertTrue(mTaskListFragmentFab != null);

        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onResume() {
        MyCrashlytics.log("TaskListFragment.onResume");

        super.onResume();
    }

    @Override
    public Loader<TaskListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new TaskListLoader(getActivity(), mTaskId);
    }

    @Override
    public void onLoadFinished(Loader<TaskListLoader.Data> loader, TaskListLoader.Data data) {
        mData = data;

        if (mTreeViewAdapter != null) {
            List<TreeNode> selected = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(selected != null);

            if (selected.isEmpty()) {
                Assert.assertTrue(!mSelectionCallback.hasActionMode());
                mSelectedTaskIds = null;
            } else {
                Assert.assertTrue(mSelectionCallback.hasActionMode());
                mSelectedTaskIds = Stream.of(selected)
                        .map(treeNode -> ((TaskAdapter.TaskWrapper) treeNode.getModelNode()).mTaskData.TaskId)
                        .collect(Collectors.toList());
            }
        }

        mTaskListFragmentFab.setOnClickListener(v -> {
            if (mTaskId == null)
                startActivity(CreateRootTaskActivity.getCreateIntent(getContext()));
            else
                startActivity(CreateChildTaskActivity.getCreateIntent(getActivity(), mTaskId));
        });

        mTreeViewAdapter = TaskAdapter.getAdapter(this, data, mSelectedTaskIds);
        Assert.assertTrue(mTreeViewAdapter != null);

        mTaskListFragmentRecycler.setAdapter(mTreeViewAdapter);

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        mTaskListFragmentFab.setVisibility(View.VISIBLE);

        if (mData.TaskDatas.isEmpty()) {
            mTaskListFragmentRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);

            if (mTaskId != null) {
                mEmptyText.setText(R.string.empty_child);
            } else {
                mEmptyText.setText(R.string.tasks_empty_root);
            }
        } else {
            mTaskListFragmentRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<TaskListLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTreeViewAdapter != null) {
            List<TreeNode> selected = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(selected != null);

            if (!selected.isEmpty()) {
                Assert.assertTrue(mSelectionCallback.hasActionMode());

                ArrayList<Integer> taskIds = Stream.of(selected)
                        .map(taskWrapper -> ((TaskAdapter.TaskWrapper) taskWrapper.getModelNode()).mTaskData.TaskId)
                        .collect(Collectors.toCollection(ArrayList::new));
                Assert.assertTrue(taskIds != null);
                Assert.assertTrue(!taskIds.isEmpty());

                outState.putIntegerArrayList(SELECTED_TASKS_KEY, taskIds);
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

    public static class TaskAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> implements TreeModelAdapter {
        private final WeakReference<TaskListFragment> mTaskListFragmentReference;

        private final int mDataId;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private WeakReference<TreeViewAdapter> mTreeViewAdapterReference;
        private WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

        public static TreeViewAdapter getAdapter(TaskListFragment taskListFragment, TaskListLoader.Data data, List<Integer> selectedTasks) {
            Assert.assertTrue(taskListFragment != null);
            Assert.assertTrue(data != null);

            TaskAdapter taskAdapter = new TaskAdapter(taskListFragment, data.DataId);

            return taskAdapter.initialize(data.TaskDatas, selectedTasks);
        }

        private TaskAdapter(TaskListFragment taskListFragment, int dataId) {
            Assert.assertTrue(taskListFragment != null);

            mTaskListFragmentReference = new WeakReference<>(taskListFragment);

            mDataId = dataId;
        }

        private TreeViewAdapter initialize(List<TaskListLoader.TaskData> taskDatas, List<Integer> selectedTasks) {
            Assert.assertTrue(taskDatas != null);

            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(false, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));
            mTreeNodeCollectionReference = new WeakReference<>(treeNodeCollection);

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            mTaskWrappers = new ArrayList<>();

            List<TreeNode> treeNodes = new ArrayList<>();

            for (TaskListLoader.TaskData taskData : taskDatas) {
                TaskWrapper taskWrapper = new TaskWrapper(new WeakReference<>(this), taskData);

                treeNodes.add(taskWrapper.initialize(selectedTasks, new WeakReference<>(treeNodeCollection)));

                mTaskWrappers.add(taskWrapper);
            }

            treeNodeCollection.setNodes(treeNodes);

            return treeViewAdapter;
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
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            TaskWrapper taskWrapper = mTaskWrappers.get(position);
            Assert.assertTrue(taskWrapper != null);

            taskWrapper.onBindViewHolder(viewHolder);
        }

        public void uncheck() {
            for (TaskWrapper taskWrapper : mTaskWrappers) {
                if (taskWrapper.mSelected) {
                    taskWrapper.mSelected = false;
                    notifyItemChanged(mTaskWrappers.indexOf(taskWrapper));
                }
            }
        }

        public List<TaskWrapper> getSelectedNodes() {
            return Stream.of(mTaskWrappers)
                    .filter(taskWrapper -> taskWrapper.mSelected)
                    .collect(Collectors.toList());
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

            DomainFactory.getDomainFactory(taskListFragment.getActivity()).setTaskEndTimeStamps(mDataId, taskIds);
        }

        public void remove(TaskWrapper taskWrapper) {
            Assert.assertTrue(taskWrapper != null);
            Assert.assertTrue(mTaskWrappers.contains(taskWrapper));

            mTaskWrappers.remove(taskWrapper);

            TreeNodeCollection treeNodeCollection = getTreeNodeCollection();
            Assert.assertTrue(treeNodeCollection != null);

            TreeNode treeNode = taskWrapper.getTreeNode();
            Assert.assertTrue(treeNode != null);

            treeNodeCollection.remove(treeNode);
        }

        public TaskListFragment getTaskListFragment() {
            TaskListFragment taskListFragment = mTaskListFragmentReference.get();
            Assert.assertTrue(taskListFragment != null);

            return taskListFragment;
        }

        public TreeViewAdapter getTreeViewAdapter() {
            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            return treeViewAdapter;
        }

        public TreeNodeCollection getTreeNodeCollection() {
            TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
            Assert.assertTrue(treeNodeCollection != null);

            return treeNodeCollection;
        }

        @Override
        public SelectionCallback getSelectionCallback() {
            TaskListFragment taskListFragment = getTaskListFragment();
            Assert.assertTrue(taskListFragment != null);

            return taskListFragment.mSelectionCallback;
        }

        private static class TaskWrapper implements ModelNode {
            private final WeakReference<TaskAdapter> mTaskAdapterReference;

            public final TaskListLoader.TaskData mTaskData;
            public boolean mSelected = false;

            private WeakReference<TreeNode> mTreeNodeReference;

            public TaskWrapper(WeakReference<TaskAdapter> taskAdapterReference, TaskListLoader.TaskData taskData) {
                Assert.assertTrue(taskAdapterReference != null);
                Assert.assertTrue(taskData != null);

                mTaskAdapterReference = taskAdapterReference;
                mTaskData = taskData;
            }

            public TreeNode initialize(List<Integer> selectedTasks, WeakReference<NodeContainer> nodeContainerReference) {
                Assert.assertTrue(nodeContainerReference != null);

                if (selectedTasks != null) {
                    Assert.assertTrue(!selectedTasks.isEmpty());
                    mSelected = selectedTasks.contains(mTaskData.TaskId);
                }

                TreeNode treeNode = new TreeNode(this, nodeContainerReference, false, mSelected);
                treeNode.setChildTreeNodes(new ArrayList<>());

                mTreeNodeReference = new WeakReference<>(treeNode);

                return treeNode;
            }

            private TreeNode getTreeNode() {
                TreeNode treeNode = mTreeNodeReference.get();
                Assert.assertTrue(treeNode != null);

                return treeNode;
            }

            private TaskAdapter getTaskAdapter() {
                TaskAdapter taskAdapter = mTaskAdapterReference.get();
                Assert.assertTrue(taskAdapter != null);

                return taskAdapter;
            }

            private TaskListFragment getTaskListFragment() {
                TaskAdapter taskAdapter = getTaskAdapter();
                Assert.assertTrue(taskAdapter != null);

                TaskListFragment taskListFragment = taskAdapter.getTaskListFragment();
                Assert.assertTrue(taskListFragment != null);

                return taskListFragment;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder viewHolder) {
                Assert.assertTrue(viewHolder != null);

                TaskHolder taskHolder = (TaskHolder) viewHolder;

                TreeNode treeNode = getTreeNode();
                Assert.assertTrue(treeNode != null);

                TaskListFragment taskListFragment = getTaskListFragment();
                Assert.assertTrue(taskListFragment != null);

                if (treeNode.isSelected())
                    taskHolder.mShowTaskRow.setBackgroundColor(ContextCompat.getColor(taskListFragment.getActivity(), R.color.selected));
                else
                    taskHolder.mShowTaskRow.setBackgroundColor(Color.TRANSPARENT);

                taskHolder.mShowTaskRow.setOnLongClickListener(treeNode.getOnLongClickListener());

                if (TextUtils.isEmpty(mTaskData.Children))
                    taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);
                else
                    taskHolder.mTaskRowImg.setVisibility(View.VISIBLE);

                taskHolder.mTaskRowName.setText(mTaskData.Name);

                if (TextUtils.isEmpty(mTaskData.ScheduleText)) {
                    taskHolder.mTaskRowDetails.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowDetails.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowDetails.setText(mTaskData.ScheduleText);
                }

                if (TextUtils.isEmpty(mTaskData.Children)) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowChildren.setText(mTaskData.Children);
                }

                taskHolder.mShowTaskRow.setOnClickListener(treeNode.getOnClickListener());
            }

            @Override
            public int getItemViewType() {
                return 0;
            }

            @Override
            public boolean selectable() {
                return true;
            }

            @Override
            public void onClick() {
                TaskListFragment taskListFragment = getTaskListFragment();
                Assert.assertTrue(taskListFragment != null);

                taskListFragment.getActivity().startActivity(ShowTaskActivity.getIntent(mTaskData.TaskId, taskListFragment.getActivity()));
            }

            @Override
            public boolean visibleWhenEmpty() {
                return true;
            }

            @Override
            public boolean visibleDuringActionMode() {
                return true;
            }

            @Override
            public int compareTo(@NonNull ModelNode another) {
                TaskListFragment taskListFragment = getTaskListFragment();
                Assert.assertTrue(taskListFragment != null);

                int comparison = Integer.valueOf(mTaskData.TaskId).compareTo(((TaskWrapper) another).mTaskData.TaskId);
                if (taskListFragment.mTaskId == null)
                    comparison = -comparison;

                return comparison;
            }

            public void removeFromParent() {
                TaskAdapter taskAdapter = getTaskAdapter();
                Assert.assertTrue(taskAdapter != null);

                taskAdapter.remove(this);
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
        }
    }
}