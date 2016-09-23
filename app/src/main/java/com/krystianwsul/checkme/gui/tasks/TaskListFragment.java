package com.krystianwsul.checkme.gui.tasks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
            Assert.assertTrue(!selected.isEmpty());

            ArrayList<Integer> taskIds = Stream.of(selected)
                    .map(treeNode -> ((TaskAdapter.TaskWrapper) treeNode.getModelNode()).mChildTaskData.TaskId)
                    .collect(Collectors.toCollection(ArrayList::new));
            Assert.assertTrue(!taskIds.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_task_share:
                    Assert.assertTrue(selected.size() == 1);

                    Utils.share(((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mChildTaskData.Name, getActivity());
                    break;
                case R.id.action_task_edit:
                    Assert.assertTrue(selected.size() == 1);

                    TaskListLoader.ChildTaskData childTaskData = ((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mChildTaskData;

                    startActivity(CreateTaskActivity.getEditIntent(getActivity(), childTaskData.TaskId));
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

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(getActivity(), mData.DataId, taskIds);

                    updateSelectAll();

                    break;
                case R.id.action_task_add:
                    Assert.assertTrue(selected.size() == 1);

                    TaskListLoader.ChildTaskData childTaskData1 = ((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mChildTaskData;
                    Assert.assertTrue(childTaskData1 != null);

                    startActivity(CreateTaskActivity.getCreateIntent(getActivity(), childTaskData1.TaskId));
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mTreeViewAdapter.onCreateActionMode();

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
            Assert.assertTrue(!selectedNodes.isEmpty());

            mActionMode.getMenu().findItem(R.id.action_task_delete).setVisible(!containsLoop(selectedNodes));

            mActionMode.getMenu().findItem(R.id.action_task_add).setVisible(false);
        }

        @Override
        protected void onOtherAdded() {
            List<TreeNode> selectedNodes = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(!selectedNodes.isEmpty());

            mActionMode.getMenu().findItem(R.id.action_task_delete).setVisible(!containsLoop(selectedNodes));
        }

        @Override
        protected void onLastRemoved() {
            mTreeViewAdapter.onDestroyActionMode();

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

            if (selected.isEmpty()) {
                Assert.assertTrue(!mSelectionCallback.hasActionMode());
                mSelectedTaskIds = null;
            } else {
                Assert.assertTrue(mSelectionCallback.hasActionMode());
                mSelectedTaskIds = Stream.of(selected)
                        .map(treeNode -> ((TaskAdapter.TaskWrapper) treeNode.getModelNode()).mChildTaskData.TaskId)
                        .collect(Collectors.toList());
            }

            List<Integer> expanded = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskIds();

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

        mTaskListFragmentRecycler.setAdapter(mTreeViewAdapter.getAdapter());

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        mTaskListFragmentFab.setVisibility(View.VISIBLE);

        if (mData.mChildTaskDatas.isEmpty() && TextUtils.isEmpty(mData.mNote)) {
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
        TaskAdapter taskAdapter = (TaskAdapter) mTreeViewAdapter.getTreeModelAdapter();

        ((TaskListListener) getActivity()).setTaskSelectAllVisibility(!taskAdapter.mTaskWrappers.isEmpty());
    }

    @Override
    public void onLoaderReset(Loader<TaskListLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTreeViewAdapter != null) {
            List<TreeNode> selected = mTreeViewAdapter.getSelectedNodes();

            if (!selected.isEmpty()) {
                Assert.assertTrue(mSelectionCallback.hasActionMode());

                ArrayList<Integer> taskIds = Stream.of(selected)
                        .map(taskWrapper -> ((TaskAdapter.TaskWrapper) taskWrapper.getModelNode()).mChildTaskData.TaskId)
                        .collect(Collectors.toCollection(ArrayList::new));
                Assert.assertTrue(taskIds != null);
                Assert.assertTrue(!taskIds.isEmpty());

                outState.putIntegerArrayList(SELECTED_TASKS_KEY, taskIds);
            }

            ArrayList<Integer> expandedTaskIds = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskIds();

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
        private static final int TYPE_TASK = 0;
        private static final int TYPE_NOTE = 1;

        private final WeakReference<TaskListFragment> mTaskListFragmentReference;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private WeakReference<TreeViewAdapter> mTreeViewAdapterReference;
        private WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

        @NonNull
        static TreeViewAdapter getAdapter(TaskListFragment taskListFragment, TaskListLoader.Data data, List<Integer> selectedTasks, List<Integer> expandedTasks) {
            Assert.assertTrue(taskListFragment != null);
            Assert.assertTrue(data != null);

            TaskAdapter taskAdapter = new TaskAdapter(taskListFragment);

            float density = taskListFragment.getActivity().getResources().getDisplayMetrics().density;

            return taskAdapter.initialize(density, data, selectedTasks, expandedTasks);
        }

        private TaskAdapter(@NonNull TaskListFragment taskListFragment) {
            mTaskListFragmentReference = new WeakReference<>(taskListFragment);
        }

        private TreeViewAdapter initialize(float density, @NonNull TaskListLoader.Data data, List<Integer> selectedTasks, List<Integer> expandedTasks) {
            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(false, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));
            mTreeNodeCollectionReference = new WeakReference<>(treeNodeCollection);

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            List<TreeNode> treeNodes = new ArrayList<>();

            if (!TextUtils.isEmpty(data.mNote)) {
                NoteNode noteNode = new NoteNode(data.mNote);

                treeNodes.add(noteNode.initialize(treeNodeCollection));
            }

            mTaskWrappers = new ArrayList<>();

            for (TaskListLoader.ChildTaskData childTaskData : data.mChildTaskDatas) {
                TaskWrapper taskWrapper = new TaskWrapper(density, 0, new WeakReference<>(this), childTaskData);

                treeNodes.add(taskWrapper.initialize(selectedTasks, new WeakReference<>(treeNodeCollection), expandedTasks));

                mTaskWrappers.add(taskWrapper);
            }

            treeNodeCollection.setNodes(treeNodes);

            return treeViewAdapter;
        }

        @Override
        public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
            getTreeViewAdapter().getNode(position).onBindViewHolder(viewHolder);
        }

        public void remove(@NonNull TaskWrapper taskWrapper) {
            Assert.assertTrue(mTaskWrappers.contains(taskWrapper));

            mTaskWrappers.remove(taskWrapper);

            TreeNodeCollection treeNodeCollection = getTreeNodeCollection();

            TreeNode treeNode = taskWrapper.getTreeNode();

            treeNodeCollection.remove(treeNode);
        }

        @NonNull
        TaskListFragment getTaskListFragment() {
            TaskListFragment taskListFragment = mTaskListFragmentReference.get();
            Assert.assertTrue(taskListFragment != null);

            return taskListFragment;
        }

        @NonNull
        TreeViewAdapter getTreeViewAdapter() {
            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            return treeViewAdapter;
        }

        @NonNull
        TreeNodeCollection getTreeNodeCollection() {
            TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
            Assert.assertTrue(treeNodeCollection != null);

            return treeNodeCollection;
        }

        @Override
        public boolean hasActionMode() {
            return getTaskListFragment().mSelectionCallback.hasActionMode();
        }

        @Override
        public void incrementSelected() {
            getTaskListFragment().mSelectionCallback.incrementSelected();
        }

        @Override
        public void decrementSelected() {
            getTaskListFragment().mSelectionCallback.decrementSelected();
        }

        @NonNull
        @Override
        public TaskAdapter getTaskAdapter() {
            return this;
        }

        @NonNull
        ArrayList<Integer> getExpandedTaskIds() {
            return Stream.of(mTaskWrappers)
                    .flatMap(TaskWrapper::getExpandedTaskIds)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private static class TaskWrapper implements ModelNode, TaskParent {
            private final WeakReference<TaskParent> mTaskParentReference;

            final TaskListLoader.ChildTaskData mChildTaskData;

            private WeakReference<TreeNode> mTreeNodeReference;

            private List<TaskWrapper> mTaskWrappers;

            private final float mDensity;
            private final int mIndentation;

            TaskWrapper(float density, int indentation, WeakReference<TaskParent> taskParentReference, TaskListLoader.ChildTaskData childTaskData) {
                Assert.assertTrue(taskParentReference != null);
                Assert.assertTrue(childTaskData != null);

                mDensity = density;
                mIndentation = indentation;
                mTaskParentReference = taskParentReference;
                mChildTaskData = childTaskData;
            }

            TreeNode initialize(@Nullable List<Integer> selectedTasks, @NonNull WeakReference<NodeContainer> nodeContainerReference, @Nullable List<Integer> expandedTasks) {
                boolean selected = false;
                if (selectedTasks != null) {
                    Assert.assertTrue(!selectedTasks.isEmpty());
                    selected = selectedTasks.contains(mChildTaskData.TaskId);
                }

                boolean expanded = false;
                if (expandedTasks != null) {
                    Assert.assertTrue(!expandedTasks.isEmpty());
                    expanded = expandedTasks.contains(mChildTaskData.TaskId);
                }

                TreeNode treeNode = new TreeNode(this, nodeContainerReference, expanded, selected);

                mTreeNodeReference = new WeakReference<>(treeNode);

                mTaskWrappers = new ArrayList<>();

                List<TreeNode> treeNodes = new ArrayList<>();

                for (TaskListLoader.ChildTaskData childTaskData : mChildTaskData.Children) {
                    TaskWrapper taskWrapper = new TaskWrapper(mDensity, mIndentation + 1, new WeakReference<>(this), childTaskData);

                    treeNodes.add(taskWrapper.initialize(selectedTasks, new WeakReference<>(treeNode), expandedTasks));

                    mTaskWrappers.add(taskWrapper);
                }

                treeNode.setChildTreeNodes(treeNodes);

                return treeNode;
            }

            @NonNull
            private TreeNode getTreeNode() {
                TreeNode treeNode = mTreeNodeReference.get();
                Assert.assertTrue(treeNode != null);

                return treeNode;
            }

            @NonNull
            private TaskParent getTaskParent() {
                TaskParent taskParent = mTaskParentReference.get();
                Assert.assertTrue(taskParent != null);

                return taskParent;
            }

            @NonNull
            public TaskAdapter getTaskAdapter() {
                return getTaskParent().getTaskAdapter();
            }

            @NonNull
            private TaskListFragment getTaskListFragment() {
                return getTaskAdapter().getTaskListFragment();
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                TaskHolder taskHolder = (TaskHolder) viewHolder;

                TreeNode treeNode = getTreeNode();

                TaskListFragment taskListFragment = getTaskListFragment();

                if (treeNode.isSelected())
                    taskHolder.mShowTaskRow.setBackgroundColor(ContextCompat.getColor(taskListFragment.getActivity(), R.color.selected));
                else
                    taskHolder.mShowTaskRow.setBackgroundColor(Color.TRANSPARENT);

                taskHolder.mShowTaskRow.setOnLongClickListener(treeNode.getOnLongClickListener());

                int padding = 48 * mIndentation;

                taskHolder.mTaskRowContainer.setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

                if (mChildTaskData.Children.isEmpty())
                    taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);
                else {
                    taskHolder.mTaskRowImg.setVisibility(View.VISIBLE);

                    if (treeNode.expanded())
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_less_black_36dp);
                    else
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_more_black_36dp);

                    taskHolder.mTaskRowImg.setOnClickListener(treeNode.getExpandListener());
                }

                taskHolder.mTaskRowName.setText(mChildTaskData.Name);
                taskHolder.mTaskRowName.setSingleLine(true);

                if (TextUtils.isEmpty(mChildTaskData.ScheduleText)) {
                    taskHolder.mTaskRowDetails.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowDetails.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowDetails.setText(mChildTaskData.ScheduleText);
                }

                if (mChildTaskData.Children.isEmpty() || treeNode.expanded()) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowChildren.setText(Stream.of(mChildTaskData.Children)
                            .map(taskData -> taskData.Name)
                            .collect(Collectors.joining(", ")));
                }

                taskHolder.mTaskRowSeparator.setVisibility(treeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);

                taskHolder.mShowTaskRow.setOnClickListener(treeNode.getOnClickListener());
            }

            @Override
            public int getItemViewType() {
                return TYPE_TASK;
            }

            @Override
            public boolean selectable() {
                return true;
            }

            @Override
            public void onClick() {
                Activity activity = getTaskListFragment().getActivity();
                Assert.assertTrue(activity != null);

                activity.startActivity(ShowTaskActivity.getIntent(mChildTaskData.TaskId, activity));
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
                if (another instanceof TaskWrapper) {
                    TaskListFragment taskListFragment = getTaskListFragment();

                    int comparison = Integer.valueOf(mChildTaskData.TaskId).compareTo(((TaskWrapper) another).mChildTaskData.TaskId);
                    if (taskListFragment.mTaskId == null && mIndentation == 0)
                        comparison = -comparison;

                    return comparison;
                } else {
                    Assert.assertTrue(another instanceof NoteNode);

                    return 1;
                }
            }

            void removeFromParent() {
                getTaskParent().remove(this);
            }

            public void remove(@NonNull TaskWrapper taskWrapper) {
                Assert.assertTrue(mTaskWrappers.contains(taskWrapper));

                mTaskWrappers.remove(taskWrapper);

                TreeNode treeNode = getTreeNode();

                TreeNode childTreeNode = taskWrapper.getTreeNode();

                treeNode.remove(childTreeNode);
            }

            @NonNull
            Stream<Integer> getExpandedTaskIds() {
                List<Integer> expandedTaskIds = new ArrayList<>();

                TreeNode treeNode = getTreeNode();

                if (treeNode.expanded()) {
                    expandedTaskIds.add(mChildTaskData.TaskId);

                    expandedTaskIds.addAll(Stream.of(mTaskWrappers)
                            .flatMap(TaskWrapper::getExpandedTaskIds)
                            .collect(Collectors.toList()));
                }

                return Stream.of(expandedTaskIds);
            }
        }

        private static class NoteNode implements ModelNode {
            private final String mNote;

            NoteNode(@NonNull String note) {
                Assert.assertTrue(!TextUtils.isEmpty(note));

                mNote = note;
            }

            @NonNull
            TreeNode initialize(@NonNull TreeNodeCollection treeNodeCollection) {
                TreeNode treeNode = new TreeNode(this, new WeakReference<>(treeNodeCollection), false, false);

                treeNode.setChildTreeNodes(new ArrayList<>());

                return treeNode;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                TaskHolder taskHolder = (TaskHolder) viewHolder;

                taskHolder.mShowTaskRow.setBackgroundColor(Color.TRANSPARENT);

                taskHolder.mShowTaskRow.setOnLongClickListener(null);

                taskHolder.mTaskRowContainer.setPadding(0, 0, 0, 0);

                taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);

                taskHolder.mTaskRowName.setText(mNote);
                taskHolder.mTaskRowName.setSingleLine(false);

                taskHolder.mTaskRowDetails.setVisibility(View.GONE);

                taskHolder.mTaskRowChildren.setVisibility(View.GONE);

                taskHolder.mTaskRowSeparator.setVisibility(View.VISIBLE);

                taskHolder.mShowTaskRow.setOnClickListener(null);
            }

            @Override
            public int getItemViewType() {
                return TYPE_NOTE;
            }

            @Override
            public boolean selectable() {
                return false;
            }

            @Override
            public void onClick() {

            }

            @Override
            public boolean visibleWhenEmpty() {
                return true;
            }

            @Override
            public boolean visibleDuringActionMode() {
                return false;
            }

            @Override
            public int compareTo(@NonNull ModelNode o) {
                Assert.assertTrue(o instanceof TaskWrapper);

                return -1;
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
        @NonNull
        TaskAdapter getTaskAdapter();

        void remove(@NonNull TaskAdapter.TaskWrapper taskWrapper);
    }
}