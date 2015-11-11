package com.example.krystianwsul.organizatortest.adapters;

import android.content.Context;
import android.content.res.Resources;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;

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

    @Override
    public View getView(int position, View convertView, ViewGroup parent) {
        LayoutInflater inflater = LayoutInflater.from(mContext);
        Group group = mGroups.get(position);

        View rowView = inflater.inflate(R.layout.show_task_row, parent, false);

        TextView tasksRowName = (TextView) rowView.findViewById(R.id.task_row_name);
        tasksRowName.setText(group.getNameText(mContext));

        TextView tasksRowSchedule = (TextView) rowView.findViewById(R.id.task_row_details);
        tasksRowSchedule.setText(group.getDetailsText(mContext));

        Resources resources = mContext.getResources();

        ImageView imgList = (ImageView) rowView.findViewById(R.id.task_row_img);
        if (group.singleInstance() && group.getSingleSinstance().getChildInstances().isEmpty())
            imgList.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            imgList.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        return rowView;
    }
}
