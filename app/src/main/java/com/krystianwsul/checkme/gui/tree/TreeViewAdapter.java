package com.krystianwsul.checkme.gui.tree;

import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;

import junit.framework.Assert;

import java.util.List;

public class TreeViewAdapter extends RecyclerView.Adapter<GroupListFragment.GroupAdapter.AbstractHolder> {
    public static final int TYPE_FAB_PADDING = 2;

    private final boolean mShowPadding;

    private TreeNodeCollection mTreeNodeCollection;

    private final TreeModelAdapter mTreeModelAdapter;

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
        return mTreeNodeCollection.displayedSize() + (mShowPadding ? 1 : 0);
    }

    @Override
    public int getItemViewType(int position) {
        if (mShowPadding && position == mTreeNodeCollection.displayedSize())
            return TYPE_FAB_PADDING;
        else
            return mTreeNodeCollection.getItemViewType(position);
    }

    public TreeModelAdapter getTreeModelAdapter() {
        return mTreeModelAdapter;
    }

    public void setTreeNodeCollection(TreeNodeCollection treeNodeCollection) {
        Assert.assertTrue(treeNodeCollection != null);

        mTreeNodeCollection = treeNodeCollection;
    }

    public SelectionCallback getSelectionCallback() {
        return mTreeModelAdapter.getSelectionCallback();
    }

    public List<TreeNode> getSelectedNodes() {
        return mTreeNodeCollection.getSelectedNodes();
    }

    public void onCreateActionMode() {
        mTreeNodeCollection.onCreateActionMode();
    }

    public void onDestroyActionMode() {
        mTreeNodeCollection.onDestroyActionMode();
    }

    public void unselect() {
        mTreeNodeCollection.unselect();
    }

    public TreeNode getNode(int position) {
        return mTreeNodeCollection.getNode(position);
    }

    public int displayedSize() {
        return mTreeNodeCollection.displayedSize();
    }
}
