package com.krystianwsul.treeadapter;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.ViewGroup;

import junit.framework.Assert;

import java.util.List;

public class TreeViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    public static final int TYPE_FAB_PADDING = 1000;

    private final boolean mShowPadding;

    @Nullable
    private TreeNodeCollection mTreeNodeCollection;

    @NonNull
    private final TreeModelAdapter mTreeModelAdapter;

    public TreeViewAdapter(boolean showPadding, @NonNull TreeModelAdapter treeModelAdapter) {
        mShowPadding = showPadding;
        mTreeModelAdapter = treeModelAdapter;
    }

    public void setTreeNodeCollection(@NonNull TreeNodeCollection treeNodeCollection) {
        if (mTreeNodeCollection != null)
            throw new SetTreeNodeCollectionCalledTwiceException();

        mTreeNodeCollection = treeNodeCollection;
    }

    @Override
    public int getItemCount() {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        return mTreeNodeCollection.displayedSize() + (mShowPadding ? 1 : 0);
    }

    @NonNull
    public TreeModelAdapter getTreeModelAdapter() {
        return mTreeModelAdapter;
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
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        return mTreeNodeCollection.getSelectedNodes();
    }

    public void onCreateActionMode() {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        mTreeNodeCollection.onCreateActionMode();
    }

    public void onDestroyActionMode() {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        mTreeNodeCollection.onDestroyActionMode();
    }

    public void unselect() {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        mTreeNodeCollection.unselect();
    }

    @NonNull
    public TreeNode getNode(int position) {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        return mTreeNodeCollection.getNode(position);
    }

    public int displayedSize() {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        return mTreeNodeCollection.displayedSize();
    }

    public void selectAll() {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        Assert.assertTrue(!mTreeModelAdapter.hasActionMode());

        mTreeNodeCollection.selectAll();
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
    public int getItemViewType(int position) {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        if (mShowPadding && position == mTreeNodeCollection.displayedSize())
            return TYPE_FAB_PADDING;
        else
            return mTreeNodeCollection.getItemViewType(position);
    }

    @SuppressWarnings("WeakerAccess")
    public static class SetTreeNodeCollectionNotCalledException extends InitializationException {
        private SetTreeNodeCollectionNotCalledException() {
            super("TreeViewAdapter.setTreeNodeCollection() has not been called.");
        }
    }

    @SuppressWarnings("WeakerAccess")
    public static class SetTreeNodeCollectionCalledTwiceException extends InitializationException {
        private SetTreeNodeCollectionCalledTwiceException() {
            super("TreeViewAdapter.setTreeNodeCollection() has already been called.");
        }
    }
}
