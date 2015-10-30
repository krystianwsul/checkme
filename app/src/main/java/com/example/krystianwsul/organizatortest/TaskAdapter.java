package com.example.krystianwsul.organizatortest;

import android.app.Activity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.schedules.Schedule;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public class TaskAdapter extends ArrayAdapter<Task> {
    private final Activity mActivity;
    private final ArrayList<Task> mTasks;

    public TaskAdapter(Activity activity, ArrayList<Task> tasks) {
        super(activity, -1, tasks);

        Assert.assertTrue(activity != null);
        Assert.assertTrue(tasks != null);
        Assert.assertTrue(!tasks.isEmpty());

        mActivity = activity;
        mTasks = tasks;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = mActivity.getLayoutInflater();
        Task task = mTasks.get(position);

        View rowView = inflater.inflate(R.layout.show_tasks_row, parent, false);

        TextView tasksRowName = (TextView) rowView.findViewById(R.id.tasks_row_name);
        tasksRowName.setText(task.getName());

        TextView tasksRowSchedule = (TextView) rowView.findViewById(R.id.tasks_row_schedule);
        Schedule schedule = task.getSchedule();
        if (schedule == null)
            tasksRowSchedule.setVisibility(View.GONE);
        else
            tasksRowSchedule.setText(schedule.getTaskText(mActivity));

        ImageView imgList = (ImageView) rowView.findViewById(R.id.tasks_row_img_list);
        if (task.getChildTasks() == null)
            imgList.setBackground(mActivity.getResources().getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            imgList.setBackground(mActivity.getResources().getDrawable(R.drawable.ic_list_black_18dp));

        return rowView;
    }
}
