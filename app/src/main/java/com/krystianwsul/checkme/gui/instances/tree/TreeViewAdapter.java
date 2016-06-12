package com.krystianwsul.checkme.gui.instances.tree;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.utils.InstanceKey;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class TreeViewAdapter extends RecyclerView.Adapter<GroupListFragment.GroupAdapter.AbstractHolder> {
    public static final int TYPE_FAB_PADDING = 2;

    private final boolean mShowPadding;

    public TreeNodeCollection mTreeNodeCollection;

    private TreeModelAdapter mTreeModelAdapter;

    public TreeViewAdapter(boolean showPadding, TreeModelAdapter treeModelAdapter) {
        Assert.assertTrue(treeModelAdapter != null);

        mShowPadding = showPadding;
        mTreeModelAdapter = treeModelAdapter;
    }

    @Override
    public GroupListFragment.GroupAdapter.AbstractHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mTreeModelAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(GroupListFragment.GroupAdapter.AbstractHolder holder, int position) {
        mTreeModelAdapter.onBindViewHolder(holder, position);
    }

    @Override
    public int getItemCount() {
        return mTreeNodeCollection.getItemCount() + (mShowPadding ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (mShowPadding && position == mTreeNodeCollection.getItemCount())
            return TYPE_FAB_PADDING;
        else
            return mTreeNodeCollection.getItemViewType(position);
    }

    public GroupListFragment.GroupAdapter getGroupAdapter() {
        return mTreeModelAdapter.getGroupAdapter();
    }

    public GroupListFragment.ExpansionState getExpansionState() {
        return mTreeNodeCollection.getExpansionState();
    }

    public void setInstanceDatas(Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes) {
        Assert.assertTrue(instanceDatas != null);

        GroupListFragment.GroupAdapter.NodeCollection nodeCollection = GroupListFragment.GroupAdapter.NodeCollection.newNodeCollection(new WeakReference<>(this));

        mTreeNodeCollection = new TreeNodeCollection(nodeCollection.getModelNodeCollection(), new WeakReference<>(this));
        mTreeNodeCollection.setInstanceDatas(instanceDatas, expansionState, selectedNodes);
    }

    public SelectionCallback getSelectionCallback() {
        return mTreeModelAdapter.getSelectionCallback();
    }

    public List<GroupListFragment.Node> getSelectedNodes() {
        return mTreeNodeCollection.getSelectedNodes();
    }

    public void onCreateActionMode() {
        mTreeNodeCollection.onCreateActionMode();
    }

    public void onDestroyActionMode() {
        mTreeNodeCollection.onDestroyActionMode();
    }
}
