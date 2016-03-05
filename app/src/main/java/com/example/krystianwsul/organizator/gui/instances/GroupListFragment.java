package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.GroupListLoader;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

public class GroupListFragment extends Fragment implements LoaderManager.LoaderCallbacks<GroupListLoader.Data> {
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

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<GroupListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<GroupListLoader.Data> loader, GroupListLoader.Data data) {
        mGroupList.setAdapter(new GroupAdapter(data, getActivity()));
    }

    @Override
    public void onLoaderReset(Loader<GroupListLoader.Data> loader) {
    }

    public static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.GroupHolder> {
        private final GroupListLoader.Data mData;

        private final Context mContext;

        private final DoneGroupContainer mDoneGroupContainer = new DoneGroupContainer();
        private final NotDoneGroupContainer mNotDoneGroupContainer = new NotDoneGroupContainer();

        private final static Comparator<Group> sComparator = new Comparator<Group>() {
            @Override
            public int compare(Group lhs, Group rhs) {
                return lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
            }
        };

        public GroupAdapter(GroupListLoader.Data data, Context context) {
            Assert.assertTrue(data != null);
            Assert.assertTrue(context != null);

            mData = data;
            mContext = context;

            ArrayList<GroupListLoader.InstanceData> doneInstanceDatas = new ArrayList<>();
            ArrayList<GroupListLoader.InstanceData> notDoneInstanceDatas = new ArrayList<>();
            for (GroupListLoader.InstanceData instanceData : data.InstanceDatas.values()) {
                if (instanceData.Done != null)
                    doneInstanceDatas.add(instanceData);
                else
                    notDoneInstanceDatas.add(instanceData);
            }

            mDoneGroupContainer.addInstanceDataRange(doneInstanceDatas);
            mNotDoneGroupContainer.addInstanceRange(notDoneInstanceDatas);
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

            groupHolder.mGroupRowDetails.setText(group.getDetailsText());

            if (group.singleInstance() && !group.getSingleInstanceData().HasChildren)
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

                groupHolder.mGroupRowCheckBox.setChecked(group.getSingleInstanceData().Done != null);
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

                GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                instanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mData.DataId, mContext, instanceData.InstanceKey, isChecked);

                if (isChecked) {
                    Assert.assertTrue(instanceData.Done != null);
                    Assert.assertTrue(mNotDoneGroupContainer.contains(group));

                    int oldPosition = indexOf(group);

                    mNotDoneGroupContainer.remove(group);
                    Group newGroup = mDoneGroupContainer.addInstanceData(instanceData);

                    int newPosition = indexOf(newGroup);

                    notifyItemMoved(oldPosition, newPosition);
                } else {
                    Assert.assertTrue(instanceData.Done == null);
                    Assert.assertTrue(mDoneGroupContainer.contains(group));

                    int oldPosition = indexOf(group);

                    mDoneGroupContainer.remove(group);
                    Group newGroup = mNotDoneGroupContainer.addInstanceData(instanceData);

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
                    GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                    return ShowInstanceActivity.getIntent(context, instanceData.InstanceKey);
                } else {
                    return ShowGroupActivity.getIntent(group, context);
                }
            }
        }

        private class DoneGroupContainer {
            private final ArrayList<Group> mGroups = new ArrayList<>();

            public Group addInstanceData(GroupListLoader.InstanceData instanceData) {
                Assert.assertTrue(instanceData != null);
                Assert.assertTrue(instanceData.Done != null);

                Group group = new Group(mData.CustomTimeDatas, instanceData.Done);
                group.addInstanceData(instanceData);
                mGroups.add(group);

                Collections.sort(mGroups, sComparator);

                return group;
            }

            public void addInstanceDataRange(Collection<GroupListLoader.InstanceData> instanceDatas) {
                Assert.assertTrue(instanceDatas != null);

                for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                    Assert.assertTrue(instanceData.Done != null);

                    Group group = new Group(mData.CustomTimeDatas, instanceData.Done);
                    group.addInstanceData(instanceData);
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

        private class NotDoneGroupContainer {
            private ArrayList<Group> mGroupArray = new ArrayList<>();
            private final TreeMap<TimeStamp, Group> mGroupTree = new TreeMap<>();

            public Group addInstanceData(GroupListLoader.InstanceData instanceData) {
                Assert.assertTrue(instanceData != null);
                Assert.assertTrue(instanceData.Done == null);

                Group group = addInstanceHelper(instanceData);
                makeArray();

                return group;
            }

            public void addInstanceRange(Collection<GroupListLoader.InstanceData> instanceDatas) {
                Assert.assertTrue(instanceDatas != null);

                for (GroupListLoader.InstanceData instanceData : instanceDatas)
                    addInstanceHelper(instanceData);
                makeArray();
            }

            private Group addInstanceHelper(GroupListLoader.InstanceData instanceData) {
                Assert.assertTrue(instanceData != null);
                Assert.assertTrue(instanceData.Done == null);

                TimeStamp timeStamp = instanceData.InstanceTimeStamp;

                if (mGroupTree.containsKey(timeStamp)) {
                    Group group = mGroupTree.get(timeStamp);
                    group.addInstanceData(instanceData);
                    return group;
                } else {
                    Group group = new Group(mData.CustomTimeDatas, timeStamp);
                    group.addInstanceData(instanceData);
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

    static class Group {
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;

        private final TimeStamp mTimeStamp;

        private final ArrayList<GroupListLoader.InstanceData> mInstanceDatas = new ArrayList<>();

        public Group(ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, TimeStamp timeStamp) {
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(timeStamp != null);

            mCustomTimeDatas = customTimeDatas;
            mTimeStamp = timeStamp;
        }

        public void addInstanceData(GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(instanceData != null);
            mInstanceDatas.add(instanceData);
        }

        public String getNameText(Context context) {
            Assert.assertTrue(!mInstanceDatas.isEmpty());
            if (singleInstance()) {
                return getSingleInstanceData().DisplayText;
            } else {
                Date date = mTimeStamp.getDate();
                HourMinute hourMinute = mTimeStamp.getHourMinute();

                String timeText;

                GroupListLoader.CustomTimeData customTimeData = getCustomTimeData(date.getDayOfWeek(), hourMinute);
                if (customTimeData != null)
                    timeText = customTimeData.Name;
                else
                    timeText = hourMinute.toString();

                return date.getDisplayText(context) + ", " + timeText;
            }
        }

        private GroupListLoader.CustomTimeData getCustomTimeData(DayOfWeek dayOfWeek, HourMinute hourMinute) {
            Assert.assertTrue(dayOfWeek != null);
            Assert.assertTrue(hourMinute != null);

            for (GroupListLoader.CustomTimeData customTimeData : mCustomTimeDatas)
                if (customTimeData.HourMinutes.get(dayOfWeek) == hourMinute)
                    return customTimeData;

            return null;
        }

        public String getDetailsText() {
            Assert.assertTrue(!mInstanceDatas.isEmpty());
            if (singleInstance()) {
                return getSingleInstanceData().Name;
            } else {
                ArrayList<String> names = new ArrayList<>();
                for (GroupListLoader.InstanceData instanceData : mInstanceDatas)
                    names.add(instanceData.Name);
                return TextUtils.join(", ", names);
            }
        }

        public TimeStamp getTimeStamp() {
            return mTimeStamp;
        }

        public boolean singleInstance() {
            Assert.assertTrue(!mInstanceDatas.isEmpty());
            return (mInstanceDatas.size() == 1);
        }

        public GroupListLoader.InstanceData getSingleInstanceData() {
            Assert.assertTrue(mInstanceDatas.size() == 1);
            return mInstanceDatas.get(0);
        }
    }
}