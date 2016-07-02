package com.krystianwsul.checkme.gui.tasks;


import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.tree.ModelNode;
import com.krystianwsul.checkme.gui.tree.NodeContainer;
import com.krystianwsul.checkme.gui.tree.TreeModelAdapter;
import com.krystianwsul.checkme.gui.tree.TreeNode;
import com.krystianwsul.checkme.gui.tree.TreeNodeCollection;
import com.krystianwsul.checkme.gui.tree.TreeViewAdapter;
import com.krystianwsul.checkme.loaders.CreateChildTaskLoader;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ParentFragment extends DialogFragment {
    private static final String EXPANDED_TASKS_KEY = "expandedTasks";

    private RecyclerView mRecyclerView;

    private TreeMap<Integer, CreateChildTaskLoader.TaskData> mTaskDatas;
    private Listener mListener;

    private TreeViewAdapter mTreeViewAdapter;
    private List<Integer> mExpandedTaskIds;

    public static ParentFragment newInstance() {
        return new ParentFragment();
    }

    public ParentFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            if (savedInstanceState.containsKey(EXPANDED_TASKS_KEY)) {
                mExpandedTaskIds = savedInstanceState.getIntegerArrayList(EXPANDED_TASKS_KEY);
                Assert.assertTrue(mExpandedTaskIds != null);
                Assert.assertTrue(!mExpandedTaskIds.isEmpty());
            }
        }

        MaterialDialog materialDialog = new MaterialDialog.Builder(getActivity())
                .title(R.string.parent_dialog_title)
                .customView(R.layout.fragment_parent, false)
                .build();

        mRecyclerView = (RecyclerView) materialDialog.getCustomView();
        Assert.assertTrue(mRecyclerView != null);

        return materialDialog;
    }

    public void initialize(TreeMap<Integer, CreateChildTaskLoader.TaskData> taskDatas, Listener listener) {
        Assert.assertTrue(taskDatas != null);
        Assert.assertTrue(listener != null);

        mTaskDatas = taskDatas;
        mListener = listener;
        if (getActivity() != null)
            initialize();
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        if (mTaskDatas != null)
            initialize();
    }

    private void initialize() {
        Assert.assertTrue(mTaskDatas != null);
        Assert.assertTrue(mListener != null);
        Assert.assertTrue(getActivity() != null);

        mRecyclerView.setLayoutManager(new LinearLayoutManager(getActivity()));

        if (mTreeViewAdapter != null) {
            List<Integer> expanded = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskIds();
            Assert.assertTrue(expanded != null);

            if (expanded.isEmpty()) {
                mExpandedTaskIds = null;
            } else {
                mExpandedTaskIds = expanded;
            }
        }

        mTreeViewAdapter = TaskAdapter.getAdapter(this, mTaskDatas, mExpandedTaskIds);
        Assert.assertTrue(mTreeViewAdapter != null);

        mRecyclerView.setAdapter(mTreeViewAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTreeViewAdapter != null) {
            ArrayList<Integer> expandedTaskIds = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedTaskIds();
            Assert.assertTrue(expandedTaskIds != null);

            if (!expandedTaskIds.isEmpty())
                outState.putIntegerArrayList(EXPANDED_TASKS_KEY, expandedTaskIds);
        }
    }

    public static class TaskAdapter implements TreeModelAdapter, TaskParent {
        private final WeakReference<ParentFragment> mParentFragmentReference;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private WeakReference<TreeViewAdapter> mTreeViewAdapterReference;
        private WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

        public static TreeViewAdapter getAdapter(ParentFragment parentFragment, TreeMap<Integer, CreateChildTaskLoader.TaskData> taskDatas, List<Integer> expandedTasks) {
            Assert.assertTrue(parentFragment != null);
            Assert.assertTrue(taskDatas != null);

            TaskAdapter taskAdapter = new TaskAdapter(parentFragment);

            float density = parentFragment.getResources().getDisplayMetrics().density;

            return taskAdapter.initialize(density, taskDatas, expandedTasks);
        }

        private TaskAdapter(ParentFragment parentFragment) {
            Assert.assertTrue(parentFragment != null);

            mParentFragmentReference = new WeakReference<>(parentFragment);
        }

        private TreeViewAdapter initialize(float density, TreeMap<Integer, CreateChildTaskLoader.TaskData> taskDatas, List<Integer> expandedTasks) {
            Assert.assertTrue(taskDatas != null);

            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(false, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));
            mTreeNodeCollectionReference = new WeakReference<>(treeNodeCollection);

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            mTaskWrappers = new ArrayList<>();

            List<TreeNode> treeNodes = new ArrayList<>();

            for (CreateChildTaskLoader.TaskData taskData : taskDatas.values()) {
                TaskWrapper taskWrapper = new TaskWrapper(density, 0, new WeakReference<>(this), taskData);

                treeNodes.add(taskWrapper.initialize(new WeakReference<>(treeNodeCollection), expandedTasks));

                mTaskWrappers.add(taskWrapper);
            }

            treeNodeCollection.setNodes(treeNodes);

            return treeViewAdapter;
        }

        @Override
        public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            ParentFragment parentFragment = getParentFragment();
            Assert.assertTrue(parentFragment != null);

            LayoutInflater inflater = LayoutInflater.from(parentFragment.getActivity());
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

        public ParentFragment getParentFragment() {
            ParentFragment parentFragment = mParentFragmentReference.get();
            Assert.assertTrue(parentFragment != null);

            return parentFragment;
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
        public boolean hasActionMode() {
            return false;
        }

        @Override
        public void incrementSelected() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void decrementSelected() {
            throw new UnsupportedOperationException();
        }

        @Override
        public TaskAdapter getTaskAdapter() {
            return this;
        }

        public ArrayList<Integer> getExpandedTaskIds() {
            return Stream.of(mTaskWrappers)
                    .flatMap(TaskWrapper::getExpandedTaskIds)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private static class TaskWrapper implements ModelNode, TaskParent {
            private final WeakReference<TaskParent> mTaskParentReference;

            public final CreateChildTaskLoader.TaskData mTaskData;

            private WeakReference<TreeNode> mTreeNodeReference;

            private List<TaskWrapper> mTaskWrappers;

            private final float mDensity;
            private final int mIndentation;

            public TaskWrapper(float density, int indentation, WeakReference<TaskParent> taskParentReference, CreateChildTaskLoader.TaskData taskData) {
                Assert.assertTrue(taskParentReference != null);
                Assert.assertTrue(taskData != null);

                mDensity = density;
                mIndentation = indentation;
                mTaskParentReference = taskParentReference;
                mTaskData = taskData;
            }

            public TreeNode initialize(WeakReference<NodeContainer> nodeContainerReference, List<Integer> expandedTasks) {
                Assert.assertTrue(nodeContainerReference != null);

                boolean expanded = false;
                if (expandedTasks != null) {
                    Assert.assertTrue(!expandedTasks.isEmpty());
                    expanded = expandedTasks.contains(mTaskData.TaskId);
                }

                TreeNode treeNode = new TreeNode(this, nodeContainerReference, expanded, false);

                mTreeNodeReference = new WeakReference<>(treeNode);

                mTaskWrappers = new ArrayList<>();

                List<TreeNode> treeNodes = new ArrayList<>();

                for (CreateChildTaskLoader.TaskData taskData : mTaskData.TaskDatas.values()) {
                    TaskWrapper taskWrapper = new TaskWrapper(mDensity, mIndentation + 1, new WeakReference<>(this), taskData);

                    treeNodes.add(taskWrapper.initialize(new WeakReference<>(treeNode), expandedTasks));

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

            private ParentFragment getParentFragment() {
                TaskAdapter taskAdapter = getTaskAdapter();
                Assert.assertTrue(taskAdapter != null);

                ParentFragment parentFragment = taskAdapter.getParentFragment();
                Assert.assertTrue(parentFragment != null);

                return parentFragment;
            }

            @Override
            public void onBindViewHolder(RecyclerView.ViewHolder viewHolder) {
                Assert.assertTrue(viewHolder != null);

                TaskHolder taskHolder = (TaskHolder) viewHolder;

                TreeNode treeNode = getTreeNode();
                Assert.assertTrue(treeNode != null);

                ParentFragment parentFragment = getParentFragment();
                Assert.assertTrue(parentFragment != null);

                if (treeNode.isSelected())
                    taskHolder.mShowTaskRow.setBackgroundColor(ContextCompat.getColor(parentFragment.getActivity(), R.color.selected));
                else
                    taskHolder.mShowTaskRow.setBackgroundColor(Color.TRANSPARENT);

                taskHolder.mShowTaskRow.setOnLongClickListener(treeNode.getOnLongClickListener());

                int padding = 48 * mIndentation;

                taskHolder.mTaskRowContainer.setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

                if (mTaskData.TaskDatas.isEmpty())
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

                if (mTaskData.TaskDatas.isEmpty() || treeNode.expanded()) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowChildren.setText(Stream.of(mTaskData.TaskDatas.values())
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
                return false;
            }

            @Override
            public void onClick() {
                ParentFragment parentFragment = getParentFragment();
                Assert.assertTrue(parentFragment != null);

                parentFragment.dismiss();

                parentFragment.mListener.onTaskSelected(mTaskData);
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
                ParentFragment parentFragment = getParentFragment();
                Assert.assertTrue(parentFragment != null);

                int comparison = Integer.valueOf(mTaskData.TaskId).compareTo(((TaskWrapper) another).mTaskData.TaskId);
                if (mIndentation == 0)
                    comparison = -comparison;

                return comparison;
            }

            public Stream<Integer> getExpandedTaskIds() {
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

        public class TaskHolder extends RecyclerView.ViewHolder {
            public final View mShowTaskRow;
            public final LinearLayout mTaskRowContainer;
            public final TextView mTaskRowName;
            public final TextView mTaskRowDetails;
            public final TextView mTaskRowChildren;
            public final ImageView mTaskRowImg;
            public final View mTaskRowSeparator;

            public TaskHolder(View showTaskRow, LinearLayout taskRowContainer, TextView taskRowName, TextView taskRowDetails, TextView taskRowChildren, ImageView taskRowImg, View taskRowSeparator) {
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
    }

    public interface Listener {
        void onTaskSelected(CreateChildTaskLoader.TaskData taskData);
    }
}
