package com.example.krystianwsul.organizatortest.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizatortest.R;
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
import java.util.TreeMap;

public class GroupListFragment extends Fragment {
    private RecyclerView mGroupList;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.group_list_fragment, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);
        mGroupList = (RecyclerView) view.findViewById(R.id.groups_list);
        mGroupList.setLayoutManager(new LinearLayoutManager(getContext()));
    }

    @Override
    public void onStart() {
        super.onStart();
        mGroupList.setAdapter(new GroupAdapter(getContext()));
    }

    public static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupHolder> {
        private final Context mContext;

        private DoneGroupContainer mDoneGroupContainer = new DoneGroupContainer();
        private NotDoneGroupContainer mNotDoneGroupContainer = new NotDoneGroupContainer();

        private static Comparator<Group> sComparator = new Comparator<Group>() {
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
            tomorrowCalendar.add(Calendar.DATE, 2);
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

            mDoneGroupContainer.addInstanceRange(doneInstances);
            mNotDoneGroupContainer.addInstanceRange(notDoneInstances);
        }

        private Group getGroup(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position < getItemCount());

            if (position < mDoneGroupContainer.size())
                return mDoneGroupContainer.get(position);
            else
                return mNotDoneGroupContainer.get(position - mDoneGroupContainer.size());
        }

        private int indexOf(Group group) {
            Assert.assertTrue(group != null);

            if (mDoneGroupContainer.contains(group)) {
                return mDoneGroupContainer.indexOf(group);
            } else {
                Assert.assertTrue(mNotDoneGroupContainer.contains(group));
                return mDoneGroupContainer.size() + mNotDoneGroupContainer.indexOf(group);
            }
        }

        @Override
        public GroupHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.show_group_row, parent, false);

            TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
            TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
            ImageView groupRowImg = (ImageView) groupRow.findViewById(R.id.group_row_img);
            CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

            return new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowImg, groupCheckBox);
        }

        @Override
        public void onBindViewHolder(final GroupHolder groupHolder, int position) {
            Group group = getGroup(position);

            groupHolder.mGroupRowName.setText(group.getNameText(mContext));

            groupHolder.mGroupRowDetails.setText(group.getDetailsText(mContext));

            if (group.singleInstance() && group.getSingleSinstance().getChildInstances().isEmpty())
                groupHolder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_24dp));
            else
                groupHolder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_list_black_24dp));

            if (group.singleInstance()) {
                groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);

                groupHolder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        groupHolder.onCheckBoxClick((CheckBox) v);
                    }
                });

                groupHolder.mGroupRowCheckBox.setChecked(group.getSingleSinstance().getDone() != null);
            } else {
                groupHolder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
            }

            groupHolder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
                @Override
                public void onClick(View view) {
                    groupHolder.onRowClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDoneGroupContainer.size() + mNotDoneGroupContainer.size();
        }

        public class GroupHolder extends RecyclerView.ViewHolder {
            public final TableLayout mGroupRow;
            public final TextView mGroupRowName;
            public final TextView mGroupRowDetails;
            public final ImageView mGroupRowImg;
            public final CheckBox mGroupRowCheckBox;

            public GroupHolder(TableLayout groupRow, TextView groupRowName, TextView groupRowDetails, ImageView groupRowImg, CheckBox groupRowCheckBox) {
                super(groupRow);

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

            public void onCheckBoxClick(CheckBox checkBox) {
                Assert.assertTrue(checkBox != null);

                int position = getAdapterPosition();
                Group group = getGroup(position);
                Assert.assertTrue(group != null);
                Assert.assertTrue(group.singleInstance());

                boolean isChecked = checkBox.isChecked();
                group.getSingleSinstance().setDone(isChecked);

                if (isChecked) {
                    Assert.assertTrue(mNotDoneGroupContainer.contains(group));

                    int oldPosition = indexOf(group);

                    mNotDoneGroupContainer.remove(group);
                    Group newGroup = mDoneGroupContainer.addInstance(group.getSingleSinstance());

                    int newPosition = indexOf(newGroup);

                    notifyItemMoved(oldPosition, newPosition);
                } else {
                    Assert.assertTrue(mDoneGroupContainer.contains(group));

                    int oldPosition = indexOf(group);

                    mDoneGroupContainer.remove(group);
                    Group newGroup = mNotDoneGroupContainer.addInstance(group.getSingleSinstance());

                    int newPosition = indexOf(newGroup);

                    if (newGroup.singleInstance()) {
                        notifyItemMoved(oldPosition, newPosition);
                    } else {
                        notifyItemRemoved(oldPosition);
                        notifyItemChanged(newPosition);
                    }
                }
            }

            public void onRowClick() {
                Group group = getGroup(getAdapterPosition());
                Assert.assertTrue(group != null);

                Intent intent = getIntent(group, mContext);
                mContext.startActivity(intent);
            }

            private Intent getIntent(Group group, Context context) {
                Assert.assertTrue(group != null);
                Assert.assertTrue(context != null);

                if (group.singleInstance()) {
                    Instance instance = group.getSingleSinstance();
                    return ShowInstanceActivity.getIntent(instance, context);
                } else {
                    return ShowGroupActivity.getIntent(group, context);
                }
            }
        }

        private static class DoneGroupContainer {
            private ArrayList<Group> mGroups = new ArrayList<>();

            public Group addInstance(Instance instance) {
                Assert.assertTrue(instance != null);
                Assert.assertTrue(instance.getDone() != null);

                Group group = new Group(instance.getDone());
                group.addInstance(instance);
                mGroups.add(group);

                Collections.sort(mGroups, sComparator);

                return group;
            }

            public void addInstanceRange(Collection<Instance> instances) {
                Assert.assertTrue(instances != null);

                for (Instance instance : instances) {
                    Assert.assertTrue(instance.getDone() != null);

                    Group group = new Group(instance.getDone());
                    group.addInstance(instance);
                    mGroups.add(group);
                }

                Collections.sort(mGroups, sComparator);
            }

            public int size() {
                return mGroups.size();
            }

            public Group get(int index) {
                return mGroups.get(index);
            }

            public int indexOf(Group group) {
                Assert.assertTrue(mGroups.contains(group));
                return mGroups.indexOf(group);
            }

            public boolean contains(Group group) {
                return mGroups.contains(group);
            }

            public void remove(Group group) {
                Assert.assertTrue(mGroups.contains(group));
                mGroups.remove(group);
            }
        }

        private static class NotDoneGroupContainer {
            private ArrayList<Group> mGroupArray = new ArrayList<>();
            private TreeMap<TimeStamp, Group> mGroupTree = new TreeMap<>();

            public Group addInstance(Instance instance) {
                Assert.assertTrue(instance != null);
                Assert.assertTrue(instance.getDone() == null);

                Group group = addInstanceHelper(instance);
                makeArray();

                return group;
            }

            public void addInstanceRange(Collection<Instance> instances) {
                Assert.assertTrue(instances != null);

                for (Instance instance : instances)
                    addInstanceHelper(instance);
                makeArray();
            }

            private Group addInstanceHelper(Instance instance) {
                Assert.assertTrue(instance != null);
                Assert.assertTrue(instance.getDone() == null);

                TimeStamp timeStamp = instance.getDateTime().getTimeStamp();

                if (mGroupTree.containsKey(timeStamp)) {
                    Group group = mGroupTree.get(timeStamp);
                    group.addInstance(instance);
                    return group;
                } else {
                    Group group = new Group(timeStamp);
                    group.addInstance(instance);
                    mGroupTree.put(timeStamp, group);
                    return group;
                }
            }

            private void makeArray() {
                mGroupArray = new ArrayList<>(mGroupTree.values());
            }

            public int size() {
                return mGroupArray.size();
            }

            public Group get(int index) {
                return mGroupArray.get(index);
            }

            public int indexOf(Group group) {
                Assert.assertTrue(mGroupArray.contains(group));
                return mGroupArray.indexOf(group);
            }

            public boolean contains(Group group) {
                return mGroupArray.contains(group);
            }

            public void remove(Group group) {
                Assert.assertTrue(mGroupArray.contains(group));
                mGroupTree.remove(group.getTimeStamp());
                mGroupArray.remove(group);
            }
        }
    }
}