package com.example.krystianwsul.organizatortest.gui.tasks;

import android.content.Context;
import android.support.v4.content.ContextCompat;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.Task;

import junit.framework.Assert;

import java.util.ArrayList;

public class TaskAdapter extends ArrayAdapter<Task> {
    private final ArrayList<Task> mTasks;

    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        super(context, -1, tasks);

        Assert.assertTrue(!tasks.isEmpty());
        mTasks = tasks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        if (convertView == null) {
            LayoutInflater inflater = LayoutInflater.from(getContext());
            convertView = inflater.inflate(R.layout.show_task_row, parent, false);

            TaskHolder taskHolder = new TaskHolder();
            taskHolder.taskRowName = (TextView) convertView.findViewById(R.id.task_row_name);
            taskHolder.taskRowDetails = (TextView) convertView.findViewById(R.id.task_row_details);
            taskHolder.taskRowImg = (ImageView) convertView.findViewById(R.id.task_row_img);
            convertView.setTag(taskHolder);
        }

        TaskHolder taskHolder = (TaskHolder) convertView.getTag();

        Task task = mTasks.get(position);

        taskHolder.taskRowName.setText(task.getName());

        String scheduleText = task.getScheduleText(getContext());
        if (TextUtils.isEmpty(scheduleText))
            taskHolder.taskRowDetails.setVisibility(View.GONE);
        else
            taskHolder.taskRowDetails.setText(scheduleText);

        if (task.getChildTasks().isEmpty())
            taskHolder.taskRowImg.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.ic_label_outline_black_24dp));
        else
            taskHolder.taskRowImg.setBackground(ContextCompat.getDrawable(getContext(), R.drawable.ic_list_black_24dp));

        return convertView;
    }

    private class TaskHolder {
        public TextView taskRowName;
        public TextView taskRowDetails;
        public ImageView taskRowImg;
    }
}
