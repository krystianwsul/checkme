package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
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
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Map;

public class GroupListFragment extends Fragment implements LoaderManager.LoaderCallbacks<GroupListLoader.Data> {
    private final static String USE_GROUPS_KEY = "useGroups";

    private final static String EXPANSION_STATE_KEY = "expansionState";

    private RecyclerView mGroupListRecycler;
    private GroupAdapter mGroupAdapter;

    private boolean mUseGroups = false;
    private TimeStamp mTimeStamp;
    private InstanceKey mInstanceKey;
    private ArrayList<InstanceKey> mInstanceKeys;

    private ExpansionState mExpansionState;

    public static GroupListFragment getGroupInstance() {
        GroupListFragment groupListFragment = new GroupListFragment();
        Bundle args = new Bundle();
        args.putBoolean(USE_GROUPS_KEY, true);
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

        if (savedInstanceState != null && savedInstanceState.containsKey(EXPANSION_STATE_KEY))
            mExpansionState = savedInstanceState.getParcelable(EXPANSION_STATE_KEY);

        mGroupListRecycler = (RecyclerView) view.findViewById(R.id.group_list_recycler);
        Assert.assertTrue(mGroupListRecycler != null);

        mGroupListRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        Bundle args = getArguments();
        if (args != null) {
            Assert.assertTrue(args.containsKey(USE_GROUPS_KEY));
            mUseGroups = args.getBoolean(USE_GROUPS_KEY, false);
            Assert.assertTrue(mUseGroups);

            setAll();
        }
    }

    private void setAll() {
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(mUseGroups);

        getLoaderManager().initLoader(0, null, this);
    }

    public void setTimeStamp(TimeStamp timeStamp) {
        Assert.assertTrue(!mUseGroups);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(timeStamp != null);
        mTimeStamp = timeStamp;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKey(InstanceKey instanceKey) {
        Assert.assertTrue(!mUseGroups);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKey != null);
        mInstanceKey = instanceKey;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKeys(ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(!mUseGroups);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKeys != null);
        mInstanceKeys = instanceKeys;
        if (!mInstanceKeys.isEmpty()) {
            getLoaderManager().initLoader(0, null, this);
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mGroupAdapter != null)
            outState.putParcelable(EXPANSION_STATE_KEY, mGroupAdapter.getExpansionState());
    }

    @Override
    public Loader<GroupListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(getActivity(), mTimeStamp, mInstanceKey, mInstanceKeys);
    }

    @Override
    public void onLoadFinished(Loader<GroupListLoader.Data> loader, GroupListLoader.Data data) {
        if (mGroupAdapter != null)
            mExpansionState = mGroupAdapter.getExpansionState();

        mGroupAdapter = new GroupAdapter(getActivity(), data.DataId, data.CustomTimeDatas, data.InstanceDatas.values(), mExpansionState);
        mGroupListRecycler.setAdapter(mGroupAdapter);
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

        private final NodeCollection mNodeCollection;

        public GroupAdapter(Context context, int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, Collection<GroupListLoader.InstanceData> instanceDatas, ExpansionState expansionState) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);

            mContext = context;
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;

            mNodeCollection = new NodeCollection(instanceDatas, expansionState);
        }

        @Override
        public int getItemViewType(int position) {
            return mNodeCollection.getItemViewType(position);
        }

        @Override
        public AbstractHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_GROUP) {
                TableLayout groupRow = (TableLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list, parent, false);

                TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
                TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
                ImageView groupRowExpand = (ImageView) groupRow.findViewById(R.id.group_row_expand);
                CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);

                return new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowExpand, groupCheckBox);
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
            Assert.assertTrue(position < getItemCount());

            Node node = mNodeCollection.getNode(position);
            node.onBindViewHolder(abstractHolder);
        }

        @Override
        public int getItemCount() {
            return mNodeCollection.getItemCount();
        }

        public ExpansionState getExpansionState() {
            return mNodeCollection.getExpansionState();
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
            public final ImageView mGroupRowExpand;
            public final CheckBox mGroupRowCheckBox;

            public GroupHolder(TableLayout groupRow, TextView groupRowName, TextView groupRowDetails, ImageView groupRowExpand, CheckBox groupRowCheckBox) {
                super(groupRow);

                Assert.assertTrue(groupRowName != null);
                Assert.assertTrue(groupRowDetails != null);
                Assert.assertTrue(groupRowExpand != null);
                Assert.assertTrue(groupRowCheckBox != null);

                mGroupRow = groupRow;
                mGroupRowName = groupRowName;
                mGroupRowDetails = groupRowDetails;
                mGroupRowExpand = groupRowExpand;
                mGroupRowCheckBox = groupRowCheckBox;
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
        }

        class NodeCollection {
            private final NotDoneGroupCollection mNotDoneGroupCollection;
            private final DividerNode mDividerNode;

            public NodeCollection(Collection<GroupListLoader.InstanceData> instanceDatas, ExpansionState expansionState) {
                Assert.assertTrue(instanceDatas != null);

                ArrayList<GroupListLoader.InstanceData> notDoneInstances = new ArrayList<>();
                ArrayList<GroupListLoader.InstanceData> doneInstances = new ArrayList<>();
                for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                    if (instanceData.Done == null)
                        notDoneInstances.add(instanceData);
                    else
                        doneInstances.add(instanceData);
                }

                boolean doneExpanded = false;
                ArrayList<TimeStamp> expandedGroups = null;
                if (expansionState != null) {
                    doneExpanded = expansionState.DoneExpanded;
                    expandedGroups = expansionState.ExpandedGroups;
                }

                mNotDoneGroupCollection = new NotDoneGroupCollection(notDoneInstances, expandedGroups);
                mDividerNode = new DividerNode(doneInstances, doneExpanded);
            }

            public int getItemViewType(int position) {
                Node node = getNode(position);
                Assert.assertTrue(node != null);

                return node.getItemViewType();
            }

            public Node getNode(int position) {
                Assert.assertTrue(position >= 0);

                if (position < mNotDoneGroupCollection.displayedSize())
                    return mNotDoneGroupCollection.getNode(position);

                Assert.assertTrue(!mDividerNode.isEmpty());

                int newPosition = position - mNotDoneGroupCollection.displayedSize();
                Assert.assertTrue(newPosition < mDividerNode.displayedSize());
                return mDividerNode.getNode(newPosition);
            }

            public int getPosition(Node node) {
                Assert.assertTrue(node != null);

                int offset = 0;

                int position = mNotDoneGroupCollection.getPosition(node);
                if (position >= 0)
                    return position;

                offset = offset + mNotDoneGroupCollection.displayedSize();

                position = mDividerNode.getPosition(node);
                Assert.assertTrue(position >= 0);

                return offset + position;
            }

            public int getItemCount() {
                return mNotDoneGroupCollection.displayedSize() + mDividerNode.displayedSize();
            }

            public ExpansionState getExpansionState() {
                ArrayList<TimeStamp> expandedGroups = mNotDoneGroupCollection.getExpandedGroups();
                return new ExpansionState(mDividerNode.expanded(), expandedGroups);
            }

            class NotDoneGroupCollection implements NodeContainer {
                private final Comparator<NotDoneGroupNode> sComparator = new Comparator<NotDoneGroupNode>() {
                    @Override
                    public int compare(NotDoneGroupNode lhs, NotDoneGroupNode rhs) {
                        int timeStampComparison = lhs.getExactTimeStamp().compareTo(rhs.getExactTimeStamp());
                        if (timeStampComparison != 0) {
                            Assert.assertTrue(mTimeStamp == null);
                            Assert.assertTrue(mInstanceKey == null);

                            return timeStampComparison;
                        } else {
                            Assert.assertTrue(!mUseGroups);
                            Assert.assertTrue(lhs.singleInstance());
                            Assert.assertTrue(rhs.singleInstance());

                            return Integer.valueOf(lhs.getSingleInstanceData().InstanceKey.TaskId).compareTo(rhs.getSingleInstanceData().InstanceKey.TaskId);
                        }
                    }
                };

                private final ArrayList<NotDoneGroupNode> mNotDoneGroupNodes = new ArrayList<>();

                public NotDoneGroupCollection(Collection<GroupListLoader.InstanceData> instanceDatas, ArrayList<TimeStamp> expandedGroups) {
                    Assert.assertTrue(instanceDatas != null);

                    if (mUseGroups) {
                        HashMap<TimeStamp, ArrayList<GroupListLoader.InstanceData>> instanceDataHash = new HashMap<>();
                        for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                            if (!instanceDataHash.containsKey(instanceData.InstanceTimeStamp))
                                instanceDataHash.put(instanceData.InstanceTimeStamp, new ArrayList<GroupListLoader.InstanceData>());
                            instanceDataHash.get(instanceData.InstanceTimeStamp).add(instanceData);
                        }

                        for (Map.Entry<TimeStamp, ArrayList<GroupListLoader.InstanceData>> entry : instanceDataHash.entrySet()) {
                            boolean expanded = false;
                            if (entry.getValue().size() > 1 && expandedGroups != null && expandedGroups.contains(entry.getKey()))
                                expanded = true;
                            NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(entry.getValue(), expanded);
                            mNotDoneGroupNodes.add(notDoneGroupNode);
                        }
                    } else {
                        for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                            ArrayList<GroupListLoader.InstanceData> dummyInstanceDatas = new ArrayList<>();
                            dummyInstanceDatas.add(instanceData);
                            NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(dummyInstanceDatas, false);
                            mNotDoneGroupNodes.add(notDoneGroupNode);
                        }
                    }

                    sort();
                }

                @Override
                public int displayedSize() {
                    int displayedSize = 0;
                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                        displayedSize += notDoneGroupNode.displayedSize();
                    return displayedSize;
                }

                @Override
                public Node getNode(int position) {
                    Assert.assertTrue(position >= 0);
                    Assert.assertTrue(position < displayedSize());

                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes) {
                        if (position < notDoneGroupNode.displayedSize())
                            return notDoneGroupNode.getNode(position);

                        position = position - notDoneGroupNode.displayedSize();
                    }

                    throw new IndexOutOfBoundsException();
                }

                @Override
                public int getPosition(Node node) {
                    int offset = 0;
                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes) {
                        int position = notDoneGroupNode.getPosition(node);
                        if (position >= 0)
                            return offset + position;
                        offset += notDoneGroupNode.displayedSize();
                    }

                    return -1;
                }

                @Override
                public boolean expanded() {
                    return true;
                }

                private Pair<Boolean, Pair<NotDoneGroupNode, NotDoneGroupNode.NotDoneInstanceNode>> addInstanceHelper(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(instanceData.Done == null);

                    ExactTimeStamp exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();

                    ArrayList<NotDoneGroupNode> timeStampNotDoneGroupNodes = new ArrayList<>();
                    if (mUseGroups)
                        for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                            if (notDoneGroupNode.getExactTimeStamp().equals(exactTimeStamp))
                                timeStampNotDoneGroupNodes.add(notDoneGroupNode);

                    if (timeStampNotDoneGroupNodes.isEmpty()) {
                        ArrayList<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
                        instanceDatas.add(instanceData);
                        NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(instanceDatas, false);
                        NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = notDoneGroupNode.mNotDoneInstanceNodes.get(0);
                        mNotDoneGroupNodes.add(notDoneGroupNode);
                        return new Pair<>(true, new Pair<>(notDoneGroupNode, notDoneInstanceNode));
                    } else {
                        Assert.assertTrue(timeStampNotDoneGroupNodes.size() == 1);
                        NotDoneGroupNode notDoneGroupNode = timeStampNotDoneGroupNodes.get(0);
                        NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = notDoneGroupNode.addInstanceData(instanceData);
                        return new Pair<>(false, new Pair<>(notDoneGroupNode, notDoneInstanceNode));
                    }
                }

                public NotDoneGroupNode get(int index) {
                    Assert.assertTrue(index >= 0);
                    Assert.assertTrue(index < mNotDoneGroupNodes.size());

                    return mNotDoneGroupNodes.get(index);
                }

                private void sort() {
                    Collections.sort(mNotDoneGroupNodes, sComparator);
                }

                public Pair<Boolean, Pair<NotDoneGroupNode, NotDoneGroupNode.NotDoneInstanceNode>> add(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(instanceData.Done == null);

                    Pair<Boolean, Pair<NotDoneGroupNode, NotDoneGroupNode.NotDoneInstanceNode>> pair = addInstanceHelper(instanceData);
                    sort();

                    return pair;
                }

                public void remove(NotDoneGroupNode notDoneGroupNode) {
                    Assert.assertTrue(notDoneGroupNode != null);
                    Assert.assertTrue(mNotDoneGroupNodes.contains(notDoneGroupNode));

                    mNotDoneGroupNodes.remove(notDoneGroupNode);
                }

                public ArrayList<TimeStamp> getExpandedGroups() {
                    ArrayList<TimeStamp> expandedGroups = new ArrayList<>();
                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                        if (notDoneGroupNode.expanded())
                            expandedGroups.add(notDoneGroupNode.getExactTimeStamp().toTimeStamp());
                    return expandedGroups;
                }
            }

            class NotDoneGroupNode implements Node, NodeContainer {
                private final ExactTimeStamp mExactTimeStamp;

                private final ArrayList<NotDoneInstanceNode> mNotDoneInstanceNodes = new ArrayList<>();

                private boolean mNotDoneGroupNodeExpanded;

                public NotDoneGroupNode(ArrayList<GroupListLoader.InstanceData> instanceDatas, boolean expanded) {
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(!instanceDatas.isEmpty());
                    Assert.assertTrue(instanceDatas.size() > 1 || !expanded);

                    mNotDoneGroupNodeExpanded = expanded;

                    mExactTimeStamp = instanceDatas.get(0).InstanceTimeStamp.toExactTimeStamp();
                    for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                        Assert.assertTrue(mExactTimeStamp.equals(instanceData.InstanceTimeStamp.toExactTimeStamp()));
                        addInstanceData(instanceData);
                    }
                }

                @Override
                public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) abstractHolder;

                    groupHolder.mGroupRowName.setText(getNameText());

                    groupHolder.mGroupRowDetails.setText(getDetailsText(getActivity()));

                    if (singleInstance()) {
                        if (getSingleInstanceData().HasChildren) {
                            groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_list_black_24dp);
                            groupHolder.mGroupRowExpand.setOnClickListener(null);
                        } else {
                            groupHolder.mGroupRowExpand.setVisibility(View.INVISIBLE);
                            groupHolder.mGroupRowExpand.setOnClickListener(null);
                        }
                    } else {
                        groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);

                        if (mNotDoneGroupNodeExpanded)
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_expand_less_black_24dp);
                        else
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_expand_more_black_24dp);

                        groupHolder.mGroupRowExpand.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int position = NodeCollection.this.getPosition(NotDoneGroupNode.this);

                                if (mNotDoneGroupNodeExpanded) { // hiding
                                    int displayedSize = displayedSize();
                                    mNotDoneGroupNodeExpanded = !mNotDoneGroupNodeExpanded;
                                    notifyItemRangeRemoved(position + 1, displayedSize - 1);
                                } else { // showing
                                    mNotDoneGroupNodeExpanded = !mNotDoneGroupNodeExpanded;
                                    notifyItemRangeInserted(position + 1, displayedSize() - 1);
                                }

                                notifyItemChanged(position);
                            }
                        });
                    }

                    if (singleInstance()) {
                        groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);

                        groupHolder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Assert.assertTrue(singleInstance());

                                GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                                Assert.assertTrue(instanceData != null);

                                instanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, instanceData.InstanceKey, true);
                                Assert.assertTrue(instanceData.Done != null);

                                TickService.startService(mContext);

                                int oldPosition = NodeCollection.this.getPosition(NotDoneGroupNode.this);
                                mNotDoneGroupCollection.remove(NotDoneGroupNode.this);

                                notifyItemRemoved(oldPosition);

                                boolean wasEmpty = mDividerNode.isEmpty();

                                DoneInstanceNode doneInstanceNode = mDividerNode.add(instanceData);

                                if (wasEmpty) {
                                    if (mDividerNode.expanded()) {
                                        int newPosition = NodeCollection.this.getPosition(doneInstanceNode);
                                        notifyItemRangeInserted(newPosition - 1, 2);
                                    } else {
                                        int dividerPosition = NodeCollection.this.getPosition(mDividerNode);
                                        notifyItemInserted(dividerPosition);
                                    }
                                } else {
                                    if (mDividerNode.expanded()) {
                                        int newPosition = NodeCollection.this.getPosition(doneInstanceNode);
                                        notifyItemInserted(newPosition);
                                    }
                                }
                            }
                        });

                        groupHolder.mGroupRowCheckBox.setChecked(getSingleInstanceData().Done != null);
                    } else {
                        groupHolder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
                        groupHolder.mGroupRowCheckBox.setOnClickListener(null);
                    }

                    groupHolder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            if (singleInstance()) {
                                GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                                getActivity().startActivity(ShowInstanceActivity.getIntent(getActivity(), instanceData.InstanceKey));
                            } else {
                                getActivity().startActivity(ShowGroupActivity.getIntent(mExactTimeStamp, getActivity()));
                            }
                        }
                    });
                }

                @Override
                public int getItemViewType() {
                    return TYPE_GROUP;
                }

                @Override
                public int displayedSize() {
                    if (mNotDoneGroupNodeExpanded) {
                        return 1 + mNotDoneInstanceNodes.size();
                    } else {
                        return 1;
                    }
                }

                @Override
                public Node getNode(int position) {
                    Assert.assertTrue(position >= 0);
                    Assert.assertTrue(position < displayedSize());

                    if (position == 0)
                        return this;

                    Assert.assertTrue(mNotDoneGroupNodeExpanded);

                    Node node = mNotDoneInstanceNodes.get(position - 1);
                    Assert.assertTrue(node != null);

                    return node;
                }

                @Override
                public int getPosition(Node node) {
                    if (node == this)
                        return 0;

                    if (!(node instanceof  NotDoneInstanceNode))
                        return -1;

                    NotDoneInstanceNode notDoneInstanceNode = (NotDoneInstanceNode) node;
                    if (mNotDoneInstanceNodes.contains(notDoneInstanceNode))
                        return mNotDoneInstanceNodes.indexOf(notDoneInstanceNode);

                    return -1;
                }

                @Override
                public boolean expanded() {
                    return mNotDoneGroupNodeExpanded;
                }

                public NotDoneInstanceNode addInstanceData(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData != null);

                    NotDoneInstanceNode notDoneInstanceNode = new NotDoneInstanceNode(instanceData);
                    mNotDoneInstanceNodes.add(notDoneInstanceNode);
                    return notDoneInstanceNode;
                }

                public String getNameText() {
                    Assert.assertTrue(!mNotDoneInstanceNodes.isEmpty());
                    if (singleInstance()) {
                        return getSingleInstanceData().Name;
                    } else {
                        ArrayList<String> names = new ArrayList<>();
                        for (NotDoneInstanceNode notDoneInstanceNode : mNotDoneInstanceNodes)
                            names.add(notDoneInstanceNode.mInstanceData.Name);
                        return TextUtils.join(", ", names);
                    }
                }

                public String getDetailsText(Context context) {
                    Assert.assertTrue(!mNotDoneInstanceNodes.isEmpty());
                    if (singleInstance()) {
                        return getSingleInstanceData().DisplayText;
                    } else {
                        Date date = mExactTimeStamp.getDate();
                        HourMinute hourMinute = mExactTimeStamp.toTimeStamp().getHourMinute();

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

                public ExactTimeStamp getExactTimeStamp() {
                    return mExactTimeStamp;
                }

                public boolean singleInstance() {
                    Assert.assertTrue(!mNotDoneInstanceNodes.isEmpty());
                    return (mNotDoneInstanceNodes.size() == 1);
                }

                public GroupListLoader.InstanceData getSingleInstanceData() {
                    Assert.assertTrue(mNotDoneInstanceNodes.size() == 1);
                    return mNotDoneInstanceNodes.get(0).mInstanceData;
                }

                class NotDoneInstanceNode implements Node {
                    public final GroupListLoader.InstanceData mInstanceData;

                    public NotDoneInstanceNode(GroupListLoader.InstanceData instanceData) {
                        Assert.assertTrue(instanceData != null);
                        mInstanceData = instanceData;
                    }
                    @Override
                    public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                        final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) abstractHolder;

                        groupHolder.mGroupRowName.setText(mInstanceData.Name);

                        groupHolder.mGroupRowDetails.setText(mInstanceData.DisplayText);

                        if (mInstanceData.HasChildren) {
                            groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_list_black_24dp);
                            groupHolder.mGroupRowExpand.setOnClickListener(null);
                        } else {
                            groupHolder.mGroupRowExpand.setVisibility(View.INVISIBLE);
                            groupHolder.mGroupRowExpand.setOnClickListener(null);
                        }

                        groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);
                        groupHolder.mGroupRowCheckBox.setChecked(false);
                        groupHolder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                Assert.assertTrue(mNotDoneGroupNodeExpanded);

                                mInstanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, mInstanceData.InstanceKey, true);
                                Assert.assertTrue(mInstanceData.Done != null);

                                TickService.startService(mContext);

                                Assert.assertTrue(mNotDoneInstanceNodes.size() >= 2);
                                int groupPosition = NodeCollection.this.getPosition(NotDoneGroupNode.this);

                                if (mNotDoneInstanceNodes.size() == 2) {
                                    mNotDoneInstanceNodes.remove(NotDoneInstanceNode.this);

                                    mNotDoneGroupNodeExpanded = false;

                                    notifyItemChanged(groupPosition);
                                    notifyItemRangeRemoved(groupPosition + 1, 2);
                                } else {
                                    Assert.assertTrue(mNotDoneInstanceNodes.size() > 2);

                                    int oldInstancePosition = NodeCollection.this.getPosition(NotDoneInstanceNode.this);

                                    mNotDoneInstanceNodes.remove(NotDoneInstanceNode.this);

                                    notifyItemChanged(groupPosition);
                                    notifyItemRemoved(oldInstancePosition);
                                }


                                DoneInstanceNode doneInstanceNode = mDividerNode.add(mInstanceData);

                                if (mDividerNode.expanded()) {
                                    int newInstancePosition = NodeCollection.this.getPosition(doneInstanceNode);
                                    notifyItemInserted(newInstancePosition);
                                }
                            }
                        });

                        groupHolder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                getActivity().startActivity(ShowInstanceActivity.getIntent(getActivity(), mInstanceData.InstanceKey));
                            }
                        });
                    }

                    @Override
                    public int getItemViewType() {
                        return TYPE_GROUP;
                    }
                }
            }

            class DividerNode implements Node, NodeContainer {
                private boolean mDoneExpanded;

                private final Comparator<DoneInstanceNode> sComparator = new Comparator<DoneInstanceNode>() {
                    @Override
                    public int compare(DoneInstanceNode lhs, DoneInstanceNode rhs) {
                        return -lhs.mInstanceData.Done.compareTo(rhs.mInstanceData.Done); // negated
                    }
                };

                private final ArrayList<NodeCollection.DoneInstanceNode> mDoneInstanceNodes = new ArrayList<>();

                public DividerNode(ArrayList<GroupListLoader.InstanceData> instanceDatas, boolean doneExpanded) {
                    Assert.assertTrue(instanceDatas != null);

                    mDoneExpanded = doneExpanded;

                    for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                        Assert.assertTrue(instanceData.Done != null);
                        mDoneInstanceNodes.add(new DoneInstanceNode(instanceData));
                    }

                    Collections.sort(mDoneInstanceNodes, sComparator);
                }

                @Override
                public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.DividerHolder dividerHolder = (GroupAdapter.DividerHolder) abstractHolder;

                    if (mDoneExpanded)
                        dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_less_black_24dp);
                    else
                        dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_more_black_24dp);

                    dividerHolder.RowGroupListDivider.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Assert.assertTrue(!mDoneInstanceNodes.isEmpty());

                            int position = NodeCollection.this.getPosition(DividerNode.this);

                            if (mDoneExpanded) { // hiding
                                int displayedSize = DividerNode.this.displayedSize();
                                mDoneExpanded = false;
                                notifyItemRangeRemoved(position + 1, displayedSize - 1);
                            } else { // showing
                                mDoneExpanded = true;
                                notifyItemRangeRemoved(position + 1, DividerNode.this.displayedSize() - 1);
                            }

                            notifyItemChanged(position);
                        }
                    });
                }

                @Override
                public int getItemViewType() {
                    return TYPE_DIVIDER;
                }

                @Override
                public int displayedSize() {
                    if (mDoneInstanceNodes.isEmpty()) {
                        return 0;
                    } else {
                        if (mDoneExpanded) {
                            return 1 + mDoneInstanceNodes.size();
                        } else {
                            return 1;
                        }
                    }
                }

                @Override
                public Node getNode(int position) {
                    Assert.assertTrue(position >= 0);
                    Assert.assertTrue(!mDoneInstanceNodes.isEmpty());

                    if (position == 0) {
                        return this;
                    } else {
                        Assert.assertTrue(mDoneExpanded);
                        Assert.assertTrue(position <= mDoneInstanceNodes.size());

                        Node node = mDoneInstanceNodes.get(position - 1);
                        Assert.assertTrue(node != null);

                        return node;
                    }
                }

                @Override
                public int getPosition(Node node) {
                    if (node == this)
                        return 0;

                    if (!(node instanceof DoneInstanceNode))
                        return -1;

                    DoneInstanceNode doneInstanceNode = (DoneInstanceNode) node;
                    if (mDoneInstanceNodes.contains(doneInstanceNode)) {
                        Assert.assertTrue(mDoneExpanded);
                        return mDoneInstanceNodes.indexOf(doneInstanceNode) + 1;
                    }

                    return -1;
                }

                @Override
                public boolean expanded() {
                    return mDoneExpanded;
                }

                public DoneInstanceNode get(int index) {
                    Assert.assertTrue(index >= 0);
                    Assert.assertTrue(index < mDoneInstanceNodes.size());

                    return mDoneInstanceNodes.get(index);
                }

                public boolean isEmpty() {
                    return mDoneInstanceNodes.isEmpty();
                }

                public DoneInstanceNode add(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData != null);

                    DoneInstanceNode doneInstanceNode = new DoneInstanceNode(instanceData);
                    mDoneInstanceNodes.add(doneInstanceNode);

                    Collections.sort(mDoneInstanceNodes, sComparator);

                    return doneInstanceNode;
                }

                public void remove(DoneInstanceNode doneInstanceNode) {
                    Assert.assertTrue(doneInstanceNode != null);
                    Assert.assertTrue(mDoneInstanceNodes.contains(doneInstanceNode));

                    mDoneInstanceNodes.remove(doneInstanceNode);
                }
            }

            class DoneInstanceNode implements Node {
                private final GroupListLoader.InstanceData mInstanceData;

                private boolean mExpanded = false;

                public DoneInstanceNode(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData != null);
                    mInstanceData = instanceData;
                }

                @Override
                public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) abstractHolder;

                    groupHolder.mGroupRowName.setText(mInstanceData.Name);

                    groupHolder.mGroupRowDetails.setText(mInstanceData.DisplayText);

                    if (mInstanceData.HasChildren) {
                        groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);

                        if (mExpanded)
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_expand_less_black_24dp);
                        else
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_expand_more_black_24dp);

                        groupHolder.mGroupRowExpand.setOnClickListener(new View.OnClickListener() {
                            @Override
                            public void onClick(View v) {
                                int position = NodeCollection.this.getPosition(DoneInstanceNode.this);
                                mExpanded = !mExpanded;
                                notifyItemChanged(position);
                            }
                        });

                    } else {
                        groupHolder.mGroupRowExpand.setVisibility(View.INVISIBLE);

                        groupHolder.mGroupRowExpand.setOnClickListener(null);
                    }

                    groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);
                    groupHolder.mGroupRowCheckBox.setChecked(true);
                    groupHolder.mGroupRowCheckBox.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            Assert.assertTrue(mDividerNode.expanded());

                            mInstanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, mInstanceData.InstanceKey, false);
                            Assert.assertTrue(mInstanceData.Done == null);

                            TickService.startService(mContext);

                            int oldPosition = NodeCollection.this.getPosition(DoneInstanceNode.this);

                            mDividerNode.remove(DoneInstanceNode.this);

                            if (mDividerNode.isEmpty())
                                notifyItemRangeRemoved(oldPosition - 1, 2);
                            else
                                notifyItemRemoved(oldPosition);

                            Pair<Boolean, Pair<NotDoneGroupNode, NotDoneGroupNode.NotDoneInstanceNode>> pair = mNotDoneGroupCollection.add(mInstanceData);
                            boolean newNotDoneGroupNode = pair.first;
                            NotDoneGroupNode notDoneGroupNode = pair.second.first;
                            NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = pair.second.second;

                            if (newNotDoneGroupNode) {
                                int newGroupPosition = NodeCollection.this.getPosition(notDoneGroupNode);
                                notifyItemInserted(newGroupPosition);
                            } else {
                                if (notDoneGroupNode.expanded()) {
                                    int newGroupPosition = NodeCollection.this.getPosition(notDoneGroupNode);
                                    int newInstancePosition = NodeCollection.this.getPosition(notDoneInstanceNode);

                                    notifyItemChanged(newGroupPosition);
                                    notifyItemInserted(newInstancePosition);
                                } else {
                                    int newGroupPosition = NodeCollection.this.getPosition(notDoneGroupNode);
                                    notifyItemChanged(newGroupPosition);
                                }
                            }
                        }
                    });

                    groupHolder.mGroupRow.setOnClickListener(new TableLayout.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            getActivity().startActivity(ShowInstanceActivity.getIntent(getActivity(), mInstanceData.InstanceKey));
                        }
                    });
                }

                @Override
                public int getItemViewType() {
                    return TYPE_GROUP;
                }
            }
        }
    }

    interface Node {
        void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder);
        int getItemViewType();
    }

    interface NodeContainer {
        int displayedSize();
        Node getNode(int position);
        int getPosition(Node node);
        boolean expanded();
    }

    static class ExpansionState implements Parcelable {
        public final boolean DoneExpanded;
        public final ArrayList<TimeStamp> ExpandedGroups;

        public ExpansionState(boolean doneExpanded, ArrayList<TimeStamp> expandedGroups) {
            Assert.assertTrue(expandedGroups != null);

            DoneExpanded = doneExpanded;
            ExpandedGroups = expandedGroups;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(DoneExpanded ? 1 : 0);
            dest.writeTypedList(ExpandedGroups);
        }

        public static Parcelable.Creator<ExpansionState> CREATOR = new Creator<ExpansionState>() {
            @Override
            public ExpansionState createFromParcel(Parcel source) {
                boolean doneExpanded = (source.readInt() == 1);

                ArrayList<TimeStamp> expandedGroups = new ArrayList<>();
                source.readTypedList(expandedGroups, TimeStamp.CREATOR);

                return new ExpansionState(doneExpanded, expandedGroups);
            }

            @Override
            public ExpansionState[] newArray(int size) {
                return new ExpansionState[size];
            }
        };
    }
}