package com.krystianwsul.checkme.gui.instances.tree;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.content.ContextCompat;
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
import android.widget.ProgressBar;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.DataDiff;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.FabUser;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.instances.EditInstanceActivity;
import com.krystianwsul.checkme.gui.instances.EditInstancesActivity;
import com.krystianwsul.checkme.gui.instances.ShowGroupActivity;
import com.krystianwsul.checkme.gui.instances.ShowInstanceActivity;
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity;
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity;
import com.krystianwsul.checkme.gui.tree.ModelNode;
import com.krystianwsul.checkme.gui.tree.NodeContainer;
import com.krystianwsul.checkme.gui.tree.TreeModelAdapter;
import com.krystianwsul.checkme.gui.tree.TreeNode;
import com.krystianwsul.checkme.gui.tree.TreeNodeCollection;
import com.krystianwsul.checkme.gui.tree.TreeViewAdapter;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

public class GroupListFragment extends AbstractFragment implements FabUser {
    private final static String EXPANSION_STATE_KEY = "expansionState";
    private final static String SELECTED_NODES_KEY = "selectedNodes";

    private ProgressBar mGroupListProgress;
    private RecyclerView mGroupListRecycler;
    private TreeViewAdapter mTreeViewAdapter;
    private TextView mEmptyText;

    private Integer mPosition;
    private MainActivity.TimeRange mTimeRange;
    private TimeStamp mTimeStamp;
    private InstanceKey mInstanceKey;
    private Set<InstanceKey> mInstanceKeys;

    private ExpansionState mExpansionState;
    private ArrayList<InstanceKey> mSelectedNodes;

    @Nullable
    private Integer mDataId;

    @Nullable
    private DataWrapper mDataWrapper;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mTreeViewAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            Assert.assertTrue(mTreeViewAdapter != null);

            List<TreeNode> treeNodes = mTreeViewAdapter.getSelectedNodes();

            List<InstanceData> instanceDatas = nodesToInstanceDatas(treeNodes);
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(!instanceDatas.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_group_edit_instance: {
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    if (instanceDatas.size() == 1) {
                        InstanceData instanceData = instanceDatas.get(0);
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
                    Utils.share(getShareData(instanceDatas), getActivity());
                    break;
                }
                case R.id.action_group_show_task: {
                    Assert.assertTrue(instanceDatas.size() == 1);

                    InstanceData instanceData = instanceDatas.get(0);
                    Assert.assertTrue(instanceData.TaskCurrent);

                    startActivity(ShowTaskActivity.newIntent(getActivity(), instanceData.InstanceKey.mTaskKey));
                    break;
                }
                case R.id.action_group_edit_task: {
                    Assert.assertTrue(instanceDatas.size() == 1);

                    InstanceData instanceData = instanceDatas.get(0);
                    Assert.assertTrue(instanceData.TaskCurrent);

                    startActivity(CreateTaskActivity.getEditIntent(getActivity(), instanceData.InstanceKey.mTaskKey));
                    break;
                }
                case R.id.action_group_delete_task: {
                    ArrayList<TaskKey> taskKeys = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.mTaskKey)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(!taskKeys.isEmpty());
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

                    DomainFactory.getDomainFactory(getActivity()).setTaskEndTimeStamps(getActivity(), ((GroupAdapter) mTreeViewAdapter.getTreeModelAdapter()).mDataId, taskKeys);

                    updateSelectAll();

                    break;
                }
                case R.id.action_group_add_task: {
                    Assert.assertTrue(instanceDatas.size() == 1);

                    InstanceData instanceData = instanceDatas.get(0);
                    Assert.assertTrue(instanceData.TaskCurrent);

                    getActivity().startActivity(CreateTaskActivity.getCreateIntent(getActivity(), instanceData.InstanceKey.mTaskKey));
                    break;
                }
                case R.id.action_group_join: {
                    ArrayList<TaskKey> taskKeys = new ArrayList<>(Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.mTaskKey)
                            .collect(Collectors.toList()));
                    Assert.assertTrue(taskKeys.size() > 1);

                    if (mInstanceKey == null) {
                        InstanceData firstInstanceData = Stream.of(instanceDatas)
                                .min((lhs, rhs) -> lhs.InstanceTimeStamp.compareTo(rhs.InstanceTimeStamp))
                                .get();

                        Date date = firstInstanceData.InstanceTimeStamp.getDate();

                        TimePair timePair = firstInstanceData.InstanceTimePair;

                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskKeys, new CreateTaskActivity.ScheduleHint(date, timePair)));
                    } else {
                        startActivity(CreateTaskActivity.getJoinIntent(getActivity(), taskKeys, mInstanceKey.mTaskKey));
                    }
                    break;
                }
                case R.id.action_group_mark_done: {
                    Assert.assertTrue(mDataId != null);
                    Assert.assertTrue(mDataWrapper != null);

                    List<InstanceKey> instanceKeys = Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey)
                            .collect(Collectors.toList());

                    ExactTimeStamp done = DomainFactory.getDomainFactory(getActivity()).setInstancesDone(getActivity(), mDataId, instanceKeys);

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

                            InstanceData instanceData = notDoneGroupNode.getSingleInstanceData();
                            instanceData.Done = done;

                            recursiveExists(instanceData);

                            GroupAdapter.NodeCollection nodeCollection = notDoneGroupNode.getNodeCollection();

                            nodeCollection.mDividerNode.add(instanceData);
                            nodeCollection.mNotDoneGroupCollection.remove(notDoneGroupNode);
                        } else {
                            GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = (GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode();

                            InstanceData instanceData = notDoneInstanceNode.mInstanceData;
                            instanceData.Done = done;

                            recursiveExists(instanceData);

                            notDoneInstanceNode.removeFromParent();

                            notDoneInstanceNode.getParentNodeCollection().mDividerNode.add(instanceData);
                        }

                        decrementSelected();
                    } while (!(selectedTreeNodes = mTreeViewAdapter.getSelectedNodes()).isEmpty());

                    updateSelectAll();

                    break;
                }
                default: {
                    throw new UnsupportedOperationException();
                }
            }
        }

        private void recursiveDelete(TreeNode treeNode, boolean root) {
            Assert.assertTrue(treeNode != null);

            InstanceData instanceData1;
            if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                instanceData1 = ((GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode()).getSingleInstanceData();
            } else if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) {
                instanceData1 = ((GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode()).mInstanceData;
            } else if (treeNode.getModelNode() instanceof DoneInstanceNode) {
                instanceData1 = ((DoneInstanceNode) treeNode.getModelNode()).mInstanceData;
            } else {
                Assert.assertTrue((treeNode.getModelNode() instanceof DividerNode));

                Stream.of(treeNode.getAllChildren())
                        .forEach(child -> recursiveDelete(child, false));

                return;
            }

            if (instanceData1.Exists || !root) {
                instanceData1.TaskCurrent = false;
                instanceData1.IsRootTask = null;
            } else {
                instanceData1.mInstanceDataParent.remove(instanceData1.InstanceKey);
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
                    DoneInstanceNode doneInstanceNode = (DoneInstanceNode) treeNode.getModelNode();

                    doneInstanceNode.removeFromParent();
                }
            }
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mTreeViewAdapter.onCreateActionMode();

            mActionMode.getMenuInflater().inflate(R.menu.menu_edit_groups, mActionMode.getMenu());

            updateFabVisibility();

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

            updateFabVisibility();

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

            List<InstanceData> instanceDatas = nodesToInstanceDatas(mTreeViewAdapter.getSelectedNodes());
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(!instanceDatas.isEmpty());

            Assert.assertTrue(Stream.of(instanceDatas).allMatch(instanceData -> (instanceData.Done == null)));

            if (instanceDatas.size() == 1) {
                InstanceData instanceData = instanceDatas.get(0);
                Assert.assertTrue(instanceData != null);

                menu.findItem(R.id.action_group_edit_instance).setVisible(instanceData.IsRootInstance);
                menu.findItem(R.id.action_group_show_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_edit_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_join).setVisible(false);
                menu.findItem(R.id.action_group_delete_task).setVisible(instanceData.TaskCurrent);
                menu.findItem(R.id.action_group_add_task).setVisible(instanceData.TaskCurrent);
            } else {
                Assert.assertTrue(instanceDatas.size() > 1);

                menu.findItem(R.id.action_group_edit_instance).setVisible(Stream.of(instanceDatas)
                        .allMatch(instanceData -> instanceData.IsRootInstance));
                menu.findItem(R.id.action_group_show_task).setVisible(false);
                menu.findItem(R.id.action_group_edit_task).setVisible(false);
                menu.findItem(R.id.action_group_add_task).setVisible(false);

                if (Stream.of(instanceDatas).allMatch(instanceData -> instanceData.TaskCurrent)) {
                    long projectIdCount = Stream.of(instanceDatas)
                            .map(instanceData -> instanceData.InstanceKey.mTaskKey.mRemoteProjectId)
                            .distinct()
                            .count();

                    Assert.assertTrue(projectIdCount > 0);

                    menu.findItem(R.id.action_group_join).setVisible(projectIdCount == 1);
                    menu.findItem(R.id.action_group_delete_task).setVisible(!containsLoop(instanceDatas));
                } else {
                    menu.findItem(R.id.action_group_join).setVisible(false);
                    menu.findItem(R.id.action_group_delete_task).setVisible(false);
                }
            }
        }

        @SuppressWarnings("BooleanMethodIsAlwaysInverted")
        private boolean containsLoop(List<InstanceData> instanceDatas) {
            Assert.assertTrue(instanceDatas != null);
            Assert.assertTrue(instanceDatas.size() > 1);

            for (InstanceData instanceData : instanceDatas) {
                Assert.assertTrue(instanceData != null);

                List<InstanceData> parents = new ArrayList<>();
                addParents(parents, instanceData);

                for (InstanceData parent : parents) {
                    Assert.assertTrue(parent != null);

                    if (instanceDatas.contains(parent))
                        return true;
                }
            }

            return false;
        }

        private void addParents(List<InstanceData> parents, InstanceData instanceData) {
            Assert.assertTrue(parents != null);
            Assert.assertTrue(instanceData != null);

            if (!(instanceData.mInstanceDataParent instanceof InstanceData))
                return;

            InstanceData parent = (InstanceData) instanceData.mInstanceDataParent;

            parents.add(parent);
            addParents(parents, parent);
        }
    };

    @Nullable
    private FloatingActionButton mFloatingActionButton;

    @NonNull
    public static GroupListFragment newInstance() {
        return new GroupListFragment();
    }

    @NonNull
    private String getShareData(@NonNull List<InstanceData> instanceDatas) {
        Assert.assertTrue(!instanceDatas.isEmpty());

        Map<InstanceKey, InstanceData> tree = new LinkedHashMap<>();

        for (InstanceData instanceData : instanceDatas) {
            Assert.assertTrue(instanceData != null);

            if (!inTree(tree, instanceData))
                tree.put(instanceData.InstanceKey, instanceData);
        }

        List<String> lines = new ArrayList<>();

        for (InstanceData instanceData : tree.values())
            printTree(lines, 0, instanceData);

        return TextUtils.join("\n", lines);
    }

    @Nullable
    public String getShareData() {
        Assert.assertTrue(mDataWrapper != null);
        Assert.assertTrue(mDataId != null);

        List<InstanceData> instanceDatas = new ArrayList<>(mDataWrapper.InstanceDatas.values());

        Collections.sort(instanceDatas, (lhs, rhs) -> {
            int timeStampComparison = lhs.InstanceTimeStamp.compareTo(rhs.InstanceTimeStamp);
            if (timeStampComparison != 0) {
                return timeStampComparison;
            } else {
                return lhs.mTaskStartExactTimeStamp.compareTo(rhs.mTaskStartExactTimeStamp);
            }
        });

        List<String> lines = new ArrayList<>();

        for (InstanceData instanceData : instanceDatas)
            printTree(lines, 1, instanceData);

        return TextUtils.join("\n", lines);
    }

    @SuppressWarnings("SimplifiableIfStatement")
    private boolean inTree(@NonNull Map<InstanceKey, InstanceData> shareTree, @NonNull InstanceData instanceData) {
        if (shareTree.isEmpty())
            return false;

        if (shareTree.containsKey(instanceData.InstanceKey))
            return true;

        return Stream.of(shareTree.values())
                .anyMatch(currInstanceData -> inTree(currInstanceData.Children, instanceData));
    }

    private void printTree(@NonNull List<String> lines, int indentation, @NonNull InstanceData instanceData) {
        lines.add(StringUtils.repeat("-", indentation) + instanceData.Name);

        Stream.of(instanceData.Children.values())
                .sortBy(child -> child.mTaskStartExactTimeStamp)
                .forEach(child -> printTree(lines, indentation + 1, child));
    }

    public GroupListFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        Assert.assertTrue(context instanceof GroupListListener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(EXPANSION_STATE_KEY)) {
            mExpansionState = savedInstanceState.getParcelable(EXPANSION_STATE_KEY);

            if (savedInstanceState.containsKey(SELECTED_NODES_KEY)) {
                mSelectedNodes = savedInstanceState.getParcelableArrayList(SELECTED_NODES_KEY);
                Assert.assertTrue(mSelectedNodes != null);
                Assert.assertTrue(!mSelectedNodes.isEmpty());
            }
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_group_list, container, false);
        Assert.assertTrue(view != null);

        mGroupListProgress = (ProgressBar) view.findViewById(R.id.group_list_progress);
        Assert.assertTrue(mGroupListProgress != null);

        mGroupListRecycler = (RecyclerView) view.findViewById(R.id.group_list_recycler);
        Assert.assertTrue(mGroupListRecycler != null);

        mGroupListRecycler.setLayoutManager(new LinearLayoutManager(getContext()));

        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        return view;
    }

    public void setAll(@NonNull MainActivity.TimeRange timeRange, int position, int dataId, @NonNull DataWrapper dataWrapper) {
        Assert.assertTrue(mPosition == null || mPosition.equals(position));
        Assert.assertTrue(mTimeRange == null || mTimeRange.equals(timeRange));
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(position >= 0);

        mPosition = position;
        mTimeRange = timeRange;

        initialize(dataId, dataWrapper);
    }

    public void setTimeStamp(@NonNull TimeStamp timeStamp, int dataId, @NonNull DataWrapper dataWrapper) {
        Assert.assertTrue(mPosition == null);
        Assert.assertTrue(mTimeRange == null);
        Assert.assertTrue(mTimeStamp == null || mTimeStamp.equals(timeStamp));
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        mTimeStamp = timeStamp;

        initialize(dataId, dataWrapper);
    }

    public void setInstanceKey(@NonNull InstanceKey instanceKey, int dataId, @NonNull DataWrapper dataWrapper) {
        Assert.assertTrue(mPosition == null);
        Assert.assertTrue(mTimeRange == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKeys == null);

        mInstanceKey = instanceKey;

        initialize(dataId, dataWrapper);
    }

    public void setInstanceKeys(@NonNull Set<InstanceKey> instanceKeys, int dataId, @NonNull DataWrapper dataWrapper) {
        Assert.assertTrue(mPosition == null);
        Assert.assertTrue(mTimeRange == null);
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);

        mInstanceKeys = instanceKeys;

        initialize(dataId, dataWrapper);
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
                List<InstanceData> instanceDatas = nodesToInstanceDatas(mTreeViewAdapter.getSelectedNodes());
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

    private void initialize(int dataId, @NonNull DataWrapper dataWrapper) {
        mGroupListProgress.setVisibility(View.GONE);

        if (mDataWrapper != null) {
            Assert.assertTrue(mDataId != null);

            DataDiff.diffData(mDataWrapper, dataWrapper);
            Log.e("asdf", "difference w data:\n" + DataDiff.getDiff());
        } else {
            Assert.assertTrue(mDataId == null);
        }

        mDataWrapper = dataWrapper;
        mDataId = dataId;

        if (mTreeViewAdapter != null) {
            mExpansionState = (((GroupAdapter) mTreeViewAdapter.getTreeModelAdapter()).getExpansionState());

            List<InstanceData> instanceDatas = nodesToInstanceDatas(mTreeViewAdapter.getSelectedNodes());
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

        Integer emptyTextId;
        if (mPosition != null) {
            Assert.assertTrue(mTimeRange != null);

            Assert.assertTrue(mTimeStamp == null);
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(mDataWrapper.TaskEditable == null);

            emptyTextId = R.string.instances_empty_root;
        } else if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(mDataWrapper.TaskEditable == null);

            emptyTextId = null;
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(mDataWrapper.TaskEditable != null);

            if (mDataWrapper.TaskEditable) {
                emptyTextId = R.string.empty_child;
            } else {
                emptyTextId = R.string.empty_disabled;
            }
        } else {
            Assert.assertTrue(mInstanceKeys != null);
            Assert.assertTrue(!mInstanceKeys.isEmpty());
            Assert.assertTrue(mDataWrapper.TaskEditable == null);

            emptyTextId = null;
        }

        updateFabVisibility();

        mTreeViewAdapter = GroupAdapter.getAdapter(this, mDataId, mDataWrapper.CustomTimeDatas, useGroups(), showPadding(), mDataWrapper.InstanceDatas.values(), mExpansionState, mSelectedNodes, mDataWrapper.TaskDatas, mDataWrapper.mNote);

        mGroupListRecycler.setAdapter(mTreeViewAdapter.getAdapter());

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        if (mDataWrapper.InstanceDatas.isEmpty() && TextUtils.isEmpty(mDataWrapper.mNote) && (mDataWrapper.TaskDatas == null || mDataWrapper.TaskDatas.isEmpty())) {
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
    }

    void updateSelectAll() {
        Assert.assertTrue(mDataWrapper != null);
        Assert.assertTrue(mDataId != null);

        ((GroupListListener) getActivity()).setGroupSelectAllVisibility(mPosition, Stream.of(mDataWrapper.InstanceDatas.values())
                .anyMatch(instanceData -> instanceData.Done == null));
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

    private static List<InstanceData> nodesToInstanceDatas(List<TreeNode> treeNodes) {
        Assert.assertTrue(treeNodes != null);

        List<InstanceData> instanceDatas = new ArrayList<>();
        for (TreeNode treeNode : treeNodes) {
            if (treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode) {
                InstanceData instanceData = ((GroupAdapter.NodeCollection.NotDoneGroupNode) treeNode.getModelNode()).getSingleInstanceData();

                instanceDatas.add(instanceData);
            } else {
                Assert.assertTrue(treeNode.getModelNode() instanceof GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode);

                instanceDatas.add(((GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode()).mInstanceData);
            }
        }

        return instanceDatas;
    }

    private static void recursiveExists(InstanceData instanceData) {
        Assert.assertTrue(instanceData != null);

        instanceData.Exists = true;

        if (instanceData.mInstanceDataParent instanceof InstanceData) {
            InstanceData instanceData1 = (InstanceData) instanceData.mInstanceDataParent;
            recursiveExists(instanceData1);
        } else {
            Assert.assertTrue(instanceData.mInstanceDataParent instanceof DataWrapper);
        }
    }

    static String getChildrenText(boolean expanded, @NonNull Collection<InstanceData> instanceDatas, @Nullable String note) {
        if (!instanceDatas.isEmpty() && !expanded) {
            Stream<InstanceData> notDone = Stream.of(instanceDatas)
                    .filter(instanceData -> instanceData.Done == null)
                    .sortBy(instanceData -> instanceData.mTaskStartExactTimeStamp);

            //noinspection ConstantConditions
            Stream<InstanceData> done = Stream.of(instanceDatas)
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

    @Override
    public void setFab(@NonNull FloatingActionButton floatingActionButton) {
        mFloatingActionButton = floatingActionButton;

        mFloatingActionButton.setOnClickListener(v -> {
            Assert.assertTrue(mDataWrapper != null);
            Assert.assertTrue(mInstanceKeys == null);

            if (mPosition != null) {
                Assert.assertTrue(mTimeRange != null);

                Assert.assertTrue(mTimeStamp == null);
                Assert.assertTrue(mInstanceKey == null);

                Assert.assertTrue(mDataWrapper.TaskEditable == null);

                startActivity(CreateTaskActivity.getCreateIntent(getActivity(), new CreateTaskActivity.ScheduleHint(rangePositionToDate(mTimeRange, mPosition))));
            } else if (mTimeStamp != null) {
                Assert.assertTrue(mInstanceKey == null);

                Assert.assertTrue(mDataWrapper.TaskEditable == null);

                Assert.assertTrue(mTimeStamp.compareTo(TimeStamp.getNow()) > 0);

                startActivity(CreateTaskActivity.getCreateIntent(getActivity(), new CreateTaskActivity.ScheduleHint(mTimeStamp.getDate(), mTimeStamp.getHourMinute())));
            } else {
                Assert.assertTrue(mInstanceKey != null);

                Assert.assertTrue(mDataWrapper.TaskEditable != null);

                Assert.assertTrue(mDataWrapper.TaskEditable);

                startActivity(CreateTaskActivity.getCreateIntent(getActivity(), mInstanceKey.mTaskKey));
            }
        });

        updateFabVisibility();
    }

    private boolean showPadding() {
        Assert.assertTrue(mDataWrapper != null);

        if (mPosition != null) {
            Assert.assertTrue(mTimeRange != null);

            Assert.assertTrue(mTimeStamp == null);
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(mDataWrapper.TaskEditable == null);

            return true;
        } else if (mTimeStamp != null) {
            Assert.assertTrue(mInstanceKey == null);
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(mDataWrapper.TaskEditable == null);

            return (mTimeStamp.compareTo(TimeStamp.getNow()) > 0);
        } else if (mInstanceKey != null) {
            Assert.assertTrue(mInstanceKeys == null);

            Assert.assertTrue(mDataWrapper.TaskEditable != null);

            return mDataWrapper.TaskEditable;
        } else {
            Assert.assertTrue(mInstanceKeys != null);
            Assert.assertTrue(!mInstanceKeys.isEmpty());
            Assert.assertTrue(mDataWrapper.TaskEditable == null);

            return false;
        }
    }

    private void updateFabVisibility() {
        if (mFloatingActionButton == null)
            return;

        if (mDataWrapper != null && !mSelectionCallback.hasActionMode() && showPadding()) {
            mFloatingActionButton.show();
        } else {
            mFloatingActionButton.hide();
        }
    }

    @Override
    public void clearFab() {
        if (mFloatingActionButton == null)
            return;

        mFloatingActionButton.setOnClickListener(null);

        mFloatingActionButton = null;
    }

    static class GroupAdapter implements TreeModelAdapter, NodeCollectionParent {
        static final int TYPE_GROUP = 0;

        @NonNull
        final GroupListFragment mGroupListFragment;

        final int mDataId;
        private final List<CustomTimeData> mCustomTimeDatas;
        private final boolean mShowFab;

        private TreeViewAdapter mTreeViewAdapter;

        private NodeCollection mNodeCollection;

        private final float mDensity;

        @NonNull
        static TreeViewAdapter getAdapter(@NonNull GroupListFragment groupListFragment, int dataId, @NonNull List<CustomTimeData> customTimeDatas, boolean useGroups, boolean showFab, @NonNull Collection<InstanceData> instanceDatas, @Nullable GroupListFragment.ExpansionState expansionState, @Nullable ArrayList<InstanceKey> selectedNodes, @Nullable List<TaskData> taskDatas, @Nullable String note) {
            GroupAdapter groupAdapter = new GroupAdapter(groupListFragment, dataId, customTimeDatas, showFab);

            return groupAdapter.initialize(useGroups, instanceDatas, expansionState, selectedNodes, taskDatas, note);
        }

        private GroupAdapter(@NonNull GroupListFragment groupListFragment, int dataId, @NonNull List<CustomTimeData> customTimeDatas, boolean showFab) {
            mGroupListFragment = groupListFragment;
            mDataId = dataId;
            mCustomTimeDatas = customTimeDatas;
            mShowFab = showFab;

            mDensity = groupListFragment.getActivity().getResources().getDisplayMetrics().density;
        }

        @NonNull
        private TreeViewAdapter initialize(boolean useGroups, Collection<InstanceData> instanceDatas, GroupListFragment.ExpansionState expansionState, ArrayList<InstanceKey> selectedNodes, List<TaskData> taskDatas, @Nullable String note) {
            Assert.assertTrue(instanceDatas != null);

            mTreeViewAdapter = new TreeViewAdapter(mShowFab, this);

            TreeNodeCollection treeNodeCollection = new TreeNodeCollection(mTreeViewAdapter);

            mNodeCollection = new NodeCollection(mDensity, 0, this, useGroups, treeNodeCollection, note);

            List<TimeStamp> expandedGroups = null;
            HashMap<InstanceKey, Boolean> expandedInstances = null;
            boolean doneExpanded = false;
            boolean unscheduledExpanded = false;
            List<TaskKey> expandedTaskKeys = null;

            if (expansionState != null) {
                expandedGroups = expansionState.ExpandedGroups;

                expandedInstances = expansionState.ExpandedInstances;

                doneExpanded = expansionState.DoneExpanded;

                unscheduledExpanded = expansionState.UnscheduledExpanded;

                expandedTaskKeys = expansionState.ExpandedTaskKeys;
            } else if (taskDatas != null) {
                unscheduledExpanded = false;
            }

            treeNodeCollection.setNodes(mNodeCollection.initialize(instanceDatas, expandedGroups, expandedInstances, doneExpanded, selectedNodes, true, taskDatas, unscheduledExpanded, expandedTaskKeys));

            mTreeViewAdapter.setTreeNodeCollection(treeNodeCollection);

            return mTreeViewAdapter;
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

            Assert.assertTrue(position < mTreeViewAdapter.getItemCount());

            if (position < mTreeViewAdapter.displayedSize()) {
                TreeNode treeNode = mTreeViewAdapter.getNode(position);
                treeNode.onBindViewHolder(viewHolder);
            } else {
                Assert.assertTrue(position == mTreeViewAdapter.displayedSize());
                Assert.assertTrue(mShowFab);
                Assert.assertTrue(position == mTreeViewAdapter.getItemCount() - 1);
            }
        }

        @Override
        public boolean hasActionMode() {
            return mGroupListFragment.mSelectionCallback.hasActionMode();
        }

        @Override
        public void incrementSelected() {
            mGroupListFragment.mSelectionCallback.incrementSelected();
        }

        @Override
        public void decrementSelected() {
            mGroupListFragment.mSelectionCallback.decrementSelected();
        }

        ExpansionState getExpansionState() {
            List<TimeStamp> expandedGroups = mNodeCollection.getExpandedGroups();
            Assert.assertTrue(expandedGroups != null);

            HashMap<InstanceKey, Boolean> expandedInstances = new HashMap<>();
            mNodeCollection.addExpandedInstances(expandedInstances);

            boolean doneExpanded = mNodeCollection.getDoneExpanded();

            boolean unscheduledExpanded = mNodeCollection.getUnscheduledExpanded();

            List<TaskKey> expandedTaskKeys = mNodeCollection.getExpandedTaskKeys();

            return new ExpansionState(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTaskKeys);
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
            @NonNull
            private final NodeCollectionParent mNodeCollectionParent;

            @NonNull
            private final NodeContainer mNodeContainer;

            private NotDoneGroupCollection mNotDoneGroupCollection;
            private DividerNode mDividerNode;
            private UnscheduledNode mUnscheduledNode;

            private final boolean mUseGroups;

            private final float mDensity;
            private final int mIndentation;

            private final String mNote;

            NodeCollection(float density, int indentation, @NonNull NodeCollectionParent nodeCollectionParent, boolean useGroups, @NonNull NodeContainer nodeContainer, @Nullable String note) {
                mDensity = density;
                mIndentation = indentation;
                mNodeCollectionParent = nodeCollectionParent;
                mUseGroups = useGroups;
                mNodeContainer = nodeContainer;
                mNote = note;
            }

            @NonNull
            List<TreeNode> initialize(@NonNull Collection<InstanceData> instanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, boolean doneExpanded, @Nullable ArrayList<InstanceKey> selectedNodes, boolean selectable, @Nullable List<TaskData> taskDatas, boolean unscheduledExpanded, @Nullable List<TaskKey> expandedTaskKeys) {
                ArrayList<InstanceData> notDoneInstanceDatas = new ArrayList<>();
                ArrayList<InstanceData> doneInstanceDatas = new ArrayList<>();
                for (InstanceData instanceData : instanceDatas) {
                    if (instanceData.Done == null)
                        notDoneInstanceDatas.add(instanceData);
                    else
                        doneInstanceDatas.add(instanceData);
                }

                List<TreeNode> rootTreeNodes = new ArrayList<>();

                if (!TextUtils.isEmpty(mNote)) {
                    Assert.assertTrue(mIndentation == 0);

                    rootTreeNodes.add(new NoteNode(mDensity, mNote, getGroupAdapter()).initialize(mNodeContainer));
                }

                mNotDoneGroupCollection = new NotDoneGroupCollection(mDensity, mIndentation, this, mNodeContainer, selectable);

                rootTreeNodes.addAll(mNotDoneGroupCollection.initialize(notDoneInstanceDatas, expandedGroups, expandedInstances, selectedNodes));

                Assert.assertTrue((mIndentation == 0) || (taskDatas == null));
                if (taskDatas != null && !taskDatas.isEmpty()) {
                    mUnscheduledNode = new UnscheduledNode(mDensity, this);

                    TreeNode unscheduledTreeNode = mUnscheduledNode.initialize(unscheduledExpanded, mNodeContainer, taskDatas, expandedTaskKeys);

                    rootTreeNodes.add(unscheduledTreeNode);
                }

                mDividerNode = new DividerNode(mDensity, mIndentation, this);

                doneExpanded = doneExpanded && !doneInstanceDatas.isEmpty();

                TreeNode dividerTreeNode = mDividerNode.initialize(doneExpanded, mNodeContainer, doneInstanceDatas, expandedInstances);
                Assert.assertTrue(dividerTreeNode != null);

                rootTreeNodes.add(dividerTreeNode);

                return rootTreeNodes;
            }

            @NonNull
            private NodeCollectionParent getNodeCollectionParent() {
                return mNodeCollectionParent;
            }

            @NonNull
            private NodeContainer getNodeContainer() {
                return mNodeContainer;
            }

            @NonNull
            GroupAdapter getGroupAdapter() {
                return getNodeCollectionParent().getGroupAdapter();
            }

            @NonNull
            NotDoneGroupCollection getNotDoneGroupCollection() {
                Assert.assertTrue(mNotDoneGroupCollection != null);

                return mNotDoneGroupCollection;
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

            List<TaskKey> getExpandedTaskKeys() {
                if (mUnscheduledNode == null)
                    return null;
                else
                    return mUnscheduledNode.getExpandedTaskKeys();
            }

            boolean getDoneExpanded() {
                return mDividerNode.expanded();
            }

            static class NotDoneGroupCollection {
                @NonNull
                private final NodeCollection mNodeCollection;

                @NonNull
                private final NodeContainer mNodeContainer;

                private final ArrayList<NotDoneGroupNode> mNotDoneGroupNodes = new ArrayList<>();

                private final float mDensity;
                private final int mIndentation;

                private final boolean mSelectable;

                private NotDoneGroupCollection(float density, int indentation, @NonNull NodeCollection nodeCollection, @NonNull NodeContainer nodeContainer, boolean selectable) {
                    mDensity = density;
                    mIndentation = indentation;
                    mNodeCollection = nodeCollection;
                    mNodeContainer = nodeContainer;
                    mSelectable = selectable;
                }

                @NonNull
                private List<TreeNode> initialize(@NonNull List<InstanceData> notDoneInstanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes) {
                    ArrayList<TreeNode> notDoneGroupTreeNodes = new ArrayList<>();

                    NodeCollection nodeCollection = getNodeCollection();

                    if (nodeCollection.mUseGroups) {
                        HashMap<TimeStamp, ArrayList<InstanceData>> instanceDataHash = new HashMap<>();
                        for (InstanceData instanceData : notDoneInstanceDatas) {
                            if (!instanceDataHash.containsKey(instanceData.InstanceTimeStamp))
                                instanceDataHash.put(instanceData.InstanceTimeStamp, new ArrayList<>());
                            instanceDataHash.get(instanceData.InstanceTimeStamp).add(instanceData);
                        }

                        for (Map.Entry<TimeStamp, ArrayList<InstanceData>> entry : instanceDataHash.entrySet()) {
                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(this, entry.getValue(), expandedGroups, expandedInstances, selectedNodes);

                            notDoneGroupTreeNodes.add(notDoneGroupTreeNode);
                        }
                    } else {
                        for (InstanceData instanceData : notDoneInstanceDatas) {
                            ArrayList<InstanceData> dummyInstanceDatas = new ArrayList<>();
                            dummyInstanceDatas.add(instanceData);

                            TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(this, dummyInstanceDatas, expandedGroups, expandedInstances, selectedNodes);

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

                public void add(@NonNull InstanceData instanceData) {
                    NodeCollection nodeCollection = getNodeCollection();

                    NodeContainer nodeContainer = nodeCollection.getNodeContainer();

                    ExactTimeStamp exactTimeStamp = instanceData.InstanceTimeStamp.toExactTimeStamp();

                    List<NotDoneGroupNode> timeStampNotDoneGroupNodes = Stream.of(mNotDoneGroupNodes)
                            .filter(notDoneGroupNode -> notDoneGroupNode.mExactTimeStamp.equals(exactTimeStamp))
                            .collect(Collectors.toList());

                    if (timeStampNotDoneGroupNodes.isEmpty() || !nodeCollection.mUseGroups) {
                        ArrayList<InstanceData> instanceDatas = new ArrayList<>();
                        instanceDatas.add(instanceData);

                        TreeNode notDoneGroupTreeNode = newNotDoneGroupNode(this, instanceDatas, null, null, null);

                        nodeContainer.add(notDoneGroupTreeNode);
                    } else {
                        Assert.assertTrue(timeStampNotDoneGroupNodes.size() == 1);

                        NotDoneGroupNode notDoneGroupNode = timeStampNotDoneGroupNodes.get(0);
                        Assert.assertTrue(notDoneGroupNode != null);

                        notDoneGroupNode.addInstanceData(instanceData);
                    }
                }

                @NonNull
                private TreeNode newNotDoneGroupNode(@NonNull NotDoneGroupCollection notDoneGroupCollection, @NonNull List<InstanceData> instanceDatas, @Nullable List<TimeStamp> expandedGroups, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    NotDoneGroupNode notDoneGroupNode = new NotDoneGroupNode(mDensity, mIndentation, notDoneGroupCollection, instanceDatas, mSelectable);

                    TreeNode notDoneGroupTreeNode = notDoneGroupNode.initialize(expandedGroups, expandedInstances, selectedNodes, mNodeContainer);
                    Assert.assertTrue(notDoneGroupTreeNode != null);

                    mNotDoneGroupNodes.add(notDoneGroupNode);

                    return notDoneGroupTreeNode;
                }

                @NonNull
                private NodeCollection getNodeCollection() {
                    return mNodeCollection;
                }

                @NonNull
                private NodeContainer getNodeContainer() {
                    return mNodeContainer;
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

            static class NoteNode extends GroupHolderNode implements ModelNode {
                @NonNull
                private final GroupAdapter mGroupAdapter;

                private final String mNote;

                private TreeNode mTreeNode;

                NoteNode(float density, @NonNull String note, @NonNull GroupAdapter groupAdapter) {
                    super(density, 0);

                    Assert.assertTrue(!TextUtils.isEmpty(note));

                    mNote = note;
                    mGroupAdapter = groupAdapter;
                }

                @NonNull
                TreeNode initialize(@NonNull NodeContainer nodeContainer) {
                    mTreeNode = new TreeNode(this, nodeContainer, false, false);

                    mTreeNode.setChildTreeNodes(new ArrayList<>());
                    return mTreeNode;
                }

                @NonNull
                TreeNode getTreeNode() {
                    Assert.assertTrue(mTreeNode != null);

                    return mTreeNode;
                }

                @NonNull
                GroupAdapter getGroupAdapter() {
                    return mGroupAdapter;
                }

                @NonNull
                private GroupListFragment getGroupListFragment() {
                    return getGroupAdapter().mGroupListFragment;
                }

                @Override
                int getNameVisibility() {
                    return View.VISIBLE;
                }

                @NonNull
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

                @NonNull
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

                @NonNull
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

                @NonNull
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

                @NonNull
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
                @NonNull
                private final NotDoneGroupCollection mNotDoneGroupCollection;

                private TreeNode mTreeNode;

                private final List<InstanceData> mInstanceDatas;

                private final ArrayList<NotDoneInstanceNode> mNotDoneInstanceNodes = new ArrayList<>();
                private NodeCollection mNodeCollection;

                final ExactTimeStamp mExactTimeStamp;

                private final boolean mSelectable;

                private NotDoneGroupNode(float density, int indentation, @NonNull NotDoneGroupCollection notDoneGroupCollection, @NonNull List<InstanceData> instanceDatas, boolean selectable) {
                    super(density, indentation);
                    Assert.assertTrue(!instanceDatas.isEmpty());

                    mNotDoneGroupCollection = notDoneGroupCollection;
                    mInstanceDatas = instanceDatas;

                    mExactTimeStamp = instanceDatas.get(0).InstanceTimeStamp.toExactTimeStamp();
                    Assert.assertTrue(Stream.of(instanceDatas)
                            .allMatch(instanceData -> instanceData.InstanceTimeStamp.toExactTimeStamp().equals(mExactTimeStamp)));

                    mSelectable = selectable;
                }

                TreeNode initialize(List<TimeStamp> expandedGroups, HashMap<InstanceKey, Boolean> expandedInstances, ArrayList<InstanceKey> selectedNodes, NodeContainer nodeContainer) {
                    Assert.assertTrue(nodeContainer != null);

                    boolean expanded;
                    boolean doneExpanded;
                    if (mInstanceDatas.size() == 1) {
                        InstanceData instanceData = mInstanceDatas.get(0);
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

                    mTreeNode = new TreeNode(this, nodeContainer, expanded, selected);

                    if (mInstanceDatas.size() == 1) {
                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, mTreeNode, null);

                        mTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceDatas.get(0).Children.values(), expandedGroups, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));
                    } else {
                        List<TreeNode> notDoneInstanceTreeNodes = Stream.of(mInstanceDatas)
                                .map(instanceData -> newChildTreeNode(instanceData, expandedInstances, selectedNodes))
                                .collect(Collectors.toList());

                        mTreeNode.setChildTreeNodes(notDoneInstanceTreeNodes);
                    }

                    return mTreeNode;
                }

                @NonNull
                InstanceData getSingleInstanceData() {
                    Assert.assertTrue(mInstanceDatas.size() == 1);

                    InstanceData instanceData = mInstanceDatas.get(0);
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

                @NonNull
                @Override
                String getName() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    if (singleInstance()) {
                        InstanceData instanceData = getSingleInstanceData();

                        return instanceData.Name;
                    } else {
                        Assert.assertTrue(!notDoneGroupTreeNode.expanded());

                        return Stream.of(mInstanceDatas)
                                .sortBy(instanceData -> instanceData.mTaskStartExactTimeStamp)
                                .map(instanceData -> instanceData.Name)
                                .collect(Collectors.joining(", "));
                    }
                }

                @NonNull
                private NotDoneGroupCollection getNotDoneGroupCollection() {
                    return mNotDoneGroupCollection;
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
                    return getGroupAdapter().mGroupListFragment;
                }

                @Override
                int getNameColor() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        InstanceData instanceData = getSingleInstanceData();

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
                        InstanceData instanceData = getSingleInstanceData();

                        if (TextUtils.isEmpty(instanceData.DisplayText)) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        return View.VISIBLE;
                    }
                }

                @NonNull
                @Override
                String getDetails() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        InstanceData instanceData = getSingleInstanceData();

                        Assert.assertTrue(!TextUtils.isEmpty(instanceData.DisplayText));

                        return instanceData.DisplayText;
                    } else {
                        ExactTimeStamp exactTimeStamp = ((NotDoneGroupNode) notDoneGroupTreeNode.getModelNode()).mExactTimeStamp;

                        Date date = exactTimeStamp.getDate();
                        HourMinute hourMinute = exactTimeStamp.toTimeStamp().getHourMinute();

                        CustomTimeData customTimeData = getCustomTimeData(date.getDayOfWeek(), hourMinute);

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
                        InstanceData instanceData = getSingleInstanceData();

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
                        InstanceData instanceData = getSingleInstanceData();

                        if ((instanceData.Children.isEmpty() || expanded()) && TextUtils.isEmpty(instanceData.mNote)) {
                            return View.GONE;
                        } else {
                            return View.VISIBLE;
                        }
                    } else {
                        return View.GONE;
                    }
                }

                @NonNull
                @Override
                String getChildren() {
                    Assert.assertTrue(singleInstance());

                    InstanceData instanceData = getSingleInstanceData();

                    Assert.assertTrue((!instanceData.Children.isEmpty() && !expanded()) || !TextUtils.isEmpty(instanceData.mNote));

                    return getChildrenText(expanded(), instanceData.Children.values(), instanceData.mNote);
                }

                @Override
                int getChildrenColor() {
                    Assert.assertTrue(singleInstance());

                    InstanceData instanceData = getSingleInstanceData();

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
                        InstanceData instanceData = getSingleInstanceData();

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
                        InstanceData instanceData = getSingleInstanceData();

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

                @NonNull
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

                @NonNull
                @Override
                View.OnClickListener getCheckBoxOnClickListener() {
                    final NotDoneGroupCollection notDoneGroupCollection = getNotDoneGroupCollection();

                    NodeCollection nodeCollection = getNodeCollection();

                    GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

                    Assert.assertTrue(singleInstance());

                    InstanceData instanceData = getSingleInstanceData();

                    Assert.assertTrue(!groupAdapter.mGroupListFragment.mSelectionCallback.hasActionMode());

                    return v -> {
                        v.setOnClickListener(null);

                        instanceData.Done = DomainFactory.getDomainFactory(groupAdapter.mGroupListFragment.getActivity()).setInstanceDone(groupAdapter.mGroupListFragment.getActivity(), groupAdapter.mDataId, instanceData.InstanceKey, true);
                        Assert.assertTrue(instanceData.Done != null);

                        recursiveExists(instanceData);

                        nodeCollection.mDividerNode.add(instanceData);

                        notDoneGroupCollection.remove(this);

                        groupAdapter.mGroupListFragment.updateSelectAll();
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
                    Assert.assertTrue(mTreeNode != null);

                    return mTreeNode.getOnLongClickListener();
                }

                @Override
                View.OnClickListener getOnClickListener() {
                    Assert.assertTrue(mTreeNode != null);

                    return mTreeNode.getOnClickListener();
                }

                @Override
                public void onClick() {
                    TreeNode notDoneGroupTreeNode = getTreeNode();

                    GroupListFragment groupListFragment = getGroupListFragment();

                    if (singleInstance()) {
                        InstanceData instanceData = getSingleInstanceData();

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), instanceData.InstanceKey));
                    } else {
                        groupListFragment.getActivity().startActivity(ShowGroupActivity.getIntent(((NotDoneGroupNode) notDoneGroupTreeNode.getModelNode()).mExactTimeStamp, groupListFragment.getActivity()));
                    }
                }

                private CustomTimeData getCustomTimeData(@NonNull DayOfWeek dayOfWeek, @NonNull HourMinute hourMinute) {
                    GroupAdapter groupAdapter = getGroupAdapter();

                    for (CustomTimeData customTimeData : groupAdapter.mCustomTimeDatas)
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

                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, notDoneGroupTreeNode, null);

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

                            return getSingleInstanceData().mTaskStartExactTimeStamp.compareTo(notDoneGroupNode.getSingleInstanceData().mTaskStartExactTimeStamp);
                        }
                    } else if (another instanceof UnscheduledNode) {
                        return -1;
                    } else {
                        Assert.assertTrue(another instanceof DividerNode);

                        return -1;
                    }
                }

                void addInstanceData(@NonNull InstanceData instanceData) {
                    Assert.assertTrue(instanceData.InstanceTimeStamp.toExactTimeStamp().equals(mExactTimeStamp));

                    Assert.assertTrue(mTreeNode != null);

                    Assert.assertTrue(!mInstanceDatas.isEmpty());
                    if (mInstanceDatas.size() == 1) {
                        Assert.assertTrue(mNotDoneInstanceNodes.isEmpty());

                        mTreeNode.removeAll();
                        mNodeCollection = null;

                        InstanceData instanceData1 = mInstanceDatas.get(0);
                        Assert.assertTrue(instanceData1 != null);

                        GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(mDensity, mIndentation, instanceData1, NotDoneGroupNode.this, mSelectable);
                        mNotDoneInstanceNodes.add(notDoneInstanceNode);

                        mTreeNode.add(notDoneInstanceNode.initialize(null, null, mTreeNode));
                    }

                    mInstanceDatas.add(instanceData);

                    mTreeNode.add(newChildTreeNode(instanceData, null, null));
                }

                @NonNull
                TreeNode newChildTreeNode(@NonNull InstanceData instanceData, @Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes) {
                    Assert.assertTrue(mTreeNode != null);

                    GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = new GroupListFragment.GroupAdapter.NodeCollection.NotDoneGroupNode.NotDoneInstanceNode(mDensity, mIndentation, instanceData, this, mSelectable);

                    TreeNode childTreeNode = notDoneInstanceNode.initialize(expandedInstances, selectedNodes, mTreeNode);

                    mNotDoneInstanceNodes.add(notDoneInstanceNode);

                    return childTreeNode;
                }

                boolean expanded() {
                    Assert.assertTrue(mTreeNode != null);

                    return mTreeNode.expanded();
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
                    Assert.assertTrue(mTreeNode != null);

                    return mTreeNode;
                }

                void removeFromParent() {
                    getNotDoneGroupCollection().remove(this);
                }

                static class NotDoneInstanceNode extends GroupHolderNode implements ModelNode, NodeCollectionParent {
                    @NonNull
                    private final NotDoneGroupNode mNotDoneGroupNode;

                    private TreeNode mTreeNode;

                    @NonNull
                    final InstanceData mInstanceData;

                    private NodeCollection mNodeCollection;

                    private final boolean mSelectable;

                    NotDoneInstanceNode(float density, int indentation, @NonNull InstanceData instanceData, @NonNull NotDoneGroupNode notDoneGroupNode, boolean selectable) {
                        super(density, indentation);

                        mInstanceData = instanceData;
                        mNotDoneGroupNode = notDoneGroupNode;
                        mSelectable = selectable;
                    }

                    TreeNode initialize(@Nullable HashMap<InstanceKey, Boolean> expandedInstances, @Nullable ArrayList<InstanceKey> selectedNodes, @NonNull TreeNode notDoneGroupTreeNode) {
                        boolean selected = (selectedNodes != null && selectedNodes.contains(mInstanceData.InstanceKey));

                        boolean expanded = false;
                        boolean doneExpanded = false;
                        if ((expandedInstances != null && expandedInstances.containsKey(mInstanceData.InstanceKey) && !mInstanceData.Children.isEmpty())) {
                            expanded = true;
                            doneExpanded = expandedInstances.get(mInstanceData.InstanceKey);
                        }

                        mTreeNode = new TreeNode(this, notDoneGroupTreeNode, expanded, selected);

                        mNodeCollection = new NodeCollection(mDensity, mIndentation + 1, this, false, mTreeNode, null);
                        mTreeNode.setChildTreeNodes(mNodeCollection.initialize(mInstanceData.Children.values(), null, expandedInstances, doneExpanded, selectedNodes, mSelectable, null, false, null));

                        return mTreeNode;
                    }

                    @NonNull
                    private TreeNode getTreeNode() {
                        Assert.assertTrue(mTreeNode != null);

                        return mTreeNode;
                    }

                    @NonNull
                    private NotDoneGroupNode getParentNotDoneGroupNode() {
                        return mNotDoneGroupNode;
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
                        return getGroupAdapter().mGroupListFragment;
                    }

                    @Override
                    int getNameVisibility() {
                        return View.VISIBLE;
                    }

                    @NonNull
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

                    @NonNull
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

                    @NonNull
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

                    @NonNull
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

                    @NonNull
                    @Override
                    View.OnClickListener getCheckBoxOnClickListener() {
                        final NotDoneGroupNode notDoneGroupNode = getParentNotDoneGroupNode();

                        final TreeNode notDoneGroupTreeNode = notDoneGroupNode.getTreeNode();

                        Assert.assertTrue(notDoneGroupTreeNode.expanded());

                        NodeCollection nodeCollection = getParentNodeCollection();

                        GroupAdapter groupAdapter = nodeCollection.getGroupAdapter();

                        Assert.assertTrue(!groupAdapter.mGroupListFragment.mSelectionCallback.hasActionMode());

                        return v -> {
                            v.setOnClickListener(null);

                            Assert.assertTrue(notDoneGroupTreeNode.expanded());

                            mInstanceData.Done = DomainFactory.getDomainFactory(groupAdapter.mGroupListFragment.getActivity()).setInstanceDone(groupAdapter.mGroupListFragment.getActivity(), groupAdapter.mDataId, mInstanceData.InstanceKey, true);
                            Assert.assertTrue(mInstanceData.Done != null);

                            recursiveExists(mInstanceData);

                            notDoneGroupNode.remove(this);

                            nodeCollection.mDividerNode.add(mInstanceData);

                            groupAdapter.mGroupListFragment.updateSelectAll();
                        };
                    }

                    @Override
                    int getSeparatorVisibility() {
                        Assert.assertTrue(mTreeNode != null);

                        return (mTreeNode.getSeparatorVisibility() ? View.VISIBLE : View.INVISIBLE);
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
                        Assert.assertTrue(mTreeNode != null);

                        return mTreeNode.getOnLongClickListener();
                    }

                    @Override
                    View.OnClickListener getOnClickListener() {
                        Assert.assertTrue(mTreeNode != null);

                        return mTreeNode.getOnClickListener();
                    }

                    @Override
                    public void onClick() {
                        GroupListFragment groupListFragment = getGroupListFragment();

                        groupListFragment.getActivity().startActivity(ShowInstanceActivity.getIntent(groupListFragment.getActivity(), mInstanceData.InstanceKey));
                    }

                    @Override
                    public int compareTo(@NonNull ModelNode another) {
                        return mInstanceData.mTaskStartExactTimeStamp.compareTo(((NotDoneInstanceNode) another).mInstanceData.mTaskStartExactTimeStamp);
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

        }

    }

    private static class ExpansionState implements Parcelable {
        final boolean DoneExpanded;

        @NonNull
        final List<TimeStamp> ExpandedGroups;

        @NonNull
        final HashMap<InstanceKey, Boolean> ExpandedInstances;

        final boolean UnscheduledExpanded;

        @Nullable
        final List<TaskKey> ExpandedTaskKeys;

        ExpansionState(boolean doneExpanded, @NonNull List<TimeStamp> expandedGroups, @NonNull HashMap<InstanceKey, Boolean> expandedInstances, boolean unscheduledExpanded, @Nullable List<TaskKey> expandedTaskKeys) {
            DoneExpanded = doneExpanded;
            ExpandedGroups = expandedGroups;
            ExpandedInstances = expandedInstances;
            UnscheduledExpanded = unscheduledExpanded;
            ExpandedTaskKeys = expandedTaskKeys;
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

            if (ExpandedTaskKeys == null) {
                dest.writeInt(0);
            } else {
                dest.writeInt(1);
                dest.writeList(ExpandedTaskKeys);
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
                List<TaskKey> expandedTaskKeys;
                if (hasTasks) {
                    expandedTaskKeys = new ArrayList<>();
                    source.readList(expandedTaskKeys, TaskKey.class.getClassLoader());
                } else {
                    expandedTaskKeys = null;
                }

                return new ExpansionState(doneExpanded, expandedGroups, expandedInstances, unscheduledExpanded, expandedTaskKeys);
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

    public static class DataWrapper implements InstanceDataParent {
        public HashMap<InstanceKey, InstanceData> InstanceDatas;

        @NonNull
        final List<CustomTimeData> CustomTimeDatas;

        @Nullable
        final Boolean TaskEditable;

        @Nullable
        final List<TaskData> TaskDatas;

        @Nullable
        final String mNote;

        public DataWrapper(@NonNull List<CustomTimeData> customTimeDatas, @Nullable Boolean taskEditable, @Nullable List<TaskData> taskDatas, @Nullable String note) {
            CustomTimeDatas = customTimeDatas;
            TaskEditable = taskEditable;
            TaskDatas = taskDatas;
            mNote = note;
        }

        public void setInstanceDatas(@NonNull HashMap<InstanceKey, InstanceData> instanceDatas) {
            InstanceDatas = instanceDatas;
        }

        @Override
        public int hashCode() {
            int hashCode = InstanceDatas.hashCode();
            hashCode += CustomTimeDatas.hashCode();
            if (TaskEditable != null)
                hashCode += (TaskEditable ? 2 : 1);
            if (TaskDatas != null)
                hashCode += TaskDatas.hashCode();
            if (!TextUtils.isEmpty(mNote))
                hashCode += mNote.hashCode();
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof DataWrapper))
                return false;

            DataWrapper dataWrapper = (DataWrapper) object;

            if (!InstanceDatas.equals(dataWrapper.InstanceDatas))
                return false;

            if (!CustomTimeDatas.equals(dataWrapper.CustomTimeDatas))
                return false;

            if ((TaskEditable == null) != (dataWrapper.TaskEditable == null))
                return false;

            if ((TaskEditable != null) && !TaskEditable.equals(dataWrapper.TaskEditable))
                return false;

            if ((TaskDatas == null) != (dataWrapper.TaskDatas == null))
                return false;

            if ((TaskDatas != null) && !TaskDatas.equals(dataWrapper.TaskDatas))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(dataWrapper.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(dataWrapper.mNote))
                return false;

            return true;
        }

        @Override
        public void remove(InstanceKey instanceKey) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(InstanceDatas.containsKey(instanceKey));

            InstanceDatas.remove(instanceKey);
        }
    }

    public static class InstanceData implements InstanceDataParent {
        @Nullable
        public ExactTimeStamp Done;

        @NonNull
        public final InstanceKey InstanceKey;

        @Nullable
        public final String DisplayText;

        public HashMap<InstanceKey, InstanceData> Children;

        @NonNull
        public final String Name;

        @NonNull
        public final TimeStamp InstanceTimeStamp;

        public boolean TaskCurrent;
        public final boolean IsRootInstance;

        @Nullable
        public Boolean IsRootTask;

        public boolean Exists;

        @NonNull
        public final TimePair InstanceTimePair;

        @Nullable
        final String mNote;

        @NonNull
        final InstanceDataParent mInstanceDataParent;

        @NonNull
        final ExactTimeStamp mTaskStartExactTimeStamp;

        public InstanceData(@Nullable ExactTimeStamp done, @NonNull InstanceKey instanceKey, @Nullable String displayText, @NonNull String name, @NonNull TimeStamp instanceTimeStamp, boolean taskCurrent, boolean isRootInstance, @Nullable Boolean isRootTask, boolean exists, @NonNull InstanceDataParent instanceDataParent, @NonNull TimePair instanceTimePair, @Nullable String note, @NonNull ExactTimeStamp taskStartExactTimeStamp) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Done = done;
            InstanceKey = instanceKey;
            DisplayText = displayText;
            Name = name;
            InstanceTimeStamp = instanceTimeStamp;
            TaskCurrent = taskCurrent;
            IsRootInstance = isRootInstance;
            IsRootTask = isRootTask;
            Exists = exists;
            InstanceTimePair = instanceTimePair;
            mInstanceDataParent = instanceDataParent;
            mNote = note;
            mTaskStartExactTimeStamp = taskStartExactTimeStamp;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (Done != null)
                hashCode += Done.hashCode();
            hashCode += InstanceKey.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            hashCode += Children.hashCode();
            hashCode += Name.hashCode();
            hashCode += InstanceTimeStamp.hashCode();
            hashCode += (TaskCurrent ? 1 : 0);
            hashCode += (IsRootInstance ? 1 : 0);
            if (IsRootTask != null)
                hashCode += (IsRootTask ? 2 : 1);
            hashCode += (Exists ? 1 : 0);
            hashCode += InstanceTimePair.hashCode();
            if (!TextUtils.isEmpty(mNote))
                hashCode += mNote.hashCode();
            hashCode += mTaskStartExactTimeStamp.hashCode();
            return hashCode;
        }

        public void setChildren(@NonNull HashMap<InstanceKey, InstanceData> children) {
            Children = children;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof InstanceData))
                return false;

            InstanceData instanceData = (InstanceData) object;

            if ((Done == null) != (instanceData.Done == null))
                return false;

            if ((Done != null) && !Done.equals(instanceData.Done))
                return false;

            if (!InstanceKey.equals(instanceData.InstanceKey))
                return false;

            if (TextUtils.isEmpty(DisplayText) != TextUtils.isEmpty(instanceData.DisplayText))
                return false;

            if (!TextUtils.isEmpty(DisplayText) && !DisplayText.equals(instanceData.DisplayText))
                return false;

            if (!Children.equals(instanceData.Children))
                return false;

            if (!Name.equals(instanceData.Name))
                return false;

            if (!InstanceTimeStamp.equals(instanceData.InstanceTimeStamp))
                return false;

            if (TaskCurrent != instanceData.TaskCurrent)
                return false;

            if (IsRootInstance != instanceData.IsRootInstance)
                return false;

            if ((IsRootTask == null) != (instanceData.IsRootTask == null))
                return false;

            if ((IsRootTask != null) && !IsRootTask.equals(instanceData.IsRootTask))
                return false;

            if (Exists != instanceData.Exists)
                return false;

            if (!InstanceTimePair.equals(instanceData.InstanceTimePair))
                return false;

            if (TextUtils.isEmpty(mNote) != TextUtils.isEmpty(instanceData.mNote))
                return false;

            if (!TextUtils.isEmpty(mNote) && !mNote.equals(instanceData.mNote))
                return false;

            if (!mTaskStartExactTimeStamp.equals(instanceData.mTaskStartExactTimeStamp))
                return false;

            return true;
        }

        @Override
        public void remove(InstanceKey instanceKey) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(Children.containsKey(instanceKey));

            Children.remove(instanceKey);
        }
    }

    public static class CustomTimeData {
        public final String Name;
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(String name, TreeMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes != null);
            Assert.assertTrue(hourMinutes.size() == 7);

            Name = name;
            HourMinutes = hourMinutes;
        }

        @Override
        public int hashCode() {
            return (Name.hashCode() + HourMinutes.hashCode());
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof CustomTimeData))
                return false;

            CustomTimeData customTimeData = (CustomTimeData) object;

            return (Name.equals(customTimeData.Name) && HourMinutes.equals(customTimeData.HourMinutes));
        }
    }

    public interface InstanceDataParent {
        void remove(InstanceKey instanceKey);
    }

    public static class TaskData {
        @NonNull
        public final TaskKey mTaskKey;

        @NonNull
        public final String Name;

        @NonNull
        final List<TaskData> Children;

        @NonNull
        final ExactTimeStamp mStartExactTimeStamp;

        @Nullable
        final String mNote;

        public TaskData(@NonNull TaskKey taskKey, @NonNull String name, @NonNull List<TaskData> children, @NonNull ExactTimeStamp startExactTimeStamp, @Nullable String note) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            mTaskKey = taskKey;
            Name = name;
            Children = children;
            mStartExactTimeStamp = startExactTimeStamp;
            mNote = note;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += mTaskKey.hashCode();
            hashCode += Name.hashCode();
            hashCode += Children.hashCode();
            hashCode += mStartExactTimeStamp.hashCode();
            if (!TextUtils.isEmpty(mNote))
                hashCode += mNote.hashCode();
            return hashCode;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof TaskData))
                return false;

            TaskData taskData = (TaskData) object;

            if (!mTaskKey.equals(taskData.mTaskKey))
                return false;

            if (!Name.equals(taskData.Name))
                return false;

            if (!Children.equals(taskData.Children))
                return false;

            if (!mStartExactTimeStamp.equals(taskData.mStartExactTimeStamp))
                return false;

            if (!Utils.stringEquals(mNote, taskData.mNote))
                return false;

            return true;
        }
    }

}