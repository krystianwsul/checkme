package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TableLayout;
import android.widget.TextView;

import com.example.krystianwsul.organizator.R;
import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.loaders.GroupListLoader;
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.TimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class InstanceListFragment extends Fragment implements LoaderManager.LoaderCallbacks<GroupListLoader.Data> {
    private RecyclerView mInstanceListRecycler;

    private TimeStamp mTimeStamp;
    private InstanceKey mInstanceKey;
    private ArrayList<InstanceKey> mInstanceKeys;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_instance_list, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        View view = getView();
        Assert.assertTrue(view != null);

        mInstanceListRecycler = (RecyclerView) view.findViewById(R.id.instance_list_recycler);
        Assert.assertTrue(mInstanceListRecycler != null);

        mInstanceListRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));
    }

    public void setTimeStamp(TimeStamp timeStamp) {
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(timeStamp != null);
        mTimeStamp = timeStamp;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKey(InstanceKey instanceKey) {
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKey != null);
        mInstanceKey = instanceKey;

        getLoaderManager().initLoader(0, null, this);
    }

    public void setInstanceKeys(ArrayList<InstanceKey> instanceKeys) {
        Assert.assertTrue(mTimeStamp == null);
        Assert.assertTrue(mInstanceKey == null);
        Assert.assertTrue(mInstanceKeys == null);

        Assert.assertTrue(instanceKeys != null);
        mInstanceKeys = instanceKeys;
        if (!mInstanceKeys.isEmpty())
            getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<GroupListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new GroupListLoader(getActivity(), mTimeStamp, mInstanceKey, mInstanceKeys);
    }

    @Override
    public void onLoadFinished(Loader<GroupListLoader.Data> loader, GroupListLoader.Data data) {
        mInstanceListRecycler.setAdapter(new InstanceAdapter(getActivity(), data.DataId, data.InstanceDatas.values()));
    }

    @Override
    public void onLoaderReset(Loader<GroupListLoader.Data> loader) {

    }

    public static class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.InstanceHolder> {
        private final static Comparator<GroupListLoader.InstanceData> sDoneComparator = new Comparator<GroupListLoader.InstanceData>() {
            @Override
            public int compare(GroupListLoader.InstanceData lhs, GroupListLoader.InstanceData rhs) {
                return lhs.Done.compareTo(rhs.Done);
            }
        };

        private final static Comparator<GroupListLoader.InstanceData> sNotDoneComparator = new Comparator<GroupListLoader.InstanceData>() {
            @Override
            public int compare(GroupListLoader.InstanceData lhs, GroupListLoader.InstanceData rhs) {
                return Integer.valueOf(lhs.InstanceKey.TaskId).compareTo(rhs.InstanceKey.TaskId);
            }
        };

        private final Context mContext;

        private final int mDataId;

        private final ArrayList<GroupListLoader.InstanceData> mDoneInstances = new ArrayList<>();
        private final ArrayList<GroupListLoader.InstanceData> mNotDoneInstances = new ArrayList<>();

        public InstanceAdapter(Context context, int dataId, Collection<GroupListLoader.InstanceData> instanceDatas) {
            Assert.assertTrue(context != null);
            Assert.assertTrue(instanceDatas != null);

            mContext = context;
            mDataId = dataId;

            for (GroupListLoader.InstanceData instanceData : instanceDatas) {
                if (instanceData.Done != null)
                    mDoneInstances.add(instanceData);
                else
                    mNotDoneInstances.add(instanceData);
            }

            sort();
        }

        private void sort() {
            Collections.sort(mDoneInstances, sDoneComparator);
            Collections.sort(mNotDoneInstances, sNotDoneComparator);
        }

        private int indexOf(GroupListLoader.InstanceData instanceData) {
            Assert.assertTrue(instanceData != null);

            if (mDoneInstances.contains(instanceData)) {
                Assert.assertTrue(!mNotDoneInstances.contains(instanceData));
                return mDoneInstances.indexOf(instanceData);
            } else {
                Assert.assertTrue(mNotDoneInstances.contains(instanceData));
                return mDoneInstances.size() + mNotDoneInstances.indexOf(instanceData);
            }
        }

        private GroupListLoader.InstanceData getData(int position) {
            Assert.assertTrue(position >= 0);
            Assert.assertTrue(position < mDoneInstances.size() + mNotDoneInstances.size());

            if (position < mDoneInstances.size())
                return mDoneInstances.get(position);
            else
                return mNotDoneInstances.get(position - mDoneInstances.size());
        }

        @Override
        public InstanceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            TableLayout instanceRow = (TableLayout) LayoutInflater.from(mContext).inflate(R.layout.row_show_instance, parent, false);

            TextView instanceRowName = (TextView) instanceRow.findViewById(R.id.instance_row_name);
            TextView instanceRowDetails = (TextView) instanceRow.findViewById(R.id.instance_row_details);
            ImageView instanceRowImg = (ImageView) instanceRow.findViewById(R.id.instance_row_img);
            CheckBox instanceRowCheckBox = (CheckBox) instanceRow.findViewById(R.id.instance_row_checkbox);

            return new InstanceHolder(instanceRow, instanceRowName, instanceRowDetails, instanceRowImg, instanceRowCheckBox);
        }

        @Override
        public void onBindViewHolder(final InstanceHolder instanceHolder, int position) {
            GroupListLoader.InstanceData instanceData = getData(position);

            instanceHolder.mInstanceRowName.setText(instanceData.Name);
            if (!TextUtils.isEmpty(instanceData.DisplayText))
                instanceHolder.mInstanceRowDetails.setText(instanceData.DisplayText);
            else
                instanceHolder.mInstanceRowDetails.setVisibility(View.GONE);

            if (!instanceData.HasChildren)
                instanceHolder.mInstanceRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_24dp));
            else
                instanceHolder.mInstanceRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_list_black_24dp));

            instanceHolder.mInstanceRowCheckBox.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    instanceHolder.onCheckBoxClick((CheckBox) v);
                }
            });

            instanceHolder.mInstanceRowCheckBox.setChecked(instanceData.Done != null);

            instanceHolder.mInstanceRow.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    instanceHolder.onRowClick();
                }
            });
        }

        @Override
        public int getItemCount() {
            return mDoneInstances.size() + mNotDoneInstances.size();
        }

        public class InstanceHolder extends RecyclerView.ViewHolder {
            public final TableLayout mInstanceRow;
            public final TextView mInstanceRowName;
            public final TextView mInstanceRowDetails;
            public final ImageView mInstanceRowImg;
            public final CheckBox mInstanceRowCheckBox;

            public InstanceHolder(TableLayout instanceRow, TextView instanceRowName, TextView instanceRowDetails, ImageView instanceRowImg, CheckBox instanceRowCheckBox) {
                super(instanceRow);

                Assert.assertTrue(instanceRowName != null);
                Assert.assertTrue(instanceRowDetails != null);
                Assert.assertTrue(instanceRowImg != null);
                Assert.assertTrue(instanceRowCheckBox != null);

                mInstanceRow = instanceRow;
                mInstanceRowName = instanceRowName;
                mInstanceRowDetails = instanceRowDetails;
                mInstanceRowImg = instanceRowImg;
                mInstanceRowCheckBox = instanceRowCheckBox;
            }

            public void onCheckBoxClick(CheckBox checkBox) {
                Assert.assertTrue(checkBox != null);

                int position = getAdapterPosition();
                GroupListLoader.InstanceData instanceData = getData(position);
                Assert.assertTrue(instanceData != null);

                boolean isChecked = checkBox.isChecked();

                instanceData.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, instanceData.InstanceKey, isChecked);

                TickService.startService(mContext);

                if (isChecked) {
                    Assert.assertTrue(mNotDoneInstances.contains(instanceData));

                    int oldPosition = indexOf(instanceData);

                    mNotDoneInstances.remove(instanceData);
                    mDoneInstances.add(instanceData);
                    sort();

                    int newPosition = indexOf(instanceData);

                    notifyItemMoved(oldPosition, newPosition);
                } else {
                    Assert.assertTrue(mDoneInstances.contains(instanceData));

                    int oldPosition = indexOf(instanceData);

                    mDoneInstances.remove(instanceData);
                    mNotDoneInstances.add(instanceData);
                    sort();

                    int newPosition = indexOf(instanceData);

                    notifyItemMoved(oldPosition, newPosition);
                }
            }

            public void onRowClick() {
                GroupListLoader.InstanceData instanceData = getData(getAdapterPosition());
                Assert.assertTrue(instanceData != null);

                mContext.startActivity(ShowInstanceActivity.getIntent(mContext, instanceData.InstanceKey));
            }
        }
    }
}
