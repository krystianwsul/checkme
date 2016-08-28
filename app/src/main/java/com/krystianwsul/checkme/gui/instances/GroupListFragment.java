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
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.DataDiff;
import com.krystianwsul.checkme.MyCrashlytics;
import com.krystianwsul.checkme.PruneService;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity;
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity;
import com.krystianwsul.checkme.gui.tree.ModelNode;
import com.krystianwsul.checkme.gui.tree.NodeContainer;
import com.krystianwsul.checkme.gui.tree.TreeModelAdapter;
import com.krystianwsul.checkme.gui.tree.TreeNode;
import com.krystianwsul.checkme.gui.tree.TreeNodeCollection;
import com.krystianwsul.checkme.gui.tree.TreeViewAdapter;
import com.krystianwsul.checkme.loaders.GroupListLoader;
import com.krystianwsul.checkme.notifications.TickService;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.HashMap;
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
                case R.id.action_group_share:
                    Assert.assertTrue(instanceDatas.size() == 1);

                    Utils.share(instanceDatas.get(0).Name, getActivity());
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

                    startActivity(CreateTaskActivity.getEditIntent(getActivity(), instanceData3.InstanceKey.TaskId));
                    break;
                case R.id.action_group_delete_task: {
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(!taskIds.isEmpty());
                    Assert.assertTrue(Stream.of(instanceDatas)
                            .allMatch(instanceData -> instanceData.TaskCurrent));

                    List<TreeNode> selectedTreeNodes = mTreeViewAdapter.getSelectedNodes();
                    Assert.assertTrue(selectedTreeNodes != null);
                    Assert.assertTrue(!selectedTreeNodes.isEmpty());

                    do {
                        TreeNode treeNode = selectedTreeNodes.get(0);
                        Assert.assertTrue(treeNode != null);

                        recursiveDelete(treeNode, true);

                        decrementSelected();
                    } while (!(selectedTreeNodes = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(((GroupAdapter) mTreeViewAdapter.getTreeModelAdapter()).mDataId, taskIds);

                    TickService.startService(getActivity());

                    break;
                }
                case R.id.action_group_add_task:
                    Assert.assertTrue(instanceDatas.size() == 1);

                    GroupListLoader.InstanceData instanceData4 = instanceDatas.get(0);
                    Assert.assertTrue(instanceData4.TaskCurrent);

                    getActivity().startActivity(CreateTaskActivity.getCreateIntent(getActivity(), instanceData4.InstanceKey.TaskId));
                    break;
                case R.id.action_group_join:
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(taskIds.size() > 1);

                    if (mInstanceKey == null) {
                        GroupListLoader.InstanceData firstInstanceData = Stream.of(instanceDatas)
                                .min((lhs, rhs) -> lhs.InstanceTimeStamp.compareTo(rhs.InstanceTimeStamp))
                                .get();

                        Date date = firstInstanceData.InstanceTimeStamp.getDate();
                        Assert.assertTrue(date != null);

                        TimePair timePair = firstInstanceData.InstanceTimePair;

                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskIds, new CreateTaskActivity.ScheduleHint(date, timePair)));
                    } else {
                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskIds, mInstanceKey.TaskId));
                    }
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        private void recursiveDelete(TreeNode treeNode, boolean root) {
            Assert.assertTrue(treeNode != null);

            GroupListLoader.InstanceData instanceData1;
            if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                instanceData1 = ((GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode()).getSingleInstanceData();
            } else if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) {
                instanceData1 = ((GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode()).mInstanceData;
            } else if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.DoneInstanceNode) {
                instanceData1 = ((GroupAdapter.NodeCollection.DoneInstanceNode) treeNode.getModelNode()).mInstanceData;
            } else {
                Assert.assertTrue((treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.DividerNode));

                Stream.of(treeNode.getAllChildren())
                        .forEach(child -> recursiveDelete(child, false));

                return;
            }

            if (instanceData1.Exists || !root) {
                instanceData1.TaskCurrent = false;
                instanceData1.IsRootTask = null;
            } else {
                GroupListLoader.InstanceDataParent instanceDataParent = instanceData1.InstanceDataParentReference.get();
                Assert.assertTrue(instanceDataParent != null);

                instanceDataParent.remove(instanceData1.InstanceKey);
            }

            if (instanceData1.Exists || !root) {
                treeNode.unselect();

                treeNode.update();

                ArrayList<TreeNode> children = new ArrayList<>(treeNode.getAllChildren());
                Stream.of(children)
                        .forEach(child -> recursiveDelete(child, false));
            } else {
                if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                    GroupAdapter.NodeCollection.NotDoneGroupNode notDoneGroupNode = (GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode();
                    Assert.assertTrue(notDoneGroupNode != null);

                    notDoneGroupNode.removeFromParent();
                } else if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) {
                    GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = (GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode();
                    Assert.assertTrue(notDoneInstanceNode != null);

                    notDoneInstanceNode.removeFromParent();
                } else {
                    GroupAdapter.NodeCollection.DoneInstanceNode doneInstanceNode = (GroupAdapter.NodeCollection.DoneInstanceNode) treeNode.getModelNode();
                    Assert.assertTrue(doneInstanceNode != null);

                    doneInstanceNode.removeFromParent();
                }
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
                menu.findItem(R.id.action_group_share).setVisible(true);
                menu.findItem(R.id.action_group_show_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_edit_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_join).setVisible(false);
                menu.findItem(R.id.action_group_delete_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_add_task).setVisible(instanceData.TaskCurrent);
            } else {
                Assert.assertTrue(instanceDatas.size() > 1);

                menu.findItem(R.id.action_group_edit_instance).setVisible(Stream.of(instanceDatas)
                        .allMatch(instanceData -> instanceData.IsRootInstance));
                menu.findItem(R.id.action_group_share).setVisible(false);
                menu.findItem(R.id.action_group_show_task).setVisible(false);
                menu.findItem(R.id.action_group_edit_task).setVisible(false);
                menu.findItem(R.id.action_group_add_task).setVisible(false);

                if (Stream.of(instanceDatas).allMatch(instanceData -> instanceData.TaskCurrent)) {
                    menu.findItem(R.id.action_group_join).setVisible(true);
                    menu.findItem(R.id.action_group_delete_task).setVisible(!containsLoop(instanceDatas));
                } else {
                    menu.findItem(R.id.action_group_join).setVisible(false);
                    menu.findItem(R.id.action_group_delete_task).setVisible(false);
                }
            }
        }

        private boolean containsLoop(List<GroupListLoader.InstanceData> instanceDatas) {
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(instanceDatas.size() > 1);

            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                Assert.assertTrue(instanceData != null);

                List<GroupListLoader.InstanceData> parents = new ArrayList<>();
                addParents(parents, instanceData);

                for (GroupListLoader.InstanceData parent : parents) {
                    Assert.assertTrue(parent != null);

                    if (instanceDatas.contains(parent))
                        return true;
                }
            }

            return false;
        }

        private void addParents(List<GroupListLoader.InstanceData> parents, GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(parents != null);
            Assert.assertTrue(instanceData != null);

            GroupListLoader.InstanceDataParent instanceDataParent = instanceData.InstanceDataParentReference.get();
            Assert.assertTrue(instanceDataParent != null);

            if (!(instanceDataParent instanceof GroupListLoader.InstanceData))
                return;

            GroupListLoader.InstanceData parent = (GroupListLoader.InstanceData) instanceDataParent;

            parents.add(parent);
            addParents(parents, parent);
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
        MyCrashlytics.log("GroupListFragment.onResume");

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
            DataDiff.diffData(mData, data);
            Log.e("asdf", "difference w data:\n" + DataDiff.getDiff());
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
            mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateTaskActivity.getCreateIntent(activity, new CreateTaskActivity.ScheduleHint(rangePositionToDate(mTimeRange, mPosition)))));

            emptyTextId = R.string.instances_empty_root;
        } else if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(data.TaskEditable == null);

            if (mTimeStamp.compareTo(TimeStamp.getNow()) > 0) {
                showFab = true;
                mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateTaskActivity.getCreateIntent(activity, new CreateTaskActivity.ScheduleHint(mTimeStamp.getDate(), mTimeStamp.getHourMinute()))));
            } else {
                showFab = false;
            }

            emptyTextId = null;
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(data.TaskEditable != null);

            if (data.TaskEditable) {
                showFab = true;
                mFloatingActionButton.setOnClickListener(v -> activity.startActivity(CreateTaskActivity.getCreateIntent(activity, mInstanceKey.TaskId)));

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

        mTreeViewAdapter = GroupAdapter.getAdapter(this, data.DataId, data.CustomTimeDatas, useGroups(), showFab, data.InstanceDatas.values(), mExpansionState, mSelectedNodes, data.TaskDatas);
        Assert.assertTrue(mTreeViewAdapter != null);

        mGroupListRecycler.setAdapter(mTreeViewAdapter);

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        if (data.InstanceDatas.isEmpty()) {
            mGroupListRecycler.setVisibility(View.GONE);

            if (emptyTextId != null) {
                mEmptyText.setVisibility(View.VISIBLE);
                mEmptyText.setText(emptyTextId);
            }
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

        private final float mDensity;

        public static TreeViewAdapter getAdapter(GroupListFragment groupListFragment, int dataId, List<GroupListLoader.CustomTimeData> customTimeDatas, boolean useGroups, boolean showFab, Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes, List<GroupListLoader.TaskData> taskDatas) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);

            GroupAdapter groupAdapter = new GroupAdapter(groupListFragment, dataId, customTimeDatas, showFab);

            return groupAdapter.initialize(useGroups, instanceDatas, expansionState, selectedNodes, taskDatas);
        }

        private GroupAdapter(GroupListFragment groupListFragment, int dataId, List<GroupListLoader.CustomTimeData> customTimeDatas, boolean showFab) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);

            mGroupListFragmentReference = new WeakReference<>(groupListFragment);
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mShowFab = showFab;

            mDensity = groupListFragment.getActivity().getResources().getDisplayMetrics().density;
        }

        private TreeViewAdapter initialize(boolean useGroups, Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes, List<GroupListLoader.TaskData> taskDatas) {
            Assert.assertTrue(instanceDatas != null);

            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(mShowFab, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));

            mNodeCollection = new NodeCollection(mDensity, 0, new WeakReference<>(this), useGroups, new WeakReference<>(treeNodeCollection));

            List<TimeStamp> expandedGroups = null;
            HashMap<InstanceKey, Boolean> expandedInstances = null;
            boolean doneExpanded = false;
            boolean unscheduledExpanded = false;
            List<Integer> expandedTasks = null;

            if (expansionState != null) {
                expandedGroups = expansionState.ExpandedGroups;
                Assert.assertTrue(expandedGroups != null);

                expandedInstances = expansionState.ExpandedInstances;
                Assert.assertTrue(expandedInstances != null);

                doneExpanded = expansionState.DoneExpanded;

                unscheduledExpanded = expansionState.UnscheduledExpanded;

                expandedTasks = expansionState.ExpandedTasks;
            } else if (taskDatas != null) {
                unscheduledExpanded = false;
            }

            treeNodeCollection.setNodes(mNodeCollection.initialize(treeViewAdapter, instanceDatas, expandedGroups, expandedInstances, doneExpanded, selectedNodes, true, taskDatas, unscheduledExpanded, expandedTasks));

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            return treeViewAdapter;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            if (viewType == TYPE_GROUP) {
                LinearLayout groupRow = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list, parent, false);

                LinearLayout groupRowContainer = (LinearLayout) groupRow.findViewById(R.id.group_row_container);
                TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
                TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
                TextView groupRowChildren = (TextView) groupRow.findViewById(R.id.group_row_children);
                ImageView groupRowExpand = (ImageView) groupRow.findViewById(R.id.group_row_expand);
                CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);
                View groupRowSeparator = groupRow.findViewById(R.id.group_row_separator);

                return new GroupHolder(groupRow, groupRowContainer, groupRowName, groupRowDetails, groupRowChildren, groupRowExpand, groupCheckBox, groupRowSeparator);
            } else {
                Assert.assertTrue(viewType == TreeViewAdapter.TYPE_FAB_PADDING);

                FrameLayout frameLayout = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list_fab_padding, parent, false);
                return new FabPaddingHolder(frameLayout);
            }
        }

        @Override
        public void onBindViewHolder(RecyclerView.ViewHolder viewHolder, int position) {
            Assert.assertTrue(position >= 0);

            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            Assert.assertTrue(position < treeViewAdapter.getItemCount());

            if (position < treeViewAdapter.displayedSize()) {
                TreeNode treeNode = treeViewAdapter.getNode(position);
                treeNode.onBindViewHolder(viewHolder);
            } else {
                Assert.assertTrue(position == treeViewAdapter.displayedSize());
                Assert.assertTrue(mShowFab);
                Assert.assertTrue(position == treeViewAdapter.getItemCount() - 1);
            }
        }

        @Override
        public boolean hasActionMode() {
            GroupListFragment groupListFragment = mGroupListFragmentReference.get();
            Assert.assertTrue(groupListFragment != null);

            return groupListFragment.mSelectionCallback.hasActionMode();
        }

        @Override
        public void incrementSelected() {
            GroupListFragment groupListFragment = mGroupListFragmentReference.get();
            Assert.assertTrue(groupListFragment != null);

            groupListFragment.mSelectionCallback.incrementSelected();
        }

        @Override
        public void decrementSelected() {
            GroupListFragment groupListFragment = mGroupListFragmentReference.get();
            Assert.assertTrue(groupListFragment != null);

            groupListFragment.mSelectionCallback.decrementSelected();
        }

        private GroupListFragment getGroupListFragment() {
            GroupListFragment groupListFragment = mGroupListFragmentReference.get();
            Assert.assertTrue(groupListFragment != null);

            return groupListFragment;
        }

        public ExpansionState getExpansionState() {
            List<TimeStamp> expandedGroups = mNodeCollection.getExpandedGroups();
            Assert.assertTrue(expandedGroups != null);

            HashMap<InstanceKey, Boolean> expandedInstances = new HashMap<>();
            mNodeCollection.addExpandedInstances(expandedInstances);

            boolean doneExpanded = mNodeCollection.getDoneExpanded();

            boolean unscheduledExpanded = mNodeCollection.getUnscheduledExpanded();

            List<Integer> expandedTasks = mNodeCollection.getExpandedTasks();

            return new ExpansionState(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTasks);
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

        public static class GroupHolder extends RecyclerView.ViewHolder {
            public final LinearLayout mGroupRow;
            public final LinearLayout mGroupRowContainer;
            public final TextView mGroupRowName;
            public final TextView mGroupRowDetails;
            public final TextView mGroupRowChildren;
            public final ImageView mGroupRowExpand;
            public final CheckBox mGroupRowCheckBox;
            public final View mGroupRowSeparator;

            public GroupHolder(LinearLayout groupRow, LinearLayout groupRowContainer, TextView groupRowName, TextView groupRowDetails, TextView groupRowChildren, ImageView groupRowExpand, CheckBox groupRowCheckBox, View groupRowSeparator) {
                super(groupRow);

                Assert.assertTrue(groupRowContainer != null);
                Assert.assertTrue(groupRowName != null);
                Assert.assertTrue(groupRowDetails != null);
                Assert.assertTrue(groupRowChildren != null);
                Assert.assertTrue(groupRowExpand != null);
                Assert.assertTrue(groupRowCheckBox != null);
                Assert.assertTrue(groupRowSeparator != null);

                mGroupRow = groupRow;
                mGroupRowContainer = groupRowContainer;
                mGroupRowName = groupRowName;
                mGroupRowDetails = groupRowDetails;
                mGroupRowChildren = groupRowChildren;
                mGroupRowExpand = groupRowExpand;
                mGroupRowCheckBox = groupRowCheckBox;
                mGroupRowSeparator = groupRowSeparator;
            }
        }

        public static class FabPaddingHolder extends RecyclerView.ViewHolder {
            FabPaddingHolder(FrameLayout frameLayout) {
                super(frameLayout);
            }
        }

        public static class NodeCollection {
            private final WeakReference<NodeCollectionParent> mNodeCollectionParentReference;
            private final WeakReference<NodeContainer> mNodeContainerReference;

            private NotDoneGroupCollection mNotDoneGroupCollection;
            private DividerNode mDividerNode;
            private UnscheduledNode mUnscheduledNode;

            private final boolean mUseGroups;

            private final float mDensity;
            private final int mIndentation;

            private NodeCollection(float density, int indentation, WeakReference<NodeCollectionParent> nodeCollectionParentReference, boolean useGroups, WeakReference<NodeContainer> nodeContainerReference) {
                Assert.assertTrue(nodeCollectionParentReference != null);
                Assert.assertTrue(nodeContainerReference != null);

                mDensity = density;
                mIndentation = indentation;
                mNodeCollectionParentReference = nodeCollectionParentReference;
                mUseGroups = useGroups;
                mNodeContainerReference = nodeContainerReference;
            }

            private List<TreeNode> initialize(TreeViewAdapter treeViewAdapter, Collection<GroupListLoader.InstanceData> instanceDatas, List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, boolean doneExpanded, ArrayList<InstanceKey> selectedNodes, boolean selectable, List<GroupListLoader.TaskData> taskDatas, boolean unscheduledExpanded, List<Integer> expandedTasks) {
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

                mNotDoneGroupCollection = new NotDoneGroupCollection(mDensity, mIndentation, new WeakReference<>(this), mNodeContainerReference, selectable);

                List<TreeNode> rootTreeNodes = mNotDoneGroupCollection.initialize(notDoneInstanceDatas, expandedGroups, expandedInstances, selectedNodes);
                Assert.assertTrue(rootTreeNodes != null);

                Assert.assertTrue((mIndentation == 0) || (taskDatas == null));
                if (taskDatas != null && !taskDatas.isEmpty()) {
                    mUnscheduledNode = new UnscheduledNode(mDensity, new WeakReference<>(this));

                    TreeNode unscheduledTreeNode = mUnscheduledNode.initialize(unscheduledExpanded, mNodeContainerReference, taskDatas, expandedTasks);
                    Assert.assertTrue(unscheduledTreeNode != null);

                    rootTreeNodes.add(unscheduledTreeNode);
                }

                mDividerNode = new DividerNode(mDensity, mIndentation, new WeakReference<>(this));

                doneExpanded = doneExpanded && !doneInstanceDatas.isEmpty();

                TreeNode dividerTreeNode = mDividerNode.initialize(doneExpanded, mNodeContainerReference, doneInstanceDatas, expandedInstances);
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

            public List<TimeStamp> getExpandedGroups() {
                return mNotDoneGroupCollection.getExpandedGroups();
            }

            public void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                mNotDoneGroupCollection.addExpandedInstances(expandedInstances);
                mDividerNode.addExpandedInstances(expandedInstances);
            }

            public boolean getUnscheduledExpanded() {
                return (mUnscheduledNode != null && mUnscheduledNode.expanded());
            }

            public List<Integer> getExpandedTasks() {
                if (mUnscheduledNode == null)
                    return null;
                else
                    return mUnscheduledNode.getExpandedTasks();
            }

            public boolean getDoneExpanded() {
                return mDividerNode.expanded();
            }

            public static class NotDoneGroupCollection {
                private final WeakReference<NodeCollection> mNodeCollectionReference;
                private final WeakReference<NodeContainer> mNodeContainerReference;

                private final ArrayList<NotDoneGroupNode> mNotDoneGroupNodes = new ArrayList<>();

                private final float mDensity;
                private final int mIndentation;

                private final boolean mSelectable;

                private NotDoneGroupCollection(float density, int indentation, WeakReference<NodeCollection> nodeCollectionReference, WeakReference<NodeContainer> nodeContainerReference, boolean selectable) {
                    Assert.assertTrue(nodeCollectionReference != null);
                    Assert.assertTrue(nodeContainerReference != null);

                    mDensity = density;
                    mIndentation = indentation;
                    mNodeCollectionReference = nodeCollectionReference;
                    mNodeContainerReference = nodeContainerReference;
                    mSelectable = selectable;
                }

                private List<TreeNode> initialize(List<GroupListLoader.InstanceData> notDoneInstanceDatas, List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes) {
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
                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), entry.getValue(), expandedGroups, expandedInstances, selectedNodes);
                            Assert.assertTrue(notDoneGroupTreeNode != null);

                            notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
                        }
                    } else {
                        for (GroupListLoader.InstanceData instanceData : notDoneInstanceDatas) {
                            ArrayList<GroupListLoader.InstanceData> dummyInstanceDatas = new ArrayList<>();
                            dummyInstanceDatas.add(instanceData);

                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), dummyInstanceDatas, expandedGroups, expandedInstances, selectedNodes);
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

                        TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), instanceDatas, null, null, null);
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        nodeContainer.add(notDoneGroupTreeNode);
                    } else {
                        Assert.assertTrue(timeStampNotDoneGroupNodes.size() == 1);

                        NotDoneGroupNode notDoneGroupNode = timeStampNotDoneGroupNodes.get(0);
                        Assert.assertTrue(notDoneGroupNode != null);

                        notDoneGroupNode.addInstanceData(instanceData);
                    }
                }

                private TreeNode newNotDoneGroupNode(WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference, List<GroupListLoader.InstanceData> instanceDatas, List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(notDoneGroupCollectionReference != null);
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(mDensity, mIndentation, notDoneGroupCollectionReference, instanceDatas, mSelectable);

                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    TreeNode notDoneGroupTreeNode = notDoneGroupNode.initialize(expandedGroups, expandedInstances, selectedNodes, mNodeContainerReference);
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

                public List<TimeStamp> getExpandedGroups() {
                    return Stream.of(mNotDoneGroupNodes)
                            .filter(notDoneGroupNode -> !notDoneGroupNode.singleInstance() && notDoneGroupNode.expanded())
                            .map(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.toTimeStamp())
                            .collect(Collectors.toList());
                }

                public void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                        notDoneGroupNode.addExpandedInstances(expandedInstances);
                }
            }

            private static abstract class GroupHolderNode {
                final float mDensity;
                final int mIndentation;

                public GroupHolderNode(float density, int indentation) {
                    mDensity = density;
                    mIndentation = indentation;
                }

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

                @SuppressWarnings("unused")
                public final void onBindViewHolder(RecyclerView.ViewHolder viewHolder) {
                    final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) viewHolder;

                    int padding = 48 * mIndentation;

                    groupHolder.mGroupRowContainer.setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

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

                @SuppressWarnings("unused")
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

                private final boolean mSelectable;

                private NotDoneGroupNode(float density, int indentation, WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference, List<GroupListLoader.InstanceData> instanceDatas, boolean selectable) {
                    super(density, indentation);
                    Assert.assertTrue(notDoneGroupCollectionReference != null);
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    mNotDoneGroupCollectionReference = notDoneGroupCollectionReference;
                    mInstanceDatas = instanceDatas;

                    mExactTimeStamp = instanceDatas.get(0).InstanceTimeStamp.toExactTimeStamp();
                    Assert.assertTrue(Stream.of(instanceDatas)
                            .allMatch(instanceData -> instanceData.InstanceTimeStamp.toExactTimeStamp().equals(mExactTimeStamp)));

                    mSelectable = selectable;
                }

                public TreeNode initialize(List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes, WeakReference<NodeContainer> nodeContainerReference) {
                    Assert.assertTrue(nodeContainerReference != null);

                    boolean expanded;
                    boolean doneExpanded;
                    if (mInstanceDatas.size() == 1) {
                        GroupListLoader.InstanceData instanceData = mInstanceDatas.get(0);
                        Assert.assertTrue(instanceData != null);

                        if (expandedInstances != null && expandedInstances.containsKey(instanceData.InstanceKey) && !instanceData.Children.isEmpty()) {
                            expanded = true;
                            doneExpanded = expandedInstances.get(instanceData.InstanceKey);
                        } else {
                            expanded = false;
                            doneExpanded = false;
                        }
                    } else {
                        expanded = (expandedGroups != null && expandedGroups.contains(mExactTimeStamp.toTimeStamp()));
                        doneExpanded = false;
                    }

                    boolean selected = (mInstanceDatas.size() == 1 && selectedNodes != null && selectedNodes.contains(mInstanceDatas.get(0).InstanceKey));

                    TreeNode notDoneGroupTreeNode = new TreeNode(this, nodeContainerReference, expanded, selected);
                    mNotDoneGroupTreeNodeReference = new WeakReference<>(notDoneGroupTreeNode);

                    if (mInstanceDatas.size() == 1) {
                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(notDoneGroupTreeNode));

                        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        notDoneGroupTreeNode.setChildTreeNodes(mNodeCollection.initialize(treeViewAdapter, mInstanceDatas.get(0).Children.values(), expandedGroups, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));
                    } else {
                        List<TreeNode> notDoneInstanceTreeNodes = Stream.of(mInstanceDatas)
                                .map(instanceData -> newChildTreeNode(instanceData, expandedInstances, selectedNodes))
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

                public void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                    Assert.assertTrue(expandedInstances != null);

                    if (!expanded())
                        return;

                    if (singleInstance()) {
                        Assert.assertTrue(!expandedInstances.containsKey(getSingleInstanceData().InstanceKey));

                        expandedInstances.put(getSingleInstanceData().InstanceKey, mNodeCollection.getDoneExpanded());
                        mNodeCollection.addExpandedInstances(expandedInstances);
                    } else {
                        for (NotDoneInstanceNode notDoneInstanceNode : mNotDoneInstanceNodes)
                            notDoneInstanceNode.addExpandedInstances(expandedInstances);
                    }
                }

                @Override
                int getNameVisibility() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();
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
                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (instanceData.Children.isEmpty() || expanded()) {
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
                    Assert.assertTrue(singleInstance());
                    Assert.assertTrue(!expanded());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    Assert.assertTrue(!instanceData.Children.isEmpty());
                    return getChildrenText(instanceData.Children.values());
                }

                @Override
                int getChildrenColor() {
                    Assert.assertTrue(singleInstance());
                    Assert.assertTrue(!expanded());

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    Assert.assertTrue(!instanceData.Children.isEmpty());

                    if (!instanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                    }
                }

                @Override
                int getExpandVisibility() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (instanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelectedChildren().size() > 0)) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        if (groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelectedChildren().size() > 0)
                            return View.INVISIBLE;
                        else
                            return View.VISIBLE;
                    }
                }

                @Override
                int getExpandImageResource() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

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
                        Assert.assertTrue(!(groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelectedChildren().size() > 0));

                        if (notDoneGroupTreeNode.expanded())
                            return R.drawable.ic_expand_less_black_36dp;
                        else
                            return R.drawable.ic_expand_more_black_36dp;
                    }
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();
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

                        recursiveExists(instanceData);

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

                        notDoneGroupTreeNode.remove(childTreeNode1);

                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(notDoneGroupTreeNode));

                        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        List<TreeNode> childTreeNodes = mNodeCollection.initialize(treeViewAdapter, mInstanceDatas.get(0).Children.values(), null, null, false, null, mSelectable, null, false, null);
                        Assert.assertTrue(childTreeNodes != null);

                        Stream.of(childTreeNodes)
                                .forEach(notDoneGroupTreeNode::add);
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

                        notDoneGroupTreeNode.removeAll();
                        mNodeCollection = null;

                        GroupListLoader.InstanceData instanceData1 = mInstanceDatas.get(0);

                        GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(mDensity, mIndentation, instanceData1, new WeakReference<>(NotDoneGroupNode.this), mSelectable);
                        mNotDoneInstanceNodes.add(notDoneInstanceNode);

                        TreeNode childTreeNode = notDoneInstanceNode.initialize(null, null, mNotDoneGroupTreeNodeReference);

                        notDoneGroupTreeNode.add(childTreeNode);
                    }

                    mInstanceDatas.add(instanceData);

                    TreeNode childTreeNode = newChildTreeNode(instanceData, null, null);
                    Assert.assertTrue(childTreeNode != null);

                    notDoneGroupTreeNode.add(childTreeNode);
                }

                public TreeNode newChildTreeNode(GroupListLoader.InstanceData instanceData, HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(mNotDoneGroupTreeNodeReference != null);

                    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(mDensity, mIndentation, instanceData, new WeakReference<>(this), mSelectable);

                    TreeNode childTreeNode = notDoneInstanceNode.initialize(expandedInstances, selectedNodes, mNotDoneGroupTreeNodeReference);

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
                    return mSelectable && mNotDoneInstanceNodes.isEmpty();
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

                    private final boolean mSelectable;

                    public NotDoneInstanceNode(float density, int indentation, GroupListLoader.InstanceData instanceData, WeakReference<NotDoneGroupNode> notDoneGroupNodeReference, boolean selectable) {
                        super(density, indentation);

                        Assert.assertTrue(instanceData != null);
                        Assert.assertTrue(notDoneGroupNodeReference != null);

                        mInstanceData = instanceData;
                        mNotDoneGroupNodeReference = notDoneGroupNodeReference;
                        mSelectable = selectable;
                    }

                    public TreeNode initialize(HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes, WeakReference<TreeNode> notDoneGroupTreeNodeReference) {
                        Assert.assertTrue(notDoneGroupTreeNodeReference != null);

                        TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        TreeNode notDoneGroupTreeNode = notDoneGroupTreeNodeReference.get();

                        boolean selected = (selectedNodes != null && selectedNodes.contains(mInstanceData.InstanceKey));

                        boolean expanded = false;
                        boolean doneExpanded = false;
                        if ((expandedInstances != null && expandedInstances.containsKey(mInstanceData.InstanceKey) && !mInstanceData.Children.isEmpty())) {
                            expanded = true;
                            doneExpanded = expandedInstances.get(mInstanceData.InstanceKey);
                        }

                        TreeNode childTreeNode = new TreeNode(this, new WeakReference<>(notDoneGroupTreeNode), expanded, selected);
                        mChildTreeNodeReference = new WeakReference<>(childTreeNode);

                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(childTreeNode));
                        childTreeNode.setChildTreeNodes(mNodeCollection.initialize(treeViewAdapter, mInstanceData.Children.values(), null, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));

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

                    private boolean expanded() {
                        TreeNode treeNode = getTreeNode();
                        Assert.assertTrue(treeNode != null);

                        return treeNode.expanded();
                    }

                    public void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                        Assert.assertTrue(expandedInstances != null);

                        if (!expanded())
                            return;

                        Assert.assertTrue(!expandedInstances.containsKey(mInstanceData.InstanceKey));

                        expandedInstances.put(mInstanceData.InstanceKey, mNodeCollection.getDoneExpanded());

                        mNodeCollection.addExpandedInstances(expandedInstances);
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
                        if (mInstanceData.Children.isEmpty() || expanded()) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    String getChildren() {
                        Assert.assertTrue(!expanded());
                        Assert.assertTrue(!mInstanceData.Children.isEmpty());
                        return getChildrenText(mInstanceData.Children.values());
                    }

                    @Override
                    int getChildrenColor() {
                        GroupListFragment groupListFragment = getGroupListFragment();
                        Assert.assertTrue(groupListFragment != null);

                        Assert.assertTrue(!expanded());
                        Assert.assertTrue(!mInstanceData.Children.isEmpty());

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

                        if (mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedChildren().size() > 0)) {
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

                        Assert.assertTrue(!(mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedChildren().size() > 0)));

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

                        Assert.assertTrue(!(mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedChildren().size() > 0)));

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

                            recursiveExists(mInstanceData);

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

                    @Override
                    public boolean selectable() {
                        return mSelectable;
                    }

                    @Override
                    public boolean visibleWhenEmpty() {
                        return true;
                    }

                    @Override
                    public boolean visibleDuringActionMode() {
                        return true;
                    }

                    public void removeFromParent() {
                        NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        notDoneGroupNode.remove(this);
                    }
                }
            }

            public static class DividerNode extends GroupHolderNode implements ModelNode {
                private final WeakReference<NodeCollection> mNodeCollectionReference;

                private WeakReference<TreeNode> mTreeNodeReference;

                private final ArrayList<DoneInstanceNode> mDoneInstanceNodes = new ArrayList<>();

                private DividerNode(float density, int indentation, WeakReference<NodeCollection> nodeCollectionReference) {
                    super(density, indentation);

                    Assert.assertTrue(nodeCollectionReference != null);

                    mNodeCollectionReference = nodeCollectionReference;
                }

                private TreeNode initialize(boolean expanded, WeakReference<NodeContainer> nodeContainerReference, List<GroupListLoader.InstanceData> doneInstanceDatas, HashMap<InstanceKey, Boolean> expandedInstances) {
                    Assert.assertTrue(!expanded || !doneInstanceDatas.isEmpty());

                    TreeNode dividerTreeNode = new TreeNode(this, nodeContainerReference, expanded, false);
                    mTreeNodeReference = new WeakReference<>(dividerTreeNode);

                    List<TreeNode> childTreeNodes = Stream.of(doneInstanceDatas)
                            .map(doneInstanceData -> newChildTreeNode(doneInstanceData, expandedInstances))
                            .collect(Collectors.toList());

                    dividerTreeNode.setChildTreeNodes(childTreeNodes);

                    return dividerTreeNode;
                }

                private TreeNode newChildTreeNode(GroupListLoader.InstanceData instanceData, HashMap<InstanceKey, Boolean> expandedInstances) {
                    Assert.assertTrue(instanceData.Done != null);

                    DoneInstanceNode doneInstanceNode = new DoneInstanceNode(mDensity, mIndentation, instanceData, new WeakReference<>(this));

                    TreeNode dividerTreeNode = mTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    TreeNode childTreeNode = doneInstanceNode.initialize(dividerTreeNode, expandedInstances);

                    mDoneInstanceNodes.add(doneInstanceNode);

                    return childTreeNode;
                }

                public boolean expanded() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return treeNode.expanded();
                }

                public void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                    Assert.assertTrue(expandedInstances != null);

                    for (DoneInstanceNode doneInstanceNode : mDoneInstanceNodes)
                        doneInstanceNode.addExpandedInstances(expandedInstances);
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
                    TreeNode dividerTreeNode = mTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    if (dividerTreeNode.expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    TreeNode dividerTreeNode = mTreeNodeReference.get();
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

                    TreeNode dividerTreeNode = mTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    Assert.assertTrue(mDoneInstanceNodes.contains(doneInstanceNode));
                    mDoneInstanceNodes.remove(doneInstanceNode);

                    TreeNode childTreeNode = doneInstanceNode.mTreeNodeReference.get();
                    Assert.assertTrue(childTreeNode != null);

                    dividerTreeNode.remove(childTreeNode);
                }

                public void add(GroupListLoader.InstanceData instanceData) {
                    TreeNode dividerTreeNode = mTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    TreeNode childTreeNode = newChildTreeNode(instanceData, null);
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
                    Assert.assertTrue(another instanceof NotDoneGroupNode || another instanceof UnscheduledNode);
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
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }
            }

            public static class DoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
                private final WeakReference<DividerNode> mDividerNodeReference;

                private WeakReference<TreeNode> mTreeNodeReference;

                private final GroupListLoader.InstanceData mInstanceData;

                private NodeCollection mNodeCollection;

                public DoneInstanceNode(float density, int indentation, GroupListLoader.InstanceData instanceData, WeakReference<DividerNode> dividerNodeReference) {
                    super(density, indentation);

                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(dividerNodeReference != null);

                    mInstanceData = instanceData;
                    mDividerNodeReference = dividerNodeReference;
                    Assert.assertTrue(mDividerNodeReference.get() != null);
                }

                public TreeNode initialize(TreeNode dividerTreeNode, HashMap<InstanceKey, Boolean> expandedInstances) {
                    Assert.assertTrue(dividerTreeNode != null);

                    TreeViewAdapter treeViewAdapter = getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    boolean expanded = false;
                    boolean doneExpanded = false;
                    if (expandedInstances != null && expandedInstances.containsKey(mInstanceData.InstanceKey) && !mInstanceData.Children.isEmpty()) {
                        expanded = true;
                        doneExpanded = expandedInstances.get(mInstanceData.InstanceKey);
                    }

                    TreeNode doneTreeNode = new TreeNode(this, new WeakReference<>(dividerTreeNode), expanded, false);
                    mTreeNodeReference = new WeakReference<>(doneTreeNode);

                    mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(doneTreeNode));
                    doneTreeNode.setChildTreeNodes(mNodeCollection.initialize(treeViewAdapter, mInstanceData.Children.values(), null, expandedInstances, doneExpanded, null, false, null, false, null));

                    return doneTreeNode;
                }

                private TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                private DividerNode getDividerNode() {
                    DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    return dividerNode;
                }

                private NodeCollection getParentNodeCollection() {
                    DividerNode dividerNode = getDividerNode();
                    Assert.assertTrue(dividerNode != null);

                    NodeCollection nodeCollection = dividerNode.getNodeCollection();
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

                private boolean expanded() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return treeNode.expanded();
                }

                public void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                    Assert.assertTrue(expandedInstances != null);

                    if (!expanded())
                        return;

                    Assert.assertTrue(!expandedInstances.containsKey(mInstanceData.InstanceKey));

                    expandedInstances.put(mInstanceData.InstanceKey, mNodeCollection.getDoneExpanded());

                    mNodeCollection.addExpandedInstances(expandedInstances);
                }

                @Override
                public GroupAdapter getGroupAdapter() {
                    NodeCollection nodeCollection = getParentNodeCollection();
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
                int getNameVisibility() {
                    return View.VISIBLE;
                }

                @Override
                String getName() {
                    return mInstanceData.Name;
                }

                @Override
                int getNameColor() {
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
                    if (mInstanceData.Children.isEmpty() || expanded()) {
                        return View.GONE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                String getChildren() {
                    Assert.assertTrue(!expanded());
                    Assert.assertTrue(!mInstanceData.Children.isEmpty());
                    return getChildrenText(mInstanceData.Children.values());
                }

                @Override
                int getChildrenColor() {
                    Assert.assertTrue(!expanded());
                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

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
                    if (mInstanceData.Children.isEmpty()) {
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

                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

                    if (treeNode.expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

                    return treeNode.getExpandListener();
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
                        TreeNode childTreeNode = mTreeNodeReference.get();
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
                    GroupListFragment groupListFragment = getGroupListFragment();
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

                public void removeFromParent() {
                    DividerNode dividerNode = getDividerNode();
                    Assert.assertTrue(dividerNode != null);

                    dividerNode.remove(this);
                }
            }

            public static class UnscheduledNode extends GroupHolderNode implements ModelNode, TaskParent {
                private final WeakReference<NodeCollection> mNodeCollectionReference;

                private WeakReference<TreeNode> mTreeNodeReference;

                private List<TaskNode> mTaskNodes;

                public UnscheduledNode(float density, WeakReference<NodeCollection> nodeCollectionReference) {
                    super(density, 0);

                    Assert.assertTrue(nodeCollectionReference != null);

                    mNodeCollectionReference = nodeCollectionReference;
                }

                private TreeNode initialize(boolean expanded, WeakReference<NodeContainer> nodeContainerReference, List<GroupListLoader.TaskData> taskDatas, List<Integer> expandedTasks) {
                    Assert.assertTrue(!expanded || !taskDatas.isEmpty());

                    TreeNode dividerTreeNode = new TreeNode(this, nodeContainerReference, expanded, false);
                    mTreeNodeReference = new WeakReference<>(dividerTreeNode);

                    mTaskNodes = new ArrayList<>();

                    List<TreeNode> childTreeNodes = Stream.of(taskDatas)
                            .map(taskData -> newChildTreeNode(taskData, expandedTasks))
                            .collect(Collectors.toList());

                    dividerTreeNode.setChildTreeNodes(childTreeNodes);

                    return dividerTreeNode;
                }

                private TreeNode newChildTreeNode(GroupListLoader.TaskData taskData, List<Integer> expandedTasks) {
                    TaskNode taskNode = new TaskNode(mDensity, 0, taskData, new WeakReference<>(this));

                    mTaskNodes.add(taskNode);

                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return taskNode.initialize(treeNode, expandedTasks);
                }

                private NodeCollection getNodeCollection() {
                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    return nodeCollection;
                }

                public boolean expanded() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return treeNode.expanded();
                }

                public List<Integer> getExpandedTasks() {
                    return Stream.of(mTaskNodes)
                            .flatMap(TaskNode::getExpandedTasks)
                            .collect(Collectors.toList());
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

                private TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                @Override
                public int compareTo(@NonNull ModelNode modelNode) {
                    if (modelNode instanceof DividerNode) {
                        return -1;
                    } else {
                        Assert.assertTrue(modelNode instanceof NotDoneGroupNode);
                        return 1;
                    }
                }

                @Override
                int getNameVisibility() {
                    return View.VISIBLE;
                }

                @Override
                String getName() {
                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    return groupListFragment.getString(R.string.noReminder);
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
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    if (treeNode.expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return treeNode.getExpandListener();
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

                @Override
                public boolean selectable() {
                    return false;
                }

                @Override
                public void onClick() {

                }

                @Override
                public boolean visibleWhenEmpty() {
                    return false;
                }

                @Override
                public boolean visibleDuringActionMode() {
                    return false;
                }
            }

            public static class TaskNode extends GroupHolderNode implements ModelNode, TaskParent {
                private final WeakReference<TaskParent> mTaskParentReference;

                private final GroupListLoader.TaskData mTaskData;

                private WeakReference<TreeNode> mTreeNodeReference;

                private List<TaskNode> mTaskNodes;

                public TaskNode(float density, int indentation, GroupListLoader.TaskData taskData, WeakReference<TaskParent> taskParentReference) {
                    super(density, indentation);

                    Assert.assertTrue(taskData != null);
                    Assert.assertTrue(taskParentReference != null);

                    mTaskData = taskData;
                    mTaskParentReference = taskParentReference;
                }

                public TreeNode initialize(TreeNode parentTreeNode, List<Integer> expandedTasks) {
                    Assert.assertTrue(parentTreeNode != null);

                    boolean expanded = (expandedTasks != null && expandedTasks.contains(mTaskData.TaskId) && !mTaskData.Children.isEmpty());

                    TreeNode taskTreeNode = new TreeNode(this, new WeakReference<>(parentTreeNode), expanded, false);
                    mTreeNodeReference = new WeakReference<>(taskTreeNode);

                    mTaskNodes = new ArrayList<>();

                    List<TreeNode> childTreeNodes = Stream.of(mTaskData.Children)
                            .map(taskData -> newChildTreeNode(taskData, expandedTasks))
                            .collect(Collectors.toList());

                    taskTreeNode.setChildTreeNodes(childTreeNodes);

                    return taskTreeNode;
                }

                private TreeNode newChildTreeNode(GroupListLoader.TaskData taskData, List<Integer> expandedTasks) {
                    TaskNode taskNode = new TaskNode(mDensity, mIndentation + 1, taskData, new WeakReference<>(this));

                    mTaskNodes.add(taskNode);

                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return taskNode.initialize(treeNode, expandedTasks);
                }

                private TaskParent getTaskParent() {
                    TaskParent taskParent = mTaskParentReference.get();
                    Assert.assertTrue(taskParent != null);

                    return taskParent;
                }

                @Override
                public GroupAdapter getGroupAdapter() {
                    TaskParent taskParent = getTaskParent();
                    Assert.assertTrue(taskParent != null);

                    GroupAdapter groupAdapter = taskParent.getGroupAdapter();
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

                private TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                private boolean expanded() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    return treeNode.expanded();
                }

                public Stream<Integer> getExpandedTasks() {
                    if (mTaskNodes.isEmpty()) {
                        Assert.assertTrue(!expanded());

                        return Stream.of(new ArrayList<>());
                    } else {
                        List<Integer> expandedTasks = new ArrayList<>();

                        if (expanded())
                            expandedTasks.add(mTaskData.TaskId);

                        return Stream.concat(Stream.of(expandedTasks), Stream.of(mTaskNodes).flatMap(TaskNode::getExpandedTasks));
                    }
                }

                @Override
                public int compareTo(@NonNull ModelNode modelNode) {
                    TaskNode other = (TaskNode) modelNode;

                    if (mIndentation == 0) {
                        return -Integer.valueOf(mTaskData.TaskId).compareTo(other.mTaskData.TaskId);
                    } else {
                        return Integer.valueOf(mTaskData.TaskId).compareTo(other.mTaskData.TaskId);
                    }
                }

                @Override
                int getNameVisibility() {
                    return View.VISIBLE;
                }

                @Override
                String getName() {
                    return mTaskData.Name;
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
                    if (mTaskData.Children.isEmpty() || expanded()) {
                        return View.GONE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                String getChildren() {
                    Assert.assertTrue(!expanded());
                    Assert.assertTrue(!mTaskData.Children.isEmpty());

                    return Stream.of(mTaskData.Children)
                            .sortBy(task -> task.TaskId)
                            .map(task -> task.Name)
                            .collect(Collectors.joining(", "));
                }

                @Override
                int getChildrenColor() {
                    Assert.assertTrue(!expanded());
                    Assert.assertTrue(!mTaskData.Children.isEmpty());

                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                }

                @Override
                int getExpandVisibility() {
                    if (mTaskData.Children.isEmpty()) {
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

                    Assert.assertTrue(!mTaskData.Children.isEmpty());

                    if (treeNode.expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    TreeNode treeNode = getTreeNode();
                    Assert.assertTrue(treeNode != null);

                    Assert.assertTrue(!mTaskData.Children.isEmpty());

                    return treeNode.getExpandListener();
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

                @Override
                public boolean selectable() {
                    return false;
                }

                @Override
                public void onClick() {
                    GroupListFragment groupListFragment = getGroupListFragment();
                    Assert.assertTrue(groupListFragment != null);

                    groupListFragment.getActivity().startActivity(ShowTaskActivity.getIntent(mTaskData.TaskId, groupListFragment.getActivity()));
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
        public final List<TimeStamp> ExpandedGroups;
        public final HashMap<InstanceKey, Boolean> ExpandedInstances;
        public final boolean UnscheduledExpanded;
        public final List<Integer> ExpandedTasks;

        public ExpansionState(boolean doneExpanded, List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, boolean unscheduledExpanded, List<Integer> expandedTasks) {
            Assert.assertTrue(expandedGroups != null);
            Assert.assertTrue(expandedInstances != null);

            DoneExpanded = doneExpanded;
            ExpandedGroups = expandedGroups;
            ExpandedInstances = expandedInstances;
            UnscheduledExpanded = unscheduledExpanded;
            ExpandedTasks = expandedTasks;
        }

        @Override
        public int describeContents() {
            return 0;
        }

        @Override
        public void writeToParcel(Parcel dest, int flags) {
            dest.writeInt(DoneExpanded ? 1 : 0);
            dest.writeTypedList(ExpandedGroups);
            dest.writeSerializable(ExpandedInstances);

            dest.writeInt(UnscheduledExpanded ? 1 : 0);

            if (ExpandedTasks == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                dest.writeList(ExpandedTasks);
            }
        }

        public static Parcelable.Creator<ExpansionState> CREATOR = new Creator<ExpansionState>() {
            @Override
            public ExpansionState createFromParcel(Parcel source) {
                boolean doneExpanded = (source.readInt() == 1);

                List<TimeStamp> expandedGroups = new ArrayList<>();
                source.readTypedList(expandedGroups, TimeStamp.CREATOR);

                @SuppressWarnings("unchecked") HashMap<InstanceKey, Boolean> expandedInstances = (HashMap<InstanceKey, Boolean>) source.readSerializable();

                boolean unscheduledExpanded = (source.readInt() == 1);

                boolean hasTasks = (source.readInt() == 1);
                List<Integer> expandedTasks;
                if (hasTasks) {
                    expandedTasks = new ArrayList<>();
                    source.readList(expandedTasks, Integer.class.getClassLoader());
                } else {
                    expandedTasks = null;
                }

                return new ExpansionState(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTasks);
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

    private interface TaskParent {
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

    private static void recursiveExists(GroupListLoader.InstanceData instanceData) {
        Assert.assertTrue(instanceData != null);

        instanceData.Exists = true;

        GroupListLoader.InstanceDataParent instanceDataParent = instanceData.InstanceDataParentReference.get();
        Assert.assertTrue(instanceDataParent != null);

        if (instanceDataParent instanceof GroupListLoader.InstanceData) {
            GroupListLoader.InstanceData instanceData1 = (GroupListLoader.InstanceData) instanceDataParent;
            recursiveExists(instanceData1);
        } else {
            Assert.assertTrue(instanceDataParent instanceof GroupListLoader.Data);
        }
    }

    private static String getChildrenText(Collection<GroupListLoader.InstanceData> instanceDatas) {
        Assert.assertTrue(instanceDatas != null);
        Assert.assertTrue(!instanceDatas.isEmpty());

        Stream<GroupListLoader.InstanceData> notDone = Stream.of(instanceDatas)
                .filter(instanceData -> instanceData.Done == null)
                .sortBy(instanceData -> instanceData.InstanceKey.TaskId);

        Stream<GroupListLoader.InstanceData> done = Stream.of(instanceDatas)
                .filter(instanceData -> instanceData.Done != null)
                .sortBy(instanceData -> -instanceData.Done.getLong());

        return Stream.concat(notDone, done)
                .map(instanceData -> instanceData.Name)
                .collect(Collectors.joining(", "));
    }
}