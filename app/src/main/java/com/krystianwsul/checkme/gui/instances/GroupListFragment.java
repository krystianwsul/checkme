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
import com.krystianwsul.checkme.gui.instances.tree.DividerModelNode;
import com.krystianwsul.checkme.gui.instances.tree.DividerTreeNode;
import com.krystianwsul.checkme.gui.instances.tree.DoneModelNode;
import com.krystianwsul.checkme.gui.instances.tree.DoneTreeNode;
import com.krystianwsul.checkme.gui.instances.tree.ModelNodeCollection;
import com.krystianwsul.checkme.gui.instances.tree.NotDoneGroupModelCollection;
import com.krystianwsul.checkme.gui.instances.tree.NotDoneGroupModelNode;
import com.krystianwsul.checkme.gui.instances.tree.NotDoneGroupTreeCollection;
import com.krystianwsul.checkme.gui.instances.tree.NotDoneGroupTreeNode;
import com.krystianwsul.checkme.gui.instances.tree.NotDoneInstanceModelNode;
import com.krystianwsul.checkme.gui.instances.tree.NotDoneInstanceTreeNode;
import com.krystianwsul.checkme.gui.instances.tree.TreeModelAdapter;
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
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Comparator;
import java.util.List;

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

                    List<Node> selectedNodes = mTreeViewAdapter.getSelectedNodes();

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(mTreeViewAdapter.getGroupAdapter().mDataId, taskIds);
                    for (GroupListLoader.InstanceData instanceData : instanceDatas)
                        instanceData.TaskCurrent = false;

                    TickService.startService(getActivity());

                    for (Node node : selectedNodes) {
                        GroupListLoader.InstanceData instanceData1;
                        if (node instanceof NotDoneGroupTreeNode) {
                            instanceData1 = ((NotDoneGroupTreeNode) node).getNotDoneGroupModelNode().getSingleInstanceData();
                        } else {
                            Assert.assertTrue(node instanceof NotDoneInstanceTreeNode);
                            instanceData1 = ((NotDoneInstanceTreeNode) node).getNotDoneInstanceNode().mInstanceData;
                        }

                        if (instanceData1.Exists) {
                            node.update();
                        } else {
                            node.removeFromParent();

                            decrementSelected();
                        }
                    }

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

                boolean isRootInstance = instanceDatas.get(0).IsRootInstance;
                Assert.assertTrue(Stream.of(instanceDatas)
                    .allMatch(instanceData -> instanceData.IsRootInstance == isRootInstance));

                menu.findItem(R.id.action_group_edit_instance).setVisible(isRootInstance);
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
            outState.putParcelable(EXPANSION_STATE_KEY, mTreeViewAdapter.getExpansionState());

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
        if (mTreeViewAdapter != null) {
            mExpansionState = mTreeViewAdapter.getExpansionState();

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

        mTreeViewAdapter = null; // fuck you!
        mGroupListRecycler.setAdapter(null);

        GroupAdapter groupAdapter = GroupAdapter.getAdapter(this, data.DataId, data.CustomTimeDatas, useGroups(), showFab);
        mTreeViewAdapter = new TreeViewAdapter(showFab, groupAdapter.getTreeModelAdapter());
        groupAdapter.setTreeViewAdapterReference(new WeakReference<>(mTreeViewAdapter));
        mTreeViewAdapter.setInstanceDatas(data.InstanceDatas.values(), mExpansionState, mSelectedNodes);
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

    public static class GroupAdapter {
        private static final int TYPE_GROUP = 0;
        private static final int TYPE_DIVIDER = 1;

        private final WeakReference<GroupListFragment> mGroupListFragmentReference;

        private final int mDataId;
        private final ArrayList<GroupListLoader.CustomTimeData> mCustomTimeDatas;
        public final boolean mUseGroups;
        private final boolean mShowFab;

        private WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

        private NodeCollection mNodeCollection;

        public static GroupAdapter getAdapter(GroupListFragment groupListFragment, int dataId, ArrayList<GroupListLoader.CustomTimeData> customTimeDatas, boolean useGroups, boolean showFab) {
            Assert.assertTrue(groupListFragment != null);
            Assert.assertTrue(customTimeDatas != null);

            return new GroupAdapter(groupListFragment, dataId, customTimeDatas, useGroups, showFab);
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

        public void setTreeViewAdapterReference(WeakReference<TreeViewAdapter> treeViewAdapterReference) {
            mTreeViewAdapterReference = treeViewAdapterReference;
        }

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
            } else if (viewType == TYPE_DIVIDER) {
                RelativeLayout rowGroupListDivider = (RelativeLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list_divider, parent, false);

                ImageView groupListDividerImage = (ImageView) rowGroupListDivider.findViewById(R.id.group_list_divider_image);
                Assert.assertTrue(groupListDividerImage != null);

                return new DividerHolder(rowGroupListDivider, groupListDividerImage);
            } else {
                Assert.assertTrue(viewType == TreeViewAdapter.TYPE_FAB_PADDING);

                FrameLayout frameLayout = (FrameLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list_fab_padding, parent, false);
                return new FabPaddingHolder(frameLayout);
            }
        }

        public void onBindViewHolder(AbstractHolder abstractHolder, int position) {
            Assert.assertTrue(position >= 0);

            TreeViewAdapter treeViewAdapter = mTreeViewAdapterReference.get();
            Assert.assertTrue(treeViewAdapter != null);

            Assert.assertTrue(position < treeViewAdapter.getItemCount());

            if (position < treeViewAdapter.displayedSize()) {
                Node node = treeViewAdapter.getNode(position);
                node.onBindViewHolder(abstractHolder);
            } else {
                Assert.assertTrue(position == treeViewAdapter.displayedSize());
                Assert.assertTrue(mShowFab);
                Assert.assertTrue(position == treeViewAdapter.getItemCount() - 1);
            }
        }

        public TreeModelAdapter getTreeModelAdapter() {
            return new TreeModelAdapter() {
                @Override
                public AbstractHolder onCreateViewHolder(ViewGroup parent, int viewType) {
                    return GroupAdapter.this.onCreateViewHolder(parent, viewType);
                }

                @Override
                public void onBindViewHolder(AbstractHolder holder, int position) {
                    GroupAdapter.this.onBindViewHolder(holder, position);
                }

                @Override
                public GroupAdapter getGroupAdapter() {
                    return GroupAdapter.this;
                }

                @Override
                public SelectionCallback getSelectionCallback() {
                    GroupListFragment groupListFragment = mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    return groupListFragment.mSelectionCallback;
                }
            };
        }

        public void setNodeCollection(NodeCollection nodeCollection) {
            Assert.assertTrue(nodeCollection != null);

            mNodeCollection = nodeCollection;
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
            private final WeakReference<TreeViewAdapter> mTreeViewAdapterReference;

            private DividerModelNode mDividerModelNode;

            public static NodeCollection newNodeCollection(WeakReference<TreeViewAdapter> treeViewAdapterReference) {
                Assert.assertTrue(treeViewAdapterReference != null);

                return new NodeCollection(treeViewAdapterReference);
            }

            private NodeCollection(WeakReference<TreeViewAdapter> treeViewAdapterReference) {
                Assert.assertTrue(treeViewAdapterReference != null);
                mTreeViewAdapterReference = treeViewAdapterReference;
            }

            public ModelNodeCollection getModelNodeCollection() {
                return new ModelNodeCollection() {
                    @Override
                    public void setDividerModelNode(DividerModelNode dividerModelNode) {
                        Assert.assertTrue(dividerModelNode != null);

                        mDividerModelNode = dividerModelNode;
                    }
                };
            }

            public static class NotDoneGroupCollection {
                private final WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

                private WeakReference<NotDoneGroupTreeCollection> mNotDoneGroupTreeCollectionReference;

                private final Comparator<NotDoneGroupTreeNode> sComparator = (lhs, rhs) -> {
                    int timeStampComparison = lhs.mExactTimeStamp.compareTo(rhs.mExactTimeStamp);
                    if (timeStampComparison != 0) {
                        return timeStampComparison;
                    } else {
                        Assert.assertTrue(lhs.getNotDoneGroupModelNode().singleInstance());
                        Assert.assertTrue(rhs.getNotDoneGroupModelNode().singleInstance());

                        return Integer.valueOf(lhs.getNotDoneGroupModelNode().getSingleInstanceData().InstanceKey.TaskId).compareTo(rhs.getNotDoneGroupModelNode().getSingleInstanceData().InstanceKey.TaskId);
                    }
                };

                public static NotDoneGroupCollection newNotDoneGroupCollection(WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
                    Assert.assertTrue(treeNodeCollectionReference != null);

                    return new NotDoneGroupCollection(treeNodeCollectionReference);
                }

                private NotDoneGroupCollection(WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
                    Assert.assertTrue(treeNodeCollectionReference != null);
                    mTreeNodeCollectionReference = treeNodeCollectionReference;
                }

                public void setNotDoneGroupTreeCollectionReference(WeakReference<NotDoneGroupTreeCollection> notDoneGroupTreeCollectionReference) {
                    Assert.assertTrue(notDoneGroupTreeCollectionReference != null);

                    mNotDoneGroupTreeCollectionReference = notDoneGroupTreeCollectionReference;
                }

                public NotDoneGroupModelCollection getNotDoneGroupModelCollection() {
                    return new NotDoneGroupModelCollection() {
                        @Override
                        public Comparator<NotDoneGroupTreeNode> getComparator() {
                            return sComparator;
                        }

                        @Override
                        public NotDoneGroupCollection getNotDoneGroupCollection() {
                            return NotDoneGroupCollection.this;
                        }

                        @Override
                        public NotDoneGroupTreeNode newNotDoneGroupNode(WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference, List<GroupListLoader.InstanceData> instanceDatas, boolean expanded, ArrayList<InstanceKey> selectedNodes) {
                            Assert.assertTrue(notDoneGroupCollectionReference != null);

                            NotDoneGroupNode notDoneGroupNode = NotDoneGroupNode.newNotDoneGroupNode(notDoneGroupCollectionReference);
                            Assert.assertTrue(notDoneGroupNode != null);

                            NotDoneGroupTreeNode notDoneGroupTreeNode = new NotDoneGroupTreeNode(notDoneGroupNode.getNotDoneGroupModelNode(), expanded, mNotDoneGroupTreeCollectionReference);
                            notDoneGroupNode.setNotDoneGroupTreeNodeReference(new WeakReference<>(notDoneGroupTreeNode));
                            notDoneGroupTreeNode.setInstanceDatas(instanceDatas, selectedNodes);

                            return notDoneGroupTreeNode;
                        }
                    };
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

            public static class NotDoneGroupNode extends GroupHolderNode {
                private final WeakReference<NotDoneGroupCollection> mNotDoneGroupCollectionReference;

                private WeakReference<NotDoneGroupTreeNode> mNotDoneGroupTreeNodeReference;

                private final ArrayList<GroupListLoader.InstanceData> mInstanceDatas = new ArrayList<>();

                private static NotDoneGroupNode newNotDoneGroupNode(WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference) {
                    Assert.assertTrue(notDoneGroupCollectionReference != null);

                    return new NotDoneGroupNode(notDoneGroupCollectionReference);
                }

                private NotDoneGroupNode(WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference) {
                    Assert.assertTrue(notDoneGroupCollectionReference != null);

                    mNotDoneGroupCollectionReference = notDoneGroupCollectionReference;
                }

                public void setNotDoneGroupTreeNodeReference(WeakReference<NotDoneGroupTreeNode> notDoneGroupTreeNodeReference) {
                    Assert.assertTrue(notDoneGroupTreeNodeReference != null);

                    mNotDoneGroupTreeNodeReference = notDoneGroupTreeNodeReference;
                }

                private GroupListLoader.InstanceData getSingleInstanceData() {
                    Assert.assertTrue(mInstanceDatas.size() == 1);
                    return mInstanceDatas.get(0);
                }

                public boolean singleInstance() {
                    Assert.assertTrue(!mInstanceDatas.isEmpty());

                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (mInstanceDatas.size() == 1 || !notDoneGroupTreeNode.expanded())
                        Assert.assertTrue(notDoneGroupTreeNode.displayedSize() == 1);
                    else
                        Assert.assertTrue(mInstanceDatas.size() + 1 == mNotDoneGroupTreeNodeReference.get().displayedSize());

                    return (mInstanceDatas.size() == 1);
                }

                @Override
                int getNameVisibility() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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

                @Override
                int getNameColor() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        Assert.assertTrue(!TextUtils.isEmpty(instanceData.DisplayText));

                        return instanceData.DisplayText;
                    } else {
                        Date date = notDoneGroupTreeNode.mExactTimeStamp.getDate();
                        HourMinute hourMinute = notDoneGroupTreeNode.mExactTimeStamp.toTimeStamp().getHourMinute();

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (TextUtils.isEmpty(instanceData.Children)) {
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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    Assert.assertTrue(singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    Assert.assertTrue(!TextUtils.isEmpty(instanceData.Children));

                    return instanceData.Children;
                }

                @Override
                int getChildrenColor() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        if (TextUtils.isEmpty(instanceData.Children)) {
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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        Assert.assertTrue(!TextUtils.isEmpty(instanceData.Children));
                        return R.drawable.ic_list_black_36dp;
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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getExpandListener();
                }

                @Override
                int getCheckBoxVisibility() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                    Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                    return false;
                }

                @Override
                View.OnClickListener getCheckBoxOnClickListener() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupAdapter groupAdapter = treeViewAdapter.getGroupAdapter();
                    Assert.assertTrue(groupAdapter != null);

                    GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                    GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                    Assert.assertTrue(instanceData != null);

                    Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                    return v -> {
                        NotDoneGroupTreeCollection notDoneGroupTreeCollection = notDoneGroupCollection.mNotDoneGroupTreeCollectionReference.get();
                        Assert.assertTrue(notDoneGroupTreeCollection != null);

                        instanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(treeViewAdapter.getGroupAdapter().mDataId, instanceData.InstanceKey, true);
                        Assert.assertTrue(instanceData.Done != null);

                        TickService.startService(groupListFragment.getActivity());

                        int oldPosition = notDoneGroupTreeCollection.remove(notDoneGroupTreeNode);

                        groupAdapter.mNodeCollection.mDividerModelNode.add(instanceData, oldPosition);
                    };
                }

                @Override
                int getSeparatorVisibility() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return (notDoneGroupTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                }

                @Override
                int getBackgroundColor() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

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
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    return notDoneGroupTreeNode.getOnClickListener();
                }

                private void onClick() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (singleInstance()) {
                        GroupListLoader.InstanceData instanceData = getSingleInstanceData();
                        Assert.assertTrue(instanceData != null);

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), instanceData.InstanceKey));
                    } else {
                        groupListFragment.getActivity().startActivity(ShowGroupActivity.getIntent(notDoneGroupTreeNode.mExactTimeStamp, groupListFragment.getActivity()));
                    }
                }

                private GroupListLoader.CustomTimeData getCustomTimeData(DayOfWeek dayOfWeek, HourMinute hourMinute) {
                    Assert.assertTrue(dayOfWeek != null);
                    Assert.assertTrue(hourMinute != null);

                    NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    for (GroupListLoader.CustomTimeData customTimeData : treeViewAdapter.getGroupAdapter().mCustomTimeDatas)
                        if (customTimeData.HourMinutes.get(dayOfWeek) == hourMinute)
                            return customTimeData;

                    return null;
                }

                public void remove(NotDoneInstanceTreeNode notDoneInstanceTreeNode) {
                    Assert.assertTrue(notDoneInstanceTreeNode != null);

                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    NotDoneInstanceNode notDoneInstanceNode = notDoneInstanceTreeNode.getNotDoneInstanceNode();
                    Assert.assertTrue(notDoneInstanceNode != null);

                    Assert.assertTrue(mInstanceDatas.contains(notDoneInstanceNode.mInstanceData));
                    mInstanceDatas.remove(notDoneInstanceNode.mInstanceData);

                    notDoneGroupTreeNode.remove(notDoneInstanceTreeNode);
                }

                public NotDoneGroupModelNode getNotDoneGroupModelNode() {
                    return new NotDoneGroupModelNode() {
                        @Override
                        public NotDoneGroupNode getNotDoneGroupNode() {
                            return NotDoneGroupNode.this;
                        }

                        @Override
                        public void onBindViewHolder(AbstractHolder abstractHolder) {
                            NotDoneGroupNode.this.onBindViewHolder(abstractHolder);
                        }

                        @Override
                        public int getItemViewType() {
                            return NotDoneGroupNode.this.getItemViewType();
                        }

                        @Override
                        public void onClick() {
                            NotDoneGroupNode.this.onClick();
                        }

                        @Override
                        public NotDoneInstanceTreeNode newNotDoneInstanceTreeNode(GroupListLoader.InstanceData instanceData, ArrayList<InstanceKey> selectedNodes) {
                            Assert.assertTrue(instanceData != null);
                            Assert.assertTrue(mNotDoneGroupTreeNodeReference != null);

                            GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(instanceData, new WeakReference<>(NotDoneGroupNode.this));
                            mInstanceDatas.add(instanceData);

                            NotDoneInstanceTreeNode notDoneInstanceTreeNode = new NotDoneInstanceTreeNode(notDoneInstanceNode.getNotDoneInstanceModelNode(), selectedNodes);
                            notDoneInstanceNode.setNotDoneInstanceTreeNodeReference(new WeakReference<>(notDoneInstanceTreeNode));
                            notDoneInstanceTreeNode.setNotDoneGroupTreeNodeReference(mNotDoneGroupTreeNodeReference);

                            return notDoneInstanceTreeNode;
                        }

                        @Override
                        public void remove(NotDoneInstanceTreeNode notDoneInstanceTreeNode) {
                            NotDoneGroupNode.this.remove(notDoneInstanceTreeNode);
                        }

                        @Override
                        public boolean singleInstance() {
                            return NotDoneGroupNode.this.singleInstance();
                        }

                        @Override
                        public GroupListLoader.InstanceData getSingleInstanceData() {
                            return NotDoneGroupNode.this.getSingleInstanceData();
                        }
                    };
                }

                public static class NotDoneInstanceNode extends GroupHolderNode {
                    private final WeakReference<NotDoneGroupNode> mNotDoneGroupNodeReference;

                    private WeakReference<NotDoneInstanceTreeNode> mNotDoneInstanceTreeNodeReference;

                    public final GroupListLoader.InstanceData mInstanceData;

                    public NotDoneInstanceNode(GroupListLoader.InstanceData instanceData, WeakReference<NotDoneGroupNode> notDoneGroupNodeReference) {
                        Assert.assertTrue(instanceData != null);
                        Assert.assertTrue(notDoneGroupNodeReference != null);

                        mInstanceData = instanceData;
                        mNotDoneGroupNodeReference = notDoneGroupNodeReference;
                    }

                    public void setNotDoneInstanceTreeNodeReference(WeakReference<NotDoneInstanceTreeNode> notDoneInstanceTreeNodeReference) {
                        Assert.assertTrue(notDoneInstanceTreeNodeReference != null);

                        mNotDoneInstanceTreeNodeReference = notDoneInstanceTreeNodeReference;
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

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
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
                        if (TextUtils.isEmpty(mInstanceData.Children)) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    String getChildren() {
                        Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.Children));
                        return mInstanceData.Children;
                    }

                    @Override
                    int getChildrenColor() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        if (!mInstanceData.TaskCurrent) {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                        } else {
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                        }
                    }

                    @Override
                    int getExpandVisibility() {
                        if (TextUtils.isEmpty(mInstanceData.Children)) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    int getExpandImageResource() {
                        Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.Children));
                        return R.drawable.ic_list_black_36dp;
                    }

                    @Override
                    View.OnClickListener getExpandOnClickListener() {
                        Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.Children));
                        return null;
                    }

                    @Override
                    int getCheckBoxVisibility() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        if (groupListFragment.mSelectionCallback.hasActionMode()) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    }

                    @Override
                    boolean getCheckBoxChecked() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                        return false;
                    }

                    @Override
                    View.OnClickListener getCheckBoxOnClickListener() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupAdapter groupAdapter = treeViewAdapter.getGroupAdapter();
                        Assert.assertTrue(groupAdapter != null);

                        GroupListFragment groupListFragment = groupAdapter.mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                        return v -> {
                            Assert.assertTrue(notDoneGroupTreeNode.expanded());

                            NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                            Assert.assertTrue(notDoneInstanceTreeNode != null);

                            mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(treeViewAdapter.getGroupAdapter().mDataId, mInstanceData.InstanceKey, true);
                            Assert.assertTrue(mInstanceData.Done != null);

                            TickService.startService(groupListFragment.getActivity());

                            int oldInstancePosition = treeNodeCollection.getPosition(notDoneInstanceTreeNode);

                            notDoneGroupNode.remove(notDoneInstanceTreeNode);

                            groupAdapter.mNodeCollection.mDividerModelNode.add(mInstanceData, oldInstancePosition);
                        };
                    }

                    @Override
                    int getSeparatorVisibility() {
                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                        Assert.assertTrue(notDoneInstanceTreeNode != null);

                        return (notDoneInstanceTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
                    }

                    @Override
                    int getBackgroundColor() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                        Assert.assertTrue(notDoneInstanceTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        if (notDoneInstanceTreeNode.isSelected())
                            return ContextCompat.getColor(groupListFragment.getActivity(), R.color.selected);
                        else
                            return Color.TRANSPARENT;
                    }

                    @Override
                    View.OnLongClickListener getOnLongClickListener() {
                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                        Assert.assertTrue(notDoneInstanceTreeNode != null);

                        return notDoneInstanceTreeNode.getOnLongClickListener();
                    }

                    @Override
                    View.OnClickListener getOnClickListener() {
                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                        Assert.assertTrue(notDoneInstanceTreeNode != null);

                        return notDoneInstanceTreeNode.getOnClickListener();
                    }

                    private void onInstanceClick() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey));
                    }

                    public NotDoneInstanceModelNode getNotDoneInstanceModelNode() {
                        return new NotDoneInstanceModelNode() {
                            @Override
                            public void onBindViewHolder(AbstractHolder abstractHolder) {
                                NotDoneInstanceNode.this.onBindViewHolder(abstractHolder);
                            }

                            @Override
                            public int getItemViewType() {
                                return NotDoneInstanceNode.this.getItemViewType();
                            }

                            @Override
                            public NotDoneInstanceNode getNotDoneInstanceNode() {
                                return NotDoneInstanceNode.this;
                            }

                            @Override
                            public void onClick() {
                                NotDoneInstanceNode.this.onInstanceClick();
                            }

                            @Override
                            public int compareTo(@NonNull NotDoneInstanceModelNode another) {
                                return Integer.valueOf(mInstanceData.InstanceKey.TaskId).compareTo(another.getNotDoneInstanceNode().mInstanceData.InstanceKey.TaskId);
                            }
                        };
                    }
                }
            }

            public static class DividerNode {
                private final WeakReference<TreeNodeCollection> mTreeNodeCollectionReference;

                private WeakReference<DividerTreeNode> mDividerTreeNodeReference;

                private final ArrayList<DoneModelNode> mDoneModelNodes = new ArrayList<>();

                public static DividerTreeNode newDividerTreeNode(ArrayList<GroupListLoader.InstanceData> instanceDatas, boolean doneExpanded, WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
                    Assert.assertTrue(instanceDatas != null);
                    Assert.assertTrue(treeNodeCollectionReference != null);

                    DividerNode dividerNode = new DividerNode(treeNodeCollectionReference);
                    DividerTreeNode dividerTreeNode = new DividerTreeNode(dividerNode.getDividerModelNode(), doneExpanded, treeNodeCollectionReference);
                    dividerNode.setDividerTreeNodeReference(new WeakReference<>(dividerTreeNode));

                    dividerTreeNode.setInstanceDatas(instanceDatas);

                    return dividerTreeNode;
                }

                private DividerNode(WeakReference<TreeNodeCollection> treeNodeCollectionReference) {
                    Assert.assertTrue(treeNodeCollectionReference != null);

                    mTreeNodeCollectionReference = treeNodeCollectionReference;
                }

                private void setDividerTreeNodeReference(WeakReference<DividerTreeNode> dividerTreeNodeReference) {
                    Assert.assertTrue(dividerTreeNodeReference != null);
                    mDividerTreeNodeReference = dividerTreeNodeReference;
                }

                public void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder) {
                    final GroupAdapter.DividerHolder dividerHolder = (GroupAdapter.DividerHolder) abstractHolder;

                    final TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    DividerTreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    if (dividerTreeNode.expanded())
                        dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_less_black_36dp);
                    else
                        dividerHolder.GroupListDividerImage.setImageResource(R.drawable.ic_expand_more_black_36dp);

                    dividerHolder.GroupListDividerImage.setOnClickListener(dividerTreeNode.getExpandListener());
                }

                public int getItemViewType() {
                    return TYPE_DIVIDER;
                }

                public void remove(DoneTreeNode doneTreeNode) {
                    Assert.assertTrue(doneTreeNode != null);

                    DividerTreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                    Assert.assertTrue(dividerTreeNode != null);

                    DoneModelNode doneModelNode = doneTreeNode.getDoneModelNode();
                    Assert.assertTrue(doneModelNode != null);
                    Assert.assertTrue(mDoneModelNodes.contains(doneModelNode));

                    mDoneModelNodes.remove(doneModelNode);

                    dividerTreeNode.remove(doneTreeNode);
                }

                public DividerModelNode getDividerModelNode() {
                    return new DividerModelNode() {
                        @Override
                        public void onBindViewHolder(AbstractHolder abstractHolder) {
                            DividerNode.this.onBindViewHolder(abstractHolder);
                        }

                        @Override
                        public int getItemViewType() {
                            return DividerNode.this.getItemViewType();
                        }

                        @Override
                        public boolean hasActionMode() {
                            final TreeNodeCollection treeNodeCollection = mTreeNodeCollectionReference.get();
                            Assert.assertTrue(treeNodeCollection != null);

                            final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                            Assert.assertTrue(treeViewAdapter != null);

                            GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                            Assert.assertTrue(groupListFragment != null);

                            return groupListFragment.mSelectionCallback.hasActionMode();
                        }
                        @Override
                        public DoneTreeNode newDoneTreeNode(GroupListLoader.InstanceData instanceData, DividerTreeNode dividerTreeNode) {
                            Assert.assertTrue(instanceData.Done != null);
                            Assert.assertTrue(dividerTreeNode != null);

                            DoneInstanceNode doneInstanceNode = new DoneInstanceNode(instanceData, new WeakReference<>(DividerNode.this));

                            DoneModelNode doneModelNode = doneInstanceNode.getDoneModelNode();
                            Assert.assertTrue(doneModelNode != null);

                            mDoneModelNodes.add(doneModelNode);

                            DoneTreeNode doneTreeNode = new DoneTreeNode(doneModelNode);
                            doneInstanceNode.setDoneTreeNodeReference(new WeakReference<>(doneTreeNode), new WeakReference<>(dividerTreeNode));

                            return doneTreeNode;
                        }

                        @Override
                        public void add(GroupListLoader.InstanceData instanceData, int oldInstancePosition) {
                            DividerTreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                            Assert.assertTrue(dividerTreeNode != null);

                            DoneTreeNode doneTreeNode = newDoneTreeNode(instanceData, dividerTreeNode);
                            Assert.assertTrue(doneTreeNode != null);

                            dividerTreeNode.add(doneTreeNode, oldInstancePosition);
                        }
                    };
                }
            }

            public static class DoneInstanceNode extends GroupHolderNode {
                private final WeakReference<DividerNode> mDividerNodeReference;
                private WeakReference<DividerTreeNode> mDividerTreeNodeReference;

                private WeakReference<DoneTreeNode> mDoneTreeNodeReference;

                private final GroupListLoader.InstanceData mInstanceData;

                public DoneInstanceNode(GroupListLoader.InstanceData instanceData, WeakReference<DividerNode> dividerNodeReference) {
                    Assert.assertTrue(instanceData != null);
                    Assert.assertTrue(dividerNodeReference != null);

                    mInstanceData = instanceData;
                    mDividerNodeReference = dividerNodeReference;
                }

                public void setDoneTreeNodeReference(WeakReference<DoneTreeNode> doneTreeNodeReference, WeakReference<DividerTreeNode> dividerTreeNodeReference) {
                    Assert.assertTrue(doneTreeNodeReference != null);
                    Assert.assertTrue(dividerTreeNodeReference != null);

                    mDoneTreeNodeReference = doneTreeNodeReference;
                    mDividerTreeNodeReference = dividerTreeNodeReference;
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

                    final TreeNodeCollection treeNodeCollection = dividerNode.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
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

                    final TreeNodeCollection treeNodeCollection = dividerNode.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (!mInstanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                    }
                }

                @Override
                int getChildrenVisibility() {
                    if (TextUtils.isEmpty(mInstanceData.Children)) {
                        return View.GONE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                String getChildren() {
                    Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.Children));

                    return mInstanceData.Children;
                }

                @Override
                int getChildrenColor() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    final TreeNodeCollection treeNodeCollection = dividerNode.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.Children));

                    if (!mInstanceData.TaskCurrent) {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textDisabled);
                    } else {
                        return ContextCompat.getColor(groupListFragment.getActivity(), R.color.textSecondary);
                    }
                }

                @Override
                int getExpandVisibility() {
                    if (TextUtils.isEmpty(mInstanceData.Children)) {
                        return View.INVISIBLE;
                    } else {
                        return View.VISIBLE;
                    }
                }

                @Override
                int getExpandImageResource() {
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    final TreeNodeCollection treeNodeCollection = dividerNode.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.Children));

                    return R.drawable.ic_list_black_36dp;
                }

                @Override
                View.OnClickListener getExpandOnClickListener() {
                    Assert.assertTrue(!TextUtils.isEmpty(mInstanceData.Children));
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

                    final TreeNodeCollection treeNodeCollection = dividerNode.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    return v -> {
                        DoneTreeNode doneTreeNode = mDoneTreeNodeReference.get();
                        Assert.assertTrue(doneTreeNode != null);

                        DividerTreeNode dividerTreeNode = mDividerTreeNodeReference.get();
                        Assert.assertTrue(dividerTreeNode != null);

                        mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(treeViewAdapter.getGroupAdapter().mDataId, mInstanceData.InstanceKey, false);
                        Assert.assertTrue(mInstanceData.Done == null);

                        TickService.startService(groupListFragment.getActivity());

                        dividerNode.remove(doneTreeNode);

                        treeNodeCollection.mNotDoneGroupTreeCollection.add(mInstanceData);
                    };
                }

                @Override
                int getSeparatorVisibility() {
                    return View.INVISIBLE;
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
                    final DividerNode dividerNode = mDividerNodeReference.get();
                    Assert.assertTrue(dividerNode != null);

                    final TreeNodeCollection treeNodeCollection = dividerNode.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getTreeViewAdapter();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    return v -> groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey));
                }

                public DoneModelNode getDoneModelNode() {
                    return new DoneModelNode() {
                        @Override
                        public DoneInstanceNode getDoneInstanceNode() {
                            return DoneInstanceNode.this;
                        }

                        @Override
                        public void onBindViewHolder(AbstractHolder abstractHolder) {
                            DoneInstanceNode.this.onBindViewHolder(abstractHolder);
                        }

                        @Override
                        public int getItemViewType() {
                            return DoneInstanceNode.this.getItemViewType();
                        }

                        @Override
                        public int compareTo(@NonNull DoneModelNode another) {
                            return -mInstanceData.Done.compareTo(another.getDoneInstanceNode().mInstanceData.Done); // negate
                        }
                    };
                }
            }
        }
    }

    public interface Node {
        void onBindViewHolder(GroupAdapter.AbstractHolder abstractHolder);
        int getItemViewType();
        void update();
        void removeFromParent();
    }

    public interface NodeContainer {
        int displayedSize();
        Node getNode(int position);
        int getPosition(Node node);
        boolean expanded();
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

    private static List<GroupListLoader.InstanceData> nodesToInstanceDatas(List<Node> nodes) {
        Assert.assertTrue(nodes != null);

        List<GroupListLoader.InstanceData> instanceDatas = new ArrayList<>();
        for (Node node : nodes) {
            if (node instanceof NotDoneGroupTreeNode) {
                GroupListLoader.InstanceData instanceData = ((NotDoneGroupTreeNode) node).getNotDoneGroupModelNode().getSingleInstanceData();
                Assert.assertTrue(instanceData != null);

                instanceDatas.add(instanceData);
            } else {
                Assert.assertTrue(node instanceof NotDoneInstanceTreeNode);

                GroupListLoader.InstanceData instanceData = ((NotDoneInstanceTreeNode) node).getNotDoneInstanceNode().mInstanceData;
                Assert.assertTrue(instanceData != null);

                instanceDatas.add(instanceData);
            }
        }

        return instanceDatas;
    }
}