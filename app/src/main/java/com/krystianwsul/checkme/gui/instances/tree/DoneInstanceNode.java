package com.krystianwsul.checkme.gui.instances.tree;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.treeadapter.ModelNode;
import com.krystianwsul.treeadapter.TreeNode;

import junit.framework.Assert;

import java.util.HashMap;

class DoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
    @NonNull
    private final DividerNode mDividerNode;

    private TreeNode mTreeNode;

    final GroupListFragment.InstanceData mInstanceData;

    private NodeCollection mNodeCollection;

    DoneInstanceNode(float density, int indentation, @NonNull GroupListFragment.InstanceData instanceData, @NonNull DividerNode dividerNode) {
        super(density, indentation);

        mInstanceData = instanceData;
        mDividerNode = dividerNode;
    }

    TreeNode initialize(@NonNull TreeNode dividerTreeNode, HashMap<InstanceKey, Boolean> expandedInstances) {
        boolean expanded = false;
        boolean doneExpanded = false;
        if (expandedInstances != null && expandedInstances.containsKey(mInstanceData.InstanceKey) && !mInstanceData.Children.isEmpty()) {
            expanded = true;
            doneExpanded = expandedInstances.get(mInstanceData.InstanceKey);
        }

        mTreeNode = new TreeNode(this, dividerTreeNode, expanded, false);

        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, mTreeNode, null);
        mTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceData.Children.values(), null, expandedInstances, doneExpanded, null, false, null, false, null));

        return mTreeNode;
    }

    @NonNull
    TreeNode getTreeNode() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode;
    }

    @NonNull
    private DividerNode getDividerNode() {
        return mDividerNode;
    }

    @NonNull
    private NodeCollection getParentNodeCollection() {
        return getDividerNode().getNodeCollection();
    }

    private boolean expanded() {
        return getTreeNode().expanded();
    }

    void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
        Assert.assertTrue(expandedInstances != null);

        if (!expanded())
            return;

        Assert.assertTrue(!expandedInstances.containsKey(mInstanceData.InstanceKey));

        expandedInstances.put(mInstanceData.InstanceKey, mNodeCollection.getDoneExpanded());

        mNodeCollection.addExpandedInstances(expandedInstances);
    }

    @NonNull
    @Override
    public GroupListFragment.GroupAdapter getGroupAdapter() {
        return getParentNodeCollection().getGroupAdapter();
    }

    @NonNull
    private GroupListFragment getGroupListFragment() {
        return getGroupAdapter().mGroupListFragment;
    }

    @Override
    int getNameVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    @Override
    String getName() {
        return mInstanceData.Name;
    }

    @Override
    int getNameColor() {
        GroupListFragment groupListFragment = getGroupListFragment();

        if (!mInstanceData.TaskCurrent) {
            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
        } else {
            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
        }
    }

    @Override
    boolean getNameSingleLine() {
        return true;
    }

    @Override
    int getDetailsVisibility() {
        if (TextUtils.isEmpty(mInstanceData.DisplayText)) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @NonNull
    @Override
    String getDetails() {
        Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.DisplayText));
        return mInstanceData.DisplayText;
    }

    @Override
    int getDetailsColor() {
        if (!mInstanceData.TaskCurrent) {
            return ContextCompat.getColor(mDividerNode.getGroupAdapter().mGroupListFragment.getActivity(), R.color.textDisabled);
        } else {
            return ContextCompat.getColor(mDividerNode.getGroupAdapter().mGroupListFragment.getActivity(), R.color.textSecondary);
        }
    }

    @Override
    int getChildrenVisibility() {
        if ((mInstanceData.Children.isEmpty() || expanded()) && TextUtils.isEmpty(mInstanceData.mNote)) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @NonNull
    @Override
    String getChildren() {
        Assert.assertTrue((!mInstanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.mNote));

        return GroupListFragment.getChildrenText(expanded(), mInstanceData.Children.values(), mInstanceData.mNote);
    }

    @Override
    int getChildrenColor() {
        Assert.assertTrue((!mInstanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.mNote));

        Activity activity = getGroupListFragment().getActivity();
        Assert.assertTrue(activity != null);

        if (!mInstanceData.TaskCurrent) {
            return ContextCompat.getColor(activity, R.color.textDisabled);
        } else {
            return ContextCompat.getColor(activity, R.color.textSecondary);
        }
    }

    @Override
    int getExpandVisibility() {
        if (mInstanceData.Children.isEmpty()) {
            return View.INVISIBLE;
        } else {
            return View.VISIBLE;
        }
    }

    @Override
    int getExpandImageResource() {
        Assert.assertTrue(!mInstanceData.Children.isEmpty());

        if (getTreeNode().expanded())
            return R.drawable.ic_expand_less_black_36dp;
        else
            return R.drawable.ic_expand_more_black_36dp;
    }

    @NonNull
    @Override
    View.OnClickListener getExpandOnClickListener() {
        Assert.assertTrue(!mInstanceData.Children.isEmpty());

        return getTreeNode().getExpandListener();
    }

    @Override
    int getCheckBoxVisibility() {
        return View.VISIBLE;
    }

    @Override
    boolean getCheckBoxChecked() {
        return true;
    }

    @NonNull
    @Override
    View.OnClickListener getCheckBoxOnClickListener() {
        final DividerNode dividerNode = getDividerNode();

        NodeCollection nodeCollection = dividerNode.getNodeCollection();

        GroupListFragment.GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

        return v -> {
            v.setOnClickListener(null);

            mInstanceData.Done = DomainFactory.getDomainFactory(groupAdapter.mGroupListFragment.getActivity()).setInstanceDone(groupAdapter.mGroupListFragment.getActivity(), groupAdapter.mDataId, mInstanceData.InstanceKey, false);
            Assert.assertTrue(mInstanceData.Done == null);

            dividerNode.remove(this);

            nodeCollection.getNotDoneGroupCollection().add(mInstanceData);

            groupAdapter.mGroupListFragment.updateSelectAll();
        };
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
    public int compareTo(@NonNull ModelNode another) {
        Assert.assertTrue(mInstanceData.Done != null);

        DoneInstanceNode doneInstanceNode = (DoneInstanceNode) another;
        Assert.assertTrue(doneInstanceNode.mInstanceData.Done != null);

        return -mInstanceData.Done.compareTo(doneInstanceNode.mInstanceData.Done); // negate
    }

    @Override
    public boolean selectable() {
        return false;
    }

    @Override
    public void onClick() {
        GroupListFragment groupListFragment = getGroupListFragment();

        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey));
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

    void removeFromParent() {
        getDividerNode().remove(this);
    }
}
