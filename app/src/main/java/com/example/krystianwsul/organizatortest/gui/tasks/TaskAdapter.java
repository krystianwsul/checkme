package com.example.krystianwsul.organizatortest.gui.tasks;

import android.app.Activity;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskAdapter extends RecyclerView.Adapter<TaskAdapter.TaskHolder> {
    private final Activity mActivity;
    private final ArrayList<Task> mTasks;

    public TaskAdapter(Activity activity, ArrayList<Task> tasks) {
        Assert.assertTrue(activity != null);
        Assert.assertTrue(tasks != null);

        mActivity = activity;
        mTasks = tasks;
    }

    @Override
    public int getItemCount() {
        return mTasks.size();
    }

    @Override
    public TaskHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(mActivity);
        View showTaskRow = inflater.inflate(R.layout.show_task_row, parent, false);

        TextView taskRowName = (TextView) showTaskRow.findViewById(R.id.task_row_name);
        TextView taskRowDetails = (TextView) showTaskRow.findViewById(R.id.task_row_details);
        ImageView taskRowImage = (ImageView) showTaskRow.findViewById(R.id.task_row_img);
        ImageView taskRowDelete = (ImageView) showTaskRow.findViewById(R.id.task_row_delete);

        return new TaskHolder(showTaskRow, taskRowName, taskRowDetails, taskRowImage, taskRowDelete);
    }

    @Override
    public void onBindViewHolder(final TaskHolder taskHolder, int position) {
        Task task = mTasks.get(position);

        taskHolder.mTaskRowName.setText(task.getName());

        String scheduleText = task.getScheduleText(mActivity);
        if (TextUtils.isEmpty(scheduleText))
            taskHolder.mTaskRowDetails.setVisibility(View.GONE);
        else
            taskHolder.mTaskRowDetails.setText(scheduleText);

        if (task.getChildTasks(TimeStamp.getNow()).isEmpty())
            taskHolder.mTaskRowImg.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_label_outline_black_24dp));
        else
            taskHolder.mTaskRowImg.setBackground(ContextCompat.getDrawable(mActivity, R.drawable.ic_list_black_24dp));

        taskHolder.mShowTaskRow.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskHolder.onRowClick();
            }
        });

        taskHolder.mTaskRowDelete.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                taskHolder.onDeleteClick();
            }
        });
    }

    public class TaskHolder extends RecyclerView.ViewHolder {
        public final View mShowTaskRow;
        public final TextView mTaskRowName;
        public final TextView mTaskRowDetails;
        public final ImageView mTaskRowImg;
        public final ImageView mTaskRowDelete;

        public TaskHolder(View showTaskRow, TextView taskRowName, TextView taskRowDetails, ImageView taskRowImg, ImageView taskRowDelete) {
            super(showTaskRow);

            Assert.assertTrue(taskRowName != null);
            Assert.assertTrue(taskRowDetails != null);
            Assert.assertTrue(taskRowImg != null);
            Assert.assertTrue(taskRowDelete != null);

            mShowTaskRow = showTaskRow;
            mTaskRowName = taskRowName;
            mTaskRowDetails = taskRowDetails;
            mTaskRowImg = taskRowImg;
            mTaskRowDelete = taskRowDelete;
        }

        public void onRowClick() {
            Task task = mTasks.get(getAdapterPosition());
            Assert.assertTrue(task != null);

            mActivity.startActivity(ShowTaskActivity.getIntent(task, mActivity));
        }

        public void onDeleteClick() {
            int position = getAdapterPosition();

            Task task = mTasks.get(position);
            Assert.assertTrue(task != null);

            task.setEndTimeStamp(TimeStamp.getNow());

            mTasks.remove(position);
            notifyItemRemoved(position);
        }
    }
}
