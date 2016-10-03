package com.krystianwsul.checkme.gui.tree;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.ViewGroup;

import junit.framework.Assert;

import java.util.List;

public class TreeViewAdapter {
    public static final int TYPE_FAB_PADDING = 1000;

    private final boolean mShowPadding;

    private TreeNodeCollection mTreeNodeCollection;

    private final TreeModelAdapter mTreeModelAdapter;

    private Adapter mAdapter;

    public TreeViewAdapter(boolean showPadding, TreeModelAdapter treeModelAdapter) {
        Assert.assertTrue(treeModelAdapter != null);

        mShowPadding = showPadding;
        mTreeModelAdapter = treeModelAdapter;
    }

    @NonNull
    public RecyclerView.Adapter<RecyclerView.ViewHolder> getAdapter() {
        Assert.assertTrue(mAdapter == null);

        mAdapter = new Adapter(this);
        return mAdapter;
    }

    public int getItemCount() {
        return mTreeNodeCollection.displayedSize() + (mShowPadding ? 1 : 0);
    }

    @NonNull
    public TreeModelAdapter getTreeModelAdapter() {
        return mTreeModelAdapter;
    }

    public void setTreeNodeCollection(TreeNodeCollection treeNodeCollection) {
        Assert.assertTrue(treeNodeCollection != null);

        mTreeNodeCollection = treeNodeCollection;
    }

    boolean hasActionMode() {
        return mTreeModelAdapter.hasActionMode();
    }

    void incrementSelected() {
        mTreeModelAdapter.incrementSelected();
    }

    void decrementSelected() {
        mTreeModelAdapter.decrementSelected();
    }

    @NonNull
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

    @NonNull
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

    void notifyItemChanged(int position) {
        Assert.assertTrue(mAdapter != null);

        Log.e("asdf", "notifyItemChanged(" + position + ")");

        mAdapter.notifyItemChanged(position);
    }

    void notifyItemInserted(int position) {
        Assert.assertTrue(mAdapter != null);

        Log.e("asdf", "notifyItemInserted(" + position + ")");

        mAdapter.notifyItemInserted(position);
    }

    void notifyItemRemoved(int position) {
        Assert.assertTrue(mAdapter != null);

        Log.e("asdf", "notifyItemRemoved(" + position + ")");

        mAdapter.notifyItemRemoved(position);
    }

    void notifyItemRangeChanged(int positionStart, int itemCount) {
        Assert.assertTrue(mAdapter != null);

        Log.e("asdf", "notifyItemRangeChanged(" + positionStart + ", " + itemCount + ")");

        mAdapter.notifyItemRangeChanged(positionStart, itemCount);
    }

    void notifyItemRangeInserted(int positionStart, int itemCount) {
        Assert.assertTrue(mAdapter != null);

        Log.e("asdf", "notifyItemRangeInserted(" + positionStart + ", " + itemCount + ")");

        mAdapter.notifyItemRangeInserted(positionStart, itemCount);
    }

    void notifyItemRangeRemoved(int positionStart, int itemCount) {
        Assert.assertTrue(mAdapter != null);

        Log.e("asdf", "notifyItemRangeInserted(" + positionStart + ", " + itemCount + ")");

        mAdapter.notifyItemRangeRemoved(positionStart, itemCount);
    }

    private static class Adapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
        @NonNull
        private final TreeViewAdapter mTreeViewAdapter;

        Adapter(@NonNull TreeViewAdapter treeViewAdapter) {
            mTreeViewAdapter = treeViewAdapter;
        }

        @NonNull
        private TreeViewAdapter getTreeViewAdapter() {
            return mTreeViewAdapter;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            return getTreeViewAdapter().mTreeModelAdapter.onCreateViewHolder(parent, viewType);
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
            getTreeViewAdapter().mTreeModelAdapter.onBindViewHolder(holder, position);
        }

        @Override
        public int getItemCount() {
            return getTreeViewAdapter().getItemCount();
        }

        @Override
        public int getItemViewType(int position) {
            if (getTreeViewAdapter().mShowPadding && position == getTreeViewAdapter().mTreeNodeCollection.displayedSize())
                return TYPE_FAB_PADDING;
            else
                return getTreeViewAdapter().mTreeNodeCollection.getItemViewType(position);
        }
    }
}
