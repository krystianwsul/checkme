package com.krystianwsul.checkme.gui.instances.tree;

import android.content.Context;
import android.os.Bundle;
import android.os.Parcel;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
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
import com.krystianwsul.checkme.gui.tasks.CreateTaskActivity;
import com.krystianwsul.checkme.gui.tasks.ShowTaskActivity;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;
import com.krystianwsul.checkme.utils.Utils;
import com.krystianwsul.checkme.utils.time.Date;
import com.krystianwsul.checkme.utils.time.DayOfWeek;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;
import com.krystianwsul.checkme.utils.time.HourMinute;
import com.krystianwsul.checkme.utils.time.TimePair;
import com.krystianwsul.checkme.utils.time.TimeStamp;
import com.krystianwsul.treeadapter.TreeModelAdapter;
import com.krystianwsul.treeadapter.TreeNode;
import com.krystianwsul.treeadapter.TreeNodeCollection;
import com.krystianwsul.treeadapter.TreeViewAdapter;

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

    final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mTreeViewAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            Assert.assertTrue(mTreeViewAdapter != null);

            List<TreeNode> treeNodes = mTreeViewAdapter.getSelectedNodes();

            List<InstanceData> instanceDatas = nodesToInstanceDatas(treeNodes);
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

                        if (treeNode.getModelNode() instanceof NotDoneGroupNode) {
                            NotDoneGroupNode notDoneGroupNode = (NotDoneGroupNode) treeNode.getModelNode();
                            Assert.assertTrue(notDoneGroupNode.singleInstance());

                            InstanceData instanceData = notDoneGroupNode.getSingleInstanceData();
                            instanceData.Done = done;

                            recursiveExists(instanceData);

                            NodeCollection nodeCollection = notDoneGroupNode.getNodeCollection();

                            nodeCollection.getDividerNode().add(instanceData);
                            nodeCollection.getNotDoneGroupCollection().remove(notDoneGroupNode);
                        } else {
                            NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = (NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode();

                            InstanceData instanceData = notDoneInstanceNode.mInstanceData;
                            instanceData.Done = done;

                            recursiveExists(instanceData);

                            notDoneInstanceNode.removeFromParent();

                            notDoneInstanceNode.getParentNodeCollection().getDividerNode().add(instanceData);
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
            if (treeNode.getModelNode() instanceof NotDoneGroupNode) {
                instanceData1 = ((NotDoneGroupNode) treeNode.getModelNode()).getSingleInstanceData();
            } else if (treeNode.getModelNode() instanceof NotDoneGroupNode.NotDoneInstanceNode) {
                instanceData1 = ((NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode()).mInstanceData;
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
                if (treeNode.getModelNode() instanceof NotDoneGroupNode) {
                    NotDoneGroupNode notDoneGroupNode = (NotDoneGroupNode) treeNode.getModelNode();

                    notDoneGroupNode.removeFromParent();
                } else if (treeNode.getModelNode() instanceof NotDoneGroupNode.NotDoneInstanceNode) {
                    NotDoneGroupNode.NotDoneInstanceNode notDoneInstanceNode = (NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode();

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
    public void onAttach(@NonNull Context context) {
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
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
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

        mGroupListRecycler.setAdapter(mTreeViewAdapter);

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
    private static Date rangePositionToDate(@NonNull MainActivity.TimeRange timeRange, int position) {
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

    @NonNull
    private static List<InstanceData> nodesToInstanceDatas(@NonNull List<TreeNode> treeNodes) {
        List<InstanceData> instanceDatas = new ArrayList<>();
        for (TreeNode treeNode : treeNodes) {
            if (treeNode.getModelNode() instanceof NotDoneGroupNode) {
                InstanceData instanceData = ((NotDoneGroupNode) treeNode.getModelNode()).getSingleInstanceData();

                instanceDatas.add(instanceData);
            } else {
                Assert.assertTrue(treeNode.getModelNode() instanceof NotDoneGroupNode.NotDoneInstanceNode);

                instanceDatas.add(((NotDoneGroupNode.NotDoneInstanceNode) treeNode.getModelNode()).mInstanceData);
            }
        }

        return instanceDatas;
    }

    static void recursiveExists(@NonNull InstanceData instanceData) {
        instanceData.Exists = true;

        if (instanceData.mInstanceDataParent instanceof InstanceData) {
            InstanceData instanceData1 = (InstanceData) instanceData.mInstanceDataParent;
            recursiveExists(instanceData1);
        } else {
            Assert.assertTrue(instanceData.mInstanceDataParent instanceof DataWrapper);
        }
    }

    @NonNull
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
        final List<CustomTimeData> mCustomTimeDatas;
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

            mTreeViewAdapter = new TreeViewAdapter(this, mShowFab ? R.layout.row_group_list_fab_padding : null);

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

        @NonNull
        @Override
        public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LinearLayout groupRow = (LinearLayout) LayoutInflater.from(parent.getContext()).inflate(R.layout.row_group_list, parent, false);

            LinearLayout groupRowContainer = (LinearLayout) groupRow.findViewById(R.id.group_row_container);
            TextView groupRowName = (TextView) groupRow.findViewById(R.id.group_row_name);
            TextView groupRowDetails = (TextView) groupRow.findViewById(R.id.group_row_details);
            TextView groupRowChildren = (TextView) groupRow.findViewById(R.id.group_row_children);
            ImageView groupRowExpand = (ImageView) groupRow.findViewById(R.id.group_row_expand);
            CheckBox groupCheckBox = (CheckBox) groupRow.findViewById(R.id.group_row_checkbox);
            View groupRowSeparator = groupRow.findViewById(R.id.group_row_separator);

            return new GroupHolder(groupRow, groupRowContainer, groupRowName, groupRowDetails, groupRowChildren, groupRowExpand, groupCheckBox, groupRowSeparator);
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
        public boolean equals(@Nullable Object object) {
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
        public void remove(@NonNull InstanceKey instanceKey) {
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
        public void remove(@NonNull InstanceKey instanceKey) {
            Assert.assertTrue(Children.containsKey(instanceKey));

            Children.remove(instanceKey);
        }
    }

    public static class CustomTimeData {
        @NonNull
        public final String Name;

        @NonNull
        public final TreeMap<DayOfWeek, HourMinute> HourMinutes;

        public CustomTimeData(@NonNull String name, @NonNull TreeMap<DayOfWeek, HourMinute> hourMinutes) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(hourMinutes.size() == 7);

            Name = name;
            HourMinutes = hourMinutes;
        }

        @Override
        public int hashCode() {
            return (Name.hashCode() + HourMinutes.hashCode());
        }

        @Override
        public boolean equals(@Nullable Object object) {
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
        void remove(@NonNull InstanceKey instanceKey);
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