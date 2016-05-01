package com.example.krystianwsul.organizator.gui.instances;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v4.util.Pair;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TableLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.gui.SelectionCallback;
import com.example.krystianwsul.organizator.gui.tasks.CreateChildTaskActivity;
import com.example.krystianwsul.organizator.gui.tasks.CreateRootTaskActivity;
import com.example.krystianwsul.organizator.gui.tasks.ShowTaskActivity;
import com.example.krystianwsul.organizator.loaders.GroupListLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.Date;
import com.example.krystianwsul.organizator.utils.time.DayOfWeek;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;
import com.example.krystianwsul.organizator.utils.time.HourMinute;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class GroupListFragment extends Fragment implements LoaderManager.LoaderCallbacks<GroupListLoader.Data> {
    private final static String DAY_KEY = "day";

    private final static String EXPANSION_STATE_KEY = "expansionState";
    private final static String SELECTED_NODES_KEY = "selectedNodes";

    private RecyclerView mGroupListRecycler;
    private GroupAdapter mGroupAdapter;
    private FloatingActionButton mFloatingActionButton;

    private Integer mDay;
    private TimeStamp mTimeStamp;
    private InstanceKey mInstanceKey;
    private ArrayList<InstanceKey> mInstanceKeys;

    private ExpansionState mExpansionState;
    private ArrayList<InstanceKey> mSelectedNodes;

    private boolean mFirst = true;

    private SelectionCallback mSelectionCallback = new SelectionCallback() {
        private Integer mOldVisibility = null;

        @Override
        protected void unselect() {
            mGroupAdapter.mNodeCollection.mNotDoneGroupCollection.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            List<GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode> selected = mGroupAdapter.mNodeCollection.mNotDoneGroupCollection.getSelected();
            Assert.assertTrue(selected != null);
            Assert.assertTrue(!selected.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_group_edit_instance:
                    Assert.assertTrue(selected.size() == 1);

                    GroupListLoader.InstanceData instanceData = selected.get(0).mInstanceData;
                    Assert.assertTrue(instanceData.IsRootInstance);

                    startActivity(EditInstanceActivity.getIntent(getActivity(), instanceData.InstanceKey));
                    break;
                case R.id.action_group_show_task:
                    Assert.assertTrue(selected.size() == 1);

                    GroupListLoader.InstanceData instanceData2 = selected.get(0).mInstanceData;
                    Assert.assertTrue(instanceData2.TaskCurrent);

                    startActivity(ShowTaskActivity.getIntent(instanceData2.InstanceKey.TaskId, getActivity()));
                    break;
                case R.id.action_group_join:
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(selected)
                            .map(notDoneInstanceNode -> notDoneInstanceNode.mInstanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(taskIds.size() > 1);

                    if (mInstanceKey == null) {
                        if (mDay != null)
                            startActivity(CreateRootTaskActivity.getJoinIntent(getActivity(), taskIds, mDay));
                        else
                            startActivity(CreateRootTaskActivity.getJoinIntent(getActivity(), taskIds));
                    } else {
                        startActivity(CreateChildTaskActivity.getJoinIntent(getActivity(), mInstanceKey.TaskId, taskIds));
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            if (mGroupAdapter.mNodeCollection.mDividerNode.mDoneInstanceNodes.size() > 0) {
                if (mGroupAdapter.mNodeCollection.mDividerNode.mDoneExpanded)
                    mGroupAdapter.notifyItemRangeRemoved(mGroupAdapter.mNodeCollection.getPosition(mGroupAdapter.mNodeCollection.mDividerNode), mGroupAdapter.mNodeCollection.mDividerNode.mDoneInstanceNodes.size() + 1);
                else
                    mGroupAdapter.notifyItemRemoved(mGroupAdapter.mNodeCollection.getPosition(mGroupAdapter.mNodeCollection.mDividerNode));
            }

            int last = mGroupAdapter.mNodeCollection.mNotDoneGroupCollection.displayedSize() - 1;
            mGroupAdapter.notifyItemChanged(last);

            mActionMode.getMenuInflater().inflate(R.menu.menu_edit_groups, mActionMode.getMenu());

            mGroupAdapter.mNodeCollection.mNotDoneGroupCollection.updateCheckBoxes();

            Assert.assertTrue(mOldVisibility == null);
            mOldVisibility = mFloatingActionButton.getVisibility();
            mFloatingActionButton.setVisibility(View.GONE);

            ((GroupListListener) getActivity()).onCreateGroupActionMode(mActionMode);

            updateMenu();
        }

        @Override
        protected void onSecondAdded() {
            updateMenu();
        }

        @Override
        protected void onOtherAdded() {
            updateMenu();
        }

        @Override
        protected void onLastRemoved() {
            if (mGroupAdapter.mNodeCollection.mDividerNode.mDoneInstanceNodes.size() > 0) {
                if (mGroupAdapter.mNodeCollection.mDividerNode.mDoneExpanded)
                    mGroupAdapter.notifyItemRangeInserted(mGroupAdapter.mNodeCollection.getPosition(mGroupAdapter.mNodeCollection.mDividerNode), mGroupAdapter.mNodeCollection.mDividerNode.mDoneInstanceNodes.size() + 1);
                else
                    mGroupAdapter.notifyItemInserted(mGroupAdapter.mNodeCollection.getPosition(mGroupAdapter.mNodeCollection.mDividerNode));
            }

            int last = mGroupAdapter.mNodeCollection.mNotDoneGroupCollection.displayedSize() - 1;
            mGroupAdapter.notifyItemChanged(last);

            mGroupAdapter.mNodeCollection.mNotDoneGroupCollection.updateCheckBoxes();

            mFloatingActionButton.setVisibility(mOldVisibility);
            mOldVisibility = null;

            ((GroupListListener) getActivity()).onDestroyGroupActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {
            updateMenu();
        }

        @Override
        protected void onOtherRemoved() {
            updateMenu();
        }

        private void updateMenu() {
            Assert.assertTrue(mActionMode != null);

            Menu menu = mActionMode.getMenu();
            Assert.assertTrue(menu != null);

            List<GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode> selected = mGroupAdapter.mNodeCollection.mNotDoneGroupCollection.getSelected();
            Assert.assertTrue(selected != null);
            Assert.assertTrue(!selected.isEmpty());

            if (selected.size() == 1) {
                GroupListLoader.InstanceData instanceData = selected.get(0).mInstanceData;
                Assert.assertTrue(instanceData != null);

                Log.e("asdf", "IsRootInstance: " + instanceData.IsRootInstance);
                menu.findItem(R.id.action_group_edit_instance).setVisible(instanceData.IsRootInstance);
                menu.findItem(R.id.action_group_show_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_join).setVisible(false);
            } else {
                Assert.assertTrue(selected.size() > 1);

                menu.findItem(R.id.action_group_edit_instance).setVisible(false);
                menu.findItem(R.id.action_group_show_task).setVisible(false);

                if (Stream.of(selected).allMatch(node -> node.mInstanceData.TaskCurrent))
                    menu.findItem(R.id.action_group_join).setVisible(true);
                else
                    menu.findItem(R.id.action_group_join).setVisible(false);
            }
        }
    };

    public static GroupListFragment getGroupInstance(int day) {
        Assert.assertTrue(day >= 0);

        GroupListFragment groupListFragment = new GroupListFragment();
        Bundle args = new Bundle();
        args.putInt(DAY_KEY, day);
        groupListFragment.setArguments(args);
        return groupListFragment;
    }

    public GroupListFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Assert.assertTrue(context instanceof GroupListListener);
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

        if (savedInstanceState != null && savedInstanceState.containsKey(EXPANSION_STATE_KEY)) {
            mExpansionState = savedInstanceState.getParcelable(EXPANSION_STATE_KEY);

            if (savedInstanceState.containsKey(SELECTED_NODES_KEY)) {
                mSelectedNodes = savedInstanceState.getParcelableArrayList(SELECTED_NODES_KEY);
                Assert.assertTrue(mSelectedNodes != null);
                Assert.assertTrue(!mSelectedNodes.isEmpty());
            }
        }

        mGroupListRecycler = (RecyclerView) view.findViewById(R.id.group_list_recycler);
        Assert.assertTrue(mGroupListRecycler != null);

        mGroupListRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mFloatingActionButton = (FloatingActionButton) view.findViewById(R.id.group_list_fab);
        Assert.assertTrue(mFloatingActionButton != null);

        Bundle args = getArguments();
        if (args != null) {
            Assert.assertTrue(args.containsKey(DAY_KEY));
            int day = args.getInt(DAY_KEY);
            Assert.assertTrue(day >= 0);

            setAll(day);
        }
    }

    private void setAll(int day) {
        Assert.assertTrue(mDay == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(day >= 0);
        mDay = day;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setTimeStamp(TimeStamp timeStamp) {
        Assert.assertTrue(mDay == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(timeStamp != null);
        mTimeStamp = timeStamp;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKey(InstanceKey instanceKey) {
        Assert.assertTrue(mDay == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKey != null);
        mInstanceKey = instanceKey;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKeys(ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(mDay == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKeys != null);
        mInstanceKeys = instanceKeys;

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean useGroups() {
        return (mDay != null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mGroupAdapter != null) {
            outState.putParcelable(EXPANSION_STATE_KEY, mGroupAdapter.getExpansionState());

            if (mSelectionCallback.hasActionMode()) {
                ArrayList<InstanceKey> selected = mGroupAdapter.getSelected();
                Assert.assertTrue(!selected.isEmpty());
                outState.putParcelableArrayList(SELECTED_NODES_KEY, selected);
            }
        }
    }

    @Override
    public Loader<GroupListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(getActivity(), mTimeStamp, mInstanceKey, mInstanceKeys, mDay);
    }

    @Override
    public void onLoadFinished(Loader<GroupListLoader.Data> loader, GroupListLoader.Data data) {
        if (mGroupAdapter != null) {
            mExpansionState = mGroupAdapter.getExpansionState();

            ArrayList<InstanceKey> selected = mGroupAdapter.getSelected();
            if (selected.isEmpty()) {
                Assert.assertTrue(!mSelectionCallback.hasActionMode());
                mSelectedNodes = null;
            } else {
                Assert.assertTrue(mSelectionCallback.hasActionMode());
                mSelectedNodes = selected;
            }
        }

        boolean showFab;
        Activity activity = getActivity();
        if (mDay != null) {
            Assert.assertTrue(mTimeStamp == null);
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(data.TaskEditable == null);

            showFab = true;
            mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateRootTaskActivity.getCreateIntent(activity, mDay)));
        } else if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(data.TaskEditable == null);

            if (mTimeStamp.compareTo(TimeStamp.getNow()) > 0) {
                showFab = true;
                mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateRootTaskActivity.getCreateIntent(activity, mTimeStamp)));
            } else {
                showFab = false;
            }
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(data.TaskEditable != null);

            if (data.TaskEditable) {
                showFab = true;
                mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateChildTaskActivity.getCreateIntent(activity, mInstanceKey.TaskId)));
            } else {
                showFab = false;
            }
        } else {
            Assert.assertTrue(data.TaskEditable == null);

            showFab = false;
        }

        if (mFirst) {
            mFloatingActionButton.setVisibility(showFab ? View.VISIBLE : View.GONE);
            mFirst = false;
        }

        mGroupAdapter = GroupAdapter.getAdapter(this, data.DataId, data.CustomTimeDatas, data.InstanceDatas.values(), mExpansionState, useGroups(), showFab, mSelectedNodes);
        mGroupListRecycler.setAdapter(mGroupAdapter);

        mSelectionCallback.setSelected(mGroupAdapter.getSelected().size());
    }

    @Override
    public void onLoaderReset(Loader<GroupListLoader.Data> loader) {
    }

    public static class GroupAdapter extends RecyclerView.Adapter<GroupAdapter.AbstractHolder> {
        private static final int TYPE_GROUP = 0;
        private static final int TYPE_DIVIDER = 1;
        private static final int TYPE_FAB_PADDING = 2;

        private final WeakReference<GroupListFragment> mGroupListFragmentReference;

        private final int mDataId;
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;
        private final boolean mUseGroups;
        private final boolean mShowFab;

        private NodeCollection mNodeCollection;

        public static GroupAdapter getAdapter(GroupListFragment groupListFragment, int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, Collection<GroupListLoader.InstanceData> instanceDatas, ExpansionState expansionState, boolean useGroups, boolean showFab, ArrayList<InstanceKey> selectedNodes) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);

            GroupAdapter groupAdapter = new GroupAdapter(groupListFragment, dataId, customTimeDatas, useGroups, showFab);
            groupAdapter.setInstanceDatas(instanceDatas, expansionState, selectedNodes);
            return groupAdapter;
        }

        private GroupAdapter(GroupListFragment groupListFragment, int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, boolean useGroups, boolean showFab) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);

            mGroupListFragmentReference = new WeakReference<>(groupListFragment);
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mUseGroups = useGroups;
            mShowFab = showFab;
        }

        private void setInstanceDatas(Collection<GroupListLoader.InstanceData> instanceDatas, ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes) {
            Assert.assertTrue(instanceDatas != null);

            mNodeCollection = NodeCollection.newNodeCollection(instanceDatas, expansionState, new WeakReference<>(this), selectedNodes);
        }

        @Override
        public int getItemViewType(int position) {
            if (mShowFab && position == mNodeCollection.getItemCount())
                return TYPE_FAB_PADDING;
            else
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
                View groupRowSeparator = groupRow.findViewById(R.id.group_row_separator);

                return new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowExpand, groupCheckBox, groupRowSeparator);
            } else if (viewType == TYPE_DIVIDER) {
                RelativeLayout rowGroupListDivider = (RelativeLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list_divider, parent, false);

                ImageView groupListDividerImage = (ImageView) rowGroupListDivider.findViewById(R.id.group_list_divider_image);
                Assert.assertTrue(groupListDividerImage != null);

                return new DividerHolder(rowGroupListDivider, groupListDividerImage);
            } else {
                Assert.assertTrue(viewType == TYPE_FAB_PADDING);

                FrameLayout frameLayout = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list_fab_padding, parent, false);
                return new FabPaddingHolder(frameLayout);
            }
        }

        @Override
        public void onBindViewHolder(AbstractHolder abstractHolder, int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position < getItemCount());

            if (position < mNodeCollection.getItemCount()) {
                Node node = mNodeCollection.getNode(position);
                node.onBindViewHolder(abstractHolder);
            } else {
                Assert.assertTrue(mShowFab);
                Assert.assertTrue(position == mNodeCollection.getItemCount());
            }
        }

        @Override
        public int getItemCount() {
            return mNodeCollection.getItemCount() + (mShowFab ? 1 : 0);
        }

        public ExpansionState getExpansionState() {
            return mNodeCollection.getExpansionState();
        }

        public ArrayList<InstanceKey> getSelected() {
            return new ArrayList<>(Stream.of(mNodeCollection.mNotDoneGroupCollection.getSelected())
                    .map(notDoneInstanceNode -> notDoneInstanceNode.mInstanceData.InstanceKey)
                    .collect(Collectors.toList()));
        }

        public static abstract class AbstractHolder extends RecyclerView.ViewHolder {
            public AbstractHolder(View view) {
                super(view);
            }
        }

        public static class GroupHolder extends AbstractHolder {
            public final TableLayout mGroupRow;
            public final TextView mGroupRowName;
            public final TextView mGroupRowDetails;
            public final ImageView mGroupRowExpand;
            public final CheckBox mGroupRowCheckBox;
            public final View mGroupRowSeparator;

            public GroupHolder(TableLayout groupRow, TextView groupRowName, TextView groupRowDetails, ImageView groupRowExpand, CheckBox groupRowCheckBox, View groupRowSeparator) {
                super(groupRow);

                Assert.assertTrue(groupRowName != null);
                Assert.assertTrue(groupRowDetails != null);
                Assert.assertTrue(groupRowExpand != null);
                Assert.assertTrue(groupRowCheckBox != null);
                Assert.assertTrue(groupRowSeparator != null);

                mGroupRow = groupRow;
                mGroupRowName = groupRowName;
                mGroupRowDetails = groupRowDetails;
                mGroupRowExpand = groupRowExpand;
                mGroupRowCheckBox = groupRowCheckBox;
                mGroupRowSeparator = groupRowSeparator;
            }
        }

        public static class DividerHolder extends AbstractHolder {
            public final RelativeLayout RowGroupListDivider;
            public final ImageView GroupListDividerImage;

            DividerHolder(RelativeLayout rowGroupListDivider, ImageView groupListDividerImage) {
                super(rowGroupListDivider);

                Assert.assertTrue(rowGroupListDivider != null);
                Assert.assertTrue(groupListDividerImage != null);

                RowGroupListDivider = rowGroupListDivider;
                GroupListDividerImage = groupListDividerImage;
            }
        }

        public static class FabPaddingHolder extends AbstractHolder {
            FabPaddingHolder(FrameLayout frameLayout) {
                super(frameLayout);
            }
        }

        static class NodeCollection {
            private final WeakReference<GroupAdapter> mGroupAdapterReference;

            private NotDoneGroupCollection mNotDoneGroupCollection;
            private DividerNode mDividerNode;

            public static NodeCollection newNodeCollection(Collection<GroupListLoader.InstanceData> instanceDatas, ExpansionState expansionState, WeakReference<GroupAdapter> groupAdapterReference, ArrayList<InstanceKey> selectedNodes) {
                Assert.assertTrue(instanceDatas != null);
                Assert.assertTrue(groupAdapterReference != null);

                NodeCollection nodeCollection = new NodeCollection(groupAdapterReference);
                nodeCollection.setInstanceDatas(instanceDatas, expansionState, selectedNodes);
                return nodeCollection;
            }

            private NodeCollection(WeakReference<GroupAdapter> groupAdapterReference) {
                Assert.assertTrue(groupAdapterReference != null);
                mGroupAdapterReference = groupAdapterReference;
            }

            private void setInstanceDatas(Collection<GroupListLoader.InstanceData> instanceDatas, ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes) {
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

                mNotDoneGroupCollection = NotDoneGroupCollection.newNotDoneGroupCollection(notDoneInstances, expandedGroups, new WeakReference<>(this), selectedNodes);
                mDividerNode = DividerNode.newDividerNode(doneInstances, doneExpanded, new WeakReference<>(this));
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

            static class NotDoneGroupCollection implements NodeContainer {
                private final WeakReference<NodeCollection> mNodeCollectionReference;

                private final Comparator<NotDoneGroupNode> sComparator = (lhs, rhs) -> {
                    int timeStampComparison = lhs.getExactTimeStamp().compareTo(rhs.getExactTimeStamp());
                    if (timeStampComparison != 0) {
                        return timeStampComparison;
                    } else {
                        Assert.assertTrue(lhs.singleInstance());
                        Assert.assertTrue(rhs.singleInstance());

                        return Integer.valueOf(lhs.getSingleInstanceData().InstanceKey.TaskId).compareTo(rhs.getSingleInstanceData().InstanceKey.TaskId);
                    }
                };

                private final ArrayList<NotDoneGroupNode> mNotDoneGroupNodes = new ArrayList<>();

                public static NotDoneGroupCollection newNotDoneGroupCollection(Collection<GroupListLoader.InstanceData> instanceDatas, ArrayList<TimeStamp> expandedGroups, WeakReference<NodeCollection> nodeCollectionReference, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(nodeCollectionReference != null);

                    NotDoneGroupCollection notDoneGroupCollection = new NotDoneGroupCollection(nodeCollectionReference);
                    notDoneGroupCollection.setInstanceDatas(instanceDatas, expandedGroups, selectedNodes);
                    return notDoneGroupCollection;
                }

                private NotDoneGroupCollection(WeakReference<NodeCollection> nodeCollectionReference) {
                    Assert.assertTrue(nodeCollectionReference != null);
                    mNodeCollectionReference = nodeCollectionReference;
                }

                private void setInstanceDatas(Collection<GroupListLoader.InstanceData> instanceDatas, ArrayList<TimeStamp> expandedGroups, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceDatas != null);

                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    if (groupAdapter.mUseGroups) {
                        HashMap<TimeStamp, ArrayList<GroupListLoader.InstanceData>> instanceDataHash = new HashMap<>();
                        for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                            if (!instanceDataHash.containsKey(instanceData.InstanceTimeStamp))
                                instanceDataHash.put(instanceData.InstanceTimeStamp, new ArrayList<>());
                            instanceDataHash.get(instanceData.InstanceTimeStamp).add(instanceData);
                        }

                        for (Map.Entry<TimeStamp, ArrayList<GroupListLoader.InstanceData>> entry : instanceDataHash.entrySet()) {
                            boolean expanded = false;
                            if (entry.getValue().size() > 1 && expandedGroups != null && expandedGroups.contains(entry.getKey()))
                                expanded = true;
                            NotDoneGroupNode notDoneGroupNode = NotDoneGroupNode.newNotDoneGroupNode(entry.getValue(), expanded, new WeakReference<>(this), selectedNodes);
                            mNotDoneGroupNodes.add(notDoneGroupNode);
                        }
                    } else {
                        for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                            ArrayList<GroupListLoader.InstanceData> dummyInstanceDatas = new ArrayList<>();
                            dummyInstanceDatas.add(instanceData);
                            NotDoneGroupNode notDoneGroupNode = NotDoneGroupNode.newNotDoneGroupNode(dummyInstanceDatas, false, new WeakReference<>(this), selectedNodes);
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

                private Pair<Boolean, Pair<NotDoneGroupNode, NotDoneGroupNode.NotDoneInstanceNode>> addInstanceHelper(GroupListLoader.InstanceData instanceData, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(instanceData.Done == null);

                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    ExactTimeStamp exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();

                    ArrayList<NotDoneGroupNode> timeStampNotDoneGroupNodes = new ArrayList<>();
                    if (groupAdapter.mUseGroups)
                        for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                            if (notDoneGroupNode.getExactTimeStamp().equals(exactTimeStamp))
                                timeStampNotDoneGroupNodes.add(notDoneGroupNode);

                    if (timeStampNotDoneGroupNodes.isEmpty()) {
                        ArrayList<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
                        instanceDatas.add(instanceData);
                        NotDoneGroupNode notDoneGroupNode = NotDoneGroupNode.newNotDoneGroupNode(instanceDatas, false, new WeakReference<>(this), selectedNodes);
                        NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = notDoneGroupNode.mNotDoneInstanceNodes.get(0);
                        mNotDoneGroupNodes.add(notDoneGroupNode);
                        return new Pair<>(true, new Pair<>(notDoneGroupNode, notDoneInstanceNode));
                    } else {
                        Assert.assertTrue(timeStampNotDoneGroupNodes.size() == 1);
                        NotDoneGroupNode notDoneGroupNode = timeStampNotDoneGroupNodes.get(0);
                        NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = notDoneGroupNode.addInstanceData(instanceData, selectedNodes);
                        notDoneGroupNode.sort();
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

                    Pair<Boolean, Pair<NotDoneGroupNode, NotDoneGroupNode.NotDoneInstanceNode>> pair = addInstanceHelper(instanceData, null);
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

                public List<NotDoneGroupNode.NotDoneInstanceNode> getSelected() {
                    return Stream.of(mNotDoneGroupNodes)
                            .flatMap(NotDoneGroupNode::getSelected)
                            .collect(Collectors.toList());
                }

                public void unselect() {
                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                        notDoneGroupNode.unselect();
                }

                public void updateCheckBoxes() {
                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                        notDoneGroupNode.updateCheckBoxes();
                }
            }

            static class NotDoneGroupNode implements Node, NodeContainer {
                private static final Comparator<NotDoneInstanceNode> sComparator = (NotDoneInstanceNode lhs, NotDoneInstanceNode rhs) -> Integer.valueOf(lhs.mInstanceData.InstanceKey.TaskId).compareTo(rhs.mInstanceData.InstanceKey.TaskId);

                private final WeakReference<NotDoneGroupCollection> mNotDoneGroupCollectionReference;

                private ExactTimeStamp mExactTimeStamp;

                private final ArrayList<NotDoneInstanceNode> mNotDoneInstanceNodes = new ArrayList<>();

                private boolean mNotDoneGroupNodeExpanded;

                public static NotDoneGroupNode newNotDoneGroupNode(ArrayList<GroupListLoader.InstanceData> instanceDatas, boolean expanded, WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(!instanceDatas.isEmpty());
                    Assert.assertTrue(instanceDatas.size() > 1 || !expanded);
                    Assert.assertTrue(notDoneGroupCollectionReference != null);

                    NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(expanded, notDoneGroupCollectionReference);
                    notDoneGroupNode.setInstanceDatas(instanceDatas, selectedNodes);
                    return notDoneGroupNode;
                }

                private NotDoneGroupNode(boolean expanded, WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference) {
                    Assert.assertTrue(notDoneGroupCollectionReference != null);

                    mNotDoneGroupCollectionReference = notDoneGroupCollectionReference;
                    mNotDoneGroupNodeExpanded = expanded;
                }

                private void setInstanceDatas(ArrayList<GroupListLoader.InstanceData> instanceDatas, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(!instanceDatas.isEmpty());
                    Assert.assertTrue(instanceDatas.size() > 1 || !mNotDoneGroupNodeExpanded);

                    mExactTimeStamp = instanceDatas.get(0).InstanceTimeStamp.toExactTimeStamp();
                    for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                        Assert.assertTrue(mExactTimeStamp.equals(instanceData.InstanceTimeStamp.toExactTimeStamp()));
                        addInstanceData(instanceData, selectedNodes);
                    }
                    sort();
                }

                @Override
                public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) abstractHolder;

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!mNotDoneGroupNodeExpanded);

                        NotDoneInstanceNode notDoneInstanceNode = mNotDoneInstanceNodes.get(0);
                        Assert.assertTrue(notDoneInstanceNode != null);

                        groupHolder.mGroupRowName.setVisibility(View.VISIBLE);
                        groupHolder.mGroupRowName.setText(notDoneInstanceNode.mInstanceData.Name);

                        groupHolder.mGroupRowDetails.setVisibility(View.VISIBLE);
                        groupHolder.mGroupRowDetails.setText(notDoneInstanceNode.mInstanceData.DisplayText);

                        if (!notDoneInstanceNode.mInstanceData.TaskCurrent) {
                            groupHolder.mGroupRowName.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled));
                            groupHolder.mGroupRowDetails.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled));
                        } else {
                            groupHolder.mGroupRowName.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary));
                            groupHolder.mGroupRowDetails.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary));
                        }

                        if (notDoneInstanceNode.mInstanceData.HasChildren) {
                            groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_list_black_36dp);
                        } else {
                            groupHolder.mGroupRowExpand.setVisibility(View.INVISIBLE);
                        }
                        groupHolder.mGroupRowExpand.setOnClickListener(null);

                        if (groupListFragment.mSelectionCallback.hasActionMode()) {
                            groupHolder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
                            groupHolder.mGroupRowCheckBox.setOnClickListener(null);
                        } else {
                            groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);
                            groupHolder.mGroupRowCheckBox.setChecked(false);
                            groupHolder.mGroupRowCheckBox.setOnClickListener(v -> {
                                notDoneInstanceNode.mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupAdapter.mDataId, notDoneInstanceNode.mInstanceData.InstanceKey, true);
                                Assert.assertTrue(notDoneInstanceNode.mInstanceData.Done != null);

                                TickService.startService(groupListFragment.getActivity());

                                int oldPosition = nodeCollection.getPosition(NotDoneGroupNode.this);
                                notDoneGroupCollection.remove(NotDoneGroupNode.this);

                                groupAdapter.notifyItemRemoved(oldPosition);

                                nodeCollection.mDividerNode.add(notDoneInstanceNode.mInstanceData, oldPosition);
                            });
                        }

                        if (notDoneInstanceNode.mSelected)
                            groupHolder.mGroupRow.setBackgroundColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected));
                        else
                            groupHolder.mGroupRow.setBackgroundColor(Color.TRANSPARENT);

                        groupHolder.mGroupRow.setOnLongClickListener(v -> {
                            onInstanceLongClick();
                            return false;
                        });
                        groupHolder.mGroupRow.setOnClickListener(v -> {
                            if (groupListFragment.mSelectionCallback.hasActionMode())
                                onInstanceLongClick();
                            else
                                onInstanceClick();
                        });
                    } else {
                        if (mNotDoneGroupNodeExpanded) {
                            groupHolder.mGroupRowName.setVisibility(View.INVISIBLE);
                        } else {
                            groupHolder.mGroupRowName.setVisibility(View.VISIBLE);

                            String nameText = Stream.of(mNotDoneInstanceNodes)
                                    .map(notDoneInstanceNode -> notDoneInstanceNode.mInstanceData.Name)
                                    .collect(Collectors.joining(", "));
                            groupHolder.mGroupRowName.setText(nameText);
                        }

                        groupHolder.mGroupRowDetails.setVisibility(View.VISIBLE);

                        Date date = mExactTimeStamp.getDate();
                        HourMinute hourMinute = mExactTimeStamp.toTimeStamp().getHourMinute();

                        GroupListLoader.CustomTimeData customTimeData = getCustomTimeData(date.getDayOfWeek(), hourMinute);

                        String timeText;
                        if (customTimeData != null)
                            timeText = customTimeData.Name;
                        else
                            timeText = hourMinute.toString();

                        String detailsText = date.getDisplayText(groupListFragment.getActivity()) + ", " + timeText;
                        groupHolder.mGroupRowDetails.setText(detailsText);

                        groupHolder.mGroupRowName.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary));
                        groupHolder.mGroupRowDetails.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary));

                        if (groupListFragment.mSelectionCallback.hasActionMode() && getSelected().count() > 0)
                            groupHolder.mGroupRowExpand.setVisibility(View.INVISIBLE);
                        else
                            groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);

                        if (mNotDoneGroupNodeExpanded)
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_expand_less_black_36dp);
                        else
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_expand_more_black_36dp);

                        groupHolder.mGroupRowExpand.setOnClickListener(v -> {
                            int position = nodeCollection.getPosition(NotDoneGroupNode.this);

                            if (mNotDoneGroupNodeExpanded) { // hiding
                                Assert.assertTrue(getSelected().count() == 0);

                                int displayedSize = displayedSize();
                                mNotDoneGroupNodeExpanded = !mNotDoneGroupNodeExpanded;
                                groupAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1);
                            } else { // showing
                                mNotDoneGroupNodeExpanded = !mNotDoneGroupNodeExpanded;
                                groupAdapter.notifyItemRangeInserted(position + 1, displayedSize() - 1);
                            }

                            if ((position) > 0 && (nodeCollection.getNode(position - 1) instanceof NotDoneGroupNode)) {
                                groupAdapter.notifyItemRangeChanged(position - 1, 2);
                            } else {
                                groupAdapter.notifyItemChanged(position);
                            }
                        });

                        if (mNotDoneGroupNodeExpanded) {
                            groupHolder.mGroupRowCheckBox.setVisibility(View.GONE);
                        } else {
                            groupHolder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
                        }
                        groupHolder.mGroupRowCheckBox.setOnClickListener(null);

                        groupHolder.mGroupRow.setBackgroundColor(Color.TRANSPARENT);
                        groupHolder.mGroupRow.setOnClickListener(v -> {
                            if (!groupListFragment.mSelectionCallback.hasActionMode())
                                groupListFragment.getActivity().startActivity(ShowGroupActivity.getIntent(mExactTimeStamp, groupListFragment.getActivity()));
                        });
                    }

                    boolean showSeparator = false;
                    if (!mNotDoneGroupNodeExpanded) {
                        int position = nodeCollection.getPosition(this);
                        boolean last = (position == notDoneGroupCollection.displayedSize() - 1);
                        if (!last) {
                            NotDoneGroupNode nextNode = (NotDoneGroupNode) nodeCollection.getNode(position + 1);
                            if (nextNode.expanded())
                                showSeparator = true;
                        } else {
                            if (nodeCollection.mDividerNode.expanded())
                                showSeparator = true;
                        }
                    }
                    if (showSeparator)
                        groupHolder.mGroupRowSeparator.setVisibility(View.VISIBLE);
                    else
                        groupHolder.mGroupRowSeparator.setVisibility(View.INVISIBLE);
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

                    if (!(node instanceof NotDoneInstanceNode))
                        return -1;

                    NotDoneInstanceNode notDoneInstanceNode = (NotDoneInstanceNode) node;
                    if (mNotDoneInstanceNodes.contains(notDoneInstanceNode))
                        return mNotDoneInstanceNodes.indexOf(notDoneInstanceNode) + 1;

                    return -1;
                }

                @Override
                public boolean expanded() {
                    return mNotDoneGroupNodeExpanded;
                }

                private void onInstanceLongClick() {
                    Assert.assertTrue(singleInstance());

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    NotDoneInstanceNode notDoneInstanceNode = mNotDoneInstanceNodes.get(0);
                    Assert.assertTrue(notDoneInstanceNode != null);

                    notDoneInstanceNode.mSelected = !notDoneInstanceNode.mSelected;

                    if (notDoneInstanceNode.mSelected) {
                        groupListFragment.mSelectionCallback.incrementSelected();
                    } else {
                        groupListFragment.mSelectionCallback.decrementSelected();
                    }
                    groupAdapter.notifyItemChanged(nodeCollection.getPosition(this));
                }

                private void onInstanceClick() {
                    Assert.assertTrue(singleInstance());

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    NotDoneInstanceNode notDoneInstanceNode = mNotDoneInstanceNodes.get(0);
                    Assert.assertTrue(notDoneInstanceNode != null);

                    groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), notDoneInstanceNode.mInstanceData.InstanceKey));
                }

                private void sort() {
                    Collections.sort(mNotDoneInstanceNodes, sComparator);
                }

                private NotDoneInstanceNode addInstanceData(GroupListLoader.InstanceData instanceData, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceData != null);

                    NotDoneInstanceNode notDoneInstanceNode = new NotDoneInstanceNode(instanceData, new WeakReference<>(this), selectedNodes);
                    mNotDoneInstanceNodes.add(notDoneInstanceNode);
                    return notDoneInstanceNode;
                }

                private GroupListLoader.CustomTimeData getCustomTimeData(DayOfWeek dayOfWeek, HourMinute hourMinute) {
                    Assert.assertTrue(dayOfWeek != null);
                    Assert.assertTrue(hourMinute != null);

                    NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    for (GroupListLoader.CustomTimeData customTimeData : groupAdapter.mCustomTimeDatas)
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

                public Stream<NotDoneGroupNode.NotDoneInstanceNode> getSelected() {
                    return Stream.of(mNotDoneInstanceNodes)
                            .filter(notDoneInstanceNode -> notDoneInstanceNode.mSelected);
                }

                public void unselect() {
                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    if (singleInstance()) {
                        NotDoneInstanceNode notDoneInstanceNode = mNotDoneInstanceNodes.get(0);
                        Assert.assertTrue(notDoneInstanceNode != null);

                        if (notDoneInstanceNode.mSelected) {
                            notDoneInstanceNode.mSelected = false;
                            groupAdapter.notifyItemChanged(nodeCollection.getPosition(this));
                        }
                    } else {
                        List<NotDoneInstanceNode> selected = getSelected().collect(Collectors.toList());
                        if (!selected.isEmpty()) {
                            Assert.assertTrue(mNotDoneGroupNodeExpanded);

                            for (NotDoneInstanceNode notDoneInstanceNode : selected) {
                                if (notDoneInstanceNode.mSelected) {
                                    notDoneInstanceNode.mSelected = false;
                                    groupAdapter.notifyItemChanged(nodeCollection.getPosition(notDoneInstanceNode));
                                }
                            }
                            groupAdapter.notifyItemChanged(nodeCollection.getPosition(this));
                        }
                    }
                }

                public void updateCheckBoxes() {
                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    if (singleInstance()) {
                        groupAdapter.notifyItemChanged(nodeCollection.getPosition(this));
                    } else {
                        groupAdapter.notifyItemRangeChanged(nodeCollection.getPosition(this) + 1, displayedSize() - 1);
                    }
                }

                static class NotDoneInstanceNode implements Node {
                    private final WeakReference<NotDoneGroupNode> mNotDoneGroupNodeReference;

                    public final GroupListLoader.InstanceData mInstanceData;

                    public boolean mSelected = false;

                    public NotDoneInstanceNode(GroupListLoader.InstanceData instanceData, WeakReference<NotDoneGroupNode> notDoneGroupNodeReference, ArrayList<InstanceKey> selectedNodes) {
                        Assert.assertTrue(instanceData != null);
                        Assert.assertTrue(notDoneGroupNodeReference != null);

                        mInstanceData = instanceData;
                        mNotDoneGroupNodeReference = notDoneGroupNodeReference;
                        if (selectedNodes != null && selectedNodes.contains(mInstanceData.InstanceKey)) {
                            mSelected = true;
                        }
                    }

                    @Override
                    public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                        final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) abstractHolder;

                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final boolean lastInGroup = (notDoneGroupNode.mNotDoneInstanceNodes.indexOf(this) == notDoneGroupNode.mNotDoneInstanceNodes.size() - 1);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                        Assert.assertTrue(nodeCollection != null);

                        final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                        Assert.assertTrue(groupAdapter != null);

                        final GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        groupHolder.mGroupRowName.setVisibility(View.VISIBLE);
                        groupHolder.mGroupRowName.setText(mInstanceData.Name);

                        if (!mInstanceData.TaskCurrent) {
                            groupHolder.mGroupRowName.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled));
                        } else {
                            groupHolder.mGroupRowName.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary));
                        }

                        groupHolder.mGroupRowDetails.setVisibility(View.GONE);

                        if (mInstanceData.HasChildren) {
                            groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);
                            groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_list_black_36dp);
                        } else {
                            groupHolder.mGroupRowExpand.setVisibility(View.INVISIBLE);
                        }
                        groupHolder.mGroupRowExpand.setOnClickListener(null);

                        if (groupListFragment.mSelectionCallback.hasActionMode()) {
                            groupHolder.mGroupRowCheckBox.setVisibility(View.INVISIBLE);
                            groupHolder.mGroupRowCheckBox.setOnClickListener(null);
                        } else {
                            groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);
                            groupHolder.mGroupRowCheckBox.setChecked(false);
                            groupHolder.mGroupRowCheckBox.setOnClickListener(v -> {
                                Assert.assertTrue(notDoneGroupNode.mNotDoneGroupNodeExpanded);

                                mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupAdapter.mDataId, mInstanceData.InstanceKey, true);
                                Assert.assertTrue(mInstanceData.Done != null);

                                TickService.startService(groupListFragment.getActivity());

                                Assert.assertTrue(notDoneGroupNode.mNotDoneInstanceNodes.size() >= 2);
                                int groupPosition = nodeCollection.getPosition(notDoneGroupNode);

                                int oldInstancePosition = nodeCollection.getPosition(NotDoneInstanceNode.this);

                                if (notDoneGroupNode.mNotDoneInstanceNodes.size() == 2) {
                                    notDoneGroupNode.mNotDoneInstanceNodes.remove(NotDoneInstanceNode.this);

                                    notDoneGroupNode.mNotDoneGroupNodeExpanded = false;

                                    if ((groupPosition > 0) && (nodeCollection.getNode(groupPosition - 1) instanceof NotDoneGroupNode))
                                        groupAdapter.notifyItemRangeChanged(groupPosition - 1, 2);
                                    else
                                        groupAdapter.notifyItemChanged(groupPosition);

                                    groupAdapter.notifyItemRangeRemoved(groupPosition + 1, 2);
                                } else {
                                    Assert.assertTrue(notDoneGroupNode.mNotDoneInstanceNodes.size() > 2);

                                    notDoneGroupNode.mNotDoneInstanceNodes.remove(NotDoneInstanceNode.this);

                                    groupAdapter.notifyItemChanged(groupPosition);
                                    groupAdapter.notifyItemRemoved(oldInstancePosition);

                                    if (lastInGroup)
                                        groupAdapter.notifyItemChanged(oldInstancePosition - 1);
                                }

                                nodeCollection.mDividerNode.add(mInstanceData, oldInstancePosition);
                            });
                        }

                        if (lastInGroup) {
                            groupHolder.mGroupRowSeparator.setVisibility(View.VISIBLE);
                        } else {
                            groupHolder.mGroupRowSeparator.setVisibility(View.INVISIBLE);
                        }


                        if (mSelected)
                            groupHolder.mGroupRow.setBackgroundColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected));
                        else
                            groupHolder.mGroupRow.setBackgroundColor(Color.TRANSPARENT);

                        groupHolder.mGroupRow.setOnLongClickListener(v -> {
                            onInstanceLongClick();
                            return false;
                        });

                        groupHolder.mGroupRow.setOnClickListener(v -> {
                            if (groupListFragment.mSelectionCallback.hasActionMode())
                                onInstanceLongClick();
                            else
                                onInstanceClick();
                        });
                    }

                    @Override
                    public int getItemViewType() {
                        return TYPE_GROUP;
                    }

                    private void onInstanceLongClick() {
                        NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        Assert.assertTrue(notDoneGroupNode.mNotDoneGroupNodeExpanded);

                        final NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                        Assert.assertTrue(nodeCollection != null);

                        final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                        Assert.assertTrue(groupAdapter != null);

                        GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        mSelected = !mSelected;

                        if (mSelected) {
                            groupListFragment.mSelectionCallback.incrementSelected();

                            if (notDoneGroupNode.getSelected().count() == 1) // first in group
                                groupAdapter.notifyItemChanged(nodeCollection.getPosition(notDoneGroupNode));
                        } else {
                            groupListFragment.mSelectionCallback.decrementSelected();

                            if (notDoneGroupNode.getSelected().count() == 0) // last in group
                                groupAdapter.notifyItemChanged(nodeCollection.getPosition(notDoneGroupNode));
                        }

                        groupAdapter.notifyItemChanged(nodeCollection.getPosition(this));
                    }

                    private void onInstanceClick() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                        Assert.assertTrue(nodeCollection != null);

                        final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                        Assert.assertTrue(groupAdapter != null);

                        final GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey));
                    }
                }
            }

            static class DividerNode implements Node, NodeContainer {
                private final WeakReference<NodeCollection> mNodeCollectionReference;

                private boolean mDoneExpanded;

                private final Comparator<DoneInstanceNode> sComparator = (DoneInstanceNode lhs, DoneInstanceNode rhs) -> -lhs.mInstanceData.Done.compareTo(rhs.mInstanceData.Done); // negated

                private final ArrayList<NodeCollection.DoneInstanceNode> mDoneInstanceNodes = new ArrayList<>();

                public static DividerNode newDividerNode(ArrayList<GroupListLoader.InstanceData> instanceDatas, boolean doneExpanded, WeakReference<NodeCollection> nodeCollectionReference) {
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(nodeCollectionReference != null);

                    DividerNode dividerNode = new DividerNode(doneExpanded, nodeCollectionReference);
                    dividerNode.setInstanceDatas(instanceDatas);
                    return dividerNode;
                }

                private DividerNode(boolean doneExpanded, WeakReference<NodeCollection> nodeCollectionReference) {
                    Assert.assertTrue(nodeCollectionReference != null);

                    mNodeCollectionReference = nodeCollectionReference;
                    mDoneExpanded = doneExpanded;
                }

                private void setInstanceDatas(ArrayList<GroupListLoader.InstanceData> instanceDatas) {
                    Assert.assertTrue(instanceDatas != null);

                    for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                        Assert.assertTrue(instanceData.Done != null);
                        mDoneInstanceNodes.add(new DoneInstanceNode(instanceData, new WeakReference<>(this)));
                    }

                    Collections.sort(mDoneInstanceNodes, sComparator);
                }

                @Override
                public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.DividerHolder dividerHolder = (GroupAdapter.DividerHolder) abstractHolder;

                    final NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    if (mDoneExpanded)
                        dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_less_black_36dp);
                    else
                        dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_more_black_36dp);

                    dividerHolder.RowGroupListDivider.setOnClickListener(v -> {
                        Assert.assertTrue(!mDoneInstanceNodes.isEmpty());

                        int position = nodeCollection.getPosition(DividerNode.this);

                        int displayedSize = DividerNode.this.displayedSize();
                        if (mDoneExpanded) { // hiding
                            mDoneExpanded = false;
                            groupAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1);
                        } else { // showing
                            mDoneExpanded = true;
                            groupAdapter.notifyItemRangeInserted(position + 1, displayedSize - 1);
                        }

                        if (nodeCollection.mNotDoneGroupCollection.displayedSize() == 0) {
                            groupAdapter.notifyItemChanged(position);
                        } else {
                            groupAdapter.notifyItemRangeChanged(position - 1, 2);
                        }
                    });
                }

                @Override
                public int getItemViewType() {
                    return TYPE_DIVIDER;
                }

                @Override
                public int displayedSize() {
                    final NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (mDoneInstanceNodes.isEmpty() || groupListFragment.mSelectionCallback.hasActionMode()) {
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
                    if (!mDoneExpanded)
                        return false;

                    final NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    return !groupListFragment.mSelectionCallback.hasActionMode();
                }

                public DoneInstanceNode get(int index) {
                    Assert.assertTrue(index >= 0);
                    Assert.assertTrue(index < mDoneInstanceNodes.size());

                    return mDoneInstanceNodes.get(index);
                }

                public boolean isEmpty() {
                    return mDoneInstanceNodes.isEmpty();
                }

                public void add(GroupListLoader.InstanceData instanceData, int oldInstancePosition) {
                    Assert.assertTrue(instanceData != null);

                    final NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    if (mDoneExpanded) {
                        Assert.assertTrue(!mDoneInstanceNodes.isEmpty());

                        int oldDividerPosition = nodeCollection.getPosition(this);
                        boolean bottomNotDone = (oldInstancePosition == oldDividerPosition);

                        DoneInstanceNode doneInstanceNode = new DoneInstanceNode(instanceData, new WeakReference<>(this));
                        mDoneInstanceNodes.add(doneInstanceNode);

                        Collections.sort(mDoneInstanceNodes, sComparator);

                        int newInstancePosition = nodeCollection.getPosition(doneInstanceNode);
                        groupAdapter.notifyItemInserted(newInstancePosition);

                        if (bottomNotDone && nodeCollection.mNotDoneGroupCollection.displayedSize() > 0) {
                            int newDividerPosition = nodeCollection.getPosition(this);
                            groupAdapter.notifyItemChanged(newDividerPosition - 1);
                        }
                    } else {
                        DoneInstanceNode doneInstanceNode = new DoneInstanceNode(instanceData, new WeakReference<>(this));
                        mDoneInstanceNodes.add(doneInstanceNode);

                        Collections.sort(mDoneInstanceNodes, sComparator);

                        if (mDoneInstanceNodes.size() == 1) {
                            Assert.assertTrue(!mDoneExpanded);
                            int newDividerPosition = nodeCollection.getPosition(this);
                            groupAdapter.notifyItemInserted(newDividerPosition);

                            if (nodeCollection.mNotDoneGroupCollection.displayedSize() != 0) {
                                groupAdapter.notifyItemChanged(newDividerPosition - 1);
                            }
                        } else {
                            if (mDoneExpanded) {
                                int newInstancePosition = nodeCollection.getPosition(doneInstanceNode);
                                groupAdapter.notifyItemInserted(newInstancePosition);
                            }
                        }
                    }
                }

                public void remove(DoneInstanceNode doneInstanceNode) {
                    Assert.assertTrue(doneInstanceNode != null);
                    Assert.assertTrue(mDoneInstanceNodes.contains(doneInstanceNode));
                    Assert.assertTrue(displayedSize() > 1);

                    final NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    if (nodeCollection.mNotDoneGroupCollection.displayedSize() == 0) {
                        int oldInstancePosition = nodeCollection.getPosition(doneInstanceNode);

                        mDoneInstanceNodes.remove(doneInstanceNode);

                        if (mDoneInstanceNodes.isEmpty()) {
                            mDoneExpanded = false;

                            int dividerPosition = nodeCollection.getPosition(this);
                            Assert.assertTrue(dividerPosition == oldInstancePosition - 1);

                            groupAdapter.notifyItemRangeRemoved(dividerPosition, 2);
                        } else {
                            groupAdapter.notifyItemRemoved(oldInstancePosition);
                        }
                    } else {
                        int oldInstancePosition = nodeCollection.getPosition(doneInstanceNode);
                        int oldDividerPosition = nodeCollection.getPosition(this);

                        mDoneInstanceNodes.remove(doneInstanceNode);

                        if (mDoneInstanceNodes.isEmpty()) {
                            mDoneExpanded = false;

                            int dividerPosition = nodeCollection.getPosition(this);
                            Assert.assertTrue(dividerPosition == oldInstancePosition - 1);

                            groupAdapter.notifyItemRangeRemoved(dividerPosition, 2);

                            groupAdapter.notifyItemChanged(nodeCollection.mNotDoneGroupCollection.displayedSize() - 1);
                        } else {
                            groupAdapter.notifyItemRemoved(oldInstancePosition);

                            groupAdapter.notifyItemChanged(oldDividerPosition - 1);
                        }
                    }
                }
            }

            static class DoneInstanceNode implements Node {
                private final WeakReference<DividerNode> mDividerNodeReference;

                private final GroupListLoader.InstanceData mInstanceData;

                public DoneInstanceNode(GroupListLoader.InstanceData instanceData, WeakReference<DividerNode> dividerNodeReference) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(dividerNodeReference != null);

                    mInstanceData = instanceData;
                    mDividerNodeReference = dividerNodeReference;
                }

                @Override
                public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) abstractHolder;

                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    final NodeCollection nodeCollection = dividerNode.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    final GroupAdapter groupAdapter = nodeCollection.mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    final GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    groupHolder.mGroupRowName.setVisibility(View.VISIBLE);
                    groupHolder.mGroupRowName.setText(mInstanceData.Name);

                    groupHolder.mGroupRowDetails.setVisibility(View.VISIBLE);
                    groupHolder.mGroupRowDetails.setText(mInstanceData.DisplayText);

                    if (!mInstanceData.TaskCurrent) {
                        groupHolder.mGroupRowName.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled));
                        groupHolder.mGroupRowDetails.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled));
                    } else {
                        groupHolder.mGroupRowName.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary));
                        groupHolder.mGroupRowDetails.setTextColor(ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary));
                    }

                    if (mInstanceData.HasChildren) {
                        groupHolder.mGroupRowExpand.setVisibility(View.VISIBLE);
                        groupHolder.mGroupRowExpand.setImageResource(R.drawable.ic_list_black_36dp);
                    } else {
                        groupHolder.mGroupRowExpand.setVisibility(View.INVISIBLE);
                    }

                    groupHolder.mGroupRowExpand.setOnClickListener(null);

                    groupHolder.mGroupRowCheckBox.setVisibility(View.VISIBLE);
                    groupHolder.mGroupRowCheckBox.setChecked(true);
                    groupHolder.mGroupRowCheckBox.setOnClickListener(v -> {
                        Assert.assertTrue(dividerNode.expanded());

                        mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupAdapter.mDataId, mInstanceData.InstanceKey, false);
                        Assert.assertTrue(mInstanceData.Done == null);

                        TickService.startService(groupListFragment.getActivity());

                        dividerNode.remove(DoneInstanceNode.this);

                        Pair<Boolean, Pair<NotDoneGroupNode, NotDoneGroupNode.NotDoneInstanceNode>> pair = nodeCollection.mNotDoneGroupCollection.add(mInstanceData);
                        boolean newNotDoneGroupNode = pair.first;
                        NotDoneGroupNode notDoneGroupNode = pair.second.first;
                        NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = pair.second.second;

                        if (newNotDoneGroupNode) {
                            int newGroupPosition = nodeCollection.getPosition(notDoneGroupNode);
                            groupAdapter.notifyItemInserted(newGroupPosition);
                        } else {
                            if (notDoneGroupNode.expanded()) {
                                int newGroupPosition = nodeCollection.getPosition(notDoneGroupNode);
                                int newInstancePosition = nodeCollection.getPosition(notDoneInstanceNode);

                                boolean last = (newGroupPosition + notDoneGroupNode.displayedSize() - 1 == newInstancePosition);

                                groupAdapter.notifyItemChanged(newGroupPosition);
                                groupAdapter.notifyItemInserted(newInstancePosition);

                                if (last)
                                    groupAdapter.notifyItemChanged(newInstancePosition - 1);
                            } else {
                                int newGroupPosition = nodeCollection.getPosition(notDoneGroupNode);
                                groupAdapter.notifyItemChanged(newGroupPosition);
                            }
                        }
                    });

                    groupHolder.mGroupRowSeparator.setVisibility(View.INVISIBLE);

                    groupHolder.mGroupRow.setBackgroundColor(Color.TRANSPARENT);

                    groupHolder.mGroupRow.setOnClickListener(v -> groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey)));
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

    public interface GroupListListener {
        void onCreateGroupActionMode(ActionMode actionMode);
        void onDestroyGroupActionMode();
    }
}