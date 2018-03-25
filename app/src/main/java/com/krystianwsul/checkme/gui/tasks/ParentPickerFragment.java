package com.krystianwsul.checkme.gui.tasks;


import android.app.Dialog;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import com.krystianwsul.checkme.gui.AbstractDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;
import com.krystianwsul.treeadapter.ModelNode;
import com.krystianwsul.treeadapter.NodeContainer;
import com.krystianwsul.treeadapter.TreeModelAdapter;
import com.krystianwsul.treeadapter.TreeNode;
import com.krystianwsul.treeadapter.TreeNodeCollection;
import com.krystianwsul.treeadapter.TreeViewAdapter;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ParentPickerFragment extends AbstractDialogFragment {
    private static final String EXPANDED_TASK_KEYS_KEY = "expandedTaskKeys";

    private static final String SHOW_DELETE_KEY = "showDelete";

    private RecyclerView mRecyclerView;

    private Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> mTaskDatas;
    private Listener mListener;

    private TreeViewAdapter mTreeViewAdapter;
    private List<CreateTaskLoader.ParentKey> mExpandedParentKeys;

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
            if (savedInstanceState.containsKey(EXPANDED_TASK_KEYS_KEY)) {
                mExpandedParentKeys = savedInstanceState.getParcelableArrayList(EXPANDED_TASK_KEYS_KEY);
                Assert.assertTrue(mExpandedParentKeys != null);
                Assert.assertTrue(!mExpandedParentKeys.isEmpty());
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

    public void initialize(@NonNull Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> taskDatas, @NonNull Listener listener) {
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
            List<CreateTaskLoader.ParentKey> expanded = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedParentKeys();

            if (expanded.isEmpty()) {
                mExpandedParentKeys = null;
            } else {
                mExpandedParentKeys = expanded;
            }
        }

        mTreeViewAdapter = TaskAdapter.getAdapter(this, mTaskDatas, mExpandedParentKeys);

        mRecyclerView.setAdapter(mTreeViewAdapter);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTreeViewAdapter != null) {
            ArrayList<CreateTaskLoader.ParentKey> expandedParentKeys = ((TaskAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpandedParentKeys();

            if (!expandedParentKeys.isEmpty())
                outState.putParcelableArrayList(EXPANDED_TASK_KEYS_KEY, expandedParentKeys);
        }
    }

    private static class TaskAdapter implements TreeModelAdapter, TaskParent {
        @NonNull
        private final ParentPickerFragment mParentPickerFragment;

        private ArrayList<TaskWrapper> mTaskWrappers;

        private TreeViewAdapter mTreeViewAdapter;

        @NonNull
        static TreeViewAdapter getAdapter(@NonNull ParentPickerFragment parentPickerFragment, @NonNull Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> taskDatas, @Nullable List<CreateTaskLoader.ParentKey> expandedParentKeys) {
            TaskAdapter taskAdapter = new TaskAdapter(parentPickerFragment);

            float density = parentPickerFragment.getResources().getDisplayMetrics().density;

            return taskAdapter.initialize(density, taskDatas, expandedParentKeys);
        }

        private TaskAdapter(@NonNull ParentPickerFragment parentPickerFragment) {
            mParentPickerFragment = parentPickerFragment;
        }

        @NonNull
        private TreeViewAdapter initialize(float density, @NonNull Map<CreateTaskLoader.ParentKey, CreateTaskLoader.ParentTreeData> taskDatas, @Nullable List<CreateTaskLoader.ParentKey> expandedParentKeys) {
            mTreeViewAdapter = new TreeViewAdapter(this);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(mTreeViewAdapter);

            mTreeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            mTaskWrappers = new ArrayList<>();

            List<TreeNode> treeNodes = new ArrayList<>();

            for (CreateTaskLoader.ParentTreeData parentTreeData : taskDatas.values()) {
                TaskWrapper taskWrapper = new TaskWrapper(density, 0, this, parentTreeData);

                treeNodes.add(taskWrapper.initialize(treeNodeCollection, expandedParentKeys));

                mTaskWrappers.add(taskWrapper);
            }

            treeNodeCollection.setNodes(treeNodes);

            return mTreeViewAdapter;
        }

        @NonNull
        @Override
        public TaskHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater inflater = LayoutInflater.from(mParentPickerFragment.getActivity());
            View showTaskRow = inflater.inflate(R.layout.row_task_list, parent, false);

            LinearLayout taskRowContainer = showTaskRow.findViewById(R.id.taskRowContainer);
            Assert.assertTrue(taskRowContainer != null);

            TextView taskRowName = showTaskRow.findViewById(R.id.taskRowName);
            Assert.assertTrue(taskRowName != null);

            TextView taskRowDetails = showTaskRow.findViewById(R.id.taskRowDetails);
            Assert.assertTrue(taskRowDetails != null);

            TextView taskRowChildren = showTaskRow.findViewById(R.id.taskRowChildren);
            Assert.assertTrue(taskRowChildren != null);

            ImageView taskRowImage = showTaskRow.findViewById(R.id.taskRowImg);
            Assert.assertTrue(taskRowImage != null);

            View taskRowSeparator = showTaskRow.findViewById(R.id.taskRowSeparator);
            Assert.assertTrue(taskRowSeparator != null);

            return new TaskHolder(showTaskRow, taskRowContainer, taskRowName, taskRowDetails, taskRowChildren, taskRowImage, taskRowSeparator);
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
        ArrayList<CreateTaskLoader.ParentKey> getExpandedParentKeys() {
            return Stream.of(mTaskWrappers)
                    .flatMap(TaskWrapper::getExpandedParentKeys)
                    .collect(Collectors.toCollection(ArrayList::new));
        }

        private static class TaskWrapper implements ModelNode, TaskParent {
            @NonNull
            private final TaskParent mTaskParent;

            final CreateTaskLoader.ParentTreeData mParentTreeData;

            private TreeNode mTreeNode;

            private List<TaskWrapper> mTaskWrappers;

            private final float mDensity;
            private final int mIndentation;

            TaskWrapper(float density, int indentation, @NonNull TaskParent taskParent, @NonNull CreateTaskLoader.ParentTreeData parentTreeData) {
                mDensity = density;
                mIndentation = indentation;
                mTaskParent = taskParent;
                mParentTreeData = parentTreeData;
            }

            @NonNull
            TreeNode initialize(@NonNull NodeContainer nodeContainer, @Nullable List<CreateTaskLoader.ParentKey> expandedParentKeys) {
                boolean expanded = false;
                if (expandedParentKeys != null) {
                    Assert.assertTrue(!expandedParentKeys.isEmpty());
                    expanded = expandedParentKeys.contains(mParentTreeData.getParentKey());
                }

                mTreeNode = new TreeNode(this, nodeContainer, expanded, false);

                mTaskWrappers = new ArrayList<>();

                List<TreeNode> treeNodes = new ArrayList<>();

                for (CreateTaskLoader.ParentTreeData parentTreeData : mParentTreeData.getParentTreeDatas().values()) {
                    TaskWrapper taskWrapper = new TaskWrapper(mDensity, mIndentation + 1, this, parentTreeData);

                    treeNodes.add(taskWrapper.initialize(mTreeNode, expandedParentKeys));

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

                if (mParentTreeData.getParentTreeDatas().isEmpty()) {
                    Assert.assertTrue(!getTreeNode().getExpandVisible());

                    taskHolder.mTaskRowImg.setVisibility(View.INVISIBLE);
                } else {
                    Assert.assertTrue(getTreeNode().getExpandVisible());

                    taskHolder.mTaskRowImg.setVisibility(View.VISIBLE);

                    if (treeNode.isExpanded())
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_less_black_36dp);
                    else
                        taskHolder.mTaskRowImg.setImageResource(R.drawable.ic_expand_more_black_36dp);

                    taskHolder.mTaskRowImg.setOnClickListener(treeNode.getExpandListener());
                }

                taskHolder.mTaskRowName.setText(mParentTreeData.getName());

                if (TextUtils.isEmpty(mParentTreeData.getScheduleText())) {
                    taskHolder.mTaskRowDetails.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowDetails.setVisibility(View.VISIBLE);
                    taskHolder.mTaskRowDetails.setText(mParentTreeData.getScheduleText());
                }

                if ((mParentTreeData.getParentTreeDatas().isEmpty() || treeNode.isExpanded()) && TextUtils.isEmpty(mParentTreeData.getNote())) {
                    taskHolder.mTaskRowChildren.setVisibility(View.GONE);
                } else {
                    taskHolder.mTaskRowChildren.setVisibility(View.VISIBLE);

                    String text;
                    if (!mParentTreeData.getParentTreeDatas().isEmpty() && !treeNode.isExpanded()) {
                        text = Stream.of(mParentTreeData.getParentTreeDatas().values())
                                .map(CreateTaskLoader.ParentTreeData::getName)
                                .collect(Collectors.joining(", "));
                    } else {
                        Assert.assertTrue(!TextUtils.isEmpty(mParentTreeData.getNote()));

                        text = mParentTreeData.getNote();
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
            public boolean isSelectable() {
                return false;
            }

            @Override
            public void onClick() {
                ParentPickerFragment parentPickerFragment = getParentFragment();

                parentPickerFragment.dismiss();

                parentPickerFragment.mListener.onTaskSelected(mParentTreeData);
            }

            @Override
            public boolean isVisibleWhenEmpty() {
                return true;
            }

            @Override
            public boolean isVisibleDuringActionMode() {
                return true;
            }

            @Override
            public boolean isSeparatorVisibleWhenNotExpanded() {
                return false;
            }

            @Override
            public int compareTo(@NonNull ModelNode another) {
                int comparison = mParentTreeData.getSortKey().compareTo(((TaskWrapper) another).mParentTreeData.getSortKey());
                if (mIndentation == 0)
                    comparison = -comparison;

                return comparison;
            }

            @NonNull
            Stream<CreateTaskLoader.ParentKey> getExpandedParentKeys() {
                List<CreateTaskLoader.ParentKey> expandedParentKeys = new ArrayList<>();

                TreeNode treeNode = getTreeNode();

                if (treeNode.isExpanded()) {
                    expandedParentKeys.add(mParentTreeData.getParentKey());

                    expandedParentKeys.addAll(Stream.of(mTaskWrappers)
                            .flatMap(TaskWrapper::getExpandedParentKeys)
                            .collect(Collectors.toList()));
                }

                return Stream.of(expandedParentKeys);
            }

            @Override
            public double getOrdinal() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void setOrdinal(double ordinal) {
                throw new UnsupportedOperationException();
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
        void onTaskSelected(@NonNull CreateTaskLoader.ParentTreeData parentTreeData);

        void onTaskDeleted();
    }
}
