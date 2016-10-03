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

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ParentPickerFragment extends DialogFragment {
    private static final String EXPANDED_TASKS_KEY = "expandedTasks";

    private static final String SHOW_DELETE_KEY = "showDelete";

    private RecyclerView mRecyclerView;

    private TreeMap<Integer, CreateTaskLoader.TaskTreeData> mTaskDatas;
    private Listener mListener;

    private TreeViewAdapter mTreeViewAdapter;
    private List<Integer> mExpandedTaskIds;

    public static ParentPickerFragment newInstance(boolean showDelete) {
        ParentPickerFragment parentPickerFragment = new ParentPickerFragment();

        Bundle args = new Bundle();
        args.putBoolean(SHOW_DELETE_KEY, showDelete);
        parentPickerFragment.setArguments(args);

        return parentPickerFragment;
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

        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .title(R.string.parent_dialog_title)
                .customView(R.layout.fragment_parent_picker, false)
                .negativeText(android.R.string.cancel);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(SHOW_DELETE_KEY));

        boolean showDelete = args.getBoolean(SHOW_DELETE_KEY);

        if (showDelete)
            builder.neutralText(R.string.delete)
                    .onNeutral((dialog, which) -> mListener.onTaskDeleted());

        MaterialDialog materialDialog = builder.build();

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

        mRecyclerView.setAdapter(mTreeViewAdapter.getAdapter());
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
        @NonNull
        private final ParentPickerFragment mParentPickerFragment;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private TreeViewAdapter mTreeViewAdapter;

        @NonNull
        static TreeViewAdapter getAdapter(@NonNull ParentPickerFragment parentPickerFragment, @NonNull TreeMap<Integer, CreateTaskLoader.TaskTreeData> taskDatas, @Nullable List<Integer> expandedTasks) {
            TaskAdapter taskAdapter = new TaskAdapter(parentPickerFragment);

            float density = parentPickerFragment.getResources().getDisplayMetrics().density;

            return taskAdapter.initialize(density, taskDatas, expandedTasks);
        }

        private TaskAdapter(@NonNull ParentPickerFragment parentPickerFragment) {
            mParentPickerFragment = parentPickerFragment;
        }

        @NonNull
        private TreeViewAdapter initialize(float density, TreeMap<Integer, CreateTaskLoader.TaskTreeData> taskDatas, List<Integer> expandedTasks) {
            Assert.assertTrue(taskDatas != null);

            mTreeViewAdapter = new TreeViewAdapter(false, this);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(mTreeViewAdapter);

            mTreeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            mTaskWrappers = new ArrayList<>();

            List<TreeNode> treeNodes = new ArrayList<>();

            for (CreateTaskLoader.TaskTreeData taskTreeData : taskDatas.values()) {
                TaskWrapper taskWrapper = new TaskWrapper(density, 0, this, taskTreeData);

                treeNodes.add(taskWrapper.initialize(treeNodeCollection, expandedTasks));

                mTaskWrappers.add(taskWrapper);
            }

            treeNodeCollection.setNodes(treeNodes);

            return mTreeViewAdapter;
        }

        @Override
        public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mParentPickerFragment.getActivity());
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

        @NonNull
        TreeViewAdapter getTreeViewAdapter() {
            Assert.assertTrue(mTreeViewAdapter != null);

            return mTreeViewAdapter;
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
            @NonNull
            private final TaskParent mTaskParent;

            final CreateTaskLoader.TaskTreeData mTaskTreeData;

            private TreeNode mTreeNode;

            private List<TaskWrapper> mTaskWrappers;

            private final float mDensity;
            private final int mIndentation;

            TaskWrapper(float density, int indentation, @NonNull TaskParent taskParent, @NonNull CreateTaskLoader.TaskTreeData taskTreeData) {
                mDensity = density;
                mIndentation = indentation;
                mTaskParent = taskParent;
                mTaskTreeData = taskTreeData;
            }

            @NonNull
            TreeNode initialize(@NonNull NodeContainer nodeContainer, @Nullable List<Integer> expandedTasks) {
                boolean expanded = false;
                if (expandedTasks != null) {
                    Assert.assertTrue(!expandedTasks.isEmpty());
                    expanded = expandedTasks.contains(mTaskTreeData.TaskId);
                }

                mTreeNode = new TreeNode(this, nodeContainer, expanded, false);

                mTaskWrappers = new ArrayList<>();

                List<TreeNode> treeNodes = new ArrayList<>();

                for (CreateTaskLoader.TaskTreeData taskTreeData : mTaskTreeData.TaskDatas.values()) {
                    TaskWrapper taskWrapper = new TaskWrapper(mDensity, mIndentation + 1, this, taskTreeData);

                    treeNodes.add(taskWrapper.initialize(mTreeNode, expandedTasks));

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
            private ParentPickerFragment getParentFragment() {
                return getTaskAdapter().mParentPickerFragment;
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

                if ((mTaskTreeData.TaskDatas.isEmpty() || treeNode.expanded()) && TextUtils.isEmpty(mTaskTreeData.mNote)) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);

                    String text;
                    if (!mTaskTreeData.TaskDatas.isEmpty() && !treeNode.expanded()) {
                        text = Stream.of(mTaskTreeData.TaskDatas.values())
                                .map(taskData -> taskData.Name)
                                .collect(Collectors.joining(", "));
                    } else {
                        Assert.assertTrue(!TextUtils.isEmpty(mTaskTreeData.mNote));

                        text = mTaskTreeData.mNote;
                    }

                    Assert.assertTrue(!TextUtils.isEmpty(text));

                    taskHolder.mTaskRowChildren.setText(text);
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
            public boolean separatorVisibleWhenNotExpanded() {
                return false;
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
        void onTaskSelected(@NonNull CreateTaskLoader.TaskTreeData taskTreeData);

        void onTaskDeleted();
    }
}
