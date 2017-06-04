package com.krystianwsul.checkme.gui.instances.tree;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.treeadapter.ModelNode;
import com.krystianwsul.treeadapter.TreeNode;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

class TaskNode extends GroupHolderNode implements ModelNode, TaskParent {
    @NonNull
    private final TaskParent mTaskParent;

    private final GroupListFragment.TaskData mTaskData;

    private TreeNode mTreeNode;

    private List<TaskNode> mTaskNodes;

    TaskNode(float density, int indentation, @NonNull GroupListFragment.TaskData taskData, @NonNull TaskParent taskParent) {
        super(density, indentation);

        mTaskData = taskData;
        mTaskParent = taskParent;
    }

    @NonNull
    TreeNode initialize(TreeNode parentTreeNode, List<TaskKey> expandedTaskKeys) {
        Assert.assertTrue(parentTreeNode != null);

        boolean expanded = (expandedTaskKeys != null && expandedTaskKeys.contains(mTaskData.mTaskKey) && !mTaskData.Children.isEmpty());

        mTreeNode = new TreeNode(this, parentTreeNode, expanded, false);

        mTaskNodes = new ArrayList<>();

        List<TreeNode> childTreeNodes = Stream.of(mTaskData.Children)
                .map(taskData -> newChildTreeNode(taskData, expandedTaskKeys))
                .collect(Collectors.toList());

        mTreeNode.setChildTreeNodes(childTreeNodes);

        return mTreeNode;
    }

    @NonNull
    private TreeNode newChildTreeNode(@NonNull GroupListFragment.TaskData taskData, @Nullable List<TaskKey> expandedTaskKeys) {
        TaskNode taskNode = new TaskNode(mDensity, mIndentation + 1, taskData, this);

        mTaskNodes.add(taskNode);

        return taskNode.initialize(getTreeNode(), expandedTaskKeys);
    }

    @NonNull
    private TaskParent getTaskParent() {
        return mTaskParent;
    }

    @NonNull
    @Override
    public GroupListFragment.GroupAdapter getGroupAdapter() {
        return getTaskParent().getGroupAdapter();
    }

    @NonNull
    private GroupListFragment getGroupListFragment() {
        return getGroupAdapter().mGroupListFragment;
    }

    @NonNull
    private TreeNode getTreeNode() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode;
    }

    private boolean expanded() {
        return getTreeNode().expanded();
    }

    Stream<TaskKey> getExpandedTaskKeys() {
        if (mTaskNodes.isEmpty()) {
            Assert.assertTrue(!expanded());

            return Stream.of(new ArrayList<>());
        } else {
            List<TaskKey> expandedTaskKeys = new ArrayList<>();

            if (expanded())
                expandedTaskKeys.add(mTaskData.mTaskKey);

            return Stream.concat(Stream.of(expandedTaskKeys), Stream.of(mTaskNodes).flatMap(TaskNode::getExpandedTaskKeys));
        }
    }

    @Override
    public int compareTo(@NonNull ModelNode modelNode) {
        TaskNode other = (TaskNode) modelNode;

        if (mIndentation == 0) {
            return -mTaskData.mStartExactTimeStamp.compareTo(other.mTaskData.mStartExactTimeStamp);
        } else {
            return mTaskData.mStartExactTimeStamp.compareTo(other.mTaskData.mStartExactTimeStamp);
        }
    }

    @Override
    int getNameVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    @Override
    String getName() {
        return mTaskData.Name;
    }

    @Override
    int getNameColor() {
        return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textPrimary);
    }

    @Override
    boolean getNameSingleLine() {
        return true;
    }

    @Override
    int getDetailsVisibility() {
        return View.GONE;
    }

    @NonNull
    @Override
    String getDetails() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getDetailsColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getChildrenVisibility() {
        if ((mTaskData.Children.isEmpty() || expanded()) && TextUtils.isEmpty(mTaskData.mNote)) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @NonNull
    @Override
    String getChildren() {
        if (!expanded() && !mTaskData.Children.isEmpty()) {
            return Stream.of(mTaskData.Children)
                    .sortBy(task -> task.mStartExactTimeStamp)
                    .map(task -> task.Name)
                    .collect(Collectors.joining(", "));
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(mTaskData.mNote));

            return mTaskData.mNote;
        }
    }

    @Override
    int getChildrenColor() {
        Assert.assertTrue((!expanded() && !mTaskData.Children.isEmpty()) || !TextUtils.isEmpty(mTaskData.mNote));

        return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textSecondary);
    }

    @Override
    int getExpandVisibility() {
        if (mTaskData.Children.isEmpty()) {
            return View.INVISIBLE;
        } else {
            return View.VISIBLE;
        }
    }

    @Override
    int getExpandImageResource() {
        TreeNode treeNode = getTreeNode();

        Assert.assertTrue(!mTaskData.Children.isEmpty());

        if (treeNode.expanded())
            return R.drawable.ic_expand_less_black_36dp;
        else
            return R.drawable.ic_expand_more_black_36dp;
    }

    @NonNull
    @Override
    View.OnClickListener getExpandOnClickListener() {
        Assert.assertTrue(!mTaskData.Children.isEmpty());

        return getTreeNode().getExpandListener();
    }

    @Override
    int getCheckBoxVisibility() {
        return View.INVISIBLE;
    }

    @Override
    boolean getCheckBoxChecked() {
        throw new UnsupportedOperationException();
    }

    @NonNull
    @Override
    View.OnClickListener getCheckBoxOnClickListener() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getSeparatorVisibility() {
        return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    int getBackgroundColor() {
        return Color.TRANSPARENT;
    }

    @Override
    View.OnLongClickListener getOnLongClickListener() {
        return getTreeNode().getOnLongClickListener();
    }

    @Override
    View.OnClickListener getOnClickListener() {
        return getTreeNode().getOnClickListener();
    }

    @Override
    public boolean selectable() {
        return false;
    }

    @Override
    public void onClick() {
        GroupListFragment groupListFragment = getGroupListFragment();

        groupListFragment.getActivity().startActivity(ShowTaskActivity.newIntent(groupListFragment.getActivity(), mTaskData.mTaskKey));
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
}
