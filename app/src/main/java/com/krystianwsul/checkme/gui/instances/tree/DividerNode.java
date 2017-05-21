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
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class DividerNode extends GroupHolderNode implements ModelNode {
    @NonNull
    private final GroupListFragment.GroupAdapter.NodeCollection mNodeCollection;

    private TreeNode mTreeNode;

    private final ArrayList<DoneInstanceNode> mDoneInstanceNodes = new ArrayList<>();

    DividerNode(float density, int indentation, @NonNull GroupListFragment.GroupAdapter.NodeCollection nodeCollection) {
        super(density, indentation);

        mNodeCollection = nodeCollection;
    }

    TreeNode initialize(boolean expanded, NodeContainer nodeContainer, List<GroupListFragment.InstanceData> doneInstanceDatas, HashMap<InstanceKey, Boolean> expandedInstances) {
        Assert.assertTrue(!expanded || !doneInstanceDatas.isEmpty());

        mTreeNode = new TreeNode(this, nodeContainer, expanded, false);

        List<TreeNode> childTreeNodes = Stream.of(doneInstanceDatas)
                .map(doneInstanceData -> newChildTreeNode(doneInstanceData, expandedInstances))
                .collect(Collectors.toList());

        mTreeNode.setChildTreeNodes(childTreeNodes);

        return mTreeNode;
    }

    @NonNull
    private TreeNode newChildTreeNode(@NonNull GroupListFragment.InstanceData instanceData, @Nullable HashMap<InstanceKey, Boolean> expandedInstances) {
        Assert.assertTrue(instanceData.Done != null);

        DoneInstanceNode doneInstanceNode = new DoneInstanceNode(mDensity, mIndentation, instanceData, this);

        TreeNode childTreeNode = doneInstanceNode.initialize(mTreeNode, expandedInstances);

        mDoneInstanceNodes.add(doneInstanceNode);

        return childTreeNode;
    }

    boolean expanded() {
        TreeNode treeNode = getTreeNode();

        return treeNode.expanded();
    }

    void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
        Assert.assertTrue(expandedInstances != null);

        for (DoneInstanceNode doneInstanceNode : mDoneInstanceNodes)
            doneInstanceNode.addExpandedInstances(expandedInstances);
    }

    @Override
    int getNameVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    @Override
    String getName() {
        return getGroupListFragment().getString(R.string.done);
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
        Assert.assertTrue(mTreeNode != null);

        if (mTreeNode.expanded())
            return R.drawable.ic_expand_less_black_36dp;
        else
            return R.drawable.ic_expand_more_black_36dp;
    }

    @NonNull
    @Override
    View.OnClickListener getExpandOnClickListener() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode.getExpandListener();
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
        TreeNode treeNode = getTreeNode();
        return (treeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    int getBackgroundColor() {
        return Color.TRANSPARENT;
    }

    @Override
    View.OnLongClickListener getOnLongClickListener() {
        TreeNode treeNode = getTreeNode();

        return treeNode.getOnLongClickListener();
    }

    @Override
    View.OnClickListener getOnClickListener() {
        TreeNode treeNode = getTreeNode();

        return treeNode.getOnClickListener();
    }

    public void remove(@NonNull DoneInstanceNode doneInstanceNode) {
        Assert.assertTrue(mTreeNode != null);

        Assert.assertTrue(mDoneInstanceNodes.contains(doneInstanceNode));
        mDoneInstanceNodes.remove(doneInstanceNode);

        mTreeNode.remove(doneInstanceNode.getTreeNode());
    }

    public void add(@NonNull GroupListFragment.InstanceData instanceData) {
        Assert.assertTrue(mTreeNode != null);

        mTreeNode.add(newChildTreeNode(instanceData, null));
    }

    @NonNull
    GroupListFragment.GroupAdapter.NodeCollection getNodeCollection() {
        return mNodeCollection;
    }

    @NonNull
    GroupListFragment.GroupAdapter getGroupAdapter() {
        return getNodeCollection().getGroupAdapter();
    }

    @NonNull
    private GroupListFragment getGroupListFragment() {
        return getGroupAdapter().mGroupListFragment;
    }

    @Override
    public boolean selectable() {
        return false;
    }

    @Override
    public void onClick() {

    }

    @Override
    public int compareTo(@NonNull ModelNode another) {
        Assert.assertTrue(another instanceof GroupListFragment.GroupAdapter.NodeCollection.NoteNode || another instanceof GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode || another instanceof UnscheduledNode);
        return 1;
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

    @NonNull
    private TreeNode getTreeNode() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode;
    }
}
