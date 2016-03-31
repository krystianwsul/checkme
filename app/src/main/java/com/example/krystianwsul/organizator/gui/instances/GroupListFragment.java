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
import com.example.krystianwsul.organizator.utils.InstanceKey;
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
    private final static String GROUPS_KEY = "groups";

    private final static String EXPANDED_KEY = "expanded";

    private RecyclerView mGroupListRecycler;

    private boolean mGroups = false;
    private TimeStamp mTimeStamp;
    private InstanceKey mInstanceKey;
    private ArrayList<InstanceKey> mInstanceKeys;

    private boolean mExpanded = false;

    public static GroupListFragment getGroupInstance() {
        GroupListFragment groupListFragment = new GroupListFragment();
        Bundle args = new Bundle();
        args.putBoolean(GROUPS_KEY, true);
        groupListFragment.setArguments(args);
        return groupListFragment;
    }

    public GroupListFragment() {

    }

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

        mGroupListRecycler = (RecyclerView) view.findViewById(R.id.group_list_recycler);
        Assert.assertTrue(mGroupListRecycler != null);

        mGroupListRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        Bundle args = getArguments();
        if (args != null) {
            Assert.assertTrue(args.containsKey(GROUPS_KEY));
            mGroups = args.getBoolean(GROUPS_KEY, false);
            Assert.assertTrue(mGroups);

            setAll();
        }
    }

    public void setAll() {
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(mGroups);

        getLoaderManager().initLoader(0, null, this);
    }

    public void setTimeStamp(TimeStamp timeStamp) {
        Assert.assertTrue(!mGroups);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(timeStamp != null);
        mTimeStamp = timeStamp;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKey(InstanceKey instanceKey) {
        Assert.assertTrue(!mGroups);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKey != null);
        mInstanceKey = instanceKey;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKeys(ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(!mGroups);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKeys != null);
        mInstanceKeys = instanceKeys;
        if (!mInstanceKeys.isEmpty())
            getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(EXPANDED_KEY, mExpanded);
    }

    @Override
    public Loader<GroupListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(getActivity(), mTimeStamp, mInstanceKey, mInstanceKeys);
    }

    @Override
    public void onLoadFinished(Loader<GroupListLoader.Data> loader, GroupListLoader.Data data) {
        mGroupListRecycler.setAdapter(new GroupAdapter(getActivity(), data.DataId, data.CustomTimeDatas, data.InstanceDatas.values()));
    }

    @Override
    public void onLoaderReset(Loader<GroupListLoader.Data> loader) {
    }

    public class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.AbstractHolder> {
        private static final int TYPE_GROUP = 0;
        private static final int TYPE_DIVIDER = 1;

        private final Context mContext;

        private final int mDataId;
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;

        private final NotDoneGroupContainer mNotDoneGroupContainer;
        private final DoneGroupContainer mDoneGroupContainer;

        public GroupAdapter(Context context, int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, Collection<GroupListLoader.InstanceData> instanceDatas) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);

            mContext = context;
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;

            ArrayList<GroupListLoader.InstanceData> notDoneInstances = new ArrayList<>();
            ArrayList<GroupListLoader.InstanceData> doneInstances = new ArrayList<>();
            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                if (instanceData.Done == null)
                    notDoneInstances.add(instanceData);
                else
                    doneInstances.add(instanceData);
            }

            mNotDoneGroupContainer = new NotDoneGroupContainer(notDoneInstances);
            mDoneGroupContainer = new DoneGroupContainer(doneInstances);
        }

        private Group getGroup(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position != mNotDoneGroupContainer.size());
            Assert.assertTrue(position <= mNotDoneGroupContainer.size() + mDoneGroupContainer.size());

            if (position < mNotDoneGroupContainer.size()) {
                return mNotDoneGroupContainer.get(position);
            } else {
                Assert.assertTrue(position != mNotDoneGroupContainer.size());
                Assert.assertTrue(position <= mNotDoneGroupContainer.size() + mDoneGroupContainer.size());

                return mDoneGroupContainer.get(position - mNotDoneGroupContainer.size() - 1);
            }
        }

        @Override
        public int getItemViewType(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position <= mNotDoneGroupContainer.size() + mDoneGroupContainer.size());

            if (position == mNotDoneGroupContainer.size())
                return TYPE_DIVIDER;
            else
                return TYPE_GROUP;
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
            Assert.assertTrue(position <= mNotDoneGroupContainer.size() + mDoneGroupContainer.size());

            if (position < mNotDoneGroupContainer.size()) {
                Group group = getGroup(position);
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
            } else if (position == mNotDoneGroupContainer.size()) {
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
            } else {
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
        }

        @Override
        public int getItemCount() {
            if (mDoneGroupContainer.isEmpty()) {
                return mNotDoneGroupContainer.size();
            } else {
                if (mExpanded)
                    return mNotDoneGroupContainer.size() + 1 + mDoneGroupContainer.size();
                else
                    return mNotDoneGroupContainer.size() + 1;
            }
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
                int oldPosition = getAdapterPosition();
                Assert.assertTrue(oldPosition >= 0);
                Assert.assertTrue(oldPosition != mNotDoneGroupContainer.size());
                Assert.assertTrue(oldPosition <= mNotDoneGroupContainer.size() + mDoneGroupContainer.size());

                if (oldPosition < mNotDoneGroupContainer.size()) {
                    Group group = getGroup(oldPosition);
                    Assert.assertTrue(group != null);
                    Assert.assertTrue(group.singleInstance());

                    GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    instanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, instanceData.InstanceKey, true);
                    Assert.assertTrue(instanceData.Done != null);

                    TickService.startService(mContext);

                    mNotDoneGroupContainer.remove(group);

                    notifyItemRemoved(oldPosition);

                    boolean wasEmpty = mDoneGroupContainer.isEmpty();

                    int newIndex = mDoneGroupContainer.add(instanceData);
                    int newPosition = mNotDoneGroupContainer.size() + 1 + newIndex;

                    if (wasEmpty) {
                        Assert.assertTrue(newPosition == mNotDoneGroupContainer.size() + 1);
                        if (mExpanded) {
                            notifyItemRangeInserted(mNotDoneGroupContainer.size(), 2);
                        } else {
                            notifyItemInserted(mNotDoneGroupContainer.size());
                        }
                    } else {
                        if (mExpanded)
                            notifyItemInserted(newPosition);
                    }
                } else {
                    Assert.assertTrue(mExpanded);

                    Group group = getGroup(oldPosition);
                    Assert.assertTrue(group != null);
                    Assert.assertTrue(group.singleInstance());

                    GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    instanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, instanceData.InstanceKey, false);
                    Assert.assertTrue(instanceData.Done == null);

                    TickService.startService(mContext);

                    mDoneGroupContainer.remove(group);

                    if (mDoneGroupContainer.isEmpty()) {
                        Assert.assertTrue(oldPosition == mNotDoneGroupContainer.size() + 1);
                        notifyItemRangeRemoved(mNotDoneGroupContainer.size(), 2);
                    } else {
                        notifyItemRemoved(oldPosition);
                    }

                    Pair<Integer, Boolean> pair = mNotDoneGroupContainer.add(instanceData);
                    int newIndex = pair.first;
                    boolean inserted = pair.second;

                    if (inserted)
                        notifyItemInserted(newIndex);
                    else
                        notifyItemChanged(newIndex);
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

                int position = getAdapterPosition();
                Assert.assertTrue(position >= 0);
                Assert.assertTrue(position != mNotDoneGroupContainer.size());
                Assert.assertTrue(position <= mNotDoneGroupContainer.size() + mDoneGroupContainer.size());

                if (position < mNotDoneGroupContainer.size()) {
                    if (group.singleInstance()) {
                        GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                        return ShowInstanceActivity.getIntent(context, instanceData.InstanceKey);
                    } else {
                        return ShowGroupActivity.getIntent(group, context);
                    }
                } else {
                    GroupListLoader.InstanceData instanceData = group.getSingleInstanceData();
                    return ShowInstanceActivity.getIntent(context, instanceData.InstanceKey);
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
                    notifyItemRangeInserted(mNotDoneGroupContainer.size() + 1, mNotDoneGroupContainer.size() + mDoneGroupContainer.size());
                    GroupListDividerImage.setImageResource(R.drawable.ic_expand_less_black_24dp);
                } else {
                    notifyItemRangeRemoved(mNotDoneGroupContainer.size() + 1, mNotDoneGroupContainer.size() + mDoneGroupContainer.size());
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

            public Pair<Integer, Boolean> add(GroupListLoader.InstanceData instanceData) {
                Assert.assertTrue(instanceData != null);
                Assert.assertTrue(instanceData.Done == null);

                Pair<Group, Boolean> pair = addInstanceHelper(instanceData);
                makeArray();

                int postition = mGroupArray.indexOf(pair.first);

                return new Pair<>(postition, pair.second);
            }

            public void remove(Group group) {
                Assert.assertTrue(group != null);
                Assert.assertTrue(mGroupArray.contains(group));

                mGroupTree.remove(group.getTimeStamp());
                mGroupArray.remove(group);
            }
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

            public boolean isEmpty() {
                return mGroups.isEmpty();
            }

            public int add(GroupListLoader.InstanceData instanceData) {
                Assert.assertTrue(instanceData != null);

                Group group = new Group(mCustomTimeDatas, instanceData.Done.toTimeStamp());
                group.addInstanceData(instanceData);
                mGroups.add(group);

                Collections.sort(mGroups, sComparator);

                return mGroups.indexOf(group);
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