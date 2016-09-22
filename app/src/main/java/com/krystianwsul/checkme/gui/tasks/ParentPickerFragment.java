package com.krystianwsul.checkme.gui.tasks;


import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.krystianwsul.checkme.loaders.CreateTaskLoader;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ParentPickerFragment extends DialogFragment {
    private static final String EXPANDED_TASKS_KEY = "expandedTasks";

    private RecyclerView mRecyclerView;

    private TreeMap<Integer, CreateTaskLoader.TaskTreeData> mTaskDatas;
    private Listener mListener;

    private TreeViewAdapter mTreeViewAdapter;
    private List<Integer> mExpandedTaskIds;

    public static ParentPickerFragment newInstance() {
        return new ParentPickerFragment();
    }

    public ParentPickerFragment() {

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
                .customView(R.layout.fragment_parent_picker, false)
                .build();

        mRecyclerView = (RecyclerView) materialDialog.getCustomView();
        Assert.assertTrue(mRecyclerView != null);

        return materialDialog;
    }

    public void initialize(TreeMap<Integer, CreateTaskLoader.TaskTreeData> taskDatas, Listener listener) {
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

            if (!expandedTaskIds.isEmpty())
                outState.putIntegerArrayList(EXPANDED_TASKS_KEY, expandedTaskIds);
        }
    }

    public static class TaskAdapter implements TreeModelAdapter, TaskParent {
        private final WeakReference<ParentPickerFragment> mParentFragmentReference;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

        static TreeViewAdapter getAdapter(ParentPickerFragment parentPickerFragment, TreeMap<Integer, CreateTaskLoader.TaskTreeData> taskDatas, List<Integer> expandedTasks) {
            Assert.assertTrue(parentPickerFragment != null);
            Assert.assertTrue(taskDatas != null);

            TaskAdapter taskAdapter = new TaskAdapter(parentPickerFragment);

            float density = parentPickerFragment.getResources().getDisplayMetrics().density;

            return taskAdapter.initialize(density, taskDatas, expandedTasks);
        }

        private TaskAdapter(ParentPickerFragment parentPickerFragment) {
            Assert.assertTrue(parentPickerFragment != null);

            mParentFragmentReference = new WeakReference<>(parentPickerFragment);
        }

        private TreeViewAdapter initialize(float density, TreeMap<Integer, CreateTaskLoader.TaskTreeData> taskDatas, List<Integer> expandedTasks) {
            Assert.assertTrue(taskDatas != null);

            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(false, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            mTaskWrappers = new ArrayList<>();

            List<TreeNode> treeNodes = new ArrayList<>();

            for (CreateTaskLoader.TaskTreeData taskTreeData : taskDatas.values()) {
                TaskWrapper taskWrapper = new TaskWrapper(density, 0, new WeakReference<>(this), taskTreeData);

                treeNodes.add(taskWrapper.initialize(new WeakReference<>(treeNodeCollection), expandedTasks));

                mTaskWrappers.add(taskWrapper);
            }

            treeNodeCollection.setNodes(treeNodes);

            return treeViewAdapter;
        }

        @Override
        public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ParentPickerFragment parentPickerFragment = getParentFragment();
            Assert.assertTrue(parentPickerFragment != null);

            LayoutInflater inflater = LayoutInflater.from(parentPickerFragment.getActivity());
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
            TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
            Assert.assertTrue(treeViewAdapter != null);

            TreeNode treeNode = treeViewAdapter.getNode(position);

            treeNode.onBindViewHolder(viewHolder);
        }

        ParentPickerFragment getParentFragment() {
            ParentPickerFragment parentPickerFragment = mParentFragmentReference.get();
            Assert.assertTrue(parentPickerFragment != null);

            return parentPickerFragment;
        }

        TreeViewAdapter getTreeViewAdapter() {
            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            return treeViewAdapter;
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

        @NonNull
        ArrayList<Integer> getExpandedTaskIds() {
            return Stream.of(mTaskWrappers)
                    .flatMap(TaskWrapper::getExpandedTaskIds)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private static class TaskWrapper implements ModelNode, TaskParent {
            private final WeakReference<TaskParent> mTaskParentReference;

            final CreateTaskLoader.TaskTreeData mTaskTreeData;

            private WeakReference<TreeNode> mTreeNodeReference;

            private List<TaskWrapper> mTaskWrappers;

            private final float mDensity;
            private final int mIndentation;

            TaskWrapper(float density, int indentation, @NonNull WeakReference<TaskParent> taskParentReference, @NonNull CreateTaskLoader.TaskTreeData taskTreeData) {
                mDensity = density;
                mIndentation = indentation;
                mTaskParentReference = taskParentReference;
                mTaskTreeData = taskTreeData;
            }

            TreeNode initialize(@NonNull WeakReference<NodeContainer> nodeContainerReference, @Nullable List<Integer> expandedTasks) {
                boolean expanded = false;
                if (expandedTasks != null) {
                    Assert.assertTrue(!expandedTasks.isEmpty());
                    expanded = expandedTasks.contains(mTaskTreeData.TaskId);
                }

                TreeNode treeNode = new TreeNode(this, nodeContainerReference, expanded, false);

                mTreeNodeReference = new WeakReference<>(treeNode);

                mTaskWrappers = new ArrayList<>();

                List<TreeNode> treeNodes = new ArrayList<>();

                for (CreateTaskLoader.TaskTreeData taskTreeData : mTaskTreeData.TaskDatas.values()) {
                    TaskWrapper taskWrapper = new TaskWrapper(mDensity, mIndentation + 1, new WeakReference<>(this), taskTreeData);

                    treeNodes.add(taskWrapper.initialize(new WeakReference<>(treeNode), expandedTasks));

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
            private ParentPickerFragment getParentFragment() {
                return getTaskAdapter().getParentFragment();
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                TaskHolder taskHolder = (TaskHolder) viewHolder;

                TreeNode treeNode = getTreeNode();

                ParentPickerFragment parentPickerFragment = getParentFragment();

                if (treeNode.isSelected())
                    taskHolder.mShowTaskRow.setBackgroundColor(ContextCompat.getColor(parentPickerFragment.getActivity(), R.color.selected));
                else
                    taskHolder.mShowTaskRow.setBackgroundColor(Color.TRANSPARENT);

                taskHolder.mShowTaskRow.setOnLongClickListener(treeNode.getOnLongClickListener());

                int padding = 48 * mIndentation;

                taskHolder.mTaskRowContainer.setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

                if (mTaskTreeData.TaskDatas.isEmpty())
                    taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);
                else {
                    taskHolder.mTaskRowImg.setVisibility(View.VISIBLE);

                    if (treeNode.expanded())
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_less_black_36dp);
                    else
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_more_black_36dp);

                    taskHolder.mTaskRowImg.setOnClickListener(treeNode.getExpandListener());
                }

                taskHolder.mTaskRowName.setText(mTaskTreeData.Name);

                if (TextUtils.isEmpty(mTaskTreeData.ScheduleText)) {
                    taskHolder.mTaskRowDetails.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowDetails.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowDetails.setText(mTaskTreeData.ScheduleText);
                }

                if (mTaskTreeData.TaskDatas.isEmpty() || treeNode.expanded()) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowChildren.setText(Stream.of(mTaskTreeData.TaskDatas.values())
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
                ParentPickerFragment parentPickerFragment = getParentFragment();

                parentPickerFragment.dismiss();

                parentPickerFragment.mListener.onTaskSelected(mTaskTreeData);
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
                int comparison = Integer.valueOf(mTaskTreeData.TaskId).compareTo(((TaskWrapper) another).mTaskTreeData.TaskId);
                if (mIndentation == 0)
                    comparison = -comparison;

                return comparison;
            }

            @NonNull
            Stream<Integer> getExpandedTaskIds() {
                List<Integer> expandedTaskIds = new ArrayList<>();

                TreeNode treeNode = getTreeNode();

                if (treeNode.expanded()) {
                    expandedTaskIds.add(mTaskTreeData.TaskId);

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

            TaskHolder(@NonNull View showTaskRow, @NonNull LinearLayout taskRowContainer, @NonNull TextView taskRowName, @NonNull TextView taskRowDetails, @NonNull TextView taskRowChildren, @NonNull ImageView taskRowImg, @NonNull View taskRowSeparator) {
                super(showTaskRow);

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
        void onTaskSelected(CreateTaskLoader.TaskTreeData taskTreeData);
    }
}
