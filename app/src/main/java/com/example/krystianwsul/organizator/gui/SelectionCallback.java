package com.example.krystianwsul.organizator.gui;

import android.support.v7.view.ActionMode;
import android.view.Menu;
import android.view.MenuItem;

import junit.framework.Assert;

public abstract class SelectionCallback implements ActionMode.Callback {
    private int mSelected = 0;

    protected ActionMode mActionMode;

    private boolean mFinishing = false;

    @Override
    public boolean onCreateActionMode(ActionMode mode, Menu menu) {
        mActionMode = mode;
        return true;
    }

    @Override
    public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
        return false;
    }

    @Override
    public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
        onMenuClick(item);

        Assert.assertTrue(!mFinishing);

        mActionMode.finish();

        return true;
    }

    @Override
    public void onDestroyActionMode(ActionMode mode) {
        Assert.assertTrue(mActionMode != null);

        if (!mFinishing) {
            Assert.assertTrue(mSelected > 0);

            for (int i = mSelected; i > 0; i--) {
                mSelected--;

                if (mSelected == 1) {
                    onSecondToLastRemoved();
                } else if (mSelected == 0) {
                    mActionMode = null;

                    onLastRemoved();
                } else {
                    onOtherRemoved();
                }
            }
        } else {
            mActionMode = null;
        }

        unselect();
    }

    public void setSelected(int selected) {
        if (selected > mSelected) {
            for (int i = mSelected; i < selected; i++)
                incrementSelected();
        } else if (selected < mSelected) {
            for (int i = mSelected; i > selected; i--)
                decrementSelected();
        }
    }

    public void incrementSelected() {
        mSelected++;

        if (mSelected == 1) {
            Assert.assertTrue(mActionMode == null);
            onFirstAdded();
        } else if (mSelected == 2) {
            Assert.assertTrue(mActionMode != null);
            onSecondAdded();
        } else {
            Assert.assertTrue(mActionMode != null);
            onOtherAdded();
        }
    }

    public void decrementSelected() {
        Assert.assertTrue(mSelected > 0);
        Assert.assertTrue(mActionMode != null);

        mSelected--;

        if (mSelected == 1) {
            onSecondToLastRemoved();
        } else if (mSelected == 0) {
            Assert.assertTrue(!mFinishing);

            mFinishing = true;
            mActionMode.finish();
            mFinishing = false;

            onLastRemoved();
        } else {
            onOtherRemoved();
        }
    }

    public boolean hasActionMode() {
        if (mActionMode == null) {
            Assert.assertTrue(mSelected == 0);
            return false;
        } else {
            Assert.assertTrue(mSelected > 0);
            return true;
        }
    }

    protected abstract void unselect();

    protected abstract void onMenuClick(MenuItem menuItem);

    protected abstract void onFirstAdded();

    protected abstract void onSecondAdded();

    protected abstract void onOtherAdded();

    protected abstract void onLastRemoved();

    protected abstract void onSecondToLastRemoved();

    protected abstract void onOtherRemoved();
}
