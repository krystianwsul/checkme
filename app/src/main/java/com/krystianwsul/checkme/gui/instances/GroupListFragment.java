package com.krystianwsul.checkme.gui.instances;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
                case R.id.action_group_edit_instance: {
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
                }
                case R.id.action_group_share: {
                    Assert.assertTrue(instanceDatas.size() == 1);

                    Utils.share(instanceDatas.get(0).Name, getActivity());
                    break;
                }
                case R.id.action_group_show_task: {
                    Assert.assertTrue(instanceDatas.size() == 1);

                    GroupListLoader.InstanceData instanceData = instanceDatas.get(0);
                    Assert.assertTrue(instanceData.TaskCurrent);

                    startActivity(ShowTaskActivity.getIntent(instanceData.InstanceKey.TaskId, getActivity()));
                    break;
                }
                case R.id.action_group_edit_task: {
                    Assert.assertTrue(instanceDatas.size() == 1);

                    GroupListLoader.InstanceData instanceData = instanceDatas.get(0);
                    Assert.assertTrue(instanceData.TaskCurrent);

                    startActivity(CreateTaskActivity.getEditIntent(getActivity(), instanceData.InstanceKey.TaskId));
                    break;
                }
                case R.id.action_group_delete_task: {
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(!taskIds.isEmpty());
                    Assert.assertTrue(Stream.of(instanceDatas)
                            .allMatch(instanceData -> instanceData.TaskCurrent));

                    List<TreeNode> selectedTreeNodes = mTreeViewAdapter.getSelectedNodes();
                    Assert.assertTrue(!selectedTreeNodes.isEmpty());

                    do {
                        TreeNode treeNode = selectedTreeNodes.get(0);
                        Assert.assertTrue(treeNode != null);

                        recursiveDelete(treeNode, true);

                        decrementSelected();
                    } while (!(selectedTreeNodes = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(getActivity(), ((GroupAdapter) mTreeViewAdapter.getTreeModelAdapter()).mDataId, taskIds);

                    updateSelectAll();

                    TickService.startService(getActivity());

                    break;
                }
                case R.id.action_group_add_task: {
                    Assert.assertTrue(instanceDatas.size() == 1);

                    GroupListLoader.InstanceData instanceData = instanceDatas.get(0);
                    Assert.assertTrue(instanceData.TaskCurrent);

                    getActivity().startActivity(CreateTaskActivity.getCreateIntent(getActivity(), instanceData.InstanceKey.TaskId));
                    break;
                }
                case R.id.action_group_join: {
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(taskIds.size() > 1);

                    if (mInstanceKey == null) {
                        GroupListLoader.InstanceData firstInstanceData = Stream.of(instanceDatas)
                                .min((lhs, rhs) -> lhs.InstanceTimeStamp.compareTo(rhs.InstanceTimeStamp))
                                .get();

                        Date date = firstInstanceData.InstanceTimeStamp.getDate();

                        TimePair timePair = firstInstanceData.InstanceTimePair;

                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskIds, new CreateTaskActivity.ScheduleHint(date, timePair)));
                    } else {
                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskIds, mInstanceKey.TaskId));
                    }
                    break;
                }
                case R.id.action_group_mark_done: {
                    List<InstanceKey> instanceKeys = Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey)
                            .collect(Collectors.toList());

                    ExactTimeStamp done = DomainFactory.getDomainFactory(getActivity()).setInstancesDone(getActivity(), mData.DataId, instanceKeys);

                    List<TreeNode> selectedTreeNodes = mTreeViewAdapter.getSelectedNodes();
                    Assert.assertTrue(!selectedTreeNodes.isEmpty());

                    do {
                        Assert.assertTrue(!selectedTreeNodes.isEmpty());

                        TreeNode treeNode = Stream.of(selectedTreeNodes)
                                .max((lhs, rhs) -> Integer.valueOf(lhs.getIndentation()).compareTo(rhs.getIndentation()))
                                .get();
                        Assert.assertTrue(treeNode != null);

                        if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                            GroupAdapter.NodeCollection.NotDoneGroupNode notDoneGroupNode = (GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode();
                            Assert.assertTrue(notDoneGroupNode.singleInstance());

                            GroupListLoader.InstanceData instanceData = notDoneGroupNode.getSingleInstanceData();
                            instanceData.Done = done;

                            recursiveExists(instanceData);

                            GroupAdapter.NodeCollection nodeCollection = notDoneGroupNode.getNodeCollection();

                            nodeCollection.mDividerNode.add(instanceData);
                            nodeCollection.mNotDoneGroupCollection.remove(notDoneGroupNode);
                        } else {
                            GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = (GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode();

                            GroupListLoader.InstanceData instanceData = notDoneInstanceNode.mInstanceData;
                            instanceData.Done = done;

                            recursiveExists(instanceData);

                            notDoneInstanceNode.removeFromParent();

                            notDoneInstanceNode.getParentNodeCollection().mDividerNode.add(instanceData);
                        }

                        decrementSelected();
                    } while (!(selectedTreeNodes = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    updateSelectAll();

                    TickService.startService(getActivity());

                    break;
                }
                default: {
                    throw new UnsupportedOperationException();
                }
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

                    notDoneGroupNode.removeFromParent();
                } else if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) {
                    GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = (GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode();

                    notDoneInstanceNode.removeFromParent();
                } else {
                    GroupAdapter.NodeCollection.DoneInstanceNode doneInstanceNode = (GroupAdapter.NodeCollection.DoneInstanceNode) treeNode.getModelNode();

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

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
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

    @NonNull
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

        mTreeViewAdapter = GroupAdapter.getAdapter(this, data.DataId, data.CustomTimeDatas, useGroups(), showFab, data.InstanceDatas.values(), mExpansionState, mSelectedNodes, data.TaskDatas, data.mNote);

        mGroupListRecycler.setAdapter(mTreeViewAdapter.getAdapter());

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        if (data.InstanceDatas.isEmpty() && TextUtils.isEmpty(data.mNote)) {
            mGroupListRecycler.setVisibility(View.GONE);

            if (emptyTextId != null) {
                mEmptyText.setVisibility(View.VISIBLE);
                mEmptyText.setText(emptyTextId);
            }
        } else {
            mGroupListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }

        updateSelectAll();

        if (mPosition != null && mPosition.equals(0)) { // 24 hack
            Assert.assertTrue(mTimeRange != null);
            // relevant hack
            PruneService.startService(getActivity());
        }
    }

    private void updateSelectAll() {
        Assert.assertTrue(mData != null);

        ((GroupListListener) getActivity()).setGroupSelectAllVisibility(mPosition, Stream.of(mData.InstanceDatas.values())
                .anyMatch(instanceData -> instanceData.Done == null));
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

        @NonNull
        static TreeViewAdapter getAdapter(GroupListFragment groupListFragment, int dataId, List<GroupListLoader.CustomTimeData> customTimeDatas, boolean useGroups, boolean showFab, Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes, List<GroupListLoader.TaskData> taskDatas, @Nullable String note) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);
            Assert.assertTrue(instanceDatas != null);

            GroupAdapter groupAdapter = new GroupAdapter(groupListFragment, dataId, customTimeDatas, showFab);

            return groupAdapter.initialize(useGroups, instanceDatas, expansionState, selectedNodes, taskDatas, note);
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

        @NonNull
        private TreeViewAdapter initialize(boolean useGroups, Collection<GroupListLoader.InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes, List<GroupListLoader.TaskData> taskDatas, @Nullable String note) {
            Assert.assertTrue(instanceDatas != null);

            TreeViewAdapter treeViewAdapter = new TreeViewAdapter(mShowFab, this);
            mTreeViewAdapterReference = new WeakReference<>(treeViewAdapter);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(new WeakReference<>(treeViewAdapter));

            mNodeCollection = new NodeCollection(mDensity, 0, new WeakReference<>(this), useGroups, new WeakReference<>(treeNodeCollection), note);

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

            treeNodeCollection.setNodes(mNodeCollection.initialize(instanceDatas, expandedGroups, expandedInstances, doneExpanded, selectedNodes, true, taskDatas, unscheduledExpanded, expandedTasks));

            treeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            return treeViewAdapter;
        }

        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
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
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder, int position) {
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

        @NonNull
        private GroupListFragment getGroupListFragment() {
            GroupListFragment groupListFragment = mGroupListFragmentReference.get();
            Assert.assertTrue(groupListFragment != null);

            return groupListFragment;
        }

        ExpansionState getExpansionState() {
            List<TimeStamp> expandedGroups = mNodeCollection.getExpandedGroups();
            Assert.assertTrue(expandedGroups != null);

            HashMap<InstanceKey, Boolean> expandedInstances = new HashMap<>();
            mNodeCollection.addExpandedInstances(expandedInstances);

            boolean doneExpanded = mNodeCollection.getDoneExpanded();

            boolean unscheduledExpanded = mNodeCollection.getUnscheduledExpanded();

            List<Integer> expandedTasks = mNodeCollection.getExpandedTasks();

            return new ExpansionState(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTasks);
        }

        @NonNull
        @Override
        public GroupAdapter getGroupAdapter() {
            return this;
        }

        static class GroupHolder extends RecyclerView.ViewHolder {
            final LinearLayout mGroupRow;
            final LinearLayout mGroupRowContainer;
            final TextView mGroupRowName;
            final TextView mGroupRowDetails;
            final TextView mGroupRowChildren;
            final ImageView mGroupRowExpand;
            final CheckBox mGroupRowCheckBox;
            final View mGroupRowSeparator;

            GroupHolder(LinearLayout groupRow, LinearLayout groupRowContainer, TextView groupRowName, TextView groupRowDetails, TextView groupRowChildren, ImageView groupRowExpand, CheckBox groupRowCheckBox, View groupRowSeparator) {
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

        static class FabPaddingHolder extends RecyclerView.ViewHolder {
            FabPaddingHolder(FrameLayout frameLayout) {
                super(frameLayout);
            }
        }

        static class NodeCollection {
            private final WeakReference<NodeCollectionParent> mNodeCollectionParentReference;
            private final WeakReference<NodeContainer> mNodeContainerReference;

            private NotDoneGroupCollection mNotDoneGroupCollection;
            private DividerNode mDividerNode;
            private UnscheduledNode mUnscheduledNode;

            private final boolean mUseGroups;

            private final float mDensity;
            private final int mIndentation;

            private final String mNote;

            private NodeCollection(float density, int indentation, @NonNull WeakReference<NodeCollectionParent> nodeCollectionParentReference, boolean useGroups, @NonNull WeakReference<NodeContainer> nodeContainerReference, @Nullable String note) {
                mDensity = density;
                mIndentation = indentation;
                mNodeCollectionParentReference = nodeCollectionParentReference;
                mUseGroups = useGroups;
                mNodeContainerReference = nodeContainerReference;
                mNote = note;
            }

            @NonNull
            private List<TreeNode> initialize(@NonNull Collection<GroupListLoader.InstanceData> instanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, boolean doneExpanded, @Nullable ArrayList<InstanceKey> selectedNodes, boolean selectable, @Nullable List<GroupListLoader.TaskData> taskDatas, boolean unscheduledExpanded, @Nullable List<Integer> expandedTasks) {
                ArrayList<GroupListLoader.InstanceData> notDoneInstanceDatas = new ArrayList<>();
                ArrayList<GroupListLoader.InstanceData> doneInstanceDatas = new ArrayList<>();
                for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                    if (instanceData.Done == null)
                        notDoneInstanceDatas.add(instanceData);
                    else
                        doneInstanceDatas.add(instanceData);
                }

                List<TreeNode> rootTreeNodes = new ArrayList<>();

                if (!TextUtils.isEmpty(mNote)) {
                    Assert.assertTrue(mIndentation == 0);

                    rootTreeNodes.add(new NoteNode(mDensity, mNote, getGroupAdapter()).initialize(mNodeContainerReference));
                }

                mNotDoneGroupCollection = new NotDoneGroupCollection(mDensity, mIndentation, new WeakReference<>(this), mNodeContainerReference, selectable);

                rootTreeNodes.addAll(mNotDoneGroupCollection.initialize(notDoneInstanceDatas, expandedGroups, expandedInstances, selectedNodes));

                Assert.assertTrue((mIndentation == 0) || (taskDatas == null));
                if (taskDatas != null && !taskDatas.isEmpty()) {
                    mUnscheduledNode = new UnscheduledNode(mDensity, new WeakReference<>(this));

                    TreeNode unscheduledTreeNode = mUnscheduledNode.initialize(unscheduledExpanded, mNodeContainerReference, taskDatas, expandedTasks);

                    rootTreeNodes.add(unscheduledTreeNode);
                }

                mDividerNode = new DividerNode(mDensity, mIndentation, new WeakReference<>(this));

                doneExpanded = doneExpanded && !doneInstanceDatas.isEmpty();

                TreeNode dividerTreeNode = mDividerNode.initialize(doneExpanded, mNodeContainerReference, doneInstanceDatas, expandedInstances);
                Assert.assertTrue(dividerTreeNode != null);

                rootTreeNodes.add(dividerTreeNode);

                return rootTreeNodes;
            }

            @NonNull
            private NodeCollectionParent getNodeCollectionParent() {
                NodeCollectionParent nodeCollectionParent = mNodeCollectionParentReference.get();
                Assert.assertTrue(nodeCollectionParent != null);

                return nodeCollectionParent;
            }

            @NonNull
            private NodeContainer getNodeContainer() {
                NodeContainer nodeContainer = mNodeContainerReference.get();
                Assert.assertTrue(nodeContainer != null);

                return nodeContainer;
            }

            @NonNull
            private GroupAdapter getGroupAdapter() {
                return getNodeCollectionParent().getGroupAdapter();
            }

            List<TimeStamp> getExpandedGroups() {
                return mNotDoneGroupCollection.getExpandedGroups();
            }

            void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                mNotDoneGroupCollection.addExpandedInstances(expandedInstances);
                mDividerNode.addExpandedInstances(expandedInstances);
            }

            boolean getUnscheduledExpanded() {
                return (mUnscheduledNode != null && mUnscheduledNode.expanded());
            }

            List<Integer> getExpandedTasks() {
                if (mUnscheduledNode == null)
                    return null;
                else
                    return mUnscheduledNode.getExpandedTasks();
            }

            boolean getDoneExpanded() {
                return mDividerNode.expanded();
            }

            static class NotDoneGroupCollection {
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

                @NonNull
                private List<TreeNode> initialize(@NonNull List<GroupListLoader.InstanceData> notDoneInstanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes) {
                    ArrayList<TreeNode> notDoneGroupTreeNodes = new ArrayList<>();

                    NodeCollection nodeCollection = getNodeCollection();

                    if (nodeCollection.mUseGroups) {
                        HashMap<TimeStamp, ArrayList<GroupListLoader.InstanceData>> instanceDataHash = new HashMap<>();
                        for (GroupListLoader.InstanceData instanceData : notDoneInstanceDatas) {
                            if (!instanceDataHash.containsKey(instanceData.InstanceTimeStamp))
                                instanceDataHash.put(instanceData.InstanceTimeStamp, new ArrayList<>());
                            instanceDataHash.get(instanceData.InstanceTimeStamp).add(instanceData);
                        }

                        for (Map.Entry<TimeStamp, ArrayList<GroupListLoader.InstanceData>> entry : instanceDataHash.entrySet()) {
                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), entry.getValue(), expandedGroups, expandedInstances, selectedNodes);

                            notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
                        }
                    } else {
                        for (GroupListLoader.InstanceData instanceData : notDoneInstanceDatas) {
                            ArrayList<GroupListLoader.InstanceData> dummyInstanceDatas = new ArrayList<>();
                            dummyInstanceDatas.add(instanceData);

                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), dummyInstanceDatas, expandedGroups, expandedInstances, selectedNodes);

                            notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
                        }
                    }

                    return notDoneGroupTreeNodes;
                }

                public void remove(@NonNull NotDoneGroupNode notDoneGroupNode) {
                    Assert.assertTrue(mNotDoneGroupNodes.contains(notDoneGroupNode));
                    mNotDoneGroupNodes.remove(notDoneGroupNode);

                    NodeContainer nodeContainer = getNodeContainer();

                    TreeNode notDoneGroupTreeNode = notDoneGroupNode.getTreeNode();

                    nodeContainer.remove(notDoneGroupTreeNode);
                }

                public void add(@NonNull GroupListLoader.InstanceData instanceData) {
                    NodeCollection nodeCollection = getNodeCollection();

                    NodeContainer nodeContainer = nodeCollection.getNodeContainer();

                    ExactTimeStamp exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();

                    List<NotDoneGroupNode> timeStampNotDoneGroupNodes = Stream.of(mNotDoneGroupNodes)
                            .filter(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.equals(exactTimeStamp))
                            .collect(Collectors.toList());

                    if (timeStampNotDoneGroupNodes.isEmpty() || !nodeCollection.mUseGroups) {
                        ArrayList<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
                        instanceDatas.add(instanceData);

                        TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(new WeakReference<>(this), instanceDatas, null, null, null);

                        nodeContainer.add(notDoneGroupTreeNode);
                    } else {
                        Assert.assertTrue(timeStampNotDoneGroupNodes.size() == 1);

                        NotDoneGroupNode notDoneGroupNode = timeStampNotDoneGroupNodes.get(0);
                        Assert.assertTrue(notDoneGroupNode != null);

                        notDoneGroupNode.addInstanceData(instanceData);
                    }
                }

                @NonNull
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

                @NonNull
                private NodeCollection getNodeCollection() {
                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    return nodeCollection;
                }

                @NonNull
                private NodeContainer getNodeContainer() {
                    NodeContainer nodeContainer = mNodeContainerReference.get();
                    Assert.assertTrue(nodeContainer != null);

                    return nodeContainer;
                }

                @NonNull
                List<TimeStamp> getExpandedGroups() {
                    return Stream.of(mNotDoneGroupNodes)
                            .filter(notDoneGroupNode -> !notDoneGroupNode.singleInstance() && notDoneGroupNode.expanded())
                            .map(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.toTimeStamp())
                            .collect(Collectors.toList());
                }

                void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                    for (NotDoneGroupNode notDoneGroupNode : mNotDoneGroupNodes)
                        notDoneGroupNode.addExpandedInstances(expandedInstances);
                }
            }

            private static abstract class GroupHolderNode {
                final float mDensity;
                final int mIndentation;

                GroupHolderNode(float density, int indentation) {
                    mDensity = density;
                    mIndentation = indentation;
                }

                abstract int getNameVisibility();
                abstract String getName();
                abstract int getNameColor();

                abstract boolean getNameSingleLine();

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
                public final void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                    final GroupAdapter.GroupHolder groupHolder = (GroupAdapter.GroupHolder) viewHolder;

                    int padding = 48 * mIndentation;

                    groupHolder.mGroupRowContainer.setPadding((int) (padding * mDensity + 0.5f), 0, 0, 0);

                    int nameVisibility = getNameVisibility();
                    //noinspection ResourceType
                    groupHolder.mGroupRowName.setVisibility(nameVisibility);
                    if (nameVisibility == View.VISIBLE) {
                        groupHolder.mGroupRowName.setText(getName());
                        groupHolder.mGroupRowName.setTextColor(getNameColor());
                        groupHolder.mGroupRowName.setSingleLine(getNameSingleLine());
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

            static class NoteNode extends GroupHolderNode implements ModelNode {
                private final WeakReference<GroupAdapter> mGroupAdapterReference;

                private final String mNote;

                private WeakReference<TreeNode> mTreeNodeReference;

                NoteNode(float density, @NonNull String note, @NonNull GroupAdapter groupAdapter) {
                    super(density, 0);

                    Assert.assertTrue(!TextUtils.isEmpty(note));

                    mNote = note;
                    mGroupAdapterReference = new WeakReference<>(groupAdapter);
                }

                @NonNull
                TreeNode initialize(@NonNull WeakReference<NodeContainer> nodeContainerReference) {
                    TreeNode treeNode = new TreeNode(this, nodeContainerReference, false, false);
                    mTreeNodeReference = new WeakReference<>(treeNode);

                    treeNode.setChildTreeNodes(new ArrayList<>());
                    return treeNode;
                }

                @NonNull
                TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                @NonNull
                GroupAdapter getGroupAdapter() {
                    GroupAdapter groupAdapter = mGroupAdapterReference.get();
                    Assert.assertTrue(groupAdapter != null);

                    return groupAdapter;
                }

                @NonNull
                private GroupListFragment getGroupListFragment() {
                    return getGroupAdapter().getGroupListFragment();
                }

                @Override
                int getNameVisibility() {
                    return View.VISIBLE;
                }

                @Override
                String getName() {
                    return mNote;
                }

                @Override
                int getNameColor() {
                    return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textPrimary);
                }

                @Override
                boolean getNameSingleLine() {
                    return false;
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
                    return View.GONE;
                }

                @Override
                int getExpandImageResource() {
                    throw new UnsupportedOperationException();
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    throw new UnsupportedOperationException();
                }

                @Override
                int getCheckBoxVisibility() {
                    return View.GONE;
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
                    return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    return Color.TRANSPARENT;
                }

                @Override
                View.OnLongClickListener getOnLongClickListener() {
                    return null;
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    return null;
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
                    return true;
                }

                @Override
                public boolean visibleDuringActionMode() {
                    return false;
                }

                @Override
                public boolean separatorVisibleWhenNotExpanded() {
                    return true;
                }

                @Override
                public int compareTo(@NonNull ModelNode o) {
                    Assert.assertTrue(o instanceof NotDoneGroupNode || o instanceof UnscheduledNode || o instanceof DividerNode);

                    return -1;
                }
            }

            static class NotDoneGroupNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
                private final WeakReference<NotDoneGroupCollection> mNotDoneGroupCollectionReference;

                private WeakReference<TreeNode> mTreeNodeReference;

                private final List<GroupListLoader.InstanceData> mInstanceDatas;

                private final ArrayList<NotDoneInstanceNode> mNotDoneInstanceNodes = new ArrayList<>();
                private NodeCollection mNodeCollection;

                final ExactTimeStamp mExactTimeStamp;

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

                TreeNode initialize(List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes, WeakReference<NodeContainer> nodeContainerReference) {
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
                    mTreeNodeReference = new WeakReference<>(notDoneGroupTreeNode);

                    if (mInstanceDatas.size() == 1) {
                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(notDoneGroupTreeNode), null);

                        notDoneGroupTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceDatas.get(0).Children.values(), expandedGroups, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));
                    } else {
                        List<TreeNode> notDoneInstanceTreeNodes = Stream.of(mInstanceDatas)
                                .map(instanceData -> newChildTreeNode(instanceData, expandedInstances, selectedNodes))
                                .collect(Collectors.toList());

                        notDoneGroupTreeNode.setChildTreeNodes(notDoneInstanceTreeNodes);
                    }

                    return notDoneGroupTreeNode;
                }

                @NonNull
                GroupListLoader.InstanceData getSingleInstanceData() {
                    Assert.assertTrue(mInstanceDatas.size() == 1);

                    GroupListLoader.InstanceData instanceData = mInstanceDatas.get(0);
                    Assert.assertTrue(instanceData != null);

                    return instanceData;
                }

                boolean singleInstance() {
                    Assert.assertTrue(!mInstanceDatas.isEmpty());

                    return (mInstanceDatas.size() == 1);
                }

                void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
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
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

                        return instanceData.Name;
                    } else {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        return Stream.of(mInstanceDatas)
                                .map(instanceData -> instanceData.Name)
                                .collect(Collectors.joining(", "));
                    }
                }

                @NonNull
                private NotDoneGroupCollection getNotDoneGroupCollection() {
                    NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    return notDoneGroupCollection;
                }

                @NonNull
                private NodeCollection getNodeCollection() {
                    return getNotDoneGroupCollection().getNodeCollection();
                }

                @NonNull
                @Override
                public GroupAdapter getGroupAdapter() {
                    return getNodeCollection().getGroupAdapter();
                }

                @NonNull
                private GroupListFragment getGroupListFragment() {
                    return getGroupAdapter().getGroupListFragment();
                }

                @Override
                int getNameColor() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

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
                boolean getNameSingleLine() {
                    return true;
                }

                @Override
                int getDetailsVisibility() {
                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

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
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

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
                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

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

                        if ((instanceData.Children.isEmpty() || expanded()) && TextUtils.isEmpty(instanceData.mNote)) {
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

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();

                    Assert.assertTrue((!instanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(instanceData.mNote));

                    return getChildrenText(expanded(), instanceData.Children.values(), instanceData.mNote);
                }

                @Override
                int getChildrenColor() {
                    Assert.assertTrue(singleInstance());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();

                    Assert.assertTrue((!instanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(instanceData.mNote));

                    Activity activity = getGroupListFragment().getActivity();
                    Assert.assertTrue(activity != null);

                    if (!instanceData.TaskCurrent) {
                        return ContextCompat.getColor(activity, R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(activity, R.color.textSecondary);
                    }
                }

                @Override
                int getExpandVisibility() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

                        if (instanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && (notDoneGroupTreeNode.getSelectedChildren().size() > 0 || notDoneGroupTreeNode.displayedSize() == 1))) {
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

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

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
                    return getTreeNode().getExpandListener();
                }

                @Override
                int getCheckBoxVisibility() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

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
                    GroupListFragment groupListFragment = getGroupListFragment();

                    Assert.assertTrue(singleInstance());

                    Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                    return false;
                }

                @Override
                View.OnClickListener getCheckBoxOnClickListener() {
                    final NotDoneGroupCollection notDoneGroupCollection = getNotDoneGroupCollection();

                    NodeCollection nodeCollection = getNodeCollection();

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

                    GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();

                    Assert.assertTrue(singleInstance());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();

                    Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                    return v -> {
                        instanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupListFragment.getActivity(), groupAdapter.mDataId, instanceData.InstanceKey, true);
                        Assert.assertTrue(instanceData.Done != null);

                        recursiveExists(instanceData);

                        TickService.startService(groupListFragment.getActivity());

                        nodeCollection.mDividerNode.add(instanceData);

                        notDoneGroupCollection.remove(this);

                        groupListFragment.updateSelectAll();
                    };
                }

                @Override
                int getSeparatorVisibility() {
                    return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

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
                    TreeNode notDoneGroupTreeNode = mTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    TreeNode notDoneGroupTreeNode = mTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getOnClickListener();
                }

                @Override
                public void onClick() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), instanceData.InstanceKey));
                    } else {
                        groupListFragment.getActivity().startActivity(ShowGroupActivity.getIntent(((NotDoneGroupNode) notDoneGroupTreeNode.getModelNode()).mExactTimeStamp, groupListFragment.getActivity()));
                    }
                }

                private GroupListLoader.CustomTimeData getCustomTimeData(@NonNull DayOfWeek dayOfWeek, @NonNull HourMinute hourMinute) {
                    GroupAdapter groupAdapter = getGroupAdapter();

                    for (GroupListLoader.CustomTimeData customTimeData : groupAdapter.mCustomTimeDatas)
                        if (customTimeData.HourMinutes.get(dayOfWeek) == hourMinute)
                            return customTimeData;

                    return null;
                }

                public void remove(@NonNull NotDoneInstanceNode notDoneInstanceNode) {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    Assert.assertTrue(mInstanceDatas.contains(notDoneInstanceNode.mInstanceData));
                    mInstanceDatas.remove(notDoneInstanceNode.mInstanceData);

                    Assert.assertTrue(mNotDoneInstanceNodes.contains(notDoneInstanceNode));
                    mNotDoneInstanceNodes.remove(notDoneInstanceNode);

                    TreeNode childTreeNode = notDoneInstanceNode.getTreeNode();
                    boolean selected = childTreeNode.isSelected();

                    if (selected)
                        childTreeNode.deselect();

                    notDoneGroupTreeNode.remove(childTreeNode);

                    Assert.assertTrue(!mInstanceDatas.isEmpty());
                    if (mInstanceDatas.size() == 1) {
                        Assert.assertTrue(mNotDoneInstanceNodes.size() == 1);

                        NotDoneInstanceNode notDoneInstanceNode1 = mNotDoneInstanceNodes.get(0);
                        Assert.assertTrue(notDoneInstanceNode1 != null);

                        TreeNode childTreeNode1 = notDoneInstanceNode1.getTreeNode();

                        mNotDoneInstanceNodes.remove(notDoneInstanceNode1);

                        notDoneGroupTreeNode.remove(childTreeNode1);

                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(notDoneGroupTreeNode), null);

                        List<TreeNode> childTreeNodes = mNodeCollection.initialize(mInstanceDatas.get(0).Children.values(), null, null, false, null, mSelectable, null, false, null);

                        Stream.of(childTreeNodes)
                                .forEach(notDoneGroupTreeNode::add);

                        if (selected)
                            getTreeNode().select();
                    }
                }

                @Override
                public int compareTo(@NonNull ModelNode another) {
                    if (another instanceof NoteNode) {
                        return 1;
                    } else if (another instanceof NotDoneGroupNode) {
                        NotDoneGroupNode notDoneGroupNode = (NotDoneGroupNode) another;

                        int timeStampComparison = mExactTimeStamp.compareTo(notDoneGroupNode.mExactTimeStamp);
                        if (timeStampComparison != 0) {
                            return timeStampComparison;
                        } else {
                            Assert.assertTrue(singleInstance());
                            Assert.assertTrue(notDoneGroupNode.singleInstance());

                            return Integer.valueOf(getSingleInstanceData().InstanceKey.TaskId).compareTo(notDoneGroupNode.getSingleInstanceData().InstanceKey.TaskId);
                        }
                    } else if (another instanceof UnscheduledNode) {
                        return -1;
                    } else {
                        Assert.assertTrue(another instanceof DividerNode);

                        return -1;
                    }
                }

                void addInstanceData(GroupListLoader.InstanceData instanceData) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(instanceData.InstanceTimeStamp.toExactTimeStamp().equals(mExactTimeStamp));

                    TreeNode notDoneGroupTreeNode = mTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    Assert.assertTrue(!mInstanceDatas.isEmpty());
                    if (mInstanceDatas.size() == 1) {
                        Assert.assertTrue(mNotDoneInstanceNodes.isEmpty());

                        notDoneGroupTreeNode.removeAll();
                        mNodeCollection = null;

                        GroupListLoader.InstanceData instanceData1 = mInstanceDatas.get(0);

                        GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(mDensity, mIndentation, instanceData1, new WeakReference<>(NotDoneGroupNode.this), mSelectable);
                        mNotDoneInstanceNodes.add(notDoneInstanceNode);

                        TreeNode childTreeNode = notDoneInstanceNode.initialize(null, null, mTreeNodeReference);

                        notDoneGroupTreeNode.add(childTreeNode);
                    }

                    mInstanceDatas.add(instanceData);

                    TreeNode childTreeNode = newChildTreeNode(instanceData, null, null);
                    Assert.assertTrue(childTreeNode != null);

                    notDoneGroupTreeNode.add(childTreeNode);
                }

                TreeNode newChildTreeNode(GroupListLoader.InstanceData instanceData, HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(mTreeNodeReference != null);

                    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(mDensity, mIndentation, instanceData, new WeakReference<>(this), mSelectable);

                    TreeNode childTreeNode = notDoneInstanceNode.initialize(expandedInstances, selectedNodes, mTreeNodeReference);

                    mNotDoneInstanceNodes.add(notDoneInstanceNode);

                    return childTreeNode;
                }

                public boolean expanded() {
                    TreeNode notDoneGroupTreeNode = mTreeNodeReference.get();
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

                @Override
                public boolean separatorVisibleWhenNotExpanded() {
                    return false;
                }

                @NonNull
                TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                void removeFromParent() {
                    getNotDoneGroupCollection().remove(this);
                }

                static class NotDoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
                    private final WeakReference<NotDoneGroupNode> mNotDoneGroupNodeReference;

                    private WeakReference<TreeNode> mTreeNodeReference;

                    final GroupListLoader.InstanceData mInstanceData;

                    private NodeCollection mNodeCollection;

                    private final boolean mSelectable;

                    NotDoneInstanceNode(float density, int indentation, GroupListLoader.InstanceData instanceData, WeakReference<NotDoneGroupNode> notDoneGroupNodeReference, boolean selectable) {
                        super(density, indentation);

                        Assert.assertTrue(instanceData != null);
                        Assert.assertTrue(notDoneGroupNodeReference != null);

                        mInstanceData = instanceData;
                        mNotDoneGroupNodeReference = notDoneGroupNodeReference;
                        mSelectable = selectable;
                    }

                    TreeNode initialize(HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes, WeakReference<TreeNode> notDoneGroupTreeNodeReference) {
                        Assert.assertTrue(notDoneGroupTreeNodeReference != null);

                        TreeNode notDoneGroupTreeNode = notDoneGroupTreeNodeReference.get();

                        boolean selected = (selectedNodes != null && selectedNodes.contains(mInstanceData.InstanceKey));

                        boolean expanded = false;
                        boolean doneExpanded = false;
                        if ((expandedInstances != null && expandedInstances.containsKey(mInstanceData.InstanceKey) && !mInstanceData.Children.isEmpty())) {
                            expanded = true;
                            doneExpanded = expandedInstances.get(mInstanceData.InstanceKey);
                        }

                        TreeNode childTreeNode = new TreeNode(this, new WeakReference<>(notDoneGroupTreeNode), expanded, selected);
                        mTreeNodeReference = new WeakReference<>(childTreeNode);

                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(childTreeNode), null);
                        childTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceData.Children.values(), null, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));

                        return childTreeNode;
                    }

                    @NonNull
                    private TreeNode getTreeNode() {
                        TreeNode treeNode = mTreeNodeReference.get();
                        Assert.assertTrue(treeNode != null);

                        return treeNode;
                    }

                    @NonNull
                    private NotDoneGroupNode getParentNotDoneGroupNode() {
                        NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        return notDoneGroupNode;
                    }

                    @NonNull
                    private NotDoneGroupCollection getParentNotDoneGroupCollection() {
                        return getParentNotDoneGroupNode().getNotDoneGroupCollection();
                    }

                    @NonNull
                    private NodeCollection getParentNodeCollection() {
                        return getParentNotDoneGroupCollection().getNodeCollection();
                    }

                    private boolean expanded() {
                        return getTreeNode().expanded();
                    }

                    void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                        Assert.assertTrue(expandedInstances != null);

                        if (!expanded())
                            return;

                        Assert.assertTrue(!expandedInstances.containsKey(mInstanceData.InstanceKey));

                        expandedInstances.put(mInstanceData.InstanceKey, mNodeCollection.getDoneExpanded());

                        mNodeCollection.addExpandedInstances(expandedInstances);
                    }

                    @NonNull
                    @Override
                    public GroupAdapter getGroupAdapter() {
                        return getParentNotDoneGroupNode().getGroupAdapter();
                    }

                    @NonNull
                    private GroupListFragment getGroupListFragment() {
                        return getGroupAdapter().getGroupListFragment();
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

                        if (!mInstanceData.TaskCurrent) {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                        } else {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
                        }
                    }

                    @Override
                    boolean getNameSingleLine() {
                        return true;
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
                        if ((mInstanceData.Children.isEmpty() || expanded()) && TextUtils.isEmpty(mInstanceData.mNote)) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    String getChildren() {
                        Assert.assertTrue((!mInstanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.mNote));

                        return getChildrenText(expanded(), mInstanceData.Children.values(), mInstanceData.mNote);
                    }

                    @Override
                    int getChildrenColor() {
                        Assert.assertTrue((!mInstanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.mNote));

                        Activity activity = getGroupListFragment().getActivity();
                        Assert.assertTrue(activity != null);

                        if (!mInstanceData.TaskCurrent) {
                            return ContextCompat.getColor(activity, R.color.textDisabled);
                        } else {
                            return ContextCompat.getColor(activity, R.color.textSecondary);
                        }
                    }

                    @Override
                    int getExpandVisibility() {
                        TreeNode treeNode = getTreeNode();

                        GroupListFragment groupListFragment = getGroupListFragment();

                        if (mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && (treeNode.getSelectedChildren().size() > 0 || treeNode.displayedSize() == 1))) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    int getExpandImageResource() {
                        TreeNode treeNode = getTreeNode();

                        GroupListFragment groupListFragment = getGroupListFragment();

                        Assert.assertTrue(!(mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedChildren().size() > 0)));

                        if (treeNode.expanded())
                            return R.drawable.ic_expand_less_black_36dp;
                        else
                            return R.drawable.ic_expand_more_black_36dp;
                    }

                    @Override
                    View.OnClickListener getExpandOnClickListener() {
                        TreeNode treeNode = getTreeNode();

                        GroupListFragment groupListFragment = getGroupListFragment();

                        Assert.assertTrue(!(mInstanceData.Children.isEmpty() || (groupListFragment.mSelectionCallback.hasActionMode() && treeNode.getSelectedChildren().size() > 0)));

                        return treeNode.getExpandListener();
                    }

                    @Override
                    int getCheckBoxVisibility() {
                        GroupListFragment groupListFragment = getGroupListFragment();

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
                        final NotDoneGroupNode notDoneGroupNode = getParentNotDoneGroupNode();

                        final TreeNode notDoneGroupTreeNode = notDoneGroupNode.getTreeNode();

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NodeCollection nodeCollection = getParentNodeCollection();

                        GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

                        GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();

                        Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                        return v -> {
                            Assert.assertTrue(notDoneGroupTreeNode.expanded());

                            mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupListFragment.getActivity(), groupAdapter.mDataId, mInstanceData.InstanceKey, true);
                            Assert.assertTrue(mInstanceData.Done != null);

                            recursiveExists(mInstanceData);

                            TickService.startService(groupListFragment.getActivity());

                            notDoneGroupNode.remove(this);

                            nodeCollection.mDividerNode.add(mInstanceData);

                            groupListFragment.updateSelectAll();
                        };
                    }

                    @Override
                    int getSeparatorVisibility() {
                        TreeNode childTreeNode = mTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        return (childTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                    }

                    @Override
                    int getBackgroundColor() {
                        final NotDoneGroupNode notDoneGroupNode = getParentNotDoneGroupNode();

                        final TreeNode notDoneGroupTreeNode = notDoneGroupNode.getTreeNode();

                        TreeNode childTreeNode = getTreeNode();

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        GroupListFragment groupListFragment = getGroupListFragment();

                        if (childTreeNode.isSelected())
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected);
                        else
                            return Color.TRANSPARENT;
                    }

                    @Override
                    View.OnLongClickListener getOnLongClickListener() {
                        TreeNode childTreeNode = mTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        return childTreeNode.getOnLongClickListener();
                    }

                    @Override
                    View.OnClickListener getOnClickListener() {
                        TreeNode childTreeNode = mTreeNodeReference.get();
                        Assert.assertTrue(childTreeNode != null);

                        return childTreeNode.getOnClickListener();
                    }

                    @Override
                    public void onClick() {
                        GroupListFragment groupListFragment = getGroupListFragment();

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

                    @Override
                    public boolean separatorVisibleWhenNotExpanded() {
                        return false;
                    }

                    void removeFromParent() {
                        getParentNotDoneGroupNode().remove(this);
                    }
                }
            }

            static class DividerNode extends GroupHolderNode implements ModelNode {
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

                void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
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
                    return getGroupListFragment().getString(R.string.done);
                }

                @Override
                int getNameColor() {
                    return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textPrimary);
                }

                @Override
                boolean getNameSingleLine() {
                    return true;
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

                @NonNull
                private NodeCollection getNodeCollection() {
                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    return nodeCollection;
                }

                @NonNull
                private GroupAdapter getGroupAdapter() {
                    return getNodeCollection().getGroupAdapter();
                }

                @NonNull
                private GroupListFragment getGroupListFragment() {
                    return getGroupAdapter().getGroupListFragment();
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
                    Assert.assertTrue(another instanceof NoteNode || another instanceof NotDoneGroupNode || another instanceof UnscheduledNode);
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

                @Override
                public boolean separatorVisibleWhenNotExpanded() {
                    return false;
                }

                TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }
            }

            static class DoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
                private final WeakReference<DividerNode> mDividerNodeReference;

                private WeakReference<TreeNode> mTreeNodeReference;

                private final GroupListLoader.InstanceData mInstanceData;

                private NodeCollection mNodeCollection;

                DoneInstanceNode(float density, int indentation, GroupListLoader.InstanceData instanceData, WeakReference<DividerNode> dividerNodeReference) {
                    super(density, indentation);

                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(dividerNodeReference != null);

                    mInstanceData = instanceData;
                    mDividerNodeReference = dividerNodeReference;
                    Assert.assertTrue(mDividerNodeReference.get() != null);
                }

                TreeNode initialize(@NonNull TreeNode dividerTreeNode, HashMap<InstanceKey, Boolean> expandedInstances) {
                    boolean expanded = false;
                    boolean doneExpanded = false;
                    if (expandedInstances != null && expandedInstances.containsKey(mInstanceData.InstanceKey) && !mInstanceData.Children.isEmpty()) {
                        expanded = true;
                        doneExpanded = expandedInstances.get(mInstanceData.InstanceKey);
                    }

                    TreeNode doneTreeNode = new TreeNode(this, new WeakReference<>(dividerTreeNode), expanded, false);
                    mTreeNodeReference = new WeakReference<>(doneTreeNode);

                    mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, new WeakReference<>(this), false, new WeakReference<>(doneTreeNode), null);
                    doneTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceData.Children.values(), null, expandedInstances, doneExpanded, null, false, null, false, null));

                    return doneTreeNode;
                }

                @NonNull
                private TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                @NonNull
                private DividerNode getDividerNode() {
                    DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    return dividerNode;
                }

                @NonNull
                private NodeCollection getParentNodeCollection() {
                    return getDividerNode().getNodeCollection();
                }

                private boolean expanded() {
                    return getTreeNode().expanded();
                }

                void addExpandedInstances(HashMap<InstanceKey, Boolean> expandedInstances) {
                    Assert.assertTrue(expandedInstances != null);

                    if (!expanded())
                        return;

                    Assert.assertTrue(!expandedInstances.containsKey(mInstanceData.InstanceKey));

                    expandedInstances.put(mInstanceData.InstanceKey, mNodeCollection.getDoneExpanded());

                    mNodeCollection.addExpandedInstances(expandedInstances);
                }

                @NonNull
                @Override
                public GroupAdapter getGroupAdapter() {
                    return getParentNodeCollection().getGroupAdapter();
                }

                @NonNull
                private GroupListFragment getGroupListFragment() {
                    return getGroupAdapter().getGroupListFragment();
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

                    if (!mInstanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textPrimary);
                    }
                }

                @Override
                boolean getNameSingleLine() {
                    return true;
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
                    if ((mInstanceData.Children.isEmpty() || expanded()) && TextUtils.isEmpty(mInstanceData.mNote)) {
                        return View.GONE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                String getChildren() {
                    Assert.assertTrue((!mInstanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.mNote));

                    return getChildrenText(expanded(), mInstanceData.Children.values(), mInstanceData.mNote);
                }

                @Override
                int getChildrenColor() {
                    Assert.assertTrue((!mInstanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(mInstanceData.mNote));

                    Activity activity = getGroupListFragment().getActivity();
                    Assert.assertTrue(activity != null);

                    if (!mInstanceData.TaskCurrent) {
                        return ContextCompat.getColor(activity, R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(activity, R.color.textSecondary);
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
                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

                    if (getTreeNode().expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    Assert.assertTrue(!mInstanceData.Children.isEmpty());

                    return getTreeNode().getExpandListener();
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
                    final DividerNode dividerNode = getDividerNode();

                    NodeCollection nodeCollection = dividerNode.getNodeCollection();

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

                    GroupListFragment groupListFragment = groupAdapter.getGroupListFragment();

                    return v -> {
                        mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(groupListFragment.getActivity(), groupAdapter.mDataId, mInstanceData.InstanceKey, false);
                        Assert.assertTrue(mInstanceData.Done == null);

                        TickService.startService(groupListFragment.getActivity());

                        dividerNode.remove(this);

                        nodeCollection.mNotDoneGroupCollection.add(mInstanceData);

                        groupListFragment.updateSelectAll();
                    };
                }

                @Override
                int getSeparatorVisibility() {
                    return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    return Color.TRANSPARENT;
                }

                @Override
                View.OnLongClickListener getOnLongClickListener() {
                    return getTreeNode().getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    return getTreeNode().getOnClickListener();
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

                @Override
                public boolean separatorVisibleWhenNotExpanded() {
                    return false;
                }

                void removeFromParent() {
                    getDividerNode().remove(this);
                }
            }

            static class UnscheduledNode extends GroupHolderNode implements ModelNode, TaskParent {
                private final WeakReference<NodeCollection> mNodeCollectionReference;

                private WeakReference<TreeNode> mTreeNodeReference;

                private List<TaskNode> mTaskNodes;

                UnscheduledNode(float density, WeakReference<NodeCollection> nodeCollectionReference) {
                    super(density, 0);

                    Assert.assertTrue(nodeCollectionReference != null);

                    mNodeCollectionReference = nodeCollectionReference;
                }

                @NonNull
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

                @NonNull
                private TreeNode newChildTreeNode(GroupListLoader.TaskData taskData, List<Integer> expandedTasks) {
                    TaskNode taskNode = new TaskNode(mDensity, 0, taskData, new WeakReference<>(this));

                    mTaskNodes.add(taskNode);

                    return taskNode.initialize(getTreeNode(), expandedTasks);
                }

                @NonNull
                private NodeCollection getNodeCollection() {
                    NodeCollection nodeCollection = mNodeCollectionReference.get();
                    Assert.assertTrue(nodeCollection != null);

                    return nodeCollection;
                }

                public boolean expanded() {
                    return getTreeNode().expanded();
                }

                List<Integer> getExpandedTasks() {
                    return Stream.of(mTaskNodes)
                            .flatMap(TaskNode::getExpandedTasks)
                            .collect(Collectors.toList());
                }

                @NonNull
                @Override
                public GroupAdapter getGroupAdapter() {
                    return getNodeCollection().getGroupAdapter();
                }

                @NonNull
                private GroupListFragment getGroupListFragment() {
                    return getGroupAdapter().getGroupListFragment();
                }

                @NonNull
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
                    return getGroupListFragment().getString(R.string.noReminder);
                }

                @Override
                int getNameColor() {
                    return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textPrimary);
                }

                @Override
                boolean getNameSingleLine() {
                    return true;
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
                    if (getTreeNode().expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    return getTreeNode().getExpandListener();
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
                    return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    return Color.TRANSPARENT;
                }

                @Override
                View.OnLongClickListener getOnLongClickListener() {
                    return getTreeNode().getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    return getTreeNode().getOnClickListener();
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

                @Override
                public boolean separatorVisibleWhenNotExpanded() {
                    return false;
                }
            }

            static class TaskNode extends GroupHolderNode implements ModelNode, TaskParent {
                private final WeakReference<TaskParent> mTaskParentReference;

                private final GroupListLoader.TaskData mTaskData;

                private WeakReference<TreeNode> mTreeNodeReference;

                private List<TaskNode> mTaskNodes;

                TaskNode(float density, int indentation, GroupListLoader.TaskData taskData, WeakReference<TaskParent> taskParentReference) {
                    super(density, indentation);

                    Assert.assertTrue(taskData != null);
                    Assert.assertTrue(taskParentReference != null);

                    mTaskData = taskData;
                    mTaskParentReference = taskParentReference;
                }

                @NonNull
                TreeNode initialize(TreeNode parentTreeNode, List<Integer> expandedTasks) {
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

                @NonNull
                private TreeNode newChildTreeNode(GroupListLoader.TaskData taskData, List<Integer> expandedTasks) {
                    TaskNode taskNode = new TaskNode(mDensity, mIndentation + 1, taskData, new WeakReference<>(this));

                    mTaskNodes.add(taskNode);

                    return taskNode.initialize(getTreeNode(), expandedTasks);
                }

                @NonNull
                private TaskParent getTaskParent() {
                    TaskParent taskParent = mTaskParentReference.get();
                    Assert.assertTrue(taskParent != null);

                    return taskParent;
                }

                @NonNull
                @Override
                public GroupAdapter getGroupAdapter() {
                    return getTaskParent().getGroupAdapter();
                }

                @NonNull
                private GroupListFragment getGroupListFragment() {
                    return getGroupAdapter().getGroupListFragment();
                }

                @NonNull
                private TreeNode getTreeNode() {
                    TreeNode treeNode = mTreeNodeReference.get();
                    Assert.assertTrue(treeNode != null);

                    return treeNode;
                }

                private boolean expanded() {
                    return getTreeNode().expanded();
                }

                Stream<Integer> getExpandedTasks() {
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
                    return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textPrimary);
                }

                @Override
                boolean getNameSingleLine() {
                    return true;
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

                    return ContextCompat.getColor(getGroupListFragment().getActivity(), R.color.textSecondary);
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

                    Assert.assertTrue(!mTaskData.Children.isEmpty());

                    if (treeNode.expanded())
                        return R.drawable.ic_expand_less_black_36dp;
                    else
                        return R.drawable.ic_expand_more_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    Assert.assertTrue(!mTaskData.Children.isEmpty());

                    return getTreeNode().getExpandListener();
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
                    return (getTreeNode().getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    return Color.TRANSPARENT;
                }

                @Override
                View.OnLongClickListener getOnLongClickListener() {
                    return getTreeNode().getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    return getTreeNode().getOnClickListener();
                }

                @Override
                public boolean selectable() {
                    return false;
                }

                @Override
                public void onClick() {
                    GroupListFragment groupListFragment = getGroupListFragment();

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

                @Override
                public boolean separatorVisibleWhenNotExpanded() {
                    return false;
                }
            }
        }
    }

    public static class ExpansionState implements Parcelable {
        final boolean DoneExpanded;
        final List<TimeStamp> ExpandedGroups;
        final HashMap<InstanceKey, Boolean> ExpandedInstances;
        final boolean UnscheduledExpanded;
        final List<Integer> ExpandedTasks;

        ExpansionState(boolean doneExpanded, List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, boolean unscheduledExpanded, List<Integer> expandedTasks) {
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

        void setGroupSelectAllVisibility(Integer position, boolean selectAllVisible);
    }

    private interface NodeCollectionParent {
        @NonNull
        GroupAdapter getGroupAdapter();
    }

    private interface TaskParent {
        @NonNull
        GroupAdapter getGroupAdapter();
    }

    @NonNull
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

    private static String getChildrenText(boolean expanded, @NonNull Collection<GroupListLoader.InstanceData> instanceDatas, @Nullable String note) {
        if (!instanceDatas.isEmpty() && !expanded) {
            Stream<GroupListLoader.InstanceData> notDone = Stream.of(instanceDatas)
                    .filter(instanceData -> instanceData.Done == null)
                    .sortBy(instanceData -> instanceData.InstanceKey.TaskId);

            Stream<GroupListLoader.InstanceData> done = Stream.of(instanceDatas)
                    .filter(instanceData -> instanceData.Done != null)
                    .sortBy(instanceData -> -instanceData.Done.getLong());

            return Stream.concat(notDone, done)
                    .map(instanceData -> instanceData.Name)
                    .collect(Collectors.joining(", "));
        } else {
            Assert.assertTrue(!TextUtils.isEmpty(note));

            return note;
        }
    }

    public void selectAll() {
        mTreeViewAdapter.selectAll();
    }

    public void destroyLoader() {
        getLoaderManager().destroyLoader(0);
    }
}