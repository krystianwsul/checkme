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
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.TreeMap;

public class GroupListFragment extends Fragment implements LoaderManager.LoaderCallbacks<GroupListLoader.Data> {
    private final static String EXPANDED_KEY = "expanded";

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

        mGroupListDone = (RecyclerView) view.findViewById(R.id.group_list_done);
        Assert.assertTrue(mGroupListDone != null);

        mGroupListDone.setLayoutManager(new LinearLayoutManager(getContext()));

        mGroupListNotDone = (RecyclerView) view.findViewById(R.id.group_list_not_done);
        Assert.assertTrue(mGroupListNotDone != null);

        mGroupListNotDone.setLayoutManager(new LinearLayoutManager(getContext()));

        if (mExpanded)
            mGroupListDone.setVisibility(View.VISIBLE);
        else
            mGroupListDone.setVisibility(View.GONE);

        getLoaderManager().initLoader(0, null, this);
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

        mGroupListNotDone.setAdapter(new NotDoneGroupAdapter(getActivity(), data.DataId, data.CustomTimeDatas, notDoneInstances, new OnCheckListner() {
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

    public interface OnCheckListner {
        void onChecked(GroupListLoader.InstanceData instanceData);
    }

    public class NotDoneGroupAdapter extends RecyclerView.Adapter<NotDoneGroupAdapter.AbstractHolder> {
        private static final int TYPE_GROUP = 0;
        private static final int TYPE_DIVIDER = 1;

        private final Context mContext;

        private final int mDataId;
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;

        private final OnCheckListner mOnCheckListener;

        private final NotDoneGroupContainer mNotDoneGroupContainer;

        public NotDoneGroupAdapter(Context context, int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, Collection<GroupListLoader.InstanceData> instanceDatas, OnCheckListner onCheckListner) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(onCheckListner != null);

            mContext = context;
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mOnCheckListener = onCheckListner;

            mNotDoneGroupContainer = new NotDoneGroupContainer(instanceDatas);
        }

        @Override
        public int getItemViewType(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position <= mNotDoneGroupContainer.size());

            if (position < mNotDoneGroupContainer.size()) {
                return TYPE_GROUP;
            } else {
                Assert.assertTrue(position == mNotDoneGroupContainer.size());
                return TYPE_DIVIDER;
            }
        }

        @Override
        public AbstractHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_GROUP) {
                TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list, parent, false);

                TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
                TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
                ImageView groupRowImg = (ImageView) groupRow.findViewById(R.id.group_row_img);
                CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

                return new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowImg, groupCheckBox);
            } else {
                Assert.assertTrue(viewType == TYPE_DIVIDER);

                LinearLayout rowGroupListDivider = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list_divider, parent, false);

                ImageView groupListDividerImage = (ImageView) rowGroupListDivider.findViewById(R.id.group_list_divider_image);
                Assert.assertTrue(groupListDividerImage != null);

                return new DividerHolder(rowGroupListDivider, groupListDividerImage);
            }
        }

        @Override
        public void onBindViewHolder(AbstractHolder abstractHolder, int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position <= mNotDoneGroupContainer.size());

            if (position < mNotDoneGroupContainer.size()) {
                Group group = mNotDoneGroupContainer.get(position);
                Assert.assertTrue(group != null);

                final GroupHolder groupHolder = (GroupHolder) abstractHolder;

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
                            groupHolder.onCheckBoxClick();
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
            } else {
                Assert.assertTrue(position == mNotDoneGroupContainer.size());

                final DividerHolder dividerHolder = (DividerHolder) abstractHolder;

                if (mExpanded)
                    dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_less_black_24dp);
                else
                    dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_more_black_24dp);

                dividerHolder.RowGroupListDivider.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        dividerHolder.onClick();
                    }
                });
            }
        }

        @Override
        public int getItemCount() {
            return mNotDoneGroupContainer.size() + 1;
        }

        public void add(GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(instanceData != null);
            Assert.assertTrue(instanceData.Done == null);

            mNotDoneGroupContainer.add(instanceData);
        }

        public void remove(Group group) {
            Assert.assertTrue(group != null);
            mNotDoneGroupContainer.remove(group);
        }

        public abstract class AbstractHolder extends RecyclerView.ViewHolder {
            public AbstractHolder(View view) {
                super(view);
            }
        }

        public class GroupHolder extends AbstractHolder {
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

            public void onCheckBoxClick() {
                int position = getAdapterPosition();
                Group group = mNotDoneGroupContainer.get(position);
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
                Group group = mNotDoneGroupContainer.get(getAdapterPosition());
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

        public class DividerHolder extends AbstractHolder {
            public final LinearLayout RowGroupListDivider;
            public final ImageView GroupListDividerImage;

            DividerHolder(LinearLayout rowGroupListDivider, ImageView groupListDividerImage) {
                super(rowGroupListDivider);

                Assert.assertTrue(rowGroupListDivider != null);
                Assert.assertTrue(groupListDividerImage != null);

                RowGroupListDivider = rowGroupListDivider;
                GroupListDividerImage = groupListDividerImage;
            }

            public void onClick() {
                mExpanded = !mExpanded;

                if (mExpanded) {
                    mGroupListDone.setVisibility(View.VISIBLE);
                    GroupListDividerImage.setImageResource(R.drawable.ic_expand_less_black_24dp);
                } else {
                    mGroupListDone.setVisibility(View.GONE);
                    GroupListDividerImage.setImageResource(R.drawable.ic_expand_more_black_24dp);
                }
            }
        }

        private class NotDoneGroupContainer {
            private ArrayList<Group> mGroupArray = new ArrayList<>();
            private final TreeMap<TimeStamp, Group> mGroupTree = new TreeMap<>();

            public NotDoneGroupContainer(Collection<GroupListLoader.InstanceData> instanceDatas) {
                Assert.assertTrue(instanceDatas != null);

                for (GroupListLoader.InstanceData instanceData : instanceDatas)
                    addInstanceHelper(instanceData);
                makeArray();
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

            public Group get(int index) {
                Assert.assertTrue(index >= 0);
                Assert.assertTrue(index < mGroupArray.size());

                return mGroupArray.get(index);
            }

            public int size() {
                return mGroupArray.size();
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
                Assert.assertTrue(group != null);
                Assert.assertTrue(mGroupArray.contains(group));

                mGroupTree.remove(group.getTimeStamp());
                mGroupArray.remove(group);
            }
        }
    }

    public static class DoneGroupAdapter extends RecyclerView.Adapter<DoneGroupAdapter.AbstractHolder> {
        private final int mDataId;
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;

        private final Context mContext;

        private final OnUncheckListner mOnUncheckListener;

        private final DoneGroupContainer mDoneGroupContainer;

        public DoneGroupAdapter(int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, ArrayList<GroupListLoader.InstanceData> instanceDatas, Context context, OnUncheckListner onUncheckListner) {
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(context != null);
            Assert.assertTrue(onUncheckListner != null);

            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mContext = context;
            mOnUncheckListener = onUncheckListner;

            mDoneGroupContainer = new DoneGroupContainer(instanceDatas);
        }

        private Group getGroup(int position) {
            return mDoneGroupContainer.get(position);
        }

        @Override
        public AbstractHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list, parent, false);

            TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
            TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
            ImageView groupRowImg = (ImageView) groupRow.findViewById(R.id.group_row_img);
            CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

            return new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowImg, groupCheckBox);
        }

        @Override
        public void onBindViewHolder(AbstractHolder abstractHolder, int position) {
            Group group = getGroup(position);
            Assert.assertTrue(group.singleInstance());

            GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
            Assert.assertTrue(instanceData != null);

            final GroupHolder groupHolder = (GroupHolder) abstractHolder;

            groupHolder.mGroupRowName.setText(group.getNameText(mContext));

            groupHolder.mGroupRowDetails.setText(group.getDetailsText());

            if (!instanceData.HasChildren)
                groupHolder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_24dp));
            else
                groupHolder.mGroupRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_list_black_24dp));

            groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);
            groupHolder.mGroupRowCheckBox.setChecked(true);
            groupHolder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    groupHolder.onCheckBoxClick();
                }
            });

            groupHolder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
                @Override
                public void onClick(View view) {
                    groupHolder.onRowClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDoneGroupContainer.size();
        }

        public void add(GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(instanceData != null);
            mDoneGroupContainer.add(instanceData);
        }

        public abstract class AbstractHolder extends RecyclerView.ViewHolder {
            public AbstractHolder(View view) {
                super(view);
            }
        }

        public class GroupHolder extends AbstractHolder {
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

                mDoneGroupContainer.remove(group);

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

        private class DoneGroupContainer {
            private final Comparator<Group> sComparator = new Comparator<Group>() {
                @Override
                public int compare(Group lhs, Group rhs) {
                    return lhs.getTimeStamp().compareTo(rhs.getTimeStamp());
                }
            };

            private final ArrayList<Group> mGroups = new ArrayList<>();

            public DoneGroupContainer(Collection<GroupListLoader.InstanceData> instanceDatas) {
                Assert.assertTrue(instanceDatas != null);

                for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                    Assert.assertTrue(instanceData.Done != null);

                    Group group = new Group(mCustomTimeDatas, instanceData.Done.toTimeStamp());
                    group.addInstanceData(instanceData);
                    mGroups.add(group);
                }

                Collections.sort(mGroups, sComparator);
            }

            public Group get(int index) {
                Assert.assertTrue(index >= 0);
                Assert.assertTrue(index < mGroups.size());

                return mGroups.get(index);
            }

            public int size() {
                return mGroups.size();
            }

            public void add(GroupListLoader.InstanceData instanceData) {
                Assert.assertTrue(instanceData != null);

                Group group = new Group(mCustomTimeDatas, instanceData.Done.toTimeStamp());
                group.addInstanceData(instanceData);
                mGroups.add(group);

                notifyItemInserted(mGroups.size() - 1);
            }

            public void remove(Group group) {
                Assert.assertTrue(group != null);
                Assert.assertTrue(mGroups.contains(group));

                mGroups.remove(group);
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