package com.krystianwsul.checkme.gui.instances.tree;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.tree.ModelNode;
import com.krystianwsul.checkme.gui.tree.NodeContainer;
import com.krystianwsul.checkme.gui.tree.TreeNode;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

class UnscheduledNode extends GroupHolderNode implements ModelNode, TaskParent {
    @NonNull
    private final NodeCollection mNodeCollection;

    private TreeNode mTreeNode;

    private List<TaskNode> mTaskNodes;

    UnscheduledNode(float density, @NonNull NodeCollection nodeCollection) {
        super(density, 0);

        mNodeCollection = nodeCollection;
    }

    @NonNull
    TreeNode initialize(boolean expanded, @NonNull NodeContainer nodeContainer, @NonNull List<GroupListFragment.TaskData> taskDatas, @Nullable List<TaskKey> expandedTaskKeys) {
        Assert.assertTrue(!expanded || !taskDatas.isEmpty());

        mTreeNode = new TreeNode(this, nodeContainer, expanded, false);

        mTaskNodes = new ArrayList<>();

        List<TreeNode> childTreeNodes = Stream.of(taskDatas)
                .map(taskData -> newChildTreeNode(taskData, expandedTaskKeys))
                .collect(Collectors.toList());

        mTreeNode.setChildTreeNodes(childTreeNodes);

        return mTreeNode;
    }

    @NonNull
    private TreeNode newChildTreeNode(@NonNull GroupListFragment.TaskData taskData, @Nullable List<TaskKey> expandedTaskKeys) {
        TaskNode taskNode = new TaskNode(mDensity, 0, taskData, this);

        mTaskNodes.add(taskNode);

        return taskNode.initialize(getTreeNode(), expandedTaskKeys);
    }

    @NonNull
    private NodeCollection getNodeCollection() {
        return mNodeCollection;
    }

    boolean expanded() {
        return getTreeNode().expanded();
    }

    List<TaskKey> getExpandedTaskKeys() {
        return Stream.of(mTaskNodes)
                .flatMap(TaskNode::getExpandedTaskKeys)
                .collect(Collectors.toList());
    }

    @NonNull
    @Override
    public GroupListFragment.GroupAdapter getGroupAdapter() {
        return getNodeCollection().getGroupAdapter();
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

    @Override
    public int compareTo(@NonNull ModelNode modelNode) {
        if (modelNode instanceof DividerNode) {
            return -1;
        } else {
            Assert.assertTrue(modelNode instanceof NotDoneGroupNode);
            return 1;
        }
    }

    @Override
    int getNameVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    @Override
    String getName() {
        return getGroupListFragment().getString(R.string.noReminder);
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
        return View.GONE;
    }

    @NonNull
    @Override
    String getChildren() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getChildrenColor() {
        throw new UnsupportedOperationException();
    }

    @Override
    int getExpandVisibility() {
        return View.VISIBLE;
    }

    @Override
    int getExpandImageResource() {
        if (getTreeNode().expanded())
            return R.drawable.ic_expand_less_black_36dp;
        else
            return R.drawable.ic_expand_more_black_36dp;
    }

    @NonNull
    @Override
    View.OnClickListener getExpandOnClickListener() {
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

    }

    @Override
    public boolean visibleWhenEmpty() {
        return false;
    }

    @Override
    public boolean visibleDuringActionMode() {
        return false;
    }

    @Override
    public boolean separatorVisibleWhenNotExpanded() {
        return false;
    }
}
