package com.example.krystianwsul.organizatortest.arrayadapters;

import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
import com.example.krystianwsul.organizatortest.ShowGroupActivity;
import com.example.krystianwsul.organizatortest.ShowInstanceActivity;
import com.example.krystianwsul.organizatortest.domainmodel.dates.Date;
import com.example.krystianwsul.organizatortest.domainmodel.dates.TimeStamp;
import com.example.krystianwsul.organizatortest.domainmodel.groups.Group;
import com.example.krystianwsul.organizatortest.domainmodel.instances.Instance;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.RootTask;
import com.example.krystianwsul.organizatortest.domainmodel.tasks.TaskFactory;
import com.example.krystianwsul.organizatortest.domainmodel.times.HourMinute;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.TreeMap;

/**
 * Created by Krystian on 10/23/2015.
 */
public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupHolder> {
    private final Context mContext;

    private ArrayList<Group> mDoneGroups = new ArrayList<>();
    private ArrayList<Group> mNotDoneGroups = new ArrayList<>();

    private Comparator<Group> mComparator = new Comparator<Group>() {
        @Override
        public int compare(Group lhs, Group rhs) {
            return lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
        }
    };

    public GroupAdapter(Context context) {
        Assert.assertTrue(context != null);

        mContext = context;

        Collection<RootTask> rootTasks = TaskFactory.getInstance().getRootTasks();

        Calendar tomorrowCalendar = Calendar.getInstance();
        tomorrowCalendar.add(Calendar.DATE, 1);
        Date tomorrowDate = new Date(tomorrowCalendar);

        ArrayList<Instance> instances = new ArrayList<>();
        for (RootTask rootTask : rootTasks)
            instances.addAll(rootTask.getInstances(null, new TimeStamp(tomorrowDate, new HourMinute(0, 0))));

        ArrayList<Instance> doneInstances = new ArrayList<>();
        ArrayList<Instance> notDoneInstances = new ArrayList<>();
        for (Instance instance : instances) {
            if (instance.getDone() != null)
                doneInstances.add(instance);
            else
                notDoneInstances.add(instance);
        }

        for (Instance instance : doneInstances) {
            Group group = new Group(instance.getDone());
            group.addInstance(instance);
            mDoneGroups.add(group);
        }

        Collections.sort(mDoneGroups, mComparator);

        HashMap<TimeStamp, Group> notDoneGroupsHash = new HashMap();
        for (Instance instance : notDoneInstances) {
            TimeStamp timeStamp = instance.getDateTime().getTimeStamp();
            if (notDoneGroupsHash.containsKey(timeStamp)) {
                notDoneGroupsHash.get(timeStamp).addInstance(instance);
            } else {
                Group group = new Group(timeStamp);
                group.addInstance(instance);
                notDoneGroupsHash.put(timeStamp, group);
            }
        }

        for (Group group : notDoneGroupsHash.values())
            mNotDoneGroups.add(group);

        Collections.sort(mNotDoneGroups, mComparator);
    }

    private Group getGroup(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < getItemCount());

        if (position < mDoneGroups.size())
            return mDoneGroups.get(position);
        else
            return mNotDoneGroups.get(position - mDoneGroups.size());
    }

    private int indexOf(Group group) {
        if (mDoneGroups.contains(group)) {
            return mDoneGroups.indexOf(group);
        } else {
            Assert.assertTrue(mNotDoneGroups.contains(group));
            return mDoneGroups.size() + mNotDoneGroups.indexOf(group);
        }
    }

    @Override
    public GroupHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.show_group_row, parent, false);

        TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
        TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
        ImageView groupRowImg = (ImageView) groupRow.findViewById(R.id.group_row_img);
        CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

        GroupHolder groupHolder = new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowImg, groupCheckBox);
        return groupHolder;
    }

    @Override
    public void onBindViewHolder(GroupHolder groupHolder, int position) {
        final Group group = getGroup(position);

        groupHolder.mGroupRowName.setText(group.getNameText(mContext));

        groupHolder.mGroupRowDetails.setText(group.getDetailsText(mContext));

        Resources resources = mContext.getResources();

        if (group.singleInstance() && group.getSingleSinstance().getChildInstances().isEmpty())
            groupHolder.mGroupRowImg.setBackground(resources.getDrawable(R.drawable.ic_label_outline_black_18dp));
        else
            groupHolder.mGroupRowImg.setBackground(resources.getDrawable(R.drawable.ic_list_black_18dp));

        if (group.singleInstance()) {
            groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);

            groupHolder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    Assert.assertTrue(group.singleInstance());

                    CheckBox checkBox = (CheckBox) v;
                    boolean isChecked = checkBox.isChecked();

                    group.getSingleSinstance().setDone(isChecked);

                    if (isChecked) {
                        Assert.assertTrue(mNotDoneGroups.contains(group));

                        int oldPosition = indexOf(group);

                        mNotDoneGroups.remove(group);
                        mDoneGroups.add(group);

                        int newPosition = indexOf(group);

                        notifyItemMoved(oldPosition, newPosition);
                    } else {
                        Assert.assertTrue(mDoneGroups.contains(group));

                        int oldPosition = indexOf(group);

                        mDoneGroups.remove(group);
                        mNotDoneGroups.add(group);
                        Collections.sort(mNotDoneGroups, mComparator);

                        int newPosition = indexOf(group);

                        notifyItemMoved(oldPosition, newPosition);
                    }
                }
            });

            groupHolder.mGroupRowCheckBox.setChecked(group.getSingleSinstance().getDone() != null);
        } else {
            groupHolder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
        }

        groupHolder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = getIntent(group, view.getContext());
                mContext.startActivity(intent);
            }

            private Intent getIntent(Group group, Context context) {
                if (group.singleInstance()) {
                    Instance instance = group.getSingleSinstance();
                    return ShowInstanceActivity.getIntent(instance, context);
                } else {
                    return ShowGroupActivity.getIntent(group, context);
                }
            }
        });
    }

    @Override
    public int getItemCount() {
        return mDoneGroups.size() + mNotDoneGroups.size();
    }

    public static class GroupHolder extends RecyclerView.ViewHolder {
        public final TableLayout mGroupRow;
        public final TextView mGroupRowName;
        public final TextView mGroupRowDetails;
        public final ImageView mGroupRowImg;
        public final CheckBox mGroupRowCheckBox;

        public GroupHolder(TableLayout groupRow, TextView groupRowName, TextView groupRowDetails, ImageView groupRowImg, CheckBox groupRowCheckBox) {
            super(groupRow);

            Assert.assertTrue(groupRow != null);
            Assert.assertTrue(groupRowName != null);
            Assert.assertTrue(groupRowDetails != null);
            Assert.assertTrue(groupRowImg != null);
            Assert.assertTrue(groupRowCheckBox != null);

            mGroupRow = groupRow;
            mGroupRowName = groupRowName;
            mGroupRowDetails = groupRowDetails;
            mGroupRowImg = groupRowImg;
            mGroupRowCheckBox = groupRowCheckBox;
        }
    }
}
