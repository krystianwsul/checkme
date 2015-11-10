package com.example.krystianwsul.organizatortest;

import android.content.Context;
import android.content.res.Resources;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;

import junit.framework.Assert;

import java.util.ArrayList;

/**
 * Created by Krystian on 10/23/2015.
 */
public class GroupAdapter extends ArrayAdapter<Group> {
    private final Context mContext;
    private final ArrayList<Group> mGroups;

    public GroupAdapter(Context context, ArrayList<Group> groups) {
        super(context, -1, groups);

        Assert.assertTrue(context != null);
        Assert.assertTrue(groups != null);
        Assert.assertTrue(!groups.isEmpty());

        mContext = context;
        mGroups = groups;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        Group group = mGroups.get(position);

        View rowView = inflater.inflate(R.layout.show_tasks_row, parent, false);

        TextView tasksRowName = (TextView) rowView.findViewById(R.id.tasks_row_name);
        tasksRowName.setText(group.getName());

        TextView tasksRowSchedule = (TextView) rowView.findViewById(R.id.tasks_row_schedule);
        String scheduleText = group.getScheduleText(mContext);
        if (TextUtils.isEmpty(scheduleText))
            tasksRowSchedule.setVisibility(View.GONE);
        else
            tasksRowSchedule.setText(scheduleText);

        Resources resources = mContext.getResources();

        ImageView imgList = (ImageView) rowView.findViewById(R.id.tasks_row_img_list);
        ArrayList<Group> childGroups = group.getChildGroups();
        if (childGroups.isEmpty())
            imgList.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            imgList.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        return rowView;
    }
}
