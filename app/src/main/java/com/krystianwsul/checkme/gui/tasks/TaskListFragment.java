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
import android.widget.LinearLayout;
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
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends Fragment implements LoaderManager.LoaderCallbacks<TaskListLoader.Data> {
    private static final String SELECTED_TASKS_KEY = "selectedTasks";
    private static final String EXPANDED_TASKS_KEY = "expandedTasks";

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
                case R.id.action_task_share:
                    Assert.assertTrue(selected.size() == 1);

                    Utils.share(((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mTaskData.Name, getActivity());
                    break;
                case R.id.action_task_edit:
                    Assert.assertTrue(selected.size() == 1);

                    TaskListLoader.TaskData taskData = ((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mTaskData;

                    startActivity(CreateTaskActivity.getEditIntent(getActivity(), taskData.TaskId));
                    break;
                case R.id.action_task_join:
                    if (mTaskId == null)
                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskIds));
                    else
                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskIds, mTaskId));
                    break;
                case R.id.action_task_delete:
                    do {
                        TreeNode treeNode = selected.get(0);
                        Assert.assertTrue(treeNode != null);

                        TaskAdapter.TaskWrapper taskWrapper = (TaskAdapter.TaskWrapper) treeNode.getModelNode();

                        taskWrapper.removeFromParent();

                        decrementSelected();
                    } while (!(selected = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(mData.DataId, taskIds);

                    updateSelectAll();

                    break;
                case R.id.action_task_add:
                    Assert.assertTrue(selected.size() == 1);

                    TaskListLoader.TaskData taskData1 = ((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mTaskData;
                    Assert.assertTrue(taskData1 != null);

                    startActivity(CreateTaskActivity.getCreateIntent(getActivity(), taskData1.TaskId));
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
            mActionMode.getMenu().findItem(R.id.action_task_share).setVisible(false);
            mActionMode.getMenu().findItem(R.id.action_task_join).setVisible(true);
            mActionMode.getMenu().findItem(R.id.action_task_edit).setVisible(false);

            List<TreeNode> selectedNodes = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(selectedNodes != null);
            Assert.assertTrue(!selectedNodes.isEmpty());

            mActionMode.getMenu().findItem(R.id.action_task_delete).setVisible(!containsLoop(selectedNodes));

            mActionMode.getMenu().findItem(R.id.action_task_add).setVisible(false);
        }

        @Override
        protected void onOtherAdded() {
            List<TreeNode> selectedNodes = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(selectedNodes != null);
            Assert.assertTrue(!selectedNodes.isEmpty());

            mActionMode.getMenu().findItem(R.id.action_task_delete).setVisible(!containsLoop(selectedNodes));
        }

        @Override
        protected void onLastRemoved() {
            mTaskListFragmentFab.setVisibility(View.VISIBLE);

            ((TaskListListener) getActivity()).onDestroyTaskActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {
            mActionMode.getMenu().findItem(R.id.action_task_share).setVisible(true);
            mActionMode.getMenu().findItem(R.id.action_task_join).setVisible(false);
            mActionMode.getMenu().findItem(R.id.action_task_edit).setVisible(true);
            mActionMode.getMenu().findItem(R.id.action_task_delete).setVisible(true);
            mActionMode.getMenu().findItem(R.id.action_task_add).setVisible(true);
        }

        @Override
        protected void onOtherRemoved() {
            List<TreeNode> selectedNodes = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(selectedNodes != null);
            Assert.assertTrue(selectedNodes.size() > 1);

            mActionMode.getMenu().findItem(R.id.action_task_delete).setVisible(!containsLoop(selectedNodes));
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean containsLoop(List<TreeNode> treeNodes) {
            Assert.assertTrue(treeNodes != null);
            Assert.assertTrue(treeNodes.size() > 1);

            for (TreeNode treeNode : treeNodes) {
                Assert.assertTrue(treeNode != null);

                List<TreeNode> parents = new ArrayList<>();
                addParents(parents, treeNode);

                for (TreeNode parent : parents) {
                    Assert.assertTrue(parent != null);

                    if (treeNodes.contains(parent))
                        return true;
                }
            }

            return false;
        }

        private void addParents(List<TreeNode> parents, TreeNode treeNode) {
            Assert.assertTrue(parents != null);
            Assert.assertTrue(treeNode != null);

            NodeContainer parent = treeNode.getParent();
            Assert.assertTrue(parent != null);

            if (!(parent instanceof TreeNode))
                return;

            TreeNode parentNode = (TreeNode) parent;

            parents.add(parentNode);
            addParents(parents, parentNode);
        }
    };

    private List<Integer> mSelectedTaskIds;
    private List<Integer> mExpandedTaskIds;

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

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_TASKS_KEY)) {
                mSelectedTaskIds = savedInstanceState.getIntegerArrayList(SELECTED_TASKS_KEY);
                Assert.assertTrue(mSelectedTaskIds != null);
                Assert.assertTrue(!mSelectedTaskIds.isEmpty());
            }

            if (savedInstanceState.containsKey(EXPANDED_TASKS_KEY)) {
                mExpandedTaskIds = savedInstanceState.getIntegerArrayList(EXPANDED_TASKS_KEY);
                Assert.assertTrue(mExpandedTaskIds != null);
                Assert.assertTrue(!mExpandedTaskIds.isEmpty());
            }
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

            List<Integer> expanded = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskIds();
            Assert.assertTrue(expanded != null);

            if (expanded.isEmpty()) {
                mExpandedTaskIds = null;
            } else {
                mExpandedTaskIds = expanded;
            }
        }

        mTaskListFragmentFab.setOnClickListener(v -> {
            if (mTaskId == null)
                startActivity(CreateTaskActivity.getCreateIntent(getContext()));
            else
                startActivity(CreateTaskActivity.getCreateIntent(getActivity(), mTaskId));
        });

        mTreeViewAdapter = TaskAdapter.getAdapter(this, data, mSelectedTaskIds, mExpandedTaskIds);
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

        updateSelectAll();
    }

    private void updateSelectAll() {
        Assert.assertTrue(mTreeViewAdapter != null);

        ((TaskListListener) getActivity()).setTaskSelectAllVisibility(mTreeViewAdapter.displayedSize() > 0);
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

            ArrayList<Integer> expandedTaskIds = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskIds();
            Assert.assertTrue(expandedTaskIds != null);

            if (!expandedTaskIds.isEmpty())
                outState.putIntegerArrayList(EXPANDED_TASKS_KEY, expandedTaskIds);
        }
    }

    public int getDataId() {
        Assert.assertTrue(mData != null);
        return mData.DataId;
    }

    public void destroyLoader() {
        getLoaderManager().destroyLoader(0);
    }

    public void selectAll() {
        mTreeViewAdapter.selectAll();
    }

    public interface TaskListListener {
        void onCreateTaskActionMode(ActionMode actionMode);
        void onDestroyTaskActionMode();

        void setTaskSelectAllVisibility(boolean selectAllVisible);
    }

    public static class TaskAdapter implements TreeModelAdapter, TaskParent {
        private final WeakReference<TaskListFragment> mTaskListFragmentReference;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private WeakReference<TreeViewAdapter> mTreeViewAdapterReference;
        private WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

        static TreeViewAdapter getAdapter(TaskListFragment taskListFragment, TaskListLoader.Data data, List<Integer> selectedTasks, List<Integer> expandedTasks) {
            Assert.assertTrue(taskListFragment != null);
            Assert.assertTrue(data != null);

            TaskAdapter taskAdapter = new TaskAdapter(taskListFragment);

            float density = taskListFragment.getActivity().getResources().getDisplayMetrics().density;

            return taskAdapter.initialize(density, data.TaskDatas, selectedTasks, expandedTasks);
        }

        private TaskAdapter(TaskListFragment taskListFragment) {
            Assert.assertTrue(taskListFragment != null);

            mTaskListFragmentReference = new WeakReference<>(taskListFragment);
        }

        private TreeViewAdapter initialize(float density, List<TaskListLoader.TaskData> taskDatas, List<Integer> selectedTasks, List<Integer> expandedTasks) {
            Assert.assertTrue(taskDatas != null);

            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(false, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));
            mTreeNodeCollectionReference = new WeakReference<>(treeNodeCollection);

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            mTaskWrappers = new ArrayList<>();

            List<TreeNode> treeNodes = new ArrayList<>();

            for (TaskListLoader.TaskData taskData : taskDatas) {
                TaskWrapper taskWrapper = new TaskWrapper(density, 0, new WeakReference<>(this), taskData);

                treeNodes.add(taskWrapper.initialize(selectedTasks, new WeakReference<>(treeNodeCollection), expandedTasks));

                mTaskWrappers.add(taskWrapper);
            }

            treeNodeCollection.setNodes(treeNodes);

            return treeViewAdapter;
        }

        @Override
        public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TaskListFragment taskListFragment = mTaskListFragmentReference.get();
            Assert.assertTrue(taskListFragment != null);

            LayoutInflater inflater = LayoutInflater.from(taskListFragment.getActivity());
            View showTaskRow = inflater.inflate(R.layout.row_task_list, parent, false);

            LinearLayout taskRowContainer = (LinearLayout) showTaskRow.findViewById(R.id.task_row_container);
            Assert.assertTrue(taskRowContainer != null);

            TextView taskRowName = (TextView) showTaskRow.findViewById(R.id.task_row_name);
            Assert.assertTrue(taskRowName != null);

            TextView taskRowDetails = (TextView) showTaskRow.findViewById(R.id.task_row_details);
            Assert.assertTrue(taskRowDetails != null);

            TextView taskRowChildren = (TextView) showTaskRow.findViewById(R.id.task_row_children);
            Assert.assertTrue(taskRowChildren != null);

            ImageView taskRowImage = (ImageView) showTaskRow.findViewById(R.id.task_row_img);
            Assert.assertTrue(taskRowImage != null);

            View taskRowSeparator = showTaskRow.findViewById(R.id.task_row_separator);
            Assert.assertTrue(taskRowSeparator != null);

            return new TaskHolder(showTaskRow, taskRowContainer, taskRowName, taskRowDetails, taskRowChildren, taskRowImage, taskRowSeparator);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
            Assert.assertTrue(treeViewAdapter != null);

            TreeNode treeNode = treeViewAdapter.getNode(position);
            Assert.assertTrue(treeNode != null);

            treeNode.onBindViewHolder(viewHolder);
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

        TaskListFragment getTaskListFragment() {
            TaskListFragment taskListFragment = mTaskListFragmentReference.get();
            Assert.assertTrue(taskListFragment != null);

            return taskListFragment;
        }

        TreeViewAdapter getTreeViewAdapter() {
            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            return treeViewAdapter;
        }

        TreeNodeCollection getTreeNodeCollection() {
            TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
            Assert.assertTrue(treeNodeCollection != null);

            return treeNodeCollection;
        }

        @Override
        public boolean hasActionMode() {
            TaskListFragment taskListFragment = getTaskListFragment();
            Assert.assertTrue(taskListFragment != null);

            return taskListFragment.mSelectionCallback.hasActionMode();
        }

        @Override
        public void incrementSelected() {
            TaskListFragment taskListFragment = getTaskListFragment();
            Assert.assertTrue(taskListFragment != null);

            taskListFragment.mSelectionCallback.incrementSelected();
        }

        @Override
        public void decrementSelected() {
            TaskListFragment taskListFragment = getTaskListFragment();
            Assert.assertTrue(taskListFragment != null);

            taskListFragment.mSelectionCallback.decrementSelected();
        }

        @Override
        public TaskAdapter getTaskAdapter() {
            return this;
        }

        ArrayList<Integer> getExpandedTaskIds() {
            return Stream.of(mTaskWrappers)
                    .flatMap(TaskWrapper::getExpandedTaskIds)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private static class TaskWrapper implements ModelNode, TaskParent {
            private final WeakReference<TaskParent> mTaskParentReference;

            final TaskListLoader.TaskData mTaskData;

            private WeakReference<TreeNode> mTreeNodeReference;

            private List<TaskWrapper> mTaskWrappers;

            private final float mDensity;
            private final int mIndentation;

            TaskWrapper(float density, int indentation, WeakReference<TaskParent> taskParentReference, TaskListLoader.TaskData taskData) {
                Assert.assertTrue(taskParentReference != null);
                Assert.assertTrue(taskData != null);

                mDensity = density;
                mIndentation = indentation;
                mTaskParentReference = taskParentReference;
                mTaskData = taskData;
            }

            TreeNode initialize(List<Integer> selectedTasks, WeakReference<NodeContainer> nodeContainerReference, List<Integer> expandedTasks) {
                Assert.assertTrue(nodeContainerReference != null);

                boolean selected = false;
                if (selectedTasks != null) {
                    Assert.assertTrue(!selectedTasks.isEmpty());
                    selected = selectedTasks.contains(mTaskData.TaskId);
                }

                boolean expanded = false;
                if (expandedTasks != null) {
                    Assert.assertTrue(!expandedTasks.isEmpty());
                    expanded = expandedTasks.contains(mTaskData.TaskId);
                }

                TreeNode treeNode = new TreeNode(this, nodeContainerReference, expanded, selected);

                mTreeNodeReference = new WeakReference<>(treeNode);

                mTaskWrappers = new ArrayList<>();

                List<TreeNode> treeNodes = new ArrayList<>();

                for (TaskListLoader.TaskData taskData : mTaskData.Children) {
                    TaskWrapper taskWrapper = new TaskWrapper(mDensity, mIndentation + 1, new WeakReference<>(this), taskData);

                    treeNodes.add(taskWrapper.initialize(selectedTasks, new WeakReference<>(treeNode), expandedTasks));

                    mTaskWrappers.add(taskWrapper);
                }

                treeNode.setChildTreeNodes(treeNodes);

                return treeNode;
            }

            private TreeNode getTreeNode() {
                TreeNode treeNode = mTreeNodeReference.get();
                Assert.assertTrue(treeNode != null);

                return treeNode;
            }

            private TaskParent getTaskParent() {
                TaskParent taskParent = mTaskParentReference.get();
                Assert.assertTrue(taskParent != null);

                return taskParent;
            }

            public TaskAdapter getTaskAdapter() {
                TaskParent taskParent = getTaskParent();
                Assert.assertTrue(taskParent != null);

                TaskAdapter taskAdapter = taskParent.getTaskAdapter();
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

                int padding = 48 * mIndentation;

                taskHolder.mTaskRowContainer.setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

                if (mTaskData.Children.isEmpty())
                    taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);
                else {
                    taskHolder.mTaskRowImg.setVisibility(View.VISIBLE);

                    if (treeNode.expanded())
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_less_black_36dp);
                    else
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_more_black_36dp);

                    taskHolder.mTaskRowImg.setOnClickListener(treeNode.getExpandListener());
                }

                taskHolder.mTaskRowName.setText(mTaskData.Name);

                if (TextUtils.isEmpty(mTaskData.ScheduleText)) {
                    taskHolder.mTaskRowDetails.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowDetails.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowDetails.setText(mTaskData.ScheduleText);
                }

                if (mTaskData.Children.isEmpty() || treeNode.expanded()) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowChildren.setText(Stream.of(mTaskData.Children)
                            .map(taskData -> taskData.Name)
                            .collect(Collectors.joining(", ")));
                }

                taskHolder.mTaskRowSeparator.setVisibility(treeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);

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
                if (taskListFragment.mTaskId == null && mIndentation == 0)
                    comparison = -comparison;

                return comparison;
            }

            void removeFromParent() {
                TaskParent taskParent = getTaskParent();
                Assert.assertTrue(taskParent != null);

                taskParent.remove(this);
            }

            public void remove(TaskWrapper taskWrapper) {
                Assert.assertTrue(taskWrapper != null);
                Assert.assertTrue(mTaskWrappers.contains(taskWrapper));

                mTaskWrappers.remove(taskWrapper);

                TreeNode treeNode = getTreeNode();
                Assert.assertTrue(treeNode != null);

                TreeNode childTreeNode = taskWrapper.getTreeNode();
                Assert.assertTrue(childTreeNode != null);

                treeNode.remove(childTreeNode);
            }

            Stream<Integer> getExpandedTaskIds() {
                List<Integer> expandedTaskIds = new ArrayList<>();

                TreeNode treeNode = getTreeNode();
                Assert.assertTrue(treeNode != null);

                if (treeNode.expanded()) {
                    expandedTaskIds.add(mTaskData.TaskId);

                    expandedTaskIds.addAll(Stream.of(mTaskWrappers)
                            .flatMap(TaskWrapper::getExpandedTaskIds)
                            .collect(Collectors.toList()));
                }

                return Stream.of(expandedTaskIds);
            }
        }

        class TaskHolder extends RecyclerView.ViewHolder {
            final View mShowTaskRow;
            final LinearLayout mTaskRowContainer;
            final TextView mTaskRowName;
            final TextView mTaskRowDetails;
            final TextView mTaskRowChildren;
            final ImageView mTaskRowImg;
            final View mTaskRowSeparator;

            TaskHolder(View showTaskRow, LinearLayout taskRowContainer, TextView taskRowName, TextView taskRowDetails, TextView taskRowChildren, ImageView taskRowImg, View taskRowSeparator) {
                super(showTaskRow);

                Assert.assertTrue(taskRowContainer != null);
                Assert.assertTrue(taskRowName != null);
                Assert.assertTrue(taskRowDetails != null);
                Assert.assertTrue(taskRowChildren != null);
                Assert.assertTrue(taskRowImg != null);
                Assert.assertTrue(taskRowSeparator != null);

                mShowTaskRow = showTaskRow;
                mTaskRowContainer = taskRowContainer;
                mTaskRowName = taskRowName;
                mTaskRowDetails = taskRowDetails;
                mTaskRowChildren = taskRowChildren;
                mTaskRowImg = taskRowImg;
                mTaskRowSeparator = taskRowSeparator;
            }
        }
    }

    private interface TaskParent {
        TaskAdapter getTaskAdapter();

        void remove(TaskAdapter.TaskWrapper taskWrapper);
    }
}