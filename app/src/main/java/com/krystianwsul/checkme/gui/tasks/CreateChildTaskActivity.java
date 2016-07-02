package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateChildTaskActivity extends CreateTaskActivity {
    public static Intent getCreateIntent(Context context, int parentTaskIdHint) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(PARENT_TASK_ID_HINT_KEY, parentTaskIdHint);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds, int parentTaskIdHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(PARENT_TASK_ID_HINT_KEY, parentTaskIdHint);
        return intent;
    }

    public static Intent getEditIntent(Context context, int taskId) {
        Intent intent = new Intent(context, CreateChildTaskActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_task_save:
                String name = mCreateTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name))
                    break;

                ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
                Assert.assertTrue(parentFragment != null);

                int parentTaskId = parentFragment.getParentTaskId();

                if (mParentTaskIdHint != null) {
                    Assert.assertTrue(mTaskId == null);
                    Assert.assertTrue(mData.TaskData == null);

                    if (mTaskIds != null)
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createJoinChildTask(mData.DataId, parentTaskId, name, mTaskIds);
                    else
                        DomainFactory.getDomainFactory(CreateChildTaskActivity.this).createChildTask(mData.DataId, parentTaskId, name);
                } else {
                    Assert.assertTrue(mTaskId != null);
                    Assert.assertTrue(mData.TaskData != null);
                    Assert.assertTrue(mTaskIds == null);

                    DomainFactory.getDomainFactory(CreateChildTaskActivity.this).updateChildTask(mData.DataId, mTaskId, name, parentTaskId);
                }

                finish();
                break;
            case android.R.id.home:
                if (tryClose())
                    finish();
                break;
            default:
                throw new UnsupportedOperationException();
        }
        return true;
    }

    @Override
    public void onLoadFinished(Loader<CreateTaskLoader.Data> loader, final CreateTaskLoader.Data data) {
        mData = data;

        FrameLayout createTaskParentFrame = (FrameLayout) findViewById(R.id.create_task_parent_frame);
        Assert.assertTrue(createTaskParentFrame != null);

        createTaskParentFrame.setVisibility(View.VISIBLE);

        mCreateTaskName.setVisibility(View.VISIBLE);

        if (mSavedInstanceState == null) {
            if (data.TaskData != null)
                mCreateTaskName.setText(data.TaskData.Name);
        }

        ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
        if (parentFragment == null) {
            if (mParentTaskIdHint != null) {
                Assert.assertTrue(mTaskId == null);

                if (mTaskIds != null)
                    parentFragment = ParentFragment.getJoinInstance(mParentTaskIdHint, mTaskIds);
                else
                    parentFragment = ParentFragment.getCreateInstance(mParentTaskIdHint);
            } else {
                Assert.assertTrue(mTaskId != null);
                Assert.assertTrue(mTaskIds == null);

                parentFragment = ParentFragment.getEditInstance(mTaskId);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.create_task_parent_frame, parentFragment)
                    .commitAllowingStateLoss();
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<CreateTaskLoader.Data> loader) {
    }

    @Override
    public void onBackPressed() {
        if (tryClose())
            super.onBackPressed();
    }

    private boolean tryClose() {
        if (dataChanged()) {
            DiscardDialogFragment discardDialogFragment = DiscardDialogFragment.newInstance();
            discardDialogFragment.setDiscardDialogListener(mDiscardDialogListener);
            discardDialogFragment.show(getSupportFragmentManager(), DISCARD_TAG);

            return false;
        } else {
            return true;
        }
    }

    @SuppressWarnings("RedundantIfStatement")
    private boolean dataChanged() {
        if (mData == null)
            return false;

        if (mTaskId != null) {
            Assert.assertTrue(mParentTaskIdHint == null);
            Assert.assertTrue(mTaskIds == null);
            Assert.assertTrue(mData.TaskData != null);

            if (!mCreateTaskName.getText().toString().equals(mData.TaskData.Name))
                return true;

            ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
            Assert.assertTrue(parentFragment != null);

            if (parentFragment.dataChanged())
                return true;
        } else {
            Assert.assertTrue(mParentTaskIdHint != null);

            if (!TextUtils.isEmpty(mCreateTaskName.getText()))
                return true;

            ParentFragment parentFragment = (ParentFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_parent_frame);
            Assert.assertTrue(parentFragment != null);

            if (parentFragment.dataChanged())
                return true;
        }

        return false;
    }
}