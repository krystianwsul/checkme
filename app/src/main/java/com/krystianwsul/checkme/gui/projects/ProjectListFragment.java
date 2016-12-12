package com.krystianwsul.checkme.gui.projects;


import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.loaders.ProjectListLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;
import java.util.TreeMap;

public class ProjectListFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<ProjectListLoader.Data> {
    private ProgressBar mProjectListProgress;
    private TextView mEmptyText;
    private RecyclerView mProjectListRecycler;

    @NonNull
    public static ProjectListFragment newInstance() {
        return new ProjectListFragment();
    }

    public ProjectListFragment() {

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

        mProjectListProgress.setVisibility(View.GONE);
        if (data.mProjectDatas.isEmpty()) {
            mProjectListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.projects_empty);
        } else {
            mProjectListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }

        mProjectListRecycler.setAdapter(new ProjectListAdapter(getActivity(), data.mProjectDatas));
    }

    @Override
    public void onLoaderReset(Loader<ProjectListLoader.Data> loader) {

    }

    private static class ProjectListAdapter extends RecyclerView.Adapter<ProjectListAdapter.Holder> {
        @NonNull
        private final Context mContext;

        @NonNull
        private final List<ProjectListLoader.ProjectData> mProjectDatas;

        ProjectListAdapter(@NonNull Context context, @NonNull TreeMap<String, ProjectListLoader.ProjectData> projectDatas) {
            mContext = context;
            mProjectDatas = new ArrayList<>(projectDatas.values());
        }

        @Override
        public Holder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mContext);

            View rowProject = layoutInflater.inflate(R.layout.row_project, parent, false);
            Assert.assertTrue(rowProject != null);

            return new Holder(rowProject);
        }

        @Override
        public void onBindViewHolder(Holder holder, int position) {
            Assert.assertTrue(holder != null);

            ProjectListLoader.ProjectData projectData = mProjectDatas.get(position);
            Assert.assertTrue(projectData != null);

            holder.mProjectName.setText(projectData.mName);

            holder.mProjectUsers.setText(projectData.mUsers);
        }

        @Override
        public int getItemCount() {
            return mProjectDatas.size();
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
}
