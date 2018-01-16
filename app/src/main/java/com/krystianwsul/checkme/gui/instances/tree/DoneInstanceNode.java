package com.krystianwsul.checkme.gui.instances.tree;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.treeadapter.ModelNode;
import com.krystianwsul.treeadapter.TreeNode;

import junit.framework.Assert;

import java.util.HashMap;

public class DoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
    @NonNull
    final GroupListFragment.InstanceData mInstanceData;

    @NonNull
    private final DividerNode mDividerNode;

    private TreeNode mTreeNode;

    private NodeCollection mNodeCollection;

    DoneInstanceNode(float density, int indentation, @NonNull GroupListFragment.InstanceData instanceData, @NonNull DividerNode dividerNode) {
        super(density, indentation);

        mInstanceData = instanceData;
        mDividerNode = dividerNode;
    }

    @NonNull
    TreeNode initialize(@NonNull TreeNode dividerTreeNode, @Nullable HashMap<InstanceKey, Boolean> expandedInstances) {
        boolean expanded = false;
        boolean doneExpanded = false;
        if (expandedInstances != null && expandedInstances.containsKey(mInstanceData.getInstanceKey()) && !mInstanceData.getChildren().isEmpty()) {
            expanded = true;
            doneExpanded = expandedInstances.get(mInstanceData.getInstanceKey());
        }

        mTreeNode = new TreeNode(this, dividerTreeNode, expanded, false);

        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, mTreeNode, null);
        mTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceData.getChildren().values(), null, expandedInstances, doneExpanded, null, false, null, false, null));

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

    void addExpandedInstances(@NonNull HashMap<InstanceKey, Boolean> expandedInstances) {
        if (!expanded())
            return;

        Assert.assertTrue(!expandedInstances.containsKey(mInstanceData.getInstanceKey()));

        expandedInstances.put(mInstanceData.getInstanceKey(), mNodeCollection.getDoneExpanded());

        mNodeCollection.addExpandedInstances(expandedInstances);
    }

    @NonNull
    @Override
    public GroupListFragment.GroupAdapter getGroupAdapter() {
        return getParentNodeCollection().getGroupAdapter();
    }

    @NonNull
    private GroupListFragment getGroupListFragment() {
        return getGroupAdapter().getMGroupListFragment();
    }

    @Override
    int getNameVisibility() {
        return View.VISIBLE;
    }

    @NonNull
    @Override
    String getName() {
        return mInstanceData.getName();
    }

    @Override
    int getNameColor() {
        GroupListFragment groupListFragment = getGroupListFragment();

        if (!mInstanceData.getTaskCurrent()) {
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
        if (TextUtils.isEmpty(mInstanceData.getDisplayText())) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @NonNull
    @Override
    String getDetails() {
        Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.getDisplayText()));
        return mInstanceData.getDisplayText();
    }

    @Override
    int getDetailsColor() {
        if (!mInstanceData.getTaskCurrent()) {
            return ContextCompat.getColor(mDividerNode.getGroupAdapter().getMGroupListFragment().getActivity(), R.color.textDisabled);
        } else {
            return ContextCompat.getColor(mDividerNode.getGroupAdapter().getMGroupListFragment().getActivity(), R.color.textSecondary);
        }
    }

    @Override
    int getChildrenVisibility() {
        if ((mInstanceData.getChildren().isEmpty() || expanded()) && TextUtils.isEmpty(mInstanceData.getMNote())) {
            return View.GONE;
        } else {
            return View.VISIBLE;
        }
    }

    @NonNull
    @Override
    String getChildren() {
        Assert.assertTrue((!mInstanceData.getChildren().isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.getMNote()));

        return GroupListFragment.Companion.getChildrenText(expanded(), mInstanceData.getChildren().values(), mInstanceData.getMNote());
    }

    @Override
    int getChildrenColor() {
        Assert.assertTrue((!mInstanceData.getChildren().isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.getMNote()));

        Activity activity = getGroupListFragment().getActivity();
        Assert.assertTrue(activity != null);

        if (!mInstanceData.getTaskCurrent()) {
            return ContextCompat.getColor(activity, R.color.textDisabled);
        } else {
            return ContextCompat.getColor(activity, R.color.textSecondary);
        }
    }

    @Override
    int getExpandVisibility() {
        if (mInstanceData.getChildren().isEmpty()) {
            Assert.assertTrue(!getTreeNode().getExpandVisible());

            return View.INVISIBLE;
        } else {
            Assert.assertTrue(getTreeNode().getExpandVisible());

            return View.VISIBLE;
        }
    }

    @Override
    int getExpandImageResource() {
        Assert.assertTrue(!mInstanceData.getChildren().isEmpty());
        Assert.assertTrue(getTreeNode().getExpandVisible());

        if (getTreeNode().expanded())
            return R.drawable.ic_expand_less_black_36dp;
        else
            return R.drawable.ic_expand_more_black_36dp;
    }

    @NonNull
    @Override
    View.OnClickListener getExpandOnClickListener() {
        Assert.assertTrue(!mInstanceData.getChildren().isEmpty());
        Assert.assertTrue(getTreeNode().getExpandVisible());

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

            mInstanceData.setDone(DomainFactory.getDomainFactory(groupAdapter.getMGroupListFragment().getActivity()).setInstanceDone(groupAdapter.getMGroupListFragment().getActivity(), groupAdapter.getMDataId(), SaveService.Source.GUI, mInstanceData.getInstanceKey(), false));
            Assert.assertTrue(mInstanceData.getDone() == null);

            dividerNode.remove(this);

            nodeCollection.getNotDoneGroupCollection().add(mInstanceData);

            groupAdapter.getMGroupListFragment().updateSelectAll();
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

    @NonNull
    @Override
    View.OnLongClickListener getOnLongClickListener() {
        return getTreeNode().getOnLongClickListener();
    }

    @NonNull
    @Override
    View.OnClickListener getOnClickListener() {
        return getTreeNode().getOnClickListener();
    }

    @Override
    public int compareTo(@NonNull ModelNode another) {
        Assert.assertTrue(mInstanceData.getDone() != null);

        DoneInstanceNode doneInstanceNode = (DoneInstanceNode) another;
        Assert.assertTrue(doneInstanceNode.mInstanceData.getDone() != null);

        return -mInstanceData.getDone().compareTo(doneInstanceNode.mInstanceData.getDone()); // negate
    }

    @Override
    public boolean selectable() {
        return false;
    }

    @Override
    public void onClick() {
        GroupListFragment groupListFragment = getGroupListFragment();

        groupListFragment.getActivity().startActivity(ShowInstanceActivity.Companion.getIntent(groupListFragment.getActivity(), mInstanceData.getInstanceKey()));
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
