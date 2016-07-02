package com.krystianwsul.checkme.gui.tasks;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.Loader;
import android.text.TextUtils;
import android.view.MenuItem;
import android.view.View;
import android.widget.FrameLayout;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.DiscardDialogFragment;
import com.krystianwsul.checkme.loaders.CreateTaskLoader;

import junit.framework.Assert;

import java.util.ArrayList;

public class CreateRootTaskActivity extends CreateTaskActivity {
    private boolean mIsTimeValid = false;

    public static Intent getCreateIntent(Context context) {
        Assert.assertTrue(context != null);
        return new Intent(context, CreateRootTaskActivity.class);
    }

    public static Intent getCreateIntent(Context context, ScheduleHint scheduleHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(scheduleHint != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        return intent;
    }

    public static Intent getJoinIntent(Context context, ArrayList<Integer> joinTaskIds, ScheduleHint scheduleHint) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(joinTaskIds != null);
        Assert.assertTrue(joinTaskIds.size() > 1);
        Assert.assertTrue(scheduleHint != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putIntegerArrayListExtra(TASK_IDS_KEY, joinTaskIds);
        intent.putExtra(SCHEDULE_HINT_KEY, scheduleHint);
        return intent;
    }

    public static Intent getEditIntent(Context context, int taskId) {
        Assert.assertTrue(context != null);

        Intent intent = new Intent(context, CreateRootTaskActivity.class);
        intent.putExtra(TASK_ID_KEY, taskId);
        return intent;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_create_task_save:
                Assert.assertTrue(mCreateTaskName != null);

                if (!mIsTimeValid)
                    break;

                String name = mCreateTaskName.getText().toString().trim();

                if (TextUtils.isEmpty(name))
                    break;

                SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
                Assert.assertTrue(schedulePickerFragment != null);

                if (mTaskId != null) {
                    schedulePickerFragment.updateRootTask(mTaskId, name);
                } else if (mTaskIds != null) {
                    schedulePickerFragment.createRootJoinTask(name, mTaskIds);
                } else {
                    schedulePickerFragment.createRootTask(name);
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

        FrameLayout createTaskScheduleFrame = (FrameLayout) findViewById(R.id.create_task_schedule_frame);
        Assert.assertTrue(createTaskScheduleFrame != null);

        createTaskScheduleFrame.setVisibility(View.VISIBLE);

        mCreateTaskName.setVisibility(View.VISIBLE);

        if (mTaskId != null && mSavedInstanceState == null) {
            Assert.assertTrue(mData.TaskData != null);

            mCreateTaskName.setText(mData.TaskData.Name);
        }

        SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
        if (schedulePickerFragment == null) {
            if (mTaskId != null) {
                Assert.assertTrue(mTaskIds == null);
                Assert.assertTrue(mScheduleHint == null);

                schedulePickerFragment = SchedulePickerFragment.getEditInstance(mTaskId);
            } else if (mTaskIds != null) {
                if (mScheduleHint == null)
                    schedulePickerFragment = SchedulePickerFragment.getJoinInstance(mTaskIds);
                else
                    schedulePickerFragment = SchedulePickerFragment.getJoinInstance(mTaskIds, mScheduleHint);
            } else {
                if (mScheduleHint == null)
                    schedulePickerFragment = SchedulePickerFragment.getCreateInstance();
                else
                    schedulePickerFragment = SchedulePickerFragment.getCreateInstance(mScheduleHint);
            }

            getSupportFragmentManager().beginTransaction()
                    .add(R.id.create_task_schedule_frame, schedulePickerFragment)
                    .commitAllowingStateLoss();
        }

        invalidateOptionsMenu();
    }

    @Override
    public void onLoaderReset(Loader<CreateTaskLoader.Data> loader) {

    }

    public void setTimeValid(boolean valid) {
        mIsTimeValid = valid;
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

        if (mTaskId == null) {
            Assert.assertTrue(mData.TaskData == null);

            if (!TextUtils.isEmpty(mCreateTaskName.getText()))
                return true;

            SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
            Assert.assertTrue(schedulePickerFragment != null);

            if (schedulePickerFragment.dataChanged())
                return true;

            return false;
        } else {
            Assert.assertTrue(mData.TaskData != null);

            if (!mCreateTaskName.getText().toString().equals(mData.TaskData.Name))
                return true;

            SchedulePickerFragment schedulePickerFragment = (SchedulePickerFragment) getSupportFragmentManager().findFragmentById(R.id.create_task_schedule_frame);
            Assert.assertTrue(schedulePickerFragment != null);

            if (schedulePickerFragment.dataChanged())
                return true;

            return false;
        }
    }
}