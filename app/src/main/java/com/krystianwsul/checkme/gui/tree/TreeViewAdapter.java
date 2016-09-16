package com.krystianwsul.checkme.gui.tree;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import junit.framework.Assert;

import java.util.List;

public class TreeViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_FAB_PADDING = 1000;

    private final boolean mShowPadding;

    private TreeNodeCollection mTreeNodeCollection;

    private final TreeModelAdapter mTreeModelAdapter;

    public TreeViewAdapter(boolean showPadding, TreeModelAdapter treeModelAdapter) {
        Assert.assertTrue(treeModelAdapter != null);

        mShowPadding = showPadding;
        mTreeModelAdapter = treeModelAdapter;
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return mTreeModelAdapter.onCreateViewHolder(parent, viewType);
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
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

    @NonNull
    public TreeModelAdapter getTreeModelAdapter() {
        return mTreeModelAdapter;
    }

    public void setTreeNodeCollection(TreeNodeCollection treeNodeCollection) {
        Assert.assertTrue(treeNodeCollection != null);

        mTreeNodeCollection = treeNodeCollection;
    }

    public boolean hasActionMode() {
        return mTreeModelAdapter.hasActionMode();
    }

    public void incrementSelected() {
        mTreeModelAdapter.incrementSelected();
    }

    public void decrementSelected() {
        mTreeModelAdapter.decrementSelected();
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

    public void selectAll() {
        Assert.assertTrue(!mTreeModelAdapter.hasActionMode());

        mTreeNodeCollection.selectAll();
    }
}
