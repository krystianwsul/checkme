package com.krystianwsul.treeadapter;

import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import junit.framework.Assert;

import java.util.List;

public class TreeViewAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    private static final int TYPE_PADDING = 1000;

    @Nullable
    @LayoutRes
    private final Integer mPaddingLayout;

    @Nullable
    private TreeNodeCollection mTreeNodeCollection;

    @NonNull
    private final TreeModelAdapter mTreeModelAdapter;

    public TreeViewAdapter(@NonNull TreeModelAdapter treeModelAdapter) {
        this(treeModelAdapter, null);
    }

    public TreeViewAdapter(@NonNull TreeModelAdapter treeModelAdapter, @Nullable @LayoutRes Integer paddingLayout) {
        mTreeModelAdapter = treeModelAdapter;
        mPaddingLayout = paddingLayout;
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

        return mTreeNodeCollection.displayedSize() + (mPaddingLayout != null ? 1 : 0);
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

    public void selectAll() {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        Assert.assertTrue(!mTreeModelAdapter.hasActionMode());

        mTreeNodeCollection.selectAll();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        if (viewType == TYPE_PADDING) {
            Assert.assertTrue(mPaddingLayout != null);

            View view = LayoutInflater.from(parent.getContext()).inflate(mPaddingLayout, parent, false);
            Assert.assertTrue(view != null);

            return new PaddingHolder(view);
        } else {
            return mTreeModelAdapter.onCreateViewHolder(parent, viewType);
        }
    }

    @Override
    public void onBindViewHolder(RecyclerView.ViewHolder holder, int position) {
        Assert.assertTrue(position >= 0);

        int itemCount = getItemCount();
        Assert.assertTrue(position < itemCount);

        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        int displayedSize = mTreeNodeCollection.displayedSize();

        if (position < displayedSize) {
            TreeNode treeNode = mTreeNodeCollection.getNode(position);
            treeNode.onBindViewHolder(holder);
        } else {
            Assert.assertTrue(position == displayedSize);
            Assert.assertTrue(mPaddingLayout != null);
            Assert.assertTrue(position == itemCount - 1);
        }
    }

    @Override
    public int getItemViewType(int position) {
        if (mTreeNodeCollection == null)
            throw new SetTreeNodeCollectionNotCalledException();

        if (mPaddingLayout != null && position == mTreeNodeCollection.displayedSize())
            return TYPE_PADDING;
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

    private static class PaddingHolder extends RecyclerView.ViewHolder {
        PaddingHolder(@NonNull View view) {
            super(view);
        }
    }
}
