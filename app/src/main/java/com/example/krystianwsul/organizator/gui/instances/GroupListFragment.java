package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.GroupListLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

public class GroupListFragment extends Fragment implements LoaderManager.LoaderCallbacks<GroupListLoader.Data> {
    private final static String EXPANDED_KEY = "expanded";

    private ImageView mGroupListExpand;

    private RecyclerView mGroupListNotDone;
    private RecyclerView mGroupListDone;

    private boolean mExpanded = false;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_group_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(EXPANDED_KEY));
            mExpanded = savedInstanceState.getBoolean(EXPANDED_KEY);
        }

        LinearLayout groupListToggle = (LinearLayout) view.findViewById(R.id.group_list_toggle);
        Assert.assertTrue(groupListToggle != null);

        mGroupListExpand = (ImageView) view.findViewById(R.id.group_list_expand);
        Assert.assertTrue(mGroupListExpand != null);

        groupListToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mExpanded = !mExpanded;
                toggle();
            }
        });

        mGroupListDone = (RecyclerView) view.findViewById(R.id.group_list_done);
        Assert.assertTrue(mGroupListDone != null);

        mGroupListDone.setLayoutManager(new LinearLayoutManager(getContext()));

        mGroupListNotDone = (RecyclerView) view.findViewById(R.id.group_list_not_done);
        Assert.assertTrue(mGroupListNotDone != null);

        mGroupListNotDone.setLayoutManager(new LinearLayoutManager(getContext()));

        toggle();

        getLoaderManager().initLoader(0, null, this);
    }

    private void toggle() {
        if (mExpanded) {
            mGroupListDone.setVisibility(View.VISIBLE);
            mGroupListExpand.setImageResource(R.drawable.ic_expand_less_black_24dp);
        } else {
            mGroupListDone.setVisibility(View.GONE);
            mGroupListExpand.setImageResource(R.drawable.ic_expand_more_black_24dp);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXPANDED_KEY, mExpanded);
    }

    @Override
    public Loader<GroupListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<GroupListLoader.Data> loader, GroupListLoader.Data data) {
        ArrayList<GroupListLoader.InstanceData> doneInstances = new ArrayList<>();
        ArrayList<GroupListLoader.InstanceData> notDoneInstances = new ArrayList<>();

        for (GroupListLoader.InstanceData instanceData : data.InstanceDatas.values()) {
            if (instanceData.Done != null)
                doneInstances.add(instanceData);
            else
                notDoneInstances.add(instanceData);
        }

        mGroupListNotDone.setAdapter(new NotDoneGroupAdapter(getActivity(), data.DataId, data.CustomTimeDatas, notDoneInstances, new NotDoneGroupAdapter.OnCheckListner() {
            @Override
            public void onChecked(GroupListLoader.InstanceData instanceData) {
                ((DoneGroupAdapter) mGroupListDone.getAdapter()).add(instanceData);
            }
        }));
        mGroupListDone.setAdapter(new DoneGroupAdapter(data.DataId, data.CustomTimeDatas, doneInstances, getActivity(), new DoneGroupAdapter.OnUncheckListner() {
            @Override
            public void onUnchecked(GroupListLoader.InstanceData instanceData) {
                ((NotDoneGroupAdapter) mGroupListNotDone.getAdapter()).add(instanceData);
            }
        }));
    }

    @Override
    public void onLoaderReset(Loader<GroupListLoader.Data> loader) {
    }

    public static class NotDoneGroupAdapter extends RecyclerView.Adapter<NotDoneGroupAdapter.Holder> {
        private final Context mContext;

        private final int mDataId;
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;

        private ArrayList<Group> mGroupArray = new ArrayList<>();
        private final TreeMap<TimeStamp, Group> mGroupTree = new TreeMap<>();

        private final OnCheckListner mOnCheckListener;

        public NotDoneGroupAdapter(Context context, int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, ArrayList<GroupListLoader.InstanceData> instanceDatas, OnCheckListner onCheckListner) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(onCheckListner != null);

            mContext = context;
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mOnCheckListener = onCheckListner;

            for (GroupListLoader.InstanceData instanceData : instanceDatas)
                addInstanceHelper(instanceData);
            makeArray();
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_show_group, parent, false);

            TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
            TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
            ImageView groupRowImg = (ImageView) groupRow.findViewById(R.id.group_row_img);
            CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

            return new Holder(groupRow, groupRowName, groupRowDetails, groupRowImg, groupCheckBox);
        }

        @Override
        public void onBindViewHolder(final Holder holder, int position) {
            Group group = mGroupArray.get(position);
            Assert.assertTrue(group != null);

            holder.mGroupRowName.setText(group.getNameText(mContext));

            holder.mGroupRowDetails.setText(group.getDetailsText());

            if (group.singleInstance() && !group.getSingleInstanceData().HasChildren)
                holder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_24dp));
            else
                holder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_list_black_24dp));

            if (group.singleInstance()) {
                holder.mGroupRowCheckBox.setVisibility(View.VISIBLE);

                holder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        holder.onCheckBoxClick();
                    }
                });

                holder.mGroupRowCheckBox.setChecked(group.getSingleInstanceData().Done != null);
            } else {
                holder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
            }

            holder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.onRowClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mGroupArray.size();
        }

        private Pair<Group, Boolean> addInstanceHelper(GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(instanceData != null);
            Assert.assertTrue(instanceData.Done == null);

            TimeStamp timeStamp = instanceData.InstanceTimeStamp;

            if (mGroupTree.containsKey(timeStamp)) {
                Group group = mGroupTree.get(timeStamp);
                group.addInstanceData(instanceData);
                return new Pair<>(group, false);
            } else {
                Group group = new Group(mCustomTimeDatas, timeStamp);
                group.addInstanceData(instanceData);
                mGroupTree.put(timeStamp, group);
                return new Pair<>(group, true);
            }
        }

        private void makeArray() {
            mGroupArray = new ArrayList<>(mGroupTree.values());
        }

        public void add(GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(instanceData != null);
            Assert.assertTrue(instanceData.Done == null);

            Pair<Group, Boolean> pair = addInstanceHelper(instanceData);
            makeArray();

            int postition = mGroupArray.indexOf(pair.first);
            if (pair.second)
                notifyItemInserted(postition);
            else
                notifyItemChanged(postition);
        }

        public void remove(Group group) {
            Assert.assertTrue(mGroupArray.contains(group));
            mGroupTree.remove(group.getTimeStamp());
            mGroupArray.remove(group);
        }

        public class Holder extends RecyclerView.ViewHolder {
            public final TableLayout mGroupRow;
            public final TextView mGroupRowName;
            public final TextView mGroupRowDetails;
            public final ImageView mGroupRowImg;
            public final CheckBox mGroupRowCheckBox;

            public Holder(TableLayout groupRow, TextView groupRowName, TextView groupRowDetails, ImageView groupRowImg, CheckBox groupRowCheckBox) {
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

            public void onCheckBoxClick() {
                int position = getAdapterPosition();
                Group group = mGroupArray.get(position);
                Assert.assertTrue(group != null);
                Assert.assertTrue(group.singleInstance());

                GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                instanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, instanceData.InstanceKey, true);
                Assert.assertTrue(instanceData.Done != null);

                TickService.startService(mContext);

                remove(group);

                notifyItemRemoved(position);

                mOnCheckListener.onChecked(instanceData);
            }

            public void onRowClick() {
                Group group = mGroupArray.get(getAdapterPosition());
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

        public interface OnCheckListner {
            void onChecked(GroupListLoader.InstanceData instanceData);
        }
    }

    public static class DoneGroupAdapter extends RecyclerView.Adapter<DoneGroupAdapter.Holder> {
        private final static Comparator<Group> sComparator = new Comparator<Group>() {
            @Override
            public int compare(Group lhs, Group rhs) {
                return lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
            }
        };

        private final ArrayList<Group> mGroups = new ArrayList<>();

        private final int mDataId;
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;

        private final Context mContext;

        private final OnUncheckListner mOnUncheckListener;

        public DoneGroupAdapter(int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, ArrayList<GroupListLoader.InstanceData> instanceDatas, Context context, OnUncheckListner onUncheckListner) {
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(context != null);
            Assert.assertTrue(onUncheckListner != null);

            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mContext = context;
            mOnUncheckListener = onUncheckListner;

            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                Assert.assertTrue(instanceData.Done != null);

                Group group = new Group(mCustomTimeDatas, instanceData.Done.toTimeStamp());
                group.addInstanceData(instanceData);
                mGroups.add(group);
            }

            Collections.sort(mGroups, sComparator);
        }

        private Group getGroup(int position) {
            return mGroups.get(position);
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_show_group, parent, false);

            TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
            TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
            ImageView groupRowImg = (ImageView) groupRow.findViewById(R.id.group_row_img);
            CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

            return new Holder(groupRow, groupRowName, groupRowDetails, groupRowImg, groupCheckBox);
        }

        @Override
        public void onBindViewHolder(final Holder holder, int position) {
            Group group = getGroup(position);
            Assert.assertTrue(group.singleInstance());

            GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
            Assert.assertTrue(instanceData != null);

            holder.mGroupRowName.setText(group.getNameText(mContext));

            holder.mGroupRowDetails.setText(group.getDetailsText());

            if (!instanceData.HasChildren)
                holder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_24dp));
            else
                holder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_list_black_24dp));

            holder.mGroupRowCheckBox.setVisibility(View.VISIBLE);
            holder.mGroupRowCheckBox.setChecked(true);
            holder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    holder.onCheckBoxClick();
                }
            });

            holder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
                @Override
                public void onClick(View view) {
                    holder.onRowClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mGroups.size();
        }

        public void add(GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(instanceData != null);

            Group group = new Group(mCustomTimeDatas, instanceData.Done.toTimeStamp());
            group.addInstanceData(instanceData);
            mGroups.add(group);

            notifyItemInserted(mGroups.size() - 1);
        }

        public class Holder extends RecyclerView.ViewHolder {
            public final TableLayout mGroupRow;
            public final TextView mGroupRowName;
            public final TextView mGroupRowDetails;
            public final ImageView mGroupRowImg;
            public final CheckBox mGroupRowCheckBox;

            public Holder(TableLayout groupRow, TextView groupRowName, TextView groupRowDetails, ImageView groupRowImg, CheckBox groupRowCheckBox) {
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

            public void onCheckBoxClick() {
                int position = getAdapterPosition();
                Group group = getGroup(position);
                Assert.assertTrue(group != null);
                Assert.assertTrue(group.singleInstance());

                GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                Assert.assertTrue(instanceData != null);

                instanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, instanceData.InstanceKey, false);
                Assert.assertTrue(instanceData.Done == null);

                TickService.startService(mContext);

                mGroups.remove(group);

                notifyItemRemoved(position);

                mOnUncheckListener.onUnchecked(instanceData);
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

                GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                return ShowInstanceActivity.getIntent(context, instanceData.InstanceKey);
            }
        }

        public interface OnUncheckListner {
            void onUnchecked(GroupListLoader.InstanceData instanceData);
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