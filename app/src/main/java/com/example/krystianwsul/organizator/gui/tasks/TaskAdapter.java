package com.example.krystianwsul.organizator.gui.tasks;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.domainmodel.Task;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {
    private final Activity mActivity;
    private final DomainFactory mDomainFactory;
    private final ArrayList<TaskWrapper> mTaskWrappers;

    private boolean mEditing = false;

    public TaskAdapter(Activity activity, DomainFactory domainFactory, ArrayList<Task> tasks) {
        Assert.assertTrue(activity != null);
        Assert.assertTrue(domainFactory != null);
        Assert.assertTrue(tasks != null);

        mActivity = activity;
        mDomainFactory = domainFactory;

        mTaskWrappers = new ArrayList<>();
        for (Task task : tasks)
            mTaskWrappers.add(new TaskWrapper(task));
    }

    @Override
    public int getItemCount() {
        return mTaskWrappers.size();
    }

    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View showTaskRow = inflater.inflate(R.layout.show_task_row, parent, false);

        TextView taskRowName = (TextView) showTaskRow.findViewById(R.id.task_row_name);
        TextView taskRowDetails = (TextView) showTaskRow.findViewById(R.id.task_row_details);
        ImageView taskRowImage = (ImageView) showTaskRow.findViewById(R.id.task_row_img);
        CheckBox taskRowCheckBox = (CheckBox) showTaskRow.findViewById(R.id.task_row_checkbox);
        ImageView taskRowDelete = (ImageView) showTaskRow.findViewById(R.id.task_row_delete);

        return new TaskHolder(showTaskRow, taskRowName, taskRowDetails, taskRowImage, taskRowCheckBox, taskRowDelete);
    }

    @Override
    public void onBindViewHolder(final TaskHolder taskHolder, int position) {
        TaskWrapper taskWrapper = mTaskWrappers.get(position);

        taskHolder.mTaskRowName.setText(taskWrapper.mTask.getName());

        String scheduleText = taskWrapper.mTask.getScheduleText(mActivity, TimeStamp.getNow());
        if (TextUtils.isEmpty(scheduleText))
            taskHolder.mTaskRowDetails.setVisibility(View.GONE);
        else
            taskHolder.mTaskRowDetails.setText(scheduleText);

        if (taskWrapper.mTask.getChildTasks(TimeStamp.getNow()).isEmpty())
            taskHolder.mTaskRowImg.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_label_outline_black_24dp));
        else
            taskHolder.mTaskRowImg.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_list_black_24dp));

        taskHolder.mShowTaskRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskHolder.onRowClick();
            }
        });

        if (mEditing) {
            taskHolder.mTaskRowCheckBox.setVisibility(View.VISIBLE);
            taskHolder.mTaskRowCheckBox.setChecked(taskWrapper.mSelected);
        } else {
            taskHolder.mTaskRowCheckBox.setVisibility(View.GONE);
        }

        taskHolder.mTaskRowCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                taskHolder.onCheckedChanged(isChecked);
            }
        });

        taskHolder.mTaskRowDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskHolder.onDeleteClick();
            }
        });
    }

    public void setEditing(boolean editing) {
        mEditing = editing;

        if (!mEditing)
            for (TaskWrapper taskWrapper : mTaskWrappers)
                taskWrapper.mSelected = false;

        notifyItemRangeChanged(0, getItemCount());
    }

    public ArrayList<Task> getSelected() {
        ArrayList<Task> tasks = new ArrayList<>();
        for (TaskWrapper taskWrapper : mTaskWrappers)
            if (taskWrapper.mSelected)
                tasks.add(taskWrapper.mTask);
        return tasks;
    }

    private static class TaskWrapper {
        public final Task mTask;
        public boolean mSelected;

        public TaskWrapper(Task task) {
            Assert.assertTrue(task != null);
            mTask = task;
        }
    }

    public class TaskHolder extends RecyclerView.ViewHolder {
        public final View mShowTaskRow;
        public final TextView mTaskRowName;
        public final TextView mTaskRowDetails;
        public final ImageView mTaskRowImg;
        public final CheckBox mTaskRowCheckBox;
        public final ImageView mTaskRowDelete;

        public TaskHolder(View showTaskRow, TextView taskRowName, TextView taskRowDetails, ImageView taskRowImg, CheckBox taskRowCheckBox, ImageView taskRowDelete) {
            super(showTaskRow);

            Assert.assertTrue(taskRowName != null);
            Assert.assertTrue(taskRowDetails != null);
            Assert.assertTrue(taskRowImg != null);
            Assert.assertTrue(taskRowCheckBox != null);
            Assert.assertTrue(taskRowDelete != null);

            mShowTaskRow = showTaskRow;
            mTaskRowName = taskRowName;
            mTaskRowDetails = taskRowDetails;
            mTaskRowImg = taskRowImg;
            mTaskRowCheckBox = taskRowCheckBox;
            mTaskRowDelete = taskRowDelete;
        }

        public void onRowClick() {
            TaskWrapper taskWrapper = mTaskWrappers.get(getAdapterPosition());
            Assert.assertTrue(taskWrapper != null);

            mActivity.startActivity(ShowTaskActivity.getIntent(taskWrapper.mTask, mActivity));
        }

        public void onDeleteClick() {
            int position = getAdapterPosition();

            TaskWrapper taskWrapper = mTaskWrappers.get(position);
            Assert.assertTrue(taskWrapper != null);

            taskWrapper.mTask.setEndTimeStamp(TimeStamp.getNow());

            mDomainFactory.save();

            mTaskWrappers.remove(position);
            notifyItemRemoved(position);
        }

        public void onCheckedChanged(boolean checked) {
            int position = getAdapterPosition();

            TaskWrapper taskWrapper = mTaskWrappers.get(position);
            Assert.assertTrue(taskWrapper != null);

            taskWrapper.mSelected = checked;
        }
    }
}
