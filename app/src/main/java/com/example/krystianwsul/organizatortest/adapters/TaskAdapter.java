package com.example.krystianwsul.organizatortest.adapters;

import android.content.Context;
import android.content.res.Resources;
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

/**
 * Created by Krystian on 10/23/2015.
 */
public class TaskAdapter extends ArrayAdapter<Task> {
    private final Context mContext;
    private final ArrayList<Task> mTasks;

    public TaskAdapter(Context context, ArrayList<Task> tasks) {
        super(context, -1, tasks);

        Assert.assertTrue(context != null);
        Assert.assertTrue(tasks != null);
        Assert.assertTrue(!tasks.isEmpty());

        mContext = context;
        mTasks = tasks;
    }

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        Task task = mTasks.get(position);

        View rowView = inflater.inflate(R.layout.show_task_row, parent, false);

        TextView tasksRowName = (TextView) rowView.findViewById(R.id.task_row_name);
        tasksRowName.setText(task.getName());

        TextView tasksRowSchedule = (TextView) rowView.findViewById(R.id.task_row_details);
        String scheduleText = task.getScheduleText(mContext);
        if (TextUtils.isEmpty(scheduleText))
            tasksRowSchedule.setVisibility(View.GONE);
        else
            tasksRowSchedule.setText(scheduleText);

        Resources resources = mContext.getResources();

        ImageView imgList = (ImageView) rowView.findViewById(R.id.task_row_img);
        if (task.getChildTasks().isEmpty())
            imgList.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            imgList.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        return rowView;
    }
}
