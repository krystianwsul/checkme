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
import android.support.v4.util.Pair;
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
            mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.unselect(mTreeViewAdapter.mTreeNodeCollection, mTreeViewAdapter);
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            Assert.assertTrue(mTreeViewAdapter != null);

            List<NotDoneInstanceTreeNode> selected = mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.getSelected();
            Assert.assertTrue(selected != null);
            Assert.assertTrue(!selected.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_group_edit_instance:
                    Assert.assertTrue(!selected.isEmpty());

                    if (selected.size() == 1) {
                        GroupListLoader.InstanceData instanceData = selected.get(0).getNotDoneInstanceNode().mInstanceData;
                        Assert.assertTrue(instanceData.IsRootInstance);

                        startActivity(EditInstanceActivity.getIntent(getActivity(), instanceData.InstanceKey));
                    } else {
                        Assert.assertTrue(selected.size() > 1);

                        Assert.assertTrue(Stream.of(selected)
                            .allMatch(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.getNotDoneInstanceNode().mInstanceData.IsRootInstance));

                        ArrayList<InstanceKey> instanceKeys = Stream.of(selected)
                                .map(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.getNotDoneInstanceNode().mInstanceData.InstanceKey)
                                .collect(Collectors.toCollection(ArrayList::new));

                        startActivity(EditInstancesActivity.getIntent(getActivity(), instanceKeys));
                    }
                    break;
                case R.id.action_group_show_task:
                    Assert.assertTrue(selected.size() == 1);

                    GroupListLoader.InstanceData instanceData2 = selected.get(0).getNotDoneInstanceNode().mInstanceData;
                    Assert.assertTrue(instanceData2.TaskCurrent);

                    startActivity(ShowTaskActivity.getIntent(instanceData2.InstanceKey.TaskId, getActivity()));
                    break;
                case R.id.action_group_edit_task:
                    Assert.assertTrue(selected.size() == 1);

                    GroupListLoader.InstanceData instanceData3 = selected.get(0).getNotDoneInstanceNode().mInstanceData;
                    Assert.assertTrue(instanceData3.TaskCurrent);

                    if (instanceData3.IsRootTask)
                        startActivity(CreateRootTaskActivity.getEditIntent(getActivity(), instanceData3.InstanceKey.TaskId));
                    else
                        startActivity(CreateChildTaskActivity.getEditIntent(getActivity(), instanceData3.InstanceKey.TaskId));
                    break;
                case R.id.action_group_delete_task: {
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(selected)
                            .map(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.getNotDoneInstanceNode().mInstanceData.InstanceKey.TaskId)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(!taskIds.isEmpty());
                    Assert.assertTrue(Stream.of(selected)
                            .allMatch(notDoneInstanceNode -> notDoneInstanceNode.getNotDoneInstanceNode().mInstanceData.TaskCurrent));

                    List<Node> selectedNodes = mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.getSelectedNodes();

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(mTreeViewAdapter.getGroupAdapter().mDataId, taskIds);
                    for (NotDoneInstanceTreeNode notDoneInstanceTreeNode : selected)
                        notDoneInstanceTreeNode.getNotDoneInstanceNode().mInstanceData.TaskCurrent = false;

                    TickService.startService(getActivity());

                    for (Node node : selectedNodes) {
                        GroupListLoader.InstanceData instanceData1;
                        if (node instanceof NotDoneGroupTreeNode) {
                            instanceData1 = ((NotDoneGroupTreeNode) node).getSingleInstanceData();
                        } else {
                            Assert.assertTrue(node instanceof NotDoneInstanceTreeNode);
                            instanceData1 = ((NotDoneInstanceTreeNode) node).getNotDoneInstanceNode().mInstanceData;
                        }

                        if (instanceData1.Exists) {
                            mTreeViewAdapter.notifyItemChanged(mTreeViewAdapter.mTreeNodeCollection.getPosition(node));
                        } else {
                            NotDoneGroupTreeNode notDoneGroupTreeNode;
                            if (node instanceof NotDoneGroupTreeNode) {
                                notDoneGroupTreeNode = (NotDoneGroupTreeNode) node;
                            } else {
                                NotDoneInstanceTreeNode notDoneInstanceTreeNode = (NotDoneInstanceTreeNode) node;

                                notDoneGroupTreeNode = notDoneInstanceTreeNode.mNotDoneGroupTreeNodeReference.get();
                                Assert.assertTrue(notDoneGroupTreeNode != null);
                            }

                            Assert.assertTrue(!notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.isEmpty());
                            if (notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.size() == 1) {
                                int position = mTreeViewAdapter.mTreeNodeCollection.getPosition(notDoneGroupTreeNode);
                                mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.remove(notDoneGroupTreeNode);
                                mTreeViewAdapter.notifyItemRemoved(position);
                            } else {
                                Assert.assertTrue(node instanceof NotDoneInstanceTreeNode);

                                notDoneGroupTreeNode.remove((NotDoneInstanceTreeNode) node, mTreeViewAdapter.mTreeNodeCollection, mTreeViewAdapter);
                            }

                            decrementSelected();
                        }
                    }

                    break;
                }
                case R.id.action_group_join:
                    ArrayList<Integer> taskIds = new ArrayList<>(Stream.of(selected)
                            .map(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.getNotDoneInstanceNode().mInstanceData.InstanceKey.TaskId)
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

            if (mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode.getTotalDoneCount() > 0) {
                if (mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode.expanded())
                    mTreeViewAdapter.notifyItemRangeRemoved(mTreeViewAdapter.mTreeNodeCollection.getPosition(mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode), mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode.getTotalDoneCount() + 1);
                else
                    mTreeViewAdapter.notifyItemRemoved(mTreeViewAdapter.mTreeNodeCollection.getPosition(mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode));
            }

            int last = mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() - 1;
            mTreeViewAdapter.notifyItemChanged(last);

            mActionMode.getMenuInflater().inflate(R.menu.menu_edit_groups, mActionMode.getMenu());

            mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.updateCheckBoxes(mTreeViewAdapter.mTreeNodeCollection, mTreeViewAdapter);

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
            if (mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode.getTotalDoneCount() > 0) {
                if (mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode.expanded())
                    mTreeViewAdapter.notifyItemRangeInserted(mTreeViewAdapter.mTreeNodeCollection.getPosition(mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode), mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode.getTotalDoneCount() + 1);
                else
                    mTreeViewAdapter.notifyItemInserted(mTreeViewAdapter.mTreeNodeCollection.getPosition(mTreeViewAdapter.mTreeNodeCollection.mDividerTreeNode));
            }

            int last = mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.displayedSize() - 1;
            mTreeViewAdapter.notifyItemChanged(last);

            mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.updateCheckBoxes(mTreeViewAdapter.mTreeNodeCollection, mTreeViewAdapter);

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

            List<NotDoneInstanceTreeNode> selected = mTreeViewAdapter.mTreeNodeCollection.mNotDoneGroupTreeCollection.getSelected();
            Assert.assertTrue(selected != null);
            Assert.assertTrue(!selected.isEmpty());
            Assert.assertTrue(Stream.of(selected).allMatch(node -> (node.getNotDoneInstanceNode().mInstanceData.Done == null)));

            if (selected.size() == 1) {
                GroupListLoader.InstanceData instanceData = selected.get(0).getNotDoneInstanceNode().mInstanceData;
                Assert.assertTrue(instanceData != null);

                menu.findItem(R.id.action_group_edit_instance).setVisible(instanceData.IsRootInstance);
                menu.findItem(R.id.action_group_show_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_edit_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_join).setVisible(false);
                menu.findItem(R.id.action_group_delete_task).setVisible(instanceData.TaskCurrent);
            } else {
                Assert.assertTrue(selected.size() > 1);

                boolean isRootInstance = selected.get(0).getNotDoneInstanceNode().mInstanceData.IsRootInstance;
                Assert.assertTrue(Stream.of(selected)
                    .allMatch(notDoneInstanceNode -> notDoneInstanceNode.getNotDoneInstanceNode().mInstanceData.IsRootInstance == isRootInstance));

                menu.findItem(R.id.action_group_edit_instance).setVisible(isRootInstance);
                menu.findItem(R.id.action_group_show_task).setVisible(false);
                menu.findItem(R.id.action_group_edit_task).setVisible(false);

                if (Stream.of(selected).allMatch(node -> node.getNotDoneInstanceNode().mInstanceData.TaskCurrent)) {
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
                ArrayList<InstanceKey> selected = mTreeViewAdapter.getSelected();
                Assert.assertTrue(!selected.isEmpty());
                outState.putParcelableArrayList(SELECTED_NODES_KEY, selected);
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

            ArrayList<InstanceKey> selected = mTreeViewAdapter.getSelected();
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

        GroupAdapter groupAdapter = GroupAdapter.getAdapter(this, data.DataId, data.CustomTimeDatas, useGroups(), showFab);
        mTreeViewAdapter = new TreeViewAdapter(showFab, groupAdapter.getTreeModelAdapter());
        groupAdapter.setTreeViewAdapterReference(new WeakReference<>(mTreeViewAdapter));
        mTreeViewAdapter.setInstanceDatas(data.InstanceDatas.values(), mExpansionState, mSelectedNodes);
        mGroupListRecycler.setAdapter(mTreeViewAdapter);

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelected().size());

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

            if (position < treeViewAdapter.mTreeNodeCollection.getItemCount()) {
                Node node = treeViewAdapter.mTreeNodeCollection.getNode(position);
                node.onBindViewHolder(abstractHolder);
            } else {
                Assert.assertTrue(mShowFab);
                Assert.assertTrue(position == treeViewAdapter.mTreeNodeCollection.getItemCount());
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
                    public NodeCollection getNodeCollection() {
                        return NodeCollection.this;
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
                        Assert.assertTrue(lhs.singleInstance());
                        Assert.assertTrue(rhs.singleInstance());

                        return Integer.valueOf(lhs.getSingleInstanceData().InstanceKey.TaskId).compareTo(rhs.getSingleInstanceData().InstanceKey.TaskId);
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

                public static NotDoneGroupNode newNotDoneGroupNode(WeakReference<NotDoneGroupCollection> notDoneGroupCollectionReference) {
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

                @Override
                int getNameVisibility() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
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

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        return notDoneInstanceNode.mInstanceData.Name;
                    } else {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        return Stream.of(notDoneGroupTreeNode.mNotDoneInstanceTreeNodes)
                                .map(notDoneInstanceTreeNode -> notDoneInstanceTreeNode.getNotDoneInstanceNode().mInstanceData.Name)
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        if (!notDoneInstanceNode.mInstanceData.TaskCurrent) {
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

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        if (TextUtils.isEmpty(notDoneInstanceNode.mInstanceData.DisplayText)) {
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        Assert.assertTrue(!TextUtils.isEmpty(notDoneInstanceNode.mInstanceData.DisplayText));

                        return notDoneInstanceNode.mInstanceData.DisplayText;
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        if (!notDoneInstanceNode.mInstanceData.TaskCurrent) {
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

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        if (TextUtils.isEmpty(notDoneInstanceNode.mInstanceData.Children)) {
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

                    Assert.assertTrue(notDoneGroupTreeNode.singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                    NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                    Assert.assertTrue(notDoneInstanceNode != null);

                    Assert.assertTrue(!TextUtils.isEmpty(notDoneInstanceNode.mInstanceData.Children));

                    return notDoneInstanceNode.mInstanceData.Children;
                }

                @Override
                int getChildrenColor() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(notDoneGroupTreeNode.singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                    NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                    Assert.assertTrue(notDoneInstanceNode != null);

                    if (!notDoneInstanceNode.mInstanceData.TaskCurrent) {
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        if (TextUtils.isEmpty(notDoneInstanceNode.mInstanceData.Children)) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        if (groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelected().count() > 0)
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                        Assert.assertTrue(notDoneInstanceNode != null);

                        Assert.assertTrue(!TextUtils.isEmpty(notDoneInstanceNode.mInstanceData.Children));
                        return R.drawable.ic_list_black_36dp;
                    } else {
                        Assert.assertTrue(!(groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelected().count() > 0));

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

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        return null;
                    } else {
                        Assert.assertTrue(!(groupListFragment.mSelectionCallback.hasActionMode() && notDoneGroupTreeNode.getSelected().count() > 0));

                        return (v -> {
                            int position = treeNodeCollection.getPosition(notDoneGroupTreeNode);

                            if (notDoneGroupTreeNode.expanded()) { // hiding
                                Assert.assertTrue(notDoneGroupTreeNode.getSelected().count() == 0);

                                int displayedSize = notDoneGroupTreeNode.displayedSize();
                                notDoneGroupTreeNode.mNotDoneGroupNodeExpanded = false;
                                treeViewAdapter.notifyItemRangeRemoved(position + 1, displayedSize - 1);
                            } else { // showing
                                notDoneGroupTreeNode.mNotDoneGroupNodeExpanded = true;
                                treeViewAdapter.notifyItemRangeInserted(position + 1, notDoneGroupTreeNode.displayedSize() - 1);
                            }

                            if ((position) > 0 && (treeNodeCollection.getNode(position - 1) instanceof NotDoneGroupNode)) {
                                treeViewAdapter.notifyItemRangeChanged(position - 1, 2);
                            } else {
                                treeViewAdapter.notifyItemChanged(position);
                            }
                        });
                    }
                }

                @Override
                int getCheckBoxVisibility() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        if (groupListFragment.mSelectionCallback.hasActionMode()) {
                            return View.INVISIBLE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        if (notDoneGroupTreeNode.mNotDoneGroupNodeExpanded) {
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(notDoneGroupTreeNode.singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    Assert.assertTrue(notDoneGroupTreeNode.singleInstance());

                    Assert.assertTrue(!notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                    NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                    Assert.assertTrue(notDoneInstanceNode != null);

                    Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                    return v -> {
                        NotDoneGroupTreeCollection notDoneGroupTreeCollection = notDoneGroupCollection.mNotDoneGroupTreeCollectionReference.get();
                        Assert.assertTrue(notDoneGroupTreeCollection != null);

                        notDoneInstanceNode.mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(treeViewAdapter.getGroupAdapter().mDataId, notDoneInstanceNode.mInstanceData.InstanceKey, true);
                        Assert.assertTrue(notDoneInstanceNode.mInstanceData.Done != null);

                        TickService.startService(groupListFragment.getActivity());

                        int oldPosition = treeNodeCollection.getPosition(notDoneGroupTreeNode);
                        notDoneGroupTreeCollection.remove(notDoneGroupTreeNode);

                        treeViewAdapter.notifyItemRemoved(oldPosition);

                        treeNodeCollection.mDividerTreeNode.add(notDoneInstanceNode.mInstanceData, oldPosition, treeNodeCollection, treeViewAdapter);
                    };
                }

                @Override
                int getSeparatorVisibility() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    NotDoneGroupTreeCollection notDoneGroupTreeCollection = notDoneGroupCollection.mNotDoneGroupTreeCollectionReference.get();
                    Assert.assertTrue(notDoneGroupTreeCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    boolean showSeparator = false;
                    if (!notDoneGroupTreeNode.mNotDoneGroupNodeExpanded) {
                        int position = treeNodeCollection.getPosition(notDoneGroupTreeNode);
                        boolean last = (position == notDoneGroupTreeCollection.displayedSize() - 1);
                        if (!last) {
                            NotDoneGroupTreeNode nextNode = (NotDoneGroupTreeNode) treeNodeCollection.getNode(position + 1);
                            if (nextNode.expanded())
                                showSeparator = true;
                        } else {
                            if (treeNodeCollection.mDividerTreeNode.visible() && treeNodeCollection.mDividerTreeNode.expanded())
                                showSeparator = true;
                        }
                    }

                    if (showSeparator)
                        return View.VISIBLE;
                    else
                        return View.INVISIBLE;
                }

                @Override
                int getBackgroundColor() {
                    NotDoneGroupTreeNode notDoneGroupTreeNode = mNotDoneGroupTreeNodeReference.get();
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    final NotDoneGroupCollection notDoneGroupCollection = mNotDoneGroupCollectionReference.get();
                    Assert.assertTrue(notDoneGroupCollection != null);

                    final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                    Assert.assertTrue(treeNodeCollection != null);

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        Assert.assertTrue(!notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0);
                        Assert.assertTrue(notDoneInstanceTreeNode != null);

                        if (notDoneInstanceTreeNode.isSelected())
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                    Assert.assertTrue(groupListFragment != null);

                    NotDoneInstanceNode notDoneInstanceNode = notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.get(0).getNotDoneInstanceNode();
                    Assert.assertTrue(notDoneInstanceNode != null);

                    if (notDoneGroupTreeNode.singleInstance()) {
                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), notDoneInstanceNode.mInstanceData.InstanceKey));
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                    Assert.assertTrue(treeViewAdapter != null);

                    for (GroupListLoader.CustomTimeData customTimeData : treeViewAdapter.getGroupAdapter().mCustomTimeDatas)
                        if (customTimeData.HourMinutes.get(dayOfWeek) == hourMinute)
                            return customTimeData;

                    return null;
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

                        Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                        Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                        Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                        Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                        Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
                        Assert.assertTrue(treeViewAdapter != null);

                        GroupListFragment groupListFragment = treeViewAdapter.getGroupAdapter().mGroupListFragmentReference.get();
                        Assert.assertTrue(groupListFragment != null);

                        Assert.assertTrue(!groupListFragment.mSelectionCallback.hasActionMode());

                        return v -> {
                            Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                            NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                            Assert.assertTrue(notDoneInstanceTreeNode != null);

                            mInstanceData.Done = DomainFactory.getDomainFactory(groupListFragment.getActivity()).setInstanceDone(treeViewAdapter.getGroupAdapter().mDataId, mInstanceData.InstanceKey, true);
                            Assert.assertTrue(mInstanceData.Done != null);

                            TickService.startService(groupListFragment.getActivity());

                            int oldInstancePosition = treeNodeCollection.getPosition(notDoneInstanceTreeNode);

                            notDoneGroupTreeNode.remove(notDoneInstanceTreeNode, treeNodeCollection, treeViewAdapter);

                            treeNodeCollection.mDividerTreeNode.add(mInstanceData, oldInstancePosition, treeNodeCollection, treeViewAdapter);
                        };
                    }

                    @Override
                    int getSeparatorVisibility() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                        Assert.assertTrue(notDoneInstanceTreeNode != null);

                        final boolean lastInGroup = (notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.indexOf(notDoneInstanceTreeNode) == notDoneGroupTreeNode.mNotDoneInstanceTreeNodes.size() - 1);

                        if (lastInGroup) {
                            return View.VISIBLE;
                        } else {
                            return View.INVISIBLE;
                        }
                    }

                    @Override
                    int getBackgroundColor() {
                        final NotDoneGroupNode notDoneGroupNode = mNotDoneGroupNodeReference.get();
                        Assert.assertTrue(notDoneGroupNode != null);

                        final NotDoneGroupTreeNode notDoneGroupTreeNode = notDoneGroupNode.mNotDoneGroupTreeNodeReference.get();
                        Assert.assertTrue(notDoneGroupTreeNode != null);

                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = mNotDoneInstanceTreeNodeReference.get();
                        Assert.assertTrue(notDoneInstanceTreeNode != null);

                        Assert.assertTrue(notDoneGroupTreeNode.mNotDoneGroupNodeExpanded);

                        NotDoneGroupCollection notDoneGroupCollection = notDoneGroupNode.mNotDoneGroupCollectionReference.get();
                        Assert.assertTrue(notDoneGroupCollection != null);

                        final TreeNodeCollection treeNodeCollection = notDoneGroupCollection.mTreeNodeCollectionReference.get();
                        Assert.assertTrue(treeNodeCollection != null);

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                        final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                            final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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
                            DoneTreeNode doneTreeNode = new DoneTreeNode(doneInstanceNode.getDoneModelNode());
                            doneInstanceNode.setDoneTreeNodeReference(new WeakReference<>(doneTreeNode), new WeakReference<>(dividerTreeNode));

                            return doneTreeNode;
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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

                        dividerTreeNode.remove(doneTreeNode, treeNodeCollection, treeViewAdapter);

                        Pair<Boolean, Pair<NotDoneGroupTreeNode, NotDoneInstanceTreeNode>> pair = treeNodeCollection.mNotDoneGroupTreeCollection.add(mInstanceData, treeNodeCollection, treeViewAdapter);
                        boolean newNotDoneGroupNode = pair.first;
                        NotDoneGroupTreeNode notDoneGroupTreeNode = pair.second.first;
                        NotDoneInstanceTreeNode notDoneInstanceTreeNode = pair.second.second;

                        if (newNotDoneGroupNode) {
                            int newGroupPosition = treeNodeCollection.getPosition(notDoneGroupTreeNode);
                            treeViewAdapter.notifyItemInserted(newGroupPosition);
                        } else {
                            if (notDoneGroupTreeNode.expanded()) {
                                int newGroupPosition = treeNodeCollection.getPosition(notDoneGroupTreeNode);
                                int newInstancePosition = treeNodeCollection.getPosition(notDoneInstanceTreeNode);

                                boolean last = (newGroupPosition + notDoneGroupTreeNode.displayedSize() - 1 == newInstancePosition);

                                treeViewAdapter.notifyItemChanged(newGroupPosition);
                                treeViewAdapter.notifyItemInserted(newInstancePosition);

                                if (last)
                                    treeViewAdapter.notifyItemChanged(newInstancePosition - 1);
                            } else {
                                int newGroupPosition = treeNodeCollection.getPosition(notDoneGroupTreeNode);
                                treeViewAdapter.notifyItemChanged(newGroupPosition);
                            }
                        }
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

                    final TreeViewAdapter treeViewAdapter = treeNodeCollection.getNodeCollection().mTreeViewAdapterReference.get();
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
}