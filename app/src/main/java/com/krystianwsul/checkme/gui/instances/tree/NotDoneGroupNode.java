package com.krystianwsul.checkme.gui.instances.tree;

import android.app.Activity;
import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.View;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.persistencemodel.SaveService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimeStamp;
import com.krystianwsul.treeadapter.ModelNode;
import com.krystianwsul.treeadapter.NodeContainer;
import com.krystianwsul.treeadapter.TreeNode;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class NotDoneGroupNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
    @NonNull
    private final NotDoneGroupCollection mNotDoneGroupCollection;

    private TreeNode mTreeNode;

    @NonNull
    private final List<GroupListFragment.InstanceData> mInstanceDatas;

    @NonNull
    private final ArrayList<NotDoneInstanceNode> mNotDoneInstanceNodes = new ArrayList<>();

    private NodeCollection mNodeCollection;

    final ExactTimeStamp mExactTimeStamp;

    private final boolean mSelectable;

    NotDoneGroupNode(float density, int indentation, @NonNull NotDoneGroupCollection notDoneGroupCollection, @NonNull List<GroupListFragment.InstanceData> instanceDatas, boolean selectable) {
        super(density, indentation);
        Assert.assertTrue(!instanceDatas.isEmpty());

        mNotDoneGroupCollection = notDoneGroupCollection;
        mInstanceDatas = instanceDatas;

        mExactTimeStamp = instanceDatas.get(0).getInstanceTimeStamp().toExactTimeStamp();
        Assert.assertTrue(Stream.of(instanceDatas)
                .allMatch(instanceData -> instanceData.getInstanceTimeStamp().toExactTimeStamp().equals(mExactTimeStamp)));

        mSelectable = selectable;
    }

    @NonNull
    TreeNode initialize(@Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes, @NonNull NodeContainer nodeContainer) {
        boolean expanded;
        boolean doneExpanded;
        if (mInstanceDatas.size() == 1) {
            GroupListFragment.InstanceData instanceData = mInstanceDatas.get(0);
            Assert.assertTrue(instanceData != null);

            if (expandedInstances != null && expandedInstances.containsKey(instanceData.getInstanceKey()) && !instanceData.getChildren().isEmpty()) {
                expanded = true;
                doneExpanded = expandedInstances.get(instanceData.getInstanceKey());
            } else {
                expanded = false;
                doneExpanded = false;
            }
        } else {
            expanded = (expandedGroups != null && expandedGroups.contains(mExactTimeStamp.toTimeStamp()));
            doneExpanded = false;
        }

        boolean selected = (mInstanceDatas.size() == 1 && selectedNodes != null && selectedNodes.contains(mInstanceDatas.get(0).getInstanceKey()));

        mTreeNode = new TreeNode(this, nodeContainer, expanded, selected);

        if (mInstanceDatas.size() == 1) {
            mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, mTreeNode, null);

            mTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceDatas.get(0).getChildren().values(), expandedGroups, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));
        } else {
            List<TreeNode> notDoneInstanceTreeNodes = Stream.of(mInstanceDatas)
                    .map(instanceData -> newChildTreeNode(instanceData, expandedInstances, selectedNodes))
                    .collect(Collectors.toList());

            mTreeNode.setChildTreeNodes(notDoneInstanceTreeNodes);
        }

        return mTreeNode;
    }

    @NonNull
    GroupListFragment.InstanceData getSingleInstanceData() {
        Assert.assertTrue(mInstanceDatas.size() == 1);

        GroupListFragment.InstanceData instanceData = mInstanceDatas.get(0);
        Assert.assertTrue(instanceData != null);

        return instanceData;
    }

    boolean singleInstance() {
        Assert.assertTrue(!mInstanceDatas.isEmpty());

        return (mInstanceDatas.size() == 1);
    }

    void addExpandedInstances(@NonNull HashMap<InstanceKey, Boolean> expandedInstances) {
        if (!expanded())
            return;

        if (singleInstance()) {
            Assert.assertTrue(!expandedInstances.containsKey(getSingleInstanceData().getInstanceKey()));

            expandedInstances.put(getSingleInstanceData().getInstanceKey(), mNodeCollection.getDoneExpanded());
            mNodeCollection.addExpandedInstances(expandedInstances);
        } else {
            for (NotDoneInstanceNode notDoneInstanceNode : mNotDoneInstanceNodes)
                notDoneInstanceNode.addExpandedInstances(expandedInstances);
        }
    }

    @Override
    int getNameVisibility() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        if (singleInstance()) {
            return View.VISIBLE;
        } else {
            if (notDoneGroupTreeNode.expanded()) {
                return View.INVISIBLE;
            } else {
                return View.VISIBLE;
            }
        }
    }

    @NonNull
    @Override
    String getName() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            return instanceData.getName();
        } else {
            Assert.assertTrue(!notDoneGroupTreeNode.expanded());

            return Stream.of(mInstanceDatas)
                    .sortBy(GroupListFragment.InstanceData::getMTaskStartExactTimeStamp)
                    .map(GroupListFragment.InstanceData::getName)
                    .collect(Collectors.joining(", "));
        }
    }

    @NonNull
    private NotDoneGroupCollection getNotDoneGroupCollection() {
        return mNotDoneGroupCollection;
    }

    @NonNull
    NodeCollection getNodeCollection() {
        return getNotDoneGroupCollection().getNodeCollection();
    }

    @NonNull
    @Override
    public GroupListFragment.GroupAdapter getGroupAdapter() {
        return getNodeCollection().getGroupAdapter();
    }

    @NonNull
    private GroupListFragment getGroupListFragment() {
        return getGroupAdapter().getMGroupListFragment();
    }

    @Override
    int getNameColor() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            if (!instanceData.getTaskCurrent()) {
                return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
            } else {
                return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
            }
        } else {
            Assert.assertTrue(!notDoneGroupTreeNode.expanded());

            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
        }
    }

    @Override
    boolean getNameSingleLine() {
        return true;
    }

    @Override
    int getDetailsVisibility() {
        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            if (TextUtils.isEmpty(instanceData.getDisplayText())) {
                return View.GONE;
            } else {
                return View.VISIBLE;
            }
        } else {
            return View.VISIBLE;
        }
    }

    @NonNull
    @Override
    String getDetails() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            Assert.assertTrue(!TextUtils.isEmpty(instanceData.getDisplayText()));

            return instanceData.getDisplayText();
        } else {
            ExactTimeStamp exactTimeStamp = ((NotDoneGroupNode) notDoneGroupTreeNode.getModelNode()).mExactTimeStamp;

            Date date = exactTimeStamp.getDate();
            HourMinute hourMinute = exactTimeStamp.toTimeStamp().getHourMinute();

            GroupListFragment.CustomTimeData customTimeData = getCustomTimeData(date.getDayOfWeek(), hourMinute);

            String timeText;
            if (customTimeData != null)
                timeText = customTimeData.getName();
            else
                timeText = hourMinute.toString();

            return date.getDisplayText(groupListFragment.getActivity()) + ", " + timeText;
        }
    }

    @Override
    int getDetailsColor() {
        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            if (!instanceData.getTaskCurrent()) {
                return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
            } else {
                return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
            }
        } else {
            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
        }
    }

    @Override
    int getChildrenVisibility() {
        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            if ((instanceData.getChildren().isEmpty() || expanded()) && TextUtils.isEmpty(instanceData.getMNote())) {
                return View.GONE;
            } else {
                return View.VISIBLE;
            }
        } else {
            return View.GONE;
        }
    }

    @NonNull
    @Override
    String getChildren() {
        Assert.assertTrue(singleInstance());

        GroupListFragment.InstanceData instanceData = getSingleInstanceData();

        Assert.assertTrue((!instanceData.getChildren().isEmpty() && !expanded()) || !TextUtils.isEmpty(instanceData.getMNote()));

        return GroupListFragment.Companion.getChildrenText(expanded(), instanceData.getChildren().values(), instanceData.getMNote());
    }

    @Override
    int getChildrenColor() {
        Assert.assertTrue(singleInstance());

        GroupListFragment.InstanceData instanceData = getSingleInstanceData();

        Assert.assertTrue((!instanceData.getChildren().isEmpty() && !expanded()) || !TextUtils.isEmpty(instanceData.getMNote()));

        Activity activity = getGroupListFragment().getActivity();
        Assert.assertTrue(activity != null);

        if (!instanceData.getTaskCurrent()) {
            return ContextCompat.getColor(activity, R.color.textDisabled);
        } else {
            return ContextCompat.getColor(activity, R.color.textSecondary);
        }
    }

    @Override
    int getExpandVisibility() {
        TreeNode treeNode = getTreeNode();

        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            boolean visibleChildren = Stream.of(treeNode.getAllChildren()).anyMatch(TreeNode::canBeShown);
            if (instanceData.getChildren().isEmpty() || (groupListFragment.getMSelectionCallback().hasActionMode() && (treeNode.hasSelectedDescendants() || !visibleChildren))) {
                Assert.assertTrue(!treeNode.getExpandVisible());
                return View.INVISIBLE;
            } else {
                Assert.assertTrue(treeNode.getExpandVisible());
                return View.VISIBLE;
            }
        } else {
            if (groupListFragment.getMSelectionCallback().hasActionMode() && treeNode.hasSelectedDescendants()) {
                Assert.assertTrue(!treeNode.getExpandVisible());
                return View.INVISIBLE;
            } else {
                Assert.assertTrue(treeNode.getExpandVisible());
                return View.VISIBLE;
            }
        }
    }

    @Override
    int getExpandImageResource() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            Assert.assertTrue(!instanceData.getChildren().isEmpty());

            if (notDoneGroupTreeNode.expanded())
                return R.drawable.ic_expand_less_black_36dp;
            else
                return R.drawable.ic_expand_more_black_36dp;
        } else {
            Assert.assertTrue(!(groupListFragment.getMSelectionCallback().hasActionMode() && notDoneGroupTreeNode.hasSelectedDescendants()));

            if (notDoneGroupTreeNode.expanded())
                return R.drawable.ic_expand_less_black_36dp;
            else
                return R.drawable.ic_expand_more_black_36dp;
        }
    }

    @NonNull
    @Override
    View.OnClickListener getExpandOnClickListener() {
        Assert.assertTrue(getTreeNode().getExpandVisible());

        return getTreeNode().getExpandListener();
    }

    @Override
    int getCheckBoxVisibility() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            if (groupListFragment.getMSelectionCallback().hasActionMode()) {
                return View.INVISIBLE;
            } else {
                return View.VISIBLE;
            }
        } else {
            if (notDoneGroupTreeNode.expanded()) {
                return View.GONE;
            } else {
                return View.INVISIBLE;
            }
        }
    }

    @Override
    boolean getCheckBoxChecked() {
        GroupListFragment groupListFragment = getGroupListFragment();

        Assert.assertTrue(singleInstance());

        Assert.assertTrue(!groupListFragment.getMSelectionCallback().hasActionMode());

        return false;
    }

    @NonNull
    @Override
    View.OnClickListener getCheckBoxOnClickListener() {
        final NotDoneGroupCollection notDoneGroupCollection = getNotDoneGroupCollection();

        NodeCollection nodeCollection = getNodeCollection();

        GroupListFragment.GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

        Assert.assertTrue(singleInstance());

        GroupListFragment.InstanceData instanceData = getSingleInstanceData();

        Assert.assertTrue(!groupAdapter.getMGroupListFragment().getMSelectionCallback().hasActionMode());

        return v -> {
            v.setOnClickListener(null);

            instanceData.setDone(DomainFactory.getDomainFactory(groupAdapter.getMGroupListFragment().getActivity()).setInstanceDone(groupAdapter.getMGroupListFragment().getActivity(), groupAdapter.getMDataId(), SaveService.Source.GUI, instanceData.getInstanceKey(), true));
            Assert.assertTrue(instanceData.getDone() != null);

            GroupListFragment.Companion.recursiveExists(instanceData);

            nodeCollection.getDividerNode().add(instanceData);

            notDoneGroupCollection.remove(this);

            groupAdapter.getMGroupListFragment().updateSelectAll();
        };
    }

    @Override
    int getSeparatorVisibility() {
        return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
    }

    @Override
    int getBackgroundColor() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            if (notDoneGroupTreeNode.isSelected())
                return ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected);
            else
                return Color.TRANSPARENT;
        } else {
            return Color.TRANSPARENT;
        }
    }

    @Override
    View.OnLongClickListener getOnLongClickListener() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode.getOnLongClickListener();
    }

    @Override
    View.OnClickListener getOnClickListener() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode.getOnClickListener();
    }

    @Override
    public void onClick() {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        GroupListFragment groupListFragment = getGroupListFragment();

        if (singleInstance()) {
            GroupListFragment.InstanceData instanceData = getSingleInstanceData();

            groupListFragment.getActivity().startActivity(ShowInstanceActivity.Companion.getIntent(groupListFragment.getActivity(), instanceData.getInstanceKey()));
        } else {
            groupListFragment.getActivity().startActivity(ShowGroupActivity.Companion.getIntent(((NotDoneGroupNode) notDoneGroupTreeNode.getModelNode()).mExactTimeStamp, groupListFragment.getActivity()));
        }
    }

    private GroupListFragment.CustomTimeData getCustomTimeData(@NonNull DayOfWeek dayOfWeek, @NonNull HourMinute hourMinute) {
        GroupListFragment.GroupAdapter groupAdapter = getGroupAdapter();

        for (GroupListFragment.CustomTimeData customTimeData : groupAdapter.getMCustomTimeDatas())
            if (customTimeData.getHourMinutes().get(dayOfWeek) == hourMinute)
                return customTimeData;

        return null;
    }

    private void remove(@NonNull NotDoneInstanceNode notDoneInstanceNode) {
        TreeNode notDoneGroupTreeNode = getTreeNode();

        Assert.assertTrue(mInstanceDatas.contains(notDoneInstanceNode.mInstanceData));
        mInstanceDatas.remove(notDoneInstanceNode.mInstanceData);

        Assert.assertTrue(mNotDoneInstanceNodes.contains(notDoneInstanceNode));
        mNotDoneInstanceNodes.remove(notDoneInstanceNode);

        TreeNode childTreeNode = notDoneInstanceNode.getTreeNode();
        boolean selected = childTreeNode.isSelected();

        if (selected)
            childTreeNode.deselect();

        notDoneGroupTreeNode.remove(childTreeNode);

        Assert.assertTrue(!mInstanceDatas.isEmpty());
        if (mInstanceDatas.size() == 1) {
            Assert.assertTrue(mNotDoneInstanceNodes.size() == 1);

            NotDoneInstanceNode notDoneInstanceNode1 = mNotDoneInstanceNodes.get(0);
            Assert.assertTrue(notDoneInstanceNode1 != null);

            TreeNode childTreeNode1 = notDoneInstanceNode1.getTreeNode();

            mNotDoneInstanceNodes.remove(notDoneInstanceNode1);

            notDoneGroupTreeNode.remove(childTreeNode1);

            mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, notDoneGroupTreeNode, null);

            List<TreeNode> childTreeNodes = mNodeCollection.initialize(mInstanceDatas.get(0).getChildren().values(), null, null, false, null, mSelectable, null, false, null);

            Stream.of(childTreeNodes)
                    .forEach(notDoneGroupTreeNode::add);

            if (selected)
                getTreeNode().select();
        }
    }

    @Override
    public int compareTo(@NonNull ModelNode another) {
        if (another instanceof NoteNode) {
            return 1;
        } else if (another instanceof NotDoneGroupNode) {
            NotDoneGroupNode notDoneGroupNode = (NotDoneGroupNode) another;

            int timeStampComparison = mExactTimeStamp.compareTo(notDoneGroupNode.mExactTimeStamp);
            if (timeStampComparison != 0) {
                return timeStampComparison;
            } else {
                Assert.assertTrue(singleInstance());
                Assert.assertTrue(notDoneGroupNode.singleInstance());

                return getSingleInstanceData().getMTaskStartExactTimeStamp().compareTo(notDoneGroupNode.getSingleInstanceData().getMTaskStartExactTimeStamp());
            }
        } else if (another instanceof UnscheduledNode) {
            return -1;
        } else {
            Assert.assertTrue(another instanceof DividerNode);

            return -1;
        }
    }

    void addInstanceData(@NonNull GroupListFragment.InstanceData instanceData) {
        Assert.assertTrue(instanceData.getInstanceTimeStamp().toExactTimeStamp().equals(mExactTimeStamp));

        Assert.assertTrue(mTreeNode != null);

        Assert.assertTrue(!mInstanceDatas.isEmpty());
        if (mInstanceDatas.size() == 1) {
            Assert.assertTrue(mNotDoneInstanceNodes.isEmpty());

            mTreeNode.removeAll();
            mNodeCollection = null;

            GroupListFragment.InstanceData instanceData1 = mInstanceDatas.get(0);
            Assert.assertTrue(instanceData1 != null);

            NotDoneInstanceNode notDoneInstanceNode = new NotDoneInstanceNode(mDensity, mIndentation, instanceData1, NotDoneGroupNode.this, mSelectable);
            mNotDoneInstanceNodes.add(notDoneInstanceNode);

            mTreeNode.add(notDoneInstanceNode.initialize(null, null, mTreeNode));
        }

        mInstanceDatas.add(instanceData);

        mTreeNode.add(newChildTreeNode(instanceData, null, null));
    }

    @NonNull
    private TreeNode newChildTreeNode(@NonNull GroupListFragment.InstanceData instanceData, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(mTreeNode != null);

        NotDoneInstanceNode notDoneInstanceNode = new NotDoneInstanceNode(mDensity, mIndentation, instanceData, this, mSelectable);

        TreeNode childTreeNode = notDoneInstanceNode.initialize(expandedInstances, selectedNodes, mTreeNode);

        mNotDoneInstanceNodes.add(notDoneInstanceNode);

        return childTreeNode;
    }

    boolean expanded() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode.expanded();
    }

    @Override
    public boolean selectable() {
        return mSelectable && mNotDoneInstanceNodes.isEmpty();
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

    @NonNull
    TreeNode getTreeNode() {
        Assert.assertTrue(mTreeNode != null);

        return mTreeNode;
    }

    void removeFromParent() {
        getNotDoneGroupCollection().remove(this);
    }

    static class NotDoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
        @NonNull
        private final NotDoneGroupNode mNotDoneGroupNode;

        private TreeNode mTreeNode;

        @NonNull
        final GroupListFragment.InstanceData mInstanceData;

        private NodeCollection mNodeCollection;

        private final boolean mSelectable;

        NotDoneInstanceNode(float density, int indentation, @NonNull GroupListFragment.InstanceData instanceData, @NonNull NotDoneGroupNode notDoneGroupNode, boolean selectable) {
            super(density, indentation);

            mInstanceData = instanceData;
            mNotDoneGroupNode = notDoneGroupNode;
            mSelectable = selectable;
        }

        TreeNode initialize(@Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes, @NonNull TreeNode notDoneGroupTreeNode) {
            boolean selected = (selectedNodes != null && selectedNodes.contains(mInstanceData.getInstanceKey()));

            boolean expanded = false;
            boolean doneExpanded = false;
            if ((expandedInstances != null && expandedInstances.containsKey(mInstanceData.getInstanceKey()) && !mInstanceData.getChildren().isEmpty())) {
                expanded = true;
                doneExpanded = expandedInstances.get(mInstanceData.getInstanceKey());
            }

            mTreeNode = new TreeNode(this, notDoneGroupTreeNode, expanded, selected);

            mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, mTreeNode, null);
            mTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceData.getChildren().values(), null, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));

            return mTreeNode;
        }

        @NonNull
        private TreeNode getTreeNode() {
            Assert.assertTrue(mTreeNode != null);

            return mTreeNode;
        }

        @NonNull
        private NotDoneGroupNode getParentNotDoneGroupNode() {
            return mNotDoneGroupNode;
        }

        @NonNull
        private NotDoneGroupCollection getParentNotDoneGroupCollection() {
            return getParentNotDoneGroupNode().getNotDoneGroupCollection();
        }

        @NonNull
        NodeCollection getParentNodeCollection() {
            return getParentNotDoneGroupCollection().getNodeCollection();
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
            return getParentNotDoneGroupNode().getGroupAdapter();
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
            TreeNode treeNode = getTreeNode();

            GroupListFragment groupListFragment = getGroupListFragment();

            boolean visibleChildren = Stream.of(treeNode.getAllChildren()).anyMatch(TreeNode::canBeShown);
            if (mInstanceData.getChildren().isEmpty() || (groupListFragment.getMSelectionCallback().hasActionMode() && (treeNode.hasSelectedDescendants() || !visibleChildren))) {
                Assert.assertTrue(!treeNode.getExpandVisible());

                return View.INVISIBLE;
            } else {
                Assert.assertTrue(treeNode.getExpandVisible());

                return View.VISIBLE;
            }
        }

        @Override
        int getExpandImageResource() {
            TreeNode treeNode = getTreeNode();

            Assert.assertTrue(treeNode.getExpandVisible());

            GroupListFragment groupListFragment = getGroupListFragment();

            Assert.assertTrue(!(mInstanceData.getChildren().isEmpty() || (groupListFragment.getMSelectionCallback().hasActionMode() && treeNode.hasSelectedDescendants())));

            if (treeNode.expanded())
                return R.drawable.ic_expand_less_black_36dp;
            else
                return R.drawable.ic_expand_more_black_36dp;
        }

        @NonNull
        @Override
        View.OnClickListener getExpandOnClickListener() {
            TreeNode treeNode = getTreeNode();

            Assert.assertTrue(treeNode.getExpandVisible());

            GroupListFragment groupListFragment = getGroupListFragment();

            Assert.assertTrue(!(mInstanceData.getChildren().isEmpty() || (groupListFragment.getMSelectionCallback().hasActionMode() && treeNode.hasSelectedDescendants())));

            return treeNode.getExpandListener();
        }

        @Override
        int getCheckBoxVisibility() {
            GroupListFragment groupListFragment = getGroupListFragment();

            if (groupListFragment.getMSelectionCallback().hasActionMode()) {
                return View.INVISIBLE;
            } else {
                return View.VISIBLE;
            }
        }

        @Override
        boolean getCheckBoxChecked() {
            return false;
        }

        @NonNull
        @Override
        View.OnClickListener getCheckBoxOnClickListener() {
            final NotDoneGroupNode notDoneGroupNode = getParentNotDoneGroupNode();

            final TreeNode notDoneGroupTreeNode = notDoneGroupNode.getTreeNode();

            Assert.assertTrue(notDoneGroupTreeNode.expanded());

            NodeCollection nodeCollection = getParentNodeCollection();

            GroupListFragment.GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

            Assert.assertTrue(!groupAdapter.getMGroupListFragment().getMSelectionCallback().hasActionMode());

            return v -> {
                v.setOnClickListener(null);

                Assert.assertTrue(notDoneGroupTreeNode.expanded());

                mInstanceData.setDone(DomainFactory.getDomainFactory(groupAdapter.getMGroupListFragment().getActivity()).setInstanceDone(groupAdapter.getMGroupListFragment().getActivity(), groupAdapter.getMDataId(), SaveService.Source.GUI, mInstanceData.getInstanceKey(), true));
                Assert.assertTrue(mInstanceData.getDone() != null);

                GroupListFragment.Companion.recursiveExists(mInstanceData);

                notDoneGroupNode.remove(this);

                nodeCollection.getDividerNode().add(mInstanceData);

                groupAdapter.getMGroupListFragment().updateSelectAll();
            };
        }

        @Override
        int getSeparatorVisibility() {
            Assert.assertTrue(mTreeNode != null);

            return (mTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
        }

        @Override
        int getBackgroundColor() {
            final NotDoneGroupNode notDoneGroupNode = getParentNotDoneGroupNode();

            final TreeNode notDoneGroupTreeNode = notDoneGroupNode.getTreeNode();

            TreeNode childTreeNode = getTreeNode();

            Assert.assertTrue(notDoneGroupTreeNode.expanded());

            GroupListFragment groupListFragment = getGroupListFragment();

            if (childTreeNode.isSelected())
                return ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected);
            else
                return Color.TRANSPARENT;
        }

        @Override
        View.OnLongClickListener getOnLongClickListener() {
            Assert.assertTrue(mTreeNode != null);

            return mTreeNode.getOnLongClickListener();
        }

        @Override
        View.OnClickListener getOnClickListener() {
            Assert.assertTrue(mTreeNode != null);

            return mTreeNode.getOnClickListener();
        }

        @Override
        public void onClick() {
            GroupListFragment groupListFragment = getGroupListFragment();

            groupListFragment.getActivity().startActivity(ShowInstanceActivity.Companion.getIntent(groupListFragment.getActivity(), mInstanceData.getInstanceKey()));
        }

        @Override
        public int compareTo(@NonNull ModelNode another) {
            return mInstanceData.getMTaskStartExactTimeStamp().compareTo(((NotDoneInstanceNode) another).mInstanceData.getMTaskStartExactTimeStamp());
        }

        @Override
        public boolean selectable() {
            return mSelectable;
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
            getParentNotDoneGroupNode().remove(this);
        }
    }
}
