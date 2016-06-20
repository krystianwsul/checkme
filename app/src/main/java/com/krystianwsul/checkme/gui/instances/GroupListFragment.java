package com.krystianwsul.checkme.gui.instances;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.EventBuffer;
import com.krystianwsul.checkme.PruneService;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.tree.ModelNode;
import com.krystianwsul.checkme.gui.instances.tree.NodeContainer;
import com.krystianwsul.checkme.gui.instances.tree.TreeModelAdapter;
import com.krystianwsul.checkme.gui.instances.tree.TreeNode;
import com.krystianwsul.checkme.gui.instances.tree.TreeNodeCollection;
import com.krystianwsul.checkme.gui.instances.tree.TreeViewAdapter;
import com.krystianwsul.checkme.gui.tasks.CreateChildTaskActivity;
import com.krystianwsul.checkme.gui.tasks.CreateRootTaskActivity;
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

public class GroupListFragment extends Fragment implements LoaderManager.LoaderCallbacks<GroupListLoader.Data> {
    private final static String POSITION_KEY = "position";
    private static final String TIME_RANGE_KEY = "timeRange";

    private final static String EXPANSION_STATE_KEY = "expansionState";
    private final static String SELECTED_NODES_KEY = "selectedNodes";

    private RecyclerView mGroupListRecycler;
    private TreeViewAdapter mTreeViewAdapter;
    private FloatingActionButton mFloatingActionButton;
    private TextView mEmptyText;

    private Integer mPosition;
    private MainActivity.TimeRange mTimeRange;
    private TimeStamp mTimeStamp;
    private InstanceKey mInstanceKey;
    private ArrayList<InstanceKey> mInstanceKeys;

    private ExpansionState mExpansionState;
    private ArrayList<InstanceKey> mSelectedNodes;

    private boolean mFirst = true;

    private GroupListLoader.Data mData;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        private Integer mOldVisibility = null;

        @Override
        protected void unselect() {
            mTreeViewAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            Assert.assertTrue(mTreeViewAdapter != null);

            List<GroupListLoader.InstanceData> instanceDatas = nodesToInstanceDatas(mTreeViewAdapter.getSelectedNodes());
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(!instanceDatas.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_group_edit_instance:
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    if (instanceDatas.size() == 1) {
                        GroupListLoader.InstanceData instanceData = instanceDatas.get(0);
                        Assert.assertTrue(instanceData.IsRootInstance);

                        startActivity(EditInstanceActivity.getIntent(getActivity(), instanceData.InstanceKey));
                    } else {
                        Assert.assertTrue(instanceDatas.size() > 1);

                        Assert.assertTrue(Stream.of(instanceDatas)
                            .allMatch(instanceData -> instanceData.IsRootInstance));

                        ArrayList<InstanceKey> instanceKeys = Stream.of(instanceDatas)
                                .map(instanceData -> instanceData.InstanceKey)
                                .collect(Collectors.toCollection(ArrayList::new));

                        startActivity(EditInstancesActivity.getIntent(getActivity(), instanceKeys));
                    }
                    break;
                case R.id.action_group_show_task:
                    Assert.assertTrue(instanceDatas.size() == 1);

                    GroupListLoader.InstanceData instanceData2 = instanceDatas.get(0);
                    Assert.assertTrue(instanceData2.TaskCurrent);

                    startActivity(ShowTaskActivity.getIntent(instanceData2.InstanceKey.TaskId, getActivity()));
                    break;
                case R.id.action_group_edit_task:
                    Assert.assertTrue(instanceDatas.size() == 1);

                    GroupListLoader.InstanceData instanceData3 = instanceDatas.get(0);
                    Assert.assertTrue(instanceData3.TaskCurrent);

                    if (instanceData3.IsRootTask)
                        startActivity(CreateRootTaskActivity.getEditIntent(getActivity(), instanceData3.InstanceKey.TaskId));
                    else
                        startActivity(CreateChildTaskActivity.getEditIntent(getActivity(), instanceData3.InstanceKey.TaskId));
                    break;
                case R.id.action_group_delete_task: {
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(!taskIds.isEmpty());
                    Assert.assertTrue(Stream.of(instanceDatas)
                            .allMatch(instanceData -> instanceData.TaskCurrent));

                    List<TreeNode> selectedTreeNodes = mTreeViewAdapter.getSelectedNodes();

                    for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                        if (instanceData.Exists) {
                            instanceData.TaskCurrent = false;
                            instanceData.IsRootTask = null;
                        } else {
                            GroupListLoader.InstanceDataParent instanceDataParent = instanceData.InstanceDataParentReference.get();
                            Assert.assertTrue(instanceDataParent != null);

                            instanceDataParent.remove(instanceData.InstanceKey);
                        }
                    }

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(((GroupAdapter) mTreeViewAdapter.getTreeModelAdapter()).mDataId, taskIds);

                    TickService.startService(getActivity());

                    do {
                        TreeNode treeNode = selectedTreeNodes.get(0);
                        Assert.assertTrue(treeNode != null);

                        GroupListLoader.InstanceData instanceData1;
                        if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                            instanceData1 = ((GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode()).getSingleInstanceData();
                        } else {
                            instanceData1 = ((GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode()).mInstanceData;
                        }

                        if (instanceData1.Exists) {
                            treeNode.unselect();

                            treeNode.update();

                            decrementSelected();
                        } else {
                            if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                                GroupAdapter.NodeCollection.NotDoneGroupNode notDoneGroupNode = (GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode();
                                Assert.assertTrue(notDoneGroupNode != null);

                                notDoneGroupNode.removeFromParent();
                            } else {
                                GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = (GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode();
                                Assert.assertTrue(notDoneInstanceNode != null);

                                notDoneInstanceNode.removeFromParent();
                            }

                            decrementSelected();
                        }
                    } while (!(selectedTreeNodes = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    break;
                }
                case R.id.action_group_join:
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(taskIds.size() > 1);

                    if (mInstanceKey == null) {
                        if (mPosition != null) {
                            Assert.assertTrue(mTimeRange != null);

                            startActivity(CreateRootTaskActivity.getJoinIntent(getActivity(), taskIds, rangePositionToDate(mTimeRange, mPosition)));
                        } else {
                            startActivity(CreateRootTaskActivity.getJoinIntent(getActivity(), taskIds));
                        }
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

            mTreeViewAdapter.onCreateActionMode();

            mActionMode.getMenuInflater().inflate(R.menu.menu_edit_groups, mActionMode.getMenu());

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
            mTreeViewAdapter.onDestroyActionMode();

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

            List<GroupListLoader.InstanceData> instanceDatas = nodesToInstanceDatas(mTreeViewAdapter.getSelectedNodes());
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(!instanceDatas.isEmpty());

            Assert.assertTrue(Stream.of(instanceDatas).allMatch(instanceData -> (instanceData.Done == null)));

            if (instanceDatas.size() == 1) {
                GroupListLoader.InstanceData instanceData = instanceDatas.get(0);
                Assert.assertTrue(instanceData != null);

                menu.findItem(R.id.action_group_edit_instance).setVisible(instanceData.IsRootInstance);
                menu.findItem(R.id.action_group_show_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_edit_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_join).setVisible(false);
                menu.findItem(R.id.action_group_delete_task).setVisible(instanceData.TaskCurrent);
            } else {
                Assert.assertTrue(instanceDatas.size() > 1);

                menu.findItem(R.id.action_group_edit_instance).setVisible(Stream.of(instanceDatas)
                        .allMatch(instanceData -> instanceData.IsRootInstance));
                menu.findItem(R.id.action_group_show_task).setVisible(false);
                menu.findItem(R.id.action_group_edit_task).setVisible(false);

                if (Stream.of(instanceDatas).allMatch(instanceData -> instanceData.TaskCurrent)) {
                    menu.findItem(R.id.action_group_join).setVisible(true);
                    menu.findItem(R.id.action_group_delete_task).setVisible(true);
                } else {
                    menu.findItem(R.id.action_group_join).setVisible(false);
                    menu.findItem(R.id.action_group_delete_task).setVisible(false);
                }
            }
        }
    };

    public static GroupListFragment getGroupInstance(MainActivity.TimeRange timeRange, int position) {
        Assert.assertTrue(timeRange != null);
        Assert.assertTrue(position >= 0);

        GroupListFragment groupListFragment = new GroupListFragment();
        Bundle args = new Bundle();
        args.putInt(POSITION_KEY, position);
        args.putSerializable(TIME_RANGE_KEY, timeRange);
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

        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        Bundle args = getArguments();
        if (args != null) {
            Assert.assertTrue(args.containsKey(POSITION_KEY));
            Assert.assertTrue(args.containsKey(TIME_RANGE_KEY));

            int position = args.getInt(POSITION_KEY);

            MainActivity.TimeRange timeRange = (MainActivity.TimeRange) args.getSerializable(TIME_RANGE_KEY);
            Assert.assertTrue(timeRange != null);

            Assert.assertTrue(position >= 0);

            setAll(timeRange, position);
        }
    }

    @Override
    public void onResume() {
        EventBuffer.getInstance().add("GroupListFragment onResume");

        super.onResume();
    }

    private void setAll(MainActivity.TimeRange timeRange, int position) {
        Assert.assertTrue(timeRange != null);

        Assert.assertTrue(mPosition == null);
        Assert.assertTrue(mTimeRange == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(position >= 0);

        mPosition = position;
        mTimeRange = timeRange;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setTimeStamp(TimeStamp timeStamp) {
        Assert.assertTrue(mPosition == null);
        Assert.assertTrue(mTimeRange == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(timeStamp != null);
        mTimeStamp = timeStamp;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKey(InstanceKey instanceKey) {
        Assert.assertTrue(mPosition == null);
        Assert.assertTrue(mTimeRange == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKey != null);
        mInstanceKey = instanceKey;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKeys(ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(mPosition == null);
        Assert.assertTrue(mTimeRange == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKeys != null);
        mInstanceKeys = instanceKeys;

        getLoaderManager().initLoader(0, null, this);
    }

    private boolean useGroups() {
        Assert.assertTrue((mPosition == null) == (mTimeRange == null));
        return (mPosition != null);
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTreeViewAdapter != null) {
            outState.putParcelable(EXPANSION_STATE_KEY, ((GroupAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpansionState());

            if (mSelectionCallback.hasActionMode()) {
                List<GroupListLoader.InstanceData> instanceDatas = nodesToInstanceDatas(mTreeViewAdapter.getSelectedNodes());
                Assert.assertTrue(instanceDatas != null);
                Assert.assertTrue(!instanceDatas.isEmpty());

                ArrayList<InstanceKey> instanceKeys = Stream.of(instanceDatas)
                        .map(instanceData -> instanceData.InstanceKey)
                        .collect(Collectors.toCollection(ArrayList::new));

                Assert.assertTrue(instanceKeys != null);
                Assert.assertTrue(!instanceKeys.isEmpty());
                outState.putParcelableArrayList(SELECTED_NODES_KEY, instanceKeys);
            }
        }
    }

    @Override
    public Loader<GroupListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(getActivity(), mTimeStamp, mInstanceKey, mInstanceKeys, mPosition, mTimeRange);
    }

    @Override
    public void onLoadFinished(Loader<GroupListLoader.Data> loader, GroupListLoader.Data data) {
        if (mData != null) {
            HashSet<InstanceKey> instanceKeys = new HashSet<>();
            instanceKeys.addAll(mData.InstanceDatas.keySet());
            instanceKeys.addAll(data.InstanceDatas.keySet());

            for (InstanceKey instanceKey : instanceKeys) {
                if (!mData.InstanceDatas.keySet().contains(instanceKey)) {
                    Log.e("asdf", data.InstanceDatas.get(instanceKey).Name + " missing from mData");
                    continue;
                }

                if (!data.InstanceDatas.keySet().contains(instanceKey)) {
                    Log.e("asdf", mData.InstanceDatas.get(instanceKey).Name + " missing from data");
                    continue;
                }

                if (!mData.InstanceDatas.get(instanceKey).equals(data.InstanceDatas.get(instanceKey))) {
                    Log.e("asdf", "here be dragons " + mData.InstanceDatas.get(instanceKey).equals(data.InstanceDatas.get(instanceKey)));
                }
            }
        }

        mData = data;

        if (mTreeViewAdapter != null) {
            mExpansionState = (((GroupAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpansionState());

            List<GroupListLoader.InstanceData> instanceDatas = nodesToInstanceDatas(mTreeViewAdapter.getSelectedNodes());
            Assert.assertTrue(instanceDatas != null);

            ArrayList<InstanceKey> instanceKeys = Stream.of(instanceDatas)
                    .map(instanceData -> instanceData.InstanceKey)
                    .collect(Collectors.toCollection(ArrayList::new));

            Assert.assertTrue(instanceKeys != null);

            if (instanceKeys.isEmpty()) {
                Assert.assertTrue(!mSelectionCallback.hasActionMode());
                mSelectedNodes = null;
            } else {
                Assert.assertTrue(mSelectionCallback.hasActionMode());
                mSelectedNodes = instanceKeys;
            }
        }

        boolean showFab;
        Activity activity = getActivity();
        Integer emptyTextId;
        if (mPosition != null) {
            Assert.assertTrue(mTimeRange != null);

            Assert.assertTrue(mTimeStamp == null);
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(data.TaskEditable == null);

            showFab = true;
            mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateRootTaskActivity.getCreateIntent(activity, rangePositionToDate(mTimeRange, mPosition))));

            emptyTextId = R.string.instances_empty_root;
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

            emptyTextId = null;
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(data.TaskEditable != null);

            if (data.TaskEditable) {
                showFab = true;
                mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateChildTaskActivity.getCreateIntent(activity, mInstanceKey.TaskId)));

                emptyTextId = R.string.empty_child;
            } else {
                showFab = false;

                emptyTextId = R.string.empty_disabled;
            }
        } else {
            Assert.assertTrue(mInstanceKeys != null);
            Assert.assertTrue(!mInstanceKeys.isEmpty());
            Assert.assertTrue(data.TaskEditable == null);

            showFab = false;

            emptyTextId = null;
        }

        if (mFirst) {
            mFloatingActionButton.setVisibility(showFab ? View.VISIBLE : View.GONE);
            mFirst = false;
        }

        mTreeViewAdapter = GroupAdapter.getAdapter(this, data.DataId, data.CustomTimeDatas, useGroups(), showFab, data.InstanceDatas.values(), mExpansionState, mSelectedNodes);
        Assert.assertTrue(mTreeViewAdapter != null);

        mGroupListRecycler.setAdapter(mTreeViewAdapter);

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        if (data.InstanceDatas.isEmpty()) {
            Assert.assertTrue(emptyTextId != null);

            mGroupListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(emptyTextId);
        } else {
            mGroupListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }

        if (mPosition != null && mPosition.equals(0)) { // 24 hack
            Assert.assertTrue(mTimeRange != null);
            // relevant hack
            PruneService.startService(getActivity());
        }
    }

    @Override
    public void onLoaderReset(Loader<GroupListLoader.Data> loader) {
    }

    public static class GroupAdapter implements TreeModelAdapter, NodeCollectionParent {
        private static final int TYPE_GROUP = 0;

        private final WeakReference<GroupListFragment> mGroupListFragmentReference;

        private final int mDataId;
        private final List<GroupListLoader.CustomTimeData> mCustomTimeDatas;
        private final boolean mShowFab;

        private WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

        private NodeCollection mNodeCollection;

        public static TreeViewAdapter getAdapter(GroupListFragment groupListFragment, int dataId, List<GroupListLoader.CustomTimeData> customTimeDatas, boolean useGroups, boolean showFab, Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);

            GroupAdapter groupAdapter = new GroupAdapter(groupListFragment, dataId, customTimeDatas, showFab);

            return groupAdapter.initialize(useGroups, instanceDatas, expansionState, selectedNodes);
        }

        private GroupAdapter(GroupListFragment groupListFragment, int dataId, List<GroupListLoader.CustomTimeData> customTimeDatas, boolean showFab) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);

            mGroupListFragmentReference = new WeakReference<>(groupListFragment);
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mShowFab = showFab;
        }

        private TreeViewAdapter initialize(boolean useGroups, Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes) {
            Assert.assertTrue(instanceDatas != null);

            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(mShowFab, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));

            mNodeCollection = new NodeCollection(new WeakReference<>(this), useGroups, new WeakReference<>(treeNodeCollection));

            treeNodeCollection.setNodes(mNodeCollection.initialize(treeViewAdapter, instanceDatas, expansionState, selectedNodes));

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            return treeViewAdapter;
        }

        @Override
        public AbstractHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_GROUP) {
                LinearLayout groupRow = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list, parent, false);

                TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
                TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
                TextView groupRowChildren = (TextView) groupRow.findViewById(R.id.group_row_children);
                ImageView groupRowExpand = (ImageView) groupRow.findViewById(R.id.group_row_expand);
                CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);
                View groupRowSeparator = groupRow.findViewById(R.id.group_row_separator);

                return new GroupHolder(groupRow, groupRowName, groupRowDetails, groupRowChildren, groupRowExpand, groupCheckBox, groupRowSeparator);
            } else {
                Assert.assertTrue(viewType == TreeViewAdapter.TYPE_FAB_PADDING);

                FrameLayout frameLayout = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list_fab_padding, parent, false);
                return new FabPaddingHolder(frameLayout);
            }
        }

        @Override
        public void onBindViewHolder(AbstractHolder abstractHolder, int position) {
            Assert.assertTrue(position >= 0);

            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            Assert.assertTrue(position < treeViewAdapter.getItemCount());

            if (position < treeViewAdapter.displayedSize()) {
                TreeNode treeNode = treeViewAdapter.getNode(position);
                treeNode.onBindViewHolder(abstractHolder);
            } else {
                Assert.assertTrue(position == treeViewAdapter.displayedSize());
                Assert.assertTrue(mShowFab);
                Assert.assertTrue(position == treeViewAdapter.getItemCount() - 1);
            }
        }

        @Override
        public SelectionCallback getSelectionCallback() {
            GroupListFragment groupListFragment = mGroupListFragmentReference.get();
            Assert.assertTrue(groupListFragment != null);

            return groupListFragment.mSelectionCallback;
        }

        private GroupListFragment getGroupListFragment() {
            GroupListFragment groupListFragment = mGroupListFragmentReference.get();
            Assert.assertTrue(groupListFragment != null);

            return groupListFragment;
        }

        public ExpansionState getExpansionState() {
            return mNodeCollection.getExpansionState();
        }

        @Override
        public GroupAdapter getGroupAdapter() {
            return this;
        }

        public TreeViewAdapter getTreeViewAdapter() {
            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            return treeViewAdapter;
        }

        public static abstract class AbstractHolder extends RecyclerView.ViewHolder {
            public AbstractHolder(View view) {
                super(view);
            }
        }

        public static class GroupHolder extends AbstractHolder {
            public final LinearLayout mGroupRow;
            public final TextView mGroupRowName;
            public final TextView mGroupRowDetails;
            public final TextView mGroupRowChildren;
            public final ImageView mGroupRowExpand;
            public final CheckBox mGroupRowCheckBox;
            public final View mGroupRowSeparator;

            public GroupHolder(LinearLayout groupRow, TextView groupRowName, TextView groupRowDetails, TextView groupRowChildren, ImageView groupRowExpand, CheckBox groupRowCheckBox, View groupRowSeparator) {
                super(groupRow);

                Assert.assertTrue(groupRowName != null);
                Assert.assertTrue(groupRowDetails != null);
                Assert.assertTrue(groupRowChildren != null);
                Assert.assertTrue(groupRowExpand != null);
                Assert.assertTrue(groupRowCheckBox != null);
                Assert.assertTrue(groupRowSeparator != null);

                mGroupRow = groupRow;
                mGroupRowName = groupRowName;
                mGroupRowDetails = groupRowDetails;
                mGroupRowChildren = groupRowChildren;
                mGroupRowExpand = groupRowExpand;
                mGroupRowCheckBox = groupRowCheckBox;
                mGroupRowSeparator = groupRowSeparator;
            }
        }

        public static class DividerHolder extends AbstractHolder {
            public final ImageView GroupListDividerImage;

            DividerHolder(RelativeLayout rowGroupListDivider, ImageView groupListDividerImage) {
                super(rowGroupListDivider);

                Assert.assertTrue(groupListDividerImage != null);

                GroupListDividerImage = groupListDividerImage;
            }
        }

        public static class FabPaddingHolder extends AbstractHolder {
            FabPaddingHolder(FrameLayout frameLayout) {
                super(frameLayout);
            }
        }

        public static class NodeCollection {
            private final WeakReference<NodeCollectionParent> mNodeCollectionParentReference;
            private final WeakReference<NodeContainer> mNodeContainerReference;

            private NotDoneGroupCollection mNotDoneGroupCollection;
            private DividerNode mDividerNode;

            private final boolean mUseGroups;

            private NodeCollection(WeakReference<NodeCollectionParent> nodeCollectionParentReference, boolean useGroups, WeakReference<NodeContainer> nodeContainerReference) {
                Assert.assertTrue(nodeCollectionParentReference != null);
                Assert.assertTrue(nodeContainerReference != null);

                mNodeCollectionParentReference = nodeCollectionParentReference;
                mUseGroups = useGroups;
                mNodeContainerReference = nodeContainerReference;
            }

            private List<TreeNode> initialize(TreeViewAdapter treeViewAdapter, Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes) {
                Assert.assertTrue(treeViewAdapter != null);
                Assert.assertTrue(instanceDatas != null);

                ArrayList<GroupListLoader.InstanceData> notDoneInstanceDatas = new ArrayList<>();
                ArrayList<GroupListLoader.InstanceData> doneInstanceDatas = new ArrayList<>();
                for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                    if (instanceData.Done == null)
                        notDoneInstanceDatas.add(instanceData);
                    else
                        doneInstanceDatas.add(instanceData);
                }

                boolean doneExpanded = false;
                ArrayList<TimeStamp> expandedGroups = null;
                if (expansionState != null) {
                    doneExpanded = expansionState.DoneExpanded;
                    expandedGroups = expansionState.ExpandedGroups;
                }

                mNotDoneGroupCollection = new NotDoneGroupCollection(new WeakReference<>(this), mNodeContainerReference);

                List<TreeNode> rootTreeNodes = mNotDoneGroupCollection.initialize(notDoneInstanceDatas, expandedGroups, selectedNodes);
                Assert.assertTrue(rootTreeNodes != null);

                mDividerNode = new DividerNode(new WeakReference<>(this));

                TreeNode dividerTreeNode = mDividerNode.initialize(doneExpanded, mNodeContainerReference, doneInstanceDatas);
                Assert.assertTrue(dividerTreeNode != null);

                rootTreeNodes.add(dividerTreeNode);

                return rootTreeNodes;
            }

            private NodeCollectionParent getNodeCollectionParent() {
                NodeCollectionParent nodeCollectionParent = mNodeCollectionParentReference.get();
                Assert.assertTrue(nodeCollectionParent != null);

                return nodeCollectionParent;
            }

            private NodeContainer getNodeContainer() {
                NodeContainer nodeContainer = mNodeContainerReference.get();
                Assert.assertTrue(nodeContainer != null);

                return nodeContainer;
            }

            private GroupAdapter getGroupAdapter() {
                NodeCollectionParent nodeCollectionParent = getNodeCollectionParent();
                Assert.assertTrue(nodeCollectionParent != null);

                GroupAdapter groupAdapter = nodeCollectionParent.getGroupAdapter();
                Assert.assertTrue(groupAdapter != null);

                return groupAdapter;
            }

            private TreeViewAdapter getTreeViewAdapter() {
                GroupAdapter groupAdapter = getGroupAdapter();
                Assert.assertTrue(groupAdapter != null);

                TreeViewAdapter treeViewAdapter = groupAdapter.getTreeViewAdapter();
                Assert.assertTrue(treeViewAdapter != null);

                return treeViewAdapter;
            }

            private TreeNodeCollection getTreeNodeCollection() {
                TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
                Assert.assertTrue(treeViewAdapter != null);

                TreeNodeCollection treeNodeCollection = treeViewAdapter.getTreeNodeCollection();
                Assert.assertTrue(treeNodeCollection != null);

                return treeNodeCollection;
            }

            private GroupListFragment getGroupListFragment() {
                GroupAdapter groupAdapter = getGroupAdapter();
                Assert.assertTrue(groupAdapter != null);

                GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();
                Assert.assertTrue(groupListFragment != null);

                return groupListFragment;
            }

            public ExpansionState getExpansionState() {
                ArrayList<TimeStamp> expandedGroups = mNotDoneGroupCollection.getExpandedGroups();

                TreeNode dividerTreeNode = mDividerNode.mDividerTreeNodeReference.get();
                Assert.assertTrue(dividerTreeNode != null);

                return new GroupListFragment.ExpansionState(dividerTreeNode.expanded(), expandedGroups);
            }

            public static class NotDoneGroupCollection {
                private final WeakReference<NodeCollection> mNodeCollectionReference;
                private final WeakReference<NodeContainer> mNodeContainerReference;

                private final ArrayList<NotDoneGroupNode> mNotDoneGroupNodes = new ArrayList<>();

                private NotDoneGroupCollection(WeakReference<NodeCollection> nodeCollectionReference, WeakReference<NodeContainer> nodeContainerReference) {
                    Assert.assertTrue(nodeCollectionReference != null);
                    Assert.assertTrue(nodeContainerReference != null);

                    mNodeCollectionReference = nodeCollectionReference;
                    mNodeContainerReference = nodeContainerReference;
                }

                private List<TreeNode> initialize(List<GroupListLoader.InstanceData> notDoneInstanceDatas, ArrayList<TimeStamp> expandedGroups, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(notDoneInstanceDatas != null);

                    ArrayList<TreeNode> notDoneGroupTreeNodes = new ArrayList<>();

                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    if (nodeCollection.mUseGroups) {
                        HashMap<TimeStamp, ArrayList<GroupListLoader.InstanceData>> instanceDataHash = new HashMap<>();
                        for (GroupListLoader.InstanceData instanceData : notDoneInstanceDatas) {
                            if (!instanceDataHash.containsKey(instanceData.InstanceTimeStamp))
                                instanceDataHash.put(instanceData.InstanceTimeStamp, new ArrayList<>());
                            instanceDataHash.get(instanceData.InstanceTimeStamp).add(instanceData);
                        }

                        for (Map.Entry<TimeStamp, ArrayList<GroupListLoader.InstanceData>> entry : instanceDataHash.entrySet()) {
                            boolean expanded = false;
                            if (entry.getValue().size() > 1 && expandedGroups != null && expandedGroups.contains(entry.getKey()))
                                expanded = true;

                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), entry.getValue(), expanded, selectedNodes);
                            Assert.assertTrue(notDoneGroupTreeNode != null);

                            notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
                        }
                    } else {
                        for (GroupListLoader.InstanceData instanceData : notDoneInstanceDatas) {
                            ArrayList<GroupListLoader.InstanceData> dummyInstanceDatas = new ArrayList<>();
                            dummyInstanceDatas.add(instanceData);

                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), dummyInstanceDatas, false, selectedNodes);
                            Assert.assertTrue(notDoneGroupTreeNode != null);

                            notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
                        }
                    }

                    return notDoneGroupTreeNodes;
                }

                public void remove(NotDoneGroupNode notDoneGroupNode) {
                    Assert.assertTrue(notDoneGroupNode != null);

                    Assert.assertTrue(mNotDoneGroupNodes.contains(notDoneGroupNode));
                    mNotDoneGroupNodes.remove(notDoneGroupNode);

                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    NodeContainer nodeContainer = getNodeContainer();
                    Assert.assertTrue(nodeContainer != null);

                    TreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    nodeContainer.remove(notDoneGroupTreeNode);
                }

                public void add(GroupListLoader.InstanceData instanceData) {
                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    NodeContainer nodeContainer = nodeCollection.getNodeContainer();
                    Assert.assertTrue(nodeContainer != null);

                    ExactTimeStamp exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();

                    List<NotDoneGroupNode> timeStampNotDoneGroupNodes = Stream.of(mNotDoneGroupNodes)
                            .filter(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.equals(exactTimeStamp))
                            .collect(Collectors.toList());

                    if (timeStampNotDoneGroupNodes.isEmpty() || !nodeCollection.mUseGroups) {
                        ArrayList<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
                        instanceDatas.add(instanceData);

                        TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), instanceDatas, false, null);
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        nodeContainer.add(notDoneGroupTreeNode);
                    } else {
                        Assert.assertTrue(timeStampNotDoneGroupNodes.size() == 1);

                        NotDoneGroupNode notDoneGroupNode = timeStampNotDoneGroupNodes.get(0);
                        Assert.assertTrue(notDoneGroupNode != null);

                        notDoneGroupNode.addInstanceData(instanceData);
                    }
                }

                private TreeNode newNotDoneGroupNode(WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference, List<GroupListLoader.InstanceData> instanceDatas, boolean expanded, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(notDoneGroupCollectionReference != null);
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(notDoneGroupCollectionReference, instanceDatas);

                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    TreeNode notDoneGroupTreeNode = notDoneGroupNode.initialize(expanded, selectedNodes, mNodeContainerReference);
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    mNotDoneGroupNodes.add(notDoneGroupNode);

                    return notDoneGroupTreeNode;
                }

                private NodeCollection getNodeCollection() {
                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    return nodeCollection;
                }

                private NodeContainer getNodeContainer() {
                    NodeContainer nodeContainer = mNodeContainerReference.get();
                    Assert.assertTrue(nodeContainer != null);

                    return nodeContainer;
                }

                private GroupAdapter getGroupAdapter() {
                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    return groupAdapter;
                }

                public ArrayList<TimeStamp> getExpandedGroups() {
                    return Stream.of(mNotDoneGroupNodes)
                            .filter(NotDoneGroupNode::expanded)
                            .map(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.toTimeStamp())
                            .collect(Collectors.toCollection(ArrayList::new));
                }
            }

            private static abstract class GroupHolderNode {
                abstract int getNameVisibility();
                abstract String getName();
                abstract int getNameColor();

                abstract int getDetailsVisibility();
                abstract String getDetails();
                abstract int getDetailsColor();

                abstract int getChildrenVisibility();
                abstract String getChildren();
                abstract int getChildrenColor();

                abstract int getExpandVisibility();
                abstract int getExpandImageResource();
                abstract View.OnClickListener getExpandOnClickListener();

                abstract int getCheckBoxVisibility();
                abstract boolean getCheckBoxChecked();
                abstract View.OnClickListener getCheckBoxOnClickListener();

                abstract int getSeparatorVisibility();

                abstract int getBackgroundColor();

                abstract View.OnLongClickListener getOnLongClickListener();
                abstract View.OnClickListener getOnClickListener();

                public final void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) abstractHolder;

                    int nameVisibility = getNameVisibility();
                    //noinspection ResourceType
                    groupHolder.mGroupRowName.setVisibility(nameVisibility);
                    if (nameVisibility == View.VISIBLE) {
                        groupHolder.mGroupRowName.setText(getName());
                        groupHolder.mGroupRowName.setTextColor(getNameColor());
                    }

                    int detailsVisibility = getDetailsVisibility();
                    //noinspection ResourceType
                    groupHolder.mGroupRowDetails.setVisibility(detailsVisibility);
                    if (detailsVisibility == View.VISIBLE) {
                        groupHolder.mGroupRowDetails.setText(getDetails());
                        groupHolder.mGroupRowDetails.setTextColor(getDetailsColor());
                    }

                    int childrenVisibility = getChildrenVisibility();
                    //noinspection ResourceType
                    groupHolder.mGroupRowChildren.setVisibility(childrenVisibility);
                    if (childrenVisibility == View.VISIBLE) {
                        groupHolder.mGroupRowChildren.setText(getChildren());
                        groupHolder.mGroupRowChildren.setTextColor(getChildrenColor());
                    }

                    int expandVisibility = getExpandVisibility();
                    //noinspection ResourceType
                    groupHolder.mGroupRowExpand.setVisibility(expandVisibility);
                    if (expandVisibility == View.VISIBLE) {
                        groupHolder.mGroupRowExpand.setImageResource(getExpandImageResource());
                        groupHolder.mGroupRowExpand.setOnClickListener(getExpandOnClickListener());
                    }

                    int checkBoxVisibility = getCheckBoxVisibility();
                    //noinspection ResourceType
                    groupHolder.mGroupRowCheckBox.setVisibility(checkBoxVisibility);
                    if (checkBoxVisibility == View.VISIBLE) {
                        groupHolder.mGroupRowCheckBox.setChecked(getCheckBoxChecked());
                        groupHolder.mGroupRowCheckBox.setOnClickListener(getCheckBoxOnClickListener());
                    }

                    //noinspection ResourceType
                    groupHolder.mGroupRowSeparator.setVisibility(getSeparatorVisibility());

                    groupHolder.mGroupRow.setBackgroundColor(getBackgroundColor());

                    groupHolder.mGroupRow.setOnLongClickListener(getOnLongClickListener());

                    groupHolder.mGroupRow.setOnClickListener(getOnClickListener());
                }

                public final int getItemViewType() {
                    return TYPE_GROUP;
                }
            }

            public static class NotDoneGroupNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
                private final WeakReference<NotDoneGroupCollection> mNotDoneGroupCollectionReference;

                private WeakReference<TreeNode> mNotDoneGroupTreeNodeReference;

                private final List<GroupListLoader.InstanceData> mInstanceDatas;

                private final ArrayList<NotDoneInstanceNode> mNotDoneInstanceNodes = new ArrayList<>();
                private NodeCollection mNodeCollection;

                public final ExactTimeStamp mExactTimeStamp;

                private NotDoneGroupNode(WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference, List<GroupListLoader.InstanceData> instanceDatas) {
                    Assert.assertTrue(notDoneGroupCollectionReference != null);
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    mNotDoneGroupCollectionReference = notDoneGroupCollectionReference;
                    mInstanceDatas = instanceDatas;

                    mExactTimeStamp = instanceDatas.get(0).InstanceTimeStamp.toExactTimeStamp();
                    Assert.assertTrue(Stream.of(instanceDatas)
                            .allMatch(instanceData -> instanceData.InstanceTimeStamp.toExactTimeStamp().equals(mExactTimeStamp)));
                }

                public TreeNode initialize(boolean expanded, ArrayList<InstanceKey> selectedNodes, WeakReference<NodeContainer> nodeContainerReference) {
                    boolean selected = (mInstanceDatas.size() == 1 && selectedNodes != null && selectedNodes.contains(mInstanceDatas.get(0).InstanceKey));

                    TreeNode notDoneGroupTreeNode = new TreeNode(this, nodeContainerReference, expanded, selected);
                    mNotDoneGroupTreeNodeReference = new WeakReference<>(notDoneGroupTreeNode);

                    if (mInstanceDatas.size() == 1) {
                        mNodeCollection = new NodeCollection(new WeakReference<>(this), false, new WeakReference<>(notDoneGroupTreeNode));

                        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        notDoneGroupTreeNode.setChildTreeNodes(mNodeCollection.initialize(treeViewAdapter, mInstanceDatas.get(0).Children.values(), null, selectedNodes));
                    } else {
                        List<TreeNode> notDoneInstanceTreeNodes = Stream.of(mInstanceDatas)
                                .map(instanceData -> newChildTreeNode(instanceData, selectedNodes))
                                .collect(Collectors.toList());

                        notDoneGroupTreeNode.setChildTreeNodes(notDoneInstanceTreeNodes);
                    }

                    return notDoneGroupTreeNode;
                }

                public GroupListLoader.InstanceData getSingleInstanceData() {
                    Assert.assertTrue(mInstanceDatas.size() == 1);
                    return mInstanceDatas.get(0);
                }

                public boolean singleInstance() {
                    Assert.assertTrue(!mInstanceDatas.isEmpty());

                    return (mInstanceDatas.size() == 1);
                }

                @Override
                int getNameVisibility() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        return View.VISIBLE;
                    } else {
                        if (notDoneGroupTreeNode.expanded()) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    }
                }

                @Override
                String getName() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        return instanceData.Name;
                    } else {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        return Stream.of(mInstanceDatas)
                                .map(instanceData -> instanceData.Name)
                                .collect(Collectors.joining(", "));
                    }
                }

                private NotDoneGroupCollection getNotDoneGroupCollection() {
                    NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    return notDoneGroupCollection;
                }

                private NodeCollection getNodeCollection() {
                    NotDoneGroupCollection notDoneGroupCollection = getNotDoneGroupCollection();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    NodeCollection nodeCollection = notDoneGroupCollection.getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    return nodeCollection;
                }

                private TreeViewAdapter getTreeViewAdapter() {
                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    TreeViewAdapter treeViewAdapter = nodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    return treeViewAdapter;
                }

                @Override
                public GroupAdapter getGroupAdapter() {
                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    return groupAdapter;
                }

                private GroupListFragment getGroupListFragment() {
                    GroupAdapter groupAdapter = getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    return groupListFragment;
                }

                @Override
                int getNameColor() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (!instanceData.TaskCurrent) {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                        } else {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
                        }
                    } else {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
                    }
                }

                @Override
                int getDetailsVisibility() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (TextUtils.isEmpty(instanceData.DisplayText)) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        return View.VISIBLE;
                    }
               }

                @Override
                String getDetails() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        Assert.assertTrue(!TextUtils.isEmpty(instanceData.DisplayText));

                        return instanceData.DisplayText;
                    } else {
                        ExactTimeStamp exactTimeStamp = ((NotDoneGroupNode) notDoneGroupTreeNode.getModelNode()).mExactTimeStamp;

                        Date date = exactTimeStamp.getDate();
                        HourMinute hourMinute = exactTimeStamp.toTimeStamp().getHourMinute();

                        GroupListLoader.CustomTimeData customTimeData = getCustomTimeData(date.getDayOfWeek(), hourMinute);

                        String timeText;
                        if (customTimeData != null)
                            timeText = customTimeData.Name;
                        else
                            timeText = hourMinute.toString();

                        return date.getDisplayText(groupListFragment.getActivity()) + ", " + timeText;
                    }
                }

                @Override
                int getDetailsColor() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (!instanceData.TaskCurrent) {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                        } else {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                        }
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                    }
                }

                @Override
                int getChildrenVisibility() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (instanceData.Children.isEmpty()) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        return View.GONE;
                    }
                }

                @Override
                String getChildren() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    Assert.assertTrue(singleInstance());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    Assert.assertTrue(!instanceData.Children.isEmpty());

                    return Stream.of(instanceData.Children.values())
                            .sortBy(child -> child.InstanceKey.TaskId)
                            .map(child -> child.Name)
                            .collect(Collectors.joining(", "));
                }

                @Override
                int getChildrenColor() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(singleInstance());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    if (!instanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                    }
                }

                @Override
                int getExpandVisibility() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final NodeCollection nodeCollection = notDoneGroupCollection.mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (instanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelectedNodes().count() > 0)) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        if (groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelectedNodes().count() > 0)
                            return View.INVISIBLE;
                        else
                            return View.VISIBLE;
                    }
                }

                @Override
                int getExpandImageResource() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        Assert.assertTrue(!instanceData.Children.isEmpty());

                        if (notDoneGroupTreeNode.expanded())
                            return R.drawable.ic_expand_less_black_36dp;
                        else
                            return R.drawable.ic_expand_more_black_36dp;
                    } else {
                        Assert.assertTrue(!(groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelectedNodes().count() > 0));

                        if (notDoneGroupTreeNode.expanded())
                            return R.drawable.ic_expand_less_black_36dp;
                        else
                            return R.drawable.ic_expand_more_black_36dp;
                    }
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getExpandListener();
                }

                @Override
                int getCheckBoxVisibility() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        if (groupListFragment.mSelectionCallback.hasActionMode()) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        if (notDoneGroupTreeNode.expanded()) {
                            return View.GONE;
                        } else {
                            return View.INVISIBLE;
                        }
                    }
                }

                @Override
                boolean getCheckBoxChecked() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(singleInstance());

                    Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                    return false;
                }

                @Override
                View.OnClickListener getCheckBoxOnClickListener() {
                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(singleInstance());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                    return v -> {
                        instanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupAdapter.mDataId, instanceData.InstanceKey, true);
                        Assert.assertTrue(instanceData.Done != null);

                        instanceData.Exists = true;

                        TickService.startService(groupListFragment.getActivity());

                        nodeCollection.mDividerNode.add(instanceData);

                        notDoneGroupCollection.remove(this);
                    };
                }

                @Override
                int getSeparatorVisibility() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return (notDoneGroupTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        if (notDoneGroupTreeNode.isSelected())
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected);
                        else
                            return Color.TRANSPARENT;
                    } else {
                        return Color.TRANSPARENT;
                    }
                }

                @Override
                View.OnLongClickListener getOnLongClickListener() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getOnClickListener();
                }

                @Override
                public void onClick() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), instanceData.InstanceKey));
                    } else {
                        groupListFragment.getActivity().startActivity(ShowGroupActivity.getIntent(((NotDoneGroupNode) notDoneGroupTreeNode.getModelNode()).mExactTimeStamp, groupListFragment.getActivity()));
                    }
                }

                private GroupListLoader.CustomTimeData getCustomTimeData(DayOfWeek dayOfWeek, HourMinute hourMinute) {
                    Assert.assertTrue(dayOfWeek != null);
                    Assert.assertTrue(hourMinute != null);

                    NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    GroupAdapter groupAdapter = getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    for (GroupListLoader.CustomTimeData customTimeData : groupAdapter.mCustomTimeDatas)
                        if (customTimeData.HourMinutes.get(dayOfWeek) == hourMinute)
                            return customTimeData;

                    return null;
                }

                public void remove(NotDoneInstanceNode notDoneInstanceNode) {
                    Assert.assertTrue(notDoneInstanceNode != null);

                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    Assert.assertTrue(mInstanceDatas.contains(notDoneInstanceNode.mInstanceData));
                    mInstanceDatas.remove(notDoneInstanceNode.mInstanceData);

                    Assert.assertTrue(mNotDoneInstanceNodes.contains(notDoneInstanceNode));
                    mNotDoneInstanceNodes.remove(notDoneInstanceNode);

                    TreeNode childTreeNode = notDoneInstanceNode.mChildTreeNodeReference.get();
                    Assert.assertTrue(childTreeNode != null);

                    notDoneGroupTreeNode.remove(childTreeNode);

                    Assert.assertTrue(!mInstanceDatas.isEmpty());
                    if (mInstanceDatas.size() == 1) {
                        Assert.assertTrue(mNotDoneInstanceNodes.size() == 1);

                        NotDoneInstanceNode notDoneInstanceNode1 = mNotDoneInstanceNodes.get(0);

                        TreeNode childTreeNode1 = notDoneInstanceNode1.mChildTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode1 != null);

                        mNotDoneInstanceNodes.remove(notDoneInstanceNode1);

                        boolean selected = childTreeNode1.isSelected();

                        notDoneGroupTreeNode.remove(childTreeNode1);

                        if (selected)
                            notDoneGroupTreeNode.select();
                    }
                }

                @Override
                public int compareTo(@NonNull ModelNode another) {
                    if (another instanceof DividerNode)
                        return -1;

                    NotDoneGroupNode notDoneGroupNode = (NotDoneGroupNode) another;

                    int timeStampComparison = mExactTimeStamp.compareTo(notDoneGroupNode.mExactTimeStamp);
                    if (timeStampComparison != 0) {
                        return timeStampComparison;
                    } else {
                        Assert.assertTrue(singleInstance());
                        Assert.assertTrue(notDoneGroupNode.singleInstance());

                        return Integer.valueOf(getSingleInstanceData().InstanceKey.TaskId).compareTo(notDoneGroupNode.getSingleInstanceData().InstanceKey.TaskId);
                    }
                }

                public void addInstanceData(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(instanceData.InstanceTimeStamp.toExactTimeStamp().equals(mExactTimeStamp));

                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    Assert.assertTrue(!mInstanceDatas.isEmpty());
                    if (mInstanceDatas.size() == 1) {
                        Assert.assertTrue(mNotDoneInstanceNodes.isEmpty());

                        GroupListLoader.InstanceData instanceData1 = mInstanceDatas.get(0);

                        GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(instanceData1, new WeakReference<>(NotDoneGroupNode.this));
                        mNotDoneInstanceNodes.add(notDoneInstanceNode);

                        TreeNode childTreeNode = notDoneInstanceNode.initialize(null, mNotDoneGroupTreeNodeReference);

                        notDoneGroupTreeNode.add(childTreeNode);
                    }

                    mInstanceDatas.add(instanceData);

                    TreeNode childTreeNode = newChildTreeNode(instanceData, null);
                    Assert.assertTrue(childTreeNode != null);

                    notDoneGroupTreeNode.add(childTreeNode);
                }

                public TreeNode newChildTreeNode(GroupListLoader.InstanceData instanceData, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(mNotDoneGroupTreeNodeReference != null);

                    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(instanceData, new WeakReference<>(this));

                    TreeNode childTreeNode = notDoneInstanceNode.initialize(selectedNodes, mNotDoneGroupTreeNodeReference);

                    mNotDoneInstanceNodes.add(notDoneInstanceNode);

                    return childTreeNode;
                }

                public boolean expanded() {
                    TreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.expanded();
                }

                @Override
                public boolean selectable() {
                    return mNotDoneInstanceNodes.isEmpty();
                }

                @Override
                public boolean visibleWhenEmpty() {
                    return true;
                }

                @Override
                public boolean visibleDuringActionMode() {
                    return true;
                }

                public TreeNode getTreeNode() {
                    TreeNode treeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                public void removeFromParent() {
                    NotDoneGroupCollection notDoneGroupCollection = getNotDoneGroupCollection();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    notDoneGroupCollection.remove(this);
                }

                public static class NotDoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
                    private final WeakReference<NotDoneGroupNode> mNotDoneGroupNodeReference;

                    private WeakReference<TreeNode> mChildTreeNodeReference;

                    public final GroupListLoader.InstanceData mInstanceData;

                    private NodeCollection mNodeCollection;

                    public NotDoneInstanceNode(GroupListLoader.InstanceData instanceData, WeakReference<NotDoneGroupNode> notDoneGroupNodeReference) {
                        Assert.assertTrue(instanceData != null);
                        Assert.assertTrue(notDoneGroupNodeReference != null);

                        mInstanceData = instanceData;
                        mNotDoneGroupNodeReference = notDoneGroupNodeReference;
                    }

                    public TreeNode initialize(ArrayList<InstanceKey> selectedNodes, WeakReference<TreeNode> notDoneGroupTreeNodeReference) {
                        Assert.assertTrue(notDoneGroupTreeNodeReference != null);

                        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        TreeNode notDoneGroupTreeNode = notDoneGroupTreeNodeReference.get();

                        TreeNode childTreeNode = new TreeNode(this, new WeakReference<>(notDoneGroupTreeNode), false, selectedNodes != null && selectedNodes.contains(mInstanceData.InstanceKey));
                        mChildTreeNodeReference = new WeakReference<>(childTreeNode);

                        mNodeCollection = new NodeCollection(new WeakReference<>(this), false, new WeakReference<>(childTreeNode));
                        childTreeNode.setChildTreeNodes(mNodeCollection.initialize(treeViewAdapter, mInstanceData.Children.values(), null, selectedNodes));

                        return childTreeNode;
                    }

                    private TreeNode getTreeNode() {
                        TreeNode treeNode = mChildTreeNodeReference.get();
                        Assert.assertTrue(treeNode != null);

                        return treeNode;
                    }

                    private NotDoneGroupNode getParentNotDoneGroupNode() {
                        NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        return notDoneGroupNode;
                    }

                    private NotDoneGroupCollection getParentNotDoneGroupCollection() {
                        NotDoneGroupNode notDoneGroupNode = getParentNotDoneGroupNode();
                        Assert.assertTrue(notDoneGroupNode != null);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.getNotDoneGroupCollection();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        return notDoneGroupCollection;
                    }

                    private NodeCollection getParentNodeCollection() {
                        NotDoneGroupCollection notDoneGroupCollection = getParentNotDoneGroupCollection();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        NodeCollection nodeCollection = notDoneGroupCollection.getNodeCollection();
                        Assert.assertTrue(nodeCollection != null);

                        return nodeCollection;
                    }

                    private TreeViewAdapter getTreeViewAdapter() {
                        NodeCollection nodeCollection = getParentNodeCollection();
                        Assert.assertTrue(nodeCollection != null);

                        TreeViewAdapter treeViewAdapter = nodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        return treeViewAdapter;
                    }

                    @Override
                    public GroupAdapter getGroupAdapter() {
                        NotDoneGroupNode notDoneGroupNode = getParentNotDoneGroupNode();
                        Assert.assertTrue(notDoneGroupNode != null);

                        GroupAdapter groupAdapter = notDoneGroupNode.getGroupAdapter();
                        Assert.assertTrue(groupAdapter != null);

                        return groupAdapter;
                    }

                    private GroupListFragment getGroupListFragment() {
                        GroupAdapter groupAdapter = getGroupAdapter();
                        Assert.assertTrue(groupAdapter != null);

                        GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        return groupListFragment;
                    }

                    @Override
                    int getNameVisibility() {
                        return View.VISIBLE;
                    }

                    @Override
                    String getName() {
                        return mInstanceData.Name;
                    }

                    @Override
                    int getNameColor() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        if (!mInstanceData.TaskCurrent) {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                        } else {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
                        }
                    }

                    @Override
                    int getDetailsVisibility() {
                        return View.GONE;
                    }

                    @Override
                    String getDetails() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    int getDetailsColor() {
                        throw new UnsupportedOperationException();
                    }

                    @Override
                    int getChildrenVisibility() {
                        if (mInstanceData.Children.isEmpty()) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    String getChildren() {
                        Assert.assertTrue(!mInstanceData.Children.isEmpty());
                        return Stream.of(mInstanceData.Children.values())
                                .sortBy(child -> child.InstanceKey.TaskId)
                                .map(child -> child.Name)
                                .collect(Collectors.joining(", "));
                    }

                    @Override
                    int getChildrenColor() {
                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        if (!mInstanceData.TaskCurrent) {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                        } else {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                        }
                    }

                    @Override
                    int getExpandVisibility() {
                        TreeNode treeNode = getTreeNode();
                        Assert.assertTrue(treeNode != null);

                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        if (mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedNodes().count() > 0)) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    int getExpandImageResource() {
                        TreeNode treeNode = getTreeNode();
                        Assert.assertTrue(treeNode != null);

                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        Assert.assertTrue(!(mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedNodes().count() > 0)));

                        if (treeNode.expanded())
                            return R.drawable.ic_expand_less_black_36dp;
                        else
                            return R.drawable.ic_expand_more_black_36dp;
                    }

                    @Override
                    View.OnClickListener getExpandOnClickListener() {
                        TreeNode treeNode = getTreeNode();
                        Assert.assertTrue(treeNode != null);

                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        Assert.assertTrue(!(mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedNodes().count() > 0)));

                        return treeNode.getExpandListener();
                    }

                    @Override
                    int getCheckBoxVisibility() {
                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        if (groupListFragment.mSelectionCallback.hasActionMode()) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    boolean getCheckBoxChecked() {
                        return false;
                    }

                    @Override
                    View.OnClickListener getCheckBoxOnClickListener() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final TreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        NodeCollection nodeCollection = getParentNodeCollection();
                        Assert.assertTrue(nodeCollection != null);

                        GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                        Assert.assertTrue(groupAdapter != null);

                        GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                        return v -> {
                            Assert.assertTrue(notDoneGroupTreeNode.expanded());

                            TreeNode childTreeNode = mChildTreeNodeReference.get();
                            Assert.assertTrue(childTreeNode != null);

                            mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupAdapter.mDataId, mInstanceData.InstanceKey, true);
                            Assert.assertTrue(mInstanceData.Done != null);

                            mInstanceData.Exists = true;

                            TickService.startService(groupListFragment.getActivity());

                            notDoneGroupNode.remove(this);

                            nodeCollection.mDividerNode.add(mInstanceData);
                        };
                    }

                    @Override
                    int getSeparatorVisibility() {
                        TreeNode childTreeNode = mChildTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        return (childTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                    }

                    @Override
                    int getBackgroundColor() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final TreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        TreeNode childTreeNode = mChildTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        if (childTreeNode.isSelected())
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected);
                        else
                            return Color.TRANSPARENT;
                    }

                    @Override
                    View.OnLongClickListener getOnLongClickListener() {
                        TreeNode childTreeNode = mChildTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        return childTreeNode.getOnLongClickListener();
                    }

                    @Override
                    View.OnClickListener getOnClickListener() {
                        TreeNode childTreeNode = mChildTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        return childTreeNode.getOnClickListener();
                    }

                    @Override
                    public void onClick() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey));
                    }

                    @Override
                    public int compareTo(@NonNull ModelNode another) {
                        return Integer.valueOf(mInstanceData.InstanceKey.TaskId).compareTo(((NotDoneInstanceNode) another).mInstanceData.InstanceKey.TaskId);
                    }

                    public void removeFromParent() {
                        NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        notDoneGroupNode.remove(this);
                    }

                    @Override
                    public boolean selectable() {
                        return true;
                    }

                    @Override
                    public boolean visibleWhenEmpty() {
                        return true;
                    }

                    @Override
                    public boolean visibleDuringActionMode() {
                        return true;
                    }
                }
            }

            public static class DividerNode extends GroupHolderNode implements ModelNode {
                private final WeakReference<NodeCollection> mNodeCollectionReference;

                private WeakReference<TreeNode> mDividerTreeNodeReference;

                private final ArrayList<DoneInstanceNode> mDoneInstanceNodes = new ArrayList<>();

                private DividerNode(WeakReference<NodeCollection> nodeCollectionReference) {
                    Assert.assertTrue(nodeCollectionReference != null);

                    mNodeCollectionReference = nodeCollectionReference;
                }

                private TreeNode initialize(boolean doneExpanded, WeakReference<NodeContainer> nodeContainerReference, List<GroupListLoader.InstanceData> doneInstanceDatas) {
                    TreeNode dividerTreeNode = new TreeNode(this, nodeContainerReference, doneExpanded, false);
                    mDividerTreeNodeReference = new WeakReference<>(dividerTreeNode);

                    List<TreeNode> childTreeNodes = Stream.of(doneInstanceDatas)
                            .map(this::newChildTreeNode)
                            .collect(Collectors.toList());

                    dividerTreeNode.setChildTreeNodes(childTreeNodes);

                    return dividerTreeNode;
                }

                private TreeNode newChildTreeNode(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData.Done != null);

                    DoneInstanceNode doneInstanceNode = new DoneInstanceNode(instanceData, new WeakReference<>(this));

                    TreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    TreeNode childTreeNode = doneInstanceNode.initialize(dividerTreeNode);

                    mDoneInstanceNodes.add(doneInstanceNode);

                    return childTreeNode;
                }

                @Override
                int getNameVisibility() {
                    return View.VISIBLE;
                }

                @Override
                String getName() {
                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    return groupListFragment.getString(R.string.done);
                }

                @Override
                int getNameColor() {
                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
                }

                @Override
                int getDetailsVisibility() {
                    return View.GONE;
                }

                @Override
                String getDetails() {
                    throw new UnsupportedOperationException();
                }

                @Override
                int getDetailsColor() {
                    throw new UnsupportedOperationException();
                }

                @Override
                int getChildrenVisibility() {
                    return View.GONE;
                }

                @Override
                String getChildren() {
                    throw new UnsupportedOperationException();
                }

                @Override
                int getChildrenColor() {
                    throw new UnsupportedOperationException();
                }

                @Override
                int getExpandVisibility() {
                    return View.VISIBLE;
                }

                @Override
                int getExpandImageResource() {
                    TreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    if (dividerTreeNode.expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    TreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    return dividerTreeNode.getExpandListener();
                }

                @Override
                int getCheckBoxVisibility() {
                    return View.INVISIBLE;
                }

                @Override
                boolean getCheckBoxChecked() {
                    throw new UnsupportedOperationException();
                }

                @Override
                View.OnClickListener getCheckBoxOnClickListener() {
                    throw new UnsupportedOperationException();
                }

                @Override
                int getSeparatorVisibility() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return (treeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);

                }

                @Override
                int getBackgroundColor() {
                    return Color.TRANSPARENT;
                }

                @Override
                View.OnLongClickListener getOnLongClickListener() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return treeNode.getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return treeNode.getOnClickListener();
                }

                public void remove(DoneInstanceNode doneInstanceNode) {
                    Assert.assertTrue(doneInstanceNode != null);

                    TreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    Assert.assertTrue(mDoneInstanceNodes.contains(doneInstanceNode));
                    mDoneInstanceNodes.remove(doneInstanceNode);

                    TreeNode childTreeNode = doneInstanceNode.mChildTreeNodeReference.get();
                    Assert.assertTrue(childTreeNode != null);

                    dividerTreeNode.remove(childTreeNode);
                }

                public void add(GroupListLoader.InstanceData instanceData) {
                    TreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    TreeNode childTreeNode = newChildTreeNode(instanceData);
                    Assert.assertTrue(childTreeNode != null);

                    dividerTreeNode.add(childTreeNode);
                }

                private NodeCollection getNodeCollection() {
                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    return nodeCollection;
                }

                private GroupAdapter getGroupAdapter() {
                    NodeCollection nodeCollection = getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    return groupAdapter;
                }

                private GroupListFragment getGroupListFragment() {
                    GroupAdapter groupAdapter = getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    return groupListFragment;
                }

                @Override
                public boolean selectable() {
                    return false;
                }

                @Override
                public void onClick() {

                }

                @Override
                public int compareTo(@NonNull ModelNode another) {
                    Assert.assertTrue(another instanceof NotDoneGroupNode);
                    return 1;
                }

                @Override
                public boolean visibleWhenEmpty() {
                    return false;
                }

                @Override
                public boolean visibleDuringActionMode() {
                    return false;
                }

                public TreeNode getTreeNode() {
                    TreeNode treeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }
            }

            public static class DoneInstanceNode extends GroupHolderNode implements ModelNode {
                private final WeakReference<DividerNode> mDividerNodeReference;

                private WeakReference<TreeNode> mChildTreeNodeReference;

                private final GroupListLoader.InstanceData mInstanceData;

                public DoneInstanceNode(GroupListLoader.InstanceData instanceData, WeakReference<DividerNode> dividerNodeReference) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(dividerNodeReference != null);

                    mInstanceData = instanceData;
                    mDividerNodeReference = dividerNodeReference;
                    Assert.assertTrue(mDividerNodeReference.get() != null);
                }

                public TreeNode initialize(TreeNode dividerTreeNode) {
                    Assert.assertTrue(dividerTreeNode != null);

                    TreeNode doneTreeNode = new TreeNode(this, new WeakReference<>(dividerTreeNode), false, false);
                    mChildTreeNodeReference = new WeakReference<>(doneTreeNode);

                    doneTreeNode.setChildTreeNodes(new ArrayList<>());

                    return doneTreeNode;
                }

                @Override
                int getNameVisibility() {
                    return View.VISIBLE;
                }

                @Override
                String getName() {
                    return mInstanceData.Name;
                }

                @Override
                int getNameColor() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    GroupListFragment groupListFragment = dividerNode.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (!mInstanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
                    }
                }

                @Override
                int getDetailsVisibility() {
                    if (TextUtils.isEmpty(mInstanceData.DisplayText)) {
                        return View.GONE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                String getDetails() {
                    Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.DisplayText));
                    return mInstanceData.DisplayText;
                }

                @Override
                int getDetailsColor() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    GroupListFragment groupListFragment = dividerNode.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (!mInstanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                    }
                }

                @Override
                int getChildrenVisibility() {
                    if (mInstanceData.Children.isEmpty()) {
                        return View.GONE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                String getChildren() {
                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

                    return Stream.of(mInstanceData.Children.values())
                            .sortBy(child -> child.InstanceKey.TaskId)
                            .map(child -> child.Name)
                            .collect(Collectors.joining(", "));
                }

                @Override
                int getChildrenColor() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    GroupListFragment groupListFragment = dividerNode.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

                    if (!mInstanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                    }
                }

                @Override
                int getExpandVisibility() {
                    if (mInstanceData.Children.isEmpty()) {
                        return View.INVISIBLE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                int getExpandImageResource() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    GroupListFragment groupListFragment = dividerNode.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

                    return R.drawable.ic_list_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    Assert.assertTrue(!mInstanceData.Children.isEmpty());
                    return null;
                }

                @Override
                int getCheckBoxVisibility() {
                    return View.VISIBLE;
                }

                @Override
                boolean getCheckBoxChecked() {
                    return true;
                }

                @Override
                View.OnClickListener getCheckBoxOnClickListener() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    NodeCollection nodeCollection = dividerNode.getNodeCollection();
                    Assert.assertTrue(nodeCollection != null);

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    return v -> {
                        TreeNode childTreeNode = mChildTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupAdapter.mDataId, mInstanceData.InstanceKey, false);
                        Assert.assertTrue(mInstanceData.Done == null);

                        TickService.startService(groupListFragment.getActivity());

                        dividerNode.remove(this);

                        nodeCollection.mNotDoneGroupCollection.add(mInstanceData);
                    };
                }

                @Override
                int getSeparatorVisibility() {
                    TreeNode childTreeNode = mChildTreeNodeReference.get();
                    Assert.assertTrue(childTreeNode != null);

                    return (childTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    return Color.TRANSPARENT;
                }

                @Override
                View.OnLongClickListener getOnLongClickListener() {
                    TreeNode childTreeNode = mChildTreeNodeReference.get();
                    Assert.assertTrue(childTreeNode != null);

                    return childTreeNode.getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    TreeNode childTreeNode = mChildTreeNodeReference.get();
                    Assert.assertTrue(childTreeNode != null);

                    return childTreeNode.getOnClickListener();
                }

                @Override
                public int compareTo(@NonNull ModelNode another) {
                    return -mInstanceData.Done.compareTo(((DoneInstanceNode) another).mInstanceData.Done); // negate
                }

                @Override
                public boolean selectable() {
                    return false;
                }

                @Override
                public void onClick() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    GroupListFragment groupListFragment = dividerNode.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey));
                }

                @Override
                public boolean visibleWhenEmpty() {
                    return true;
                }

                @Override
                public boolean visibleDuringActionMode() {
                    return true;
                }
            }
        }
    }

    public static class ExpansionState implements Parcelable {
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

    private interface NodeCollectionParent {
        GroupAdapter getGroupAdapter();
    }

    private static Date rangePositionToDate(MainActivity.TimeRange timeRange, int position) {
        Assert.assertTrue(timeRange != null);
        Assert.assertTrue(position >= 0);

        Calendar calendar = Calendar.getInstance();

        if (position > 0) {
            switch (timeRange) {
                case DAY:
                    calendar.add(Calendar.DATE, position);
                    break;
                case WEEK:
                    calendar.add(Calendar.WEEK_OF_YEAR, position);
                    calendar.set(Calendar.DAY_OF_WEEK, calendar.getFirstDayOfWeek());
                    break;
                case MONTH:
                    calendar.add(Calendar.MONTH, position);
                    calendar.set(Calendar.DAY_OF_MONTH, 1);
            }
        }

        return new Date(calendar);
    }

    private static List<GroupListLoader.InstanceData> nodesToInstanceDatas(List<TreeNode> treeNodes) {
        Assert.assertTrue(treeNodes != null);

        List<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
        for (TreeNode treeNode : treeNodes) {
            if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                GroupListLoader.InstanceData instanceData = ((GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode()).getSingleInstanceData();
                Assert.assertTrue(instanceData != null);

                instanceDatas.add(instanceData);
            } else {
                Assert.assertTrue(treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode);

                GroupListLoader.InstanceData instanceData = ((GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode()).mInstanceData;
                Assert.assertTrue(instanceData != null);

                instanceDatas.add(instanceData);
            }
        }

        return instanceDatas;
    }
}