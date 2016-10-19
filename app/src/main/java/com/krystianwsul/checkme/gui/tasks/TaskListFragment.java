package com.krystianwsul.checkme.gui.tasks;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.tree.ModelNode;
import com.krystianwsul.checkme.gui.tree.NodeContainer;
import com.krystianwsul.checkme.gui.tree.TreeModelAdapter;
import com.krystianwsul.checkme.gui.tree.TreeNode;
import com.krystianwsul.checkme.gui.tree.TreeNodeCollection;
import com.krystianwsul.checkme.gui.tree.TreeViewAdapter;
import com.krystianwsul.checkme.loaders.TaskListLoader;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.Utils;

import junit.framework.Assert;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

public class TaskListFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<TaskListLoader.Data> {
    private static final String SELECTED_TASK_KEYS_KEY = "selectedTaskKeys";
    private static final String EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys";

    private static final String ALL_TASKS_KEY = "allTasks";
    private static final String TASK_KEY_KEY = "taskKey";

    private RecyclerView mTaskListFragmentRecycler;
    private FloatingActionButton mTaskListFragmentFab;
    private TextView mEmptyText;

    @Nullable
    private TaskKey mTaskKey;

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

            List<TaskAdapter.TaskWrapper> taskWrappers = Stream.of(selected)
                    .map(treeNode -> ((TaskAdapter.TaskWrapper) treeNode.getModelNode()))
                    .collect(Collectors.toList());

            List<TaskListLoader.ChildTaskData> childTaskDatas = Stream.of(taskWrappers)
                    .map(taskWrapper -> taskWrapper.mChildTaskData)
                    .collect(Collectors.toList());

            ArrayList<TaskKey> taskKeys = Stream.of(childTaskDatas)
                    .map(childTaskData -> childTaskData.mTaskKey)
                    .collect(Collectors.toCollection(ArrayList::new));

            switch (menuItem.getItemId()) {
                case R.id.action_task_share:
                    Utils.share(getShareData(childTaskDatas), getActivity());

                    break;
                case R.id.action_task_edit:
                    Assert.assertTrue(selected.size() == 1);

                    TaskListLoader.ChildTaskData childTaskData = ((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mChildTaskData;

                    startActivity(CreateTaskActivity.getEditIntent(getActivity(), childTaskData.mTaskKey));
                    break;
                case R.id.action_task_join:
                    if (mTaskKey == null)
                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskKeys));
                    else
                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskKeys, mTaskKey));
                    break;
                case R.id.action_task_delete:
                    do {
                        TreeNode treeNode = selected.get(0);
                        Assert.assertTrue(treeNode != null);

                        TaskAdapter.TaskWrapper taskWrapper = (TaskAdapter.TaskWrapper) treeNode.getModelNode();

                        taskWrapper.removeFromParent();

                        decrementSelected();
                    } while (!(selected = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(getActivity(), mData.DataId, taskKeys);

                    updateSelectAll();

                    break;
                case R.id.action_task_add:
                    Assert.assertTrue(selected.size() == 1);

                    TaskListLoader.ChildTaskData childTaskData1 = ((TaskAdapter.TaskWrapper) selected.get(0).getModelNode()).mChildTaskData;
                    Assert.assertTrue(childTaskData1 != null);

                    startActivity(CreateTaskActivity.getCreateIntent(getActivity(), childTaskData1.mTaskKey));
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

    @NonNull
    private String getShareData(@NonNull List<TaskListLoader.ChildTaskData> childTaskDatas) {
        Assert.assertTrue(!childTaskDatas.isEmpty());

        List<TaskListLoader.ChildTaskData> tree = new ArrayList<>();

        for (TaskListLoader.ChildTaskData childTaskData : childTaskDatas) {
            Assert.assertTrue(childTaskData != null);

            if (!inTree(tree, childTaskData))
                tree.add(childTaskData);
        }

        List<String> lines = new ArrayList<>();

        for (TaskListLoader.ChildTaskData childTaskData : tree)
            printTree(lines, 0, childTaskData);

        return TextUtils.join("\n", lines);
    }

    @Nullable
    public String getShareData() {
        Assert.assertTrue(mData != null);

        List<String> lines = new ArrayList<>();

        for (TaskListLoader.ChildTaskData childTaskData : mData.mChildTaskDatas)
            printTree(lines, 1, childTaskData);

        Log.e("asdf", "test:\n" + TextUtils.join("\n", lines));

        return TextUtils.join("\n", lines);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean inTree(@NonNull List<TaskListLoader.ChildTaskData> shareTree, @NonNull TaskListLoader.ChildTaskData childTaskData) {
        if (shareTree.isEmpty())
            return false;

        if (shareTree.contains(childTaskData))
            return true;

        return Stream.of(shareTree)
                .anyMatch(currChildTaskData -> inTree(currChildTaskData.Children, childTaskData));
    }

    private void printTree(@NonNull List<String> lines, int indentation, @NonNull TaskListLoader.ChildTaskData childTaskData) {
        lines.add(StringUtils.repeat("-", indentation) + childTaskData.Name);

        Stream.of(childTaskData.Children)
                .forEach(child -> printTree(lines, indentation + 1, child));
    }

    private List<TaskKey> mSelectedTaskKeys;
    private List<TaskKey> mExpandedTaskIds;

    public static TaskListFragment getInstance() {
        TaskListFragment taskListFragment = new TaskListFragment();

        Bundle args = new Bundle();
        args.putBoolean(ALL_TASKS_KEY, true);
        taskListFragment.setArguments(args);

        return taskListFragment;
    }

    @NonNull
    public static TaskListFragment getInstance(@NonNull TaskKey taskKey) {
        TaskListFragment taskListFragment = new TaskListFragment();

        Bundle args = new Bundle();
        args.putParcelable(TASK_KEY_KEY, taskKey);
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

        if (args.containsKey(TASK_KEY_KEY)) {
            Assert.assertTrue(!allTasks);

            mTaskKey = args.getParcelable(TASK_KEY_KEY);
            Assert.assertTrue(mTaskKey != null);
        } else {
            Assert.assertTrue(allTasks);

            mTaskKey = null;
        }

        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(SELECTED_TASK_KEYS_KEY)) {
                mSelectedTaskKeys = savedInstanceState.getParcelableArrayList(SELECTED_TASK_KEYS_KEY);
                Assert.assertTrue(mSelectedTaskKeys != null);
                Assert.assertTrue(!mSelectedTaskKeys.isEmpty());
            }

            if (savedInstanceState.containsKey(EXPANDED_TASK_KEYS_KEY)) {
                mExpandedTaskIds = savedInstanceState.getParcelableArrayList(EXPANDED_TASK_KEYS_KEY);
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
    public Loader<TaskListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new TaskListLoader(getActivity(), mTaskKey);
    }

    @Override
    public void onLoadFinished(Loader<TaskListLoader.Data> loader, TaskListLoader.Data data) {
        mData = data;

        if (mTreeViewAdapter != null) {
            List<TreeNode> selected = mTreeViewAdapter.getSelectedNodes();

            if (selected.isEmpty()) {
                Assert.assertTrue(!mSelectionCallback.hasActionMode());
                mSelectedTaskKeys = null;
            } else {
                Assert.assertTrue(mSelectionCallback.hasActionMode());
                mSelectedTaskKeys = Stream.of(selected)
                        .map(treeNode -> ((TaskAdapter.TaskWrapper) treeNode.getModelNode()).mChildTaskData.mTaskKey)
                        .collect(Collectors.toList());
            }

            List<TaskKey> expanded = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskKeys();

            if (expanded.isEmpty()) {
                mExpandedTaskIds = null;
            } else {
                mExpandedTaskIds = expanded;
            }
        }

        mTaskListFragmentFab.setOnClickListener(v -> {
            if (mTaskKey == null)
                startActivity(CreateTaskActivity.getCreateIntent(getContext()));
            else
                startActivity(CreateTaskActivity.getCreateIntent(getActivity(), mTaskKey));
        });

        mTreeViewAdapter = TaskAdapter.getAdapter(this, data, mSelectedTaskKeys, mExpandedTaskIds);

        mTaskListFragmentRecycler.setAdapter(mTreeViewAdapter.getAdapter());

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        mTaskListFragmentFab.setVisibility(View.VISIBLE);

        if (mData.mChildTaskDatas.isEmpty() && TextUtils.isEmpty(mData.mNote)) {
            mTaskListFragmentRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);

            if (mTaskKey != null) {
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

                ArrayList<TaskKey> taskKeys = Stream.of(selected)
                        .map(taskWrapper -> ((TaskAdapter.TaskWrapper) taskWrapper.getModelNode()).mChildTaskData.mTaskKey)
                        .collect(Collectors.toCollection(ArrayList::new));
                Assert.assertTrue(taskKeys != null);
                Assert.assertTrue(!taskKeys.isEmpty());

                outState.putParcelableArrayList(SELECTED_TASK_KEYS_KEY, taskKeys);
            }

            ArrayList<TaskKey> expandedTaskKeys = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskKeys();

            if (!expandedTaskKeys.isEmpty())
                outState.putParcelableArrayList(EXPANDED_TASK_KEYS_KEY, expandedTaskKeys);
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

        @NonNull
        private final TaskListFragment mTaskListFragment;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private TreeViewAdapter mTreeViewAdapter;
        private TreeNodeCollection mTreeNodeCollection;

        @NonNull
        static TreeViewAdapter getAdapter(TaskListFragment taskListFragment, TaskListLoader.Data data, List<TaskKey> selectedTaskKeys, List<TaskKey> expandedTaskKeys) {
            Assert.assertTrue(taskListFragment != null);
            Assert.assertTrue(data != null);

            TaskAdapter taskAdapter = new TaskAdapter(taskListFragment);

            float density = taskListFragment.getActivity().getResources().getDisplayMetrics().density;

            return taskAdapter.initialize(density, data, selectedTaskKeys, expandedTaskKeys);
        }

        private TaskAdapter(@NonNull TaskListFragment taskListFragment) {
            mTaskListFragment = taskListFragment;
        }

        @NonNull
        private TreeViewAdapter initialize(float density, @NonNull TaskListLoader.Data data, List<TaskKey> selectedTaskKeys, List<TaskKey> expandedTaskKeys) {
            mTreeViewAdapter = new TreeViewAdapter(false, this);

            mTreeNodeCollection = new TreeNodeCollection(mTreeViewAdapter);

            mTreeViewAdapter.setTreeNodeCollection(mTreeNodeCollection);

            List<TreeNode> treeNodes = new ArrayList<>();

            if (!TextUtils.isEmpty(data.mNote)) {
                NoteNode noteNode = new NoteNode(data.mNote);

                treeNodes.add(noteNode.initialize(mTreeNodeCollection));
            }

            mTaskWrappers = new ArrayList<>();

            for (TaskListLoader.ChildTaskData childTaskData : data.mChildTaskDatas) {
                Assert.assertTrue(childTaskData != null);

                TaskWrapper taskWrapper = new TaskWrapper(density, 0, this, childTaskData);

                treeNodes.add(taskWrapper.initialize(selectedTaskKeys, mTreeNodeCollection, expandedTaskKeys));

                mTaskWrappers.add(taskWrapper);
            }

            mTreeNodeCollection.setNodes(treeNodes);

            return mTreeViewAdapter;
        }

        @Override
        public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mTaskListFragment.getActivity());
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
            return mTaskListFragment;
        }

        @NonNull
        TreeViewAdapter getTreeViewAdapter() {
            Assert.assertTrue(mTreeViewAdapter != null);

            return mTreeViewAdapter;
        }

        @NonNull
        TreeNodeCollection getTreeNodeCollection() {
            Assert.assertTrue(mTreeNodeCollection != null);

            return mTreeNodeCollection;
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
        ArrayList<TaskKey> getExpandedTaskKeys() {
            return Stream.of(mTaskWrappers)
                    .flatMap(TaskWrapper::getExpandedTaskKeys)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private static class TaskWrapper implements ModelNode, TaskParent {
            @NonNull
            private final TaskParent mTaskParent;

            final TaskListLoader.ChildTaskData mChildTaskData;

            private TreeNode mTreeNode;

            private List<TaskWrapper> mTaskWrappers;

            private final float mDensity;
            private final int mIndentation;

            TaskWrapper(float density, int indentation, @NonNull TaskParent taskParent, @NonNull TaskListLoader.ChildTaskData childTaskData) {
                mDensity = density;
                mIndentation = indentation;
                mTaskParent = taskParent;
                mChildTaskData = childTaskData;
            }

            @NonNull
            TreeNode initialize(@Nullable List<TaskKey> selectedTaskKeys, @NonNull NodeContainer nodeContainer, @Nullable List<TaskKey> expandedTaskKeys) {
                boolean selected = false;
                if (selectedTaskKeys != null) {
                    Assert.assertTrue(!selectedTaskKeys.isEmpty());
                    selected = selectedTaskKeys.contains(mChildTaskData.mTaskKey);
                }

                boolean expanded = false;
                if (expandedTaskKeys != null) {
                    Assert.assertTrue(!expandedTaskKeys.isEmpty());
                    expanded = expandedTaskKeys.contains(mChildTaskData.mTaskKey);
                }

                mTreeNode = new TreeNode(this, nodeContainer, expanded, selected);

                mTaskWrappers = new ArrayList<>();

                List<TreeNode> treeNodes = new ArrayList<>();

                for (TaskListLoader.ChildTaskData childTaskData : mChildTaskData.Children) {
                    Assert.assertTrue(childTaskData != null);

                    TaskWrapper taskWrapper = new TaskWrapper(mDensity, mIndentation + 1, this, childTaskData);

                    treeNodes.add(taskWrapper.initialize(selectedTaskKeys, mTreeNode, expandedTaskKeys));

                    mTaskWrappers.add(taskWrapper);
                }

                mTreeNode.setChildTreeNodes(treeNodes);

                return mTreeNode;
            }

            @NonNull
            private TreeNode getTreeNode() {
                Assert.assertTrue(mTreeNode != null);

                return mTreeNode;
            }

            @NonNull
            private TaskParent getTaskParent() {
                return mTaskParent;
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

                if ((mChildTaskData.Children.isEmpty() || treeNode.expanded()) && TextUtils.isEmpty(mChildTaskData.mNote)) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);

                    String text;
                    if (!mChildTaskData.Children.isEmpty() && !treeNode.expanded()) {
                        text = Stream.of(mChildTaskData.Children)
                                .map(taskData -> taskData.Name)
                                .collect(Collectors.joining(", "));
                    } else {
                        Assert.assertTrue(!TextUtils.isEmpty(mChildTaskData.mNote));

                        text = mChildTaskData.mNote;
                    }

                    Assert.assertTrue(!TextUtils.isEmpty(text));

                    taskHolder.mTaskRowChildren.setText(text);
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

                activity.startActivity(ShowTaskActivity.newIntent(activity, mChildTaskData.mTaskKey));
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
            public boolean separatorVisibleWhenNotExpanded() {
                return false;
            }

            @Override
            public int compareTo(@NonNull ModelNode another) {
                if (another instanceof TaskWrapper) {
                    TaskListFragment taskListFragment = getTaskListFragment();

                    int comparison = mChildTaskData.mStartExactTimeStamp.compareTo(((TaskWrapper) another).mChildTaskData.mStartExactTimeStamp);
                    if (taskListFragment.mTaskKey == null && mIndentation == 0)
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
            Stream<TaskKey> getExpandedTaskKeys() {
                List<TaskKey> expandedTaskKeys = new ArrayList<>();

                TreeNode treeNode = getTreeNode();

                if (treeNode.expanded()) {
                    expandedTaskKeys.add(mChildTaskData.mTaskKey);

                    expandedTaskKeys.addAll(Stream.of(mTaskWrappers)
                            .flatMap(TaskWrapper::getExpandedTaskKeys)
                            .collect(Collectors.toList()));
                }

                return Stream.of(expandedTaskKeys);
            }
        }

        private static class NoteNode implements ModelNode {
            private final String mNote;

            private TreeNode mTreeNode;

            NoteNode(@NonNull String note) {
                Assert.assertTrue(!TextUtils.isEmpty(note));

                mNote = note;
            }

            @NonNull
            TreeNode initialize(@NonNull TreeNodeCollection treeNodeCollection) {
                mTreeNode = new TreeNode(this, treeNodeCollection, false, false);
                mTreeNode.setChildTreeNodes(new ArrayList<>());

                return mTreeNode;
            }

            @NonNull
            private TreeNode getTreeNode() {
                Assert.assertTrue(mTreeNode != null);

                return mTreeNode;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                TaskHolder taskHolder = (TaskHolder) viewHolder;

                TreeNode treeNode = getTreeNode();

                taskHolder.mShowTaskRow.setBackgroundColor(Color.TRANSPARENT);

                taskHolder.mShowTaskRow.setOnLongClickListener(null);

                taskHolder.mTaskRowContainer.setPadding(0, 0, 0, 0);

                taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);

                taskHolder.mTaskRowName.setText(mNote);
                taskHolder.mTaskRowName.setSingleLine(false);

                taskHolder.mTaskRowDetails.setVisibility(View.GONE);

                taskHolder.mTaskRowChildren.setVisibility(View.GONE);

                taskHolder.mTaskRowSeparator.setVisibility(treeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);

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
            public boolean separatorVisibleWhenNotExpanded() {
                return true;
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