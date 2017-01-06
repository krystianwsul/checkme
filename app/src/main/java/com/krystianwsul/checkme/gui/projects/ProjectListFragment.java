package com.krystianwsul.checkme.gui.projects;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.FabUser;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.tree.ModelNode;
import com.krystianwsul.checkme.gui.tree.TreeModelAdapter;
import com.krystianwsul.checkme.gui.tree.TreeNode;
import com.krystianwsul.checkme.gui.tree.TreeNodeCollection;
import com.krystianwsul.checkme.gui.tree.TreeViewAdapter;
import com.krystianwsul.checkme.loaders.ProjectListLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;

public class ProjectListFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<ProjectListLoader.Data>, FabUser {
    private static final String SELECTED_PROJECT_IDS = "selectedProjectIds";

    private ProgressBar mProjectListProgress;
    private TextView mEmptyText;
    private RecyclerView mProjectListRecycler;

    @Nullable
    private FloatingActionButton mProjectListFab;

    @Nullable
    private TreeViewAdapter mTreeViewAdapter;

    private Integer mDataId;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            Assert.assertTrue(mTreeViewAdapter != null);

            mTreeViewAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            Assert.assertTrue(mTreeViewAdapter != null);

            List<TreeNode> selected = mTreeViewAdapter.getSelectedNodes();
            Assert.assertTrue(!selected.isEmpty());

            List<ProjectListAdapter.ProjectNode> projectNodes = Stream.of(selected)
                    .map(treeNode -> ((ProjectListAdapter.ProjectNode) treeNode.getModelNode()))
                    .collect(Collectors.toList());

            Set<String> projectIds = Stream.of(projectNodes)
                    .map(projectNode -> projectNode.mProjectData.mId)
                    .collect(Collectors.toSet());

            switch (menuItem.getItemId()) {
                case R.id.action_project_delete:
                    Assert.assertTrue(mDataId != null);

                    for (TreeNode treeNode : selected) {
                        Assert.assertTrue(treeNode != null);

                        ProjectListAdapter.ProjectNode projectNode = (ProjectListAdapter.ProjectNode) treeNode.getModelNode();

                        projectNode.remove();

                        decrementSelected();
                    }

                    DomainFactory.getDomainFactory(getActivity()).setProjectEndTimeStamps(getActivity(), mDataId, projectIds);

                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        protected void onFirstAdded() {
            Assert.assertTrue(mTreeViewAdapter != null);

            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mTreeViewAdapter.onCreateActionMode();

            mActionMode.getMenuInflater().inflate(R.menu.menu_projects, mActionMode.getMenu());

            updateFabVisibility();
        }

        @Override
        protected void onSecondAdded() {

        }

        @Override
        protected void onOtherAdded() {

        }

        @Override
        protected void onLastRemoved() {
            Assert.assertTrue(mTreeViewAdapter != null);

            mTreeViewAdapter.onDestroyActionMode();

            updateFabVisibility();
        }

        @Override
        protected void onSecondToLastRemoved() {

        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    @NonNull
    private Set<String> mSelectedProjectIds = new HashSet<>();

    @NonNull
    public static ProjectListFragment newInstance() {
        return new ProjectListFragment();
    }

    public ProjectListFragment() {

    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_PROJECT_IDS)) {
            List<String> selectedProjectIds = savedInstanceState.getStringArrayList(SELECTED_PROJECT_IDS);
            Assert.assertTrue(selectedProjectIds != null);

            mSelectedProjectIds = new HashSet<>(selectedProjectIds);
        }
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_project_list, container, false);

        mProjectListProgress = (ProgressBar) view.findViewById(R.id.projectListProgress);
        Assert.assertTrue(mProjectListProgress != null);

        mEmptyText = (TextView) view.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        mProjectListRecycler = (RecyclerView) view.findViewById(R.id.projectListRecycler);
        Assert.assertTrue(mProjectListRecycler != null);

        return view;
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mProjectListRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<ProjectListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new ProjectListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<ProjectListLoader.Data> loader, ProjectListLoader.Data data) {
        Assert.assertTrue(data != null);

        mDataId = data.DataId;

        mProjectListProgress.setVisibility(View.GONE);
        if (data.mProjectDatas.isEmpty()) {
            mProjectListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.projects_empty);
        } else {
            mProjectListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }

        if (mTreeViewAdapter != null)
            mSelectedProjectIds = Stream.of(mTreeViewAdapter.getSelectedNodes())
                    .map(treeNode -> ((ProjectListAdapter.ProjectNode) treeNode.getModelNode()).mProjectData.mId)
                    .collect(Collectors.toSet());

        mTreeViewAdapter = new ProjectListAdapter(getActivity()).initialize(data.mProjectDatas);
        mProjectListRecycler.setAdapter(mTreeViewAdapter.getAdapter());

        mSelectionCallback.setSelected(mTreeViewAdapter.getSelectedNodes().size());

        updateFabVisibility();
    }

    @Override
    public void onLoaderReset(Loader<ProjectListLoader.Data> loader) {

    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mTreeViewAdapter != null)
            mSelectedProjectIds = Stream.of(mTreeViewAdapter.getSelectedNodes())
                    .map(treeNode -> ((ProjectListAdapter.ProjectNode) treeNode.getModelNode()).mProjectData.mId)
                    .collect(Collectors.toSet());

        outState.putStringArrayList(SELECTED_PROJECT_IDS, new ArrayList<>(mSelectedProjectIds));
    }

    @Override
    public void setFab(@NonNull FloatingActionButton floatingActionButton) {
        mProjectListFab = floatingActionButton;

        mProjectListFab.setOnClickListener(v -> startActivity(ShowProjectActivity.newIntent(getActivity())));

        updateFabVisibility();
    }

    private void updateFabVisibility() {
        if (mProjectListFab == null)
            return;

        if (mDataId != null && !mSelectionCallback.hasActionMode()) {
            mProjectListFab.show();
        } else {
            mProjectListFab.hide();
        }
    }

    @Override
    public void clearFab() {
        if (mProjectListFab == null)
            return;

        mProjectListFab.setOnClickListener(null);

        mProjectListFab = null;
    }

    private class ProjectListAdapter implements TreeModelAdapter {
        @NonNull
        private final Context mContext;

        private List<ProjectNode> mProjectNodes;

        private TreeViewAdapter mTreeViewAdapter;
        private TreeNodeCollection mTreeNodeCollection;

        ProjectListAdapter(@NonNull Context context) {
            mContext = context;
        }

        private TreeViewAdapter initialize(@NonNull TreeMap<String, ProjectListLoader.ProjectData> projectDatas) {
            mProjectNodes = Stream.of(projectDatas.values())
                    .map(projectData -> new ProjectNode(this, projectData))
                    .collect(Collectors.toList());

            mTreeViewAdapter = new TreeViewAdapter(false, this);
            mTreeNodeCollection = new TreeNodeCollection(mTreeViewAdapter);
            mTreeViewAdapter.setTreeNodeCollection(mTreeNodeCollection);

            mTreeNodeCollection.setNodes(Stream.of(mProjectNodes)
                    .map(projectNode -> projectNode.initialize(mTreeNodeCollection))
                    .collect(Collectors.toList()));

            return mTreeViewAdapter;
        }

        @Override
        public Holder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);

            View rowProject = layoutInflater.inflate(R.layout.row_project, parent, false);
            Assert.assertTrue(rowProject != null);

            return new Holder(rowProject);
        }

        @Override
        public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
            mTreeViewAdapter.getNode(position).onBindViewHolder(holder);
        }

        @Override
        public boolean hasActionMode() {
            return mSelectionCallback.hasActionMode();
        }

        @Override
        public void incrementSelected() {
            mSelectionCallback.incrementSelected();
        }

        @Override
        public void decrementSelected() {
            mSelectionCallback.decrementSelected();
        }

        private void remove(@NonNull ProjectNode projectNode) {
            Assert.assertTrue(mProjectNodes.contains(projectNode));

            mProjectNodes.remove(projectNode);

            TreeNode treeNode = projectNode.mTreeNode;

            mTreeNodeCollection.remove(treeNode);
        }

        private class ProjectNode implements ModelNode {
            @NonNull
            private final ProjectListAdapter mProjectListAdapter;

            @NonNull
            private final ProjectListLoader.ProjectData mProjectData;

            private TreeNode mTreeNode;

            ProjectNode(@NonNull ProjectListAdapter projectListAdapter, @NonNull ProjectListLoader.ProjectData projectData) {
                mProjectListAdapter = projectListAdapter;
                mProjectData = projectData;
            }

            @NonNull
            TreeNode initialize(@NonNull TreeNodeCollection treeNodeCollection) {
                mTreeNode = new TreeNode(this, treeNodeCollection, false, mSelectedProjectIds.contains(mProjectData.mId));
                mTreeNode.setChildTreeNodes(new ArrayList<>());
                return mTreeNode;
            }

            @Override
            public void onBindViewHolder(@NonNull RecyclerView.ViewHolder viewHolder) {
                Holder holder = (Holder) viewHolder;

                holder.mProjectName.setText(mProjectData.mName);

                holder.mProjectUsers.setText(mProjectData.mUsers);

                holder.itemView.setOnClickListener(mTreeNode.getOnClickListener());

                holder.itemView.setOnLongClickListener(mTreeNode.getOnLongClickListener());

                if (mTreeNode.isSelected())
                    holder.itemView.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.selected));
                else
                    holder.itemView.setBackgroundColor(Color.TRANSPARENT);
            }

            @Override
            public int getItemViewType() {
                return 0;
            }

            @Override
            public boolean selectable() {
                return true;
            }

            @Override
            public void onClick() {
                mContext.startActivity(ShowProjectActivity.newIntent(mContext, mProjectData.mId));
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

            @Override
            public int compareTo(@NonNull ModelNode modelNode) {
                Assert.assertTrue(modelNode instanceof ProjectNode);

                ProjectNode projectNode = (ProjectNode) modelNode;

                return mProjectData.mId.compareTo(projectNode.mProjectData.mId);
            }

            void remove() {
                mProjectListAdapter.remove(this);
            }
        }
    }

    static class Holder extends RecyclerView.ViewHolder {
        @NonNull
        final TextView mProjectName;

        @NonNull
        final TextView mProjectUsers;

        Holder(View itemView) {
            super(itemView);

            mProjectName = (TextView) itemView.findViewById(R.id.project_name);
            Assert.assertTrue(mProjectName != null);

            mProjectUsers = (TextView) itemView.findViewById(R.id.project_users);
            Assert.assertTrue(mProjectUsers != null);
        }
    }
}
