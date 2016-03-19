package com.example.krystianwsul.organizator.gui.instances;

import android.content.Context;
import android.support.v4.content.ContextCompat;
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
import com.example.krystianwsul.organizator.notifications.TickService;
import com.example.krystianwsul.organizator.utils.InstanceKey;
import com.example.krystianwsul.organizator.utils.time.ExactTimeStamp;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;

public class InstanceAdapter extends RecyclerView.Adapter<InstanceAdapter.InstanceHolder> {
    private final Context mContext;

    private final int mDataId;
    private final Collection<Data> mDatas;

    private final ArrayList<Data> mDoneInstances = new ArrayList<>();
    private final ArrayList<Data> mNotDoneInstances = new ArrayList<>();

    private final static Comparator<Data> sComparator = new Comparator<Data>() {
        @Override
        public int compare(Data lhs, Data rhs) {
            Assert.assertTrue(lhs.Done != null);
            Assert.assertTrue(rhs.Done != null);

            return lhs.Done.compareTo(rhs.Done);
        }
    };

    public InstanceAdapter(Context context, int dataId, Collection<Data> datas) {
        Assert.assertTrue(context != null);
        Assert.assertTrue(datas != null);
        Assert.assertTrue(!datas.isEmpty());

        mContext = context;
        mDataId = dataId;
        mDatas = datas;

        for (Data data : datas) {
            if (data.Done != null)
                mDoneInstances.add(data);
            else
                mNotDoneInstances.add(data);
        }

        sort();
    }

    private void sort() {
        Collections.sort(mDoneInstances, sComparator);
    }

    private Data getData(int position) {
        Assert.assertTrue(position >= 0);
        Assert.assertTrue(position < mDoneInstances.size() + mNotDoneInstances.size());

        if (position < mDoneInstances.size())
            return mDoneInstances.get(position);
        else
            return mNotDoneInstances.get(position - mDoneInstances.size());
    }

    @Override
    public InstanceHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        TableLayout instanceRow = (TableLayout) LayoutInflater.from(mContext).inflate(R.layout.show_instance_row, parent, false);

        TextView instanceRowName = (TextView) instanceRow.findViewById(R.id.instance_row_name);
        TextView instanceRowDetails = (TextView) instanceRow.findViewById(R.id.instance_row_details);
        ImageView instanceRowImg = (ImageView) instanceRow.findViewById(R.id.instance_row_img);
        CheckBox instanceRowCheckBox = (CheckBox) instanceRow.findViewById(R.id.instance_row_checkbox);

        return new InstanceHolder(instanceRow, instanceRowName, instanceRowDetails, instanceRowImg, instanceRowCheckBox);
    }

    @Override
    public void onBindViewHolder(final InstanceHolder instanceHolder, int position) {
        Data data = getData(position);

        instanceHolder.mInstanceRowName.setText(data.Name);
        if (!TextUtils.isEmpty(data.DisplayText))
            instanceHolder.mInstanceRowDetails.setText(data.DisplayText);
        else
            instanceHolder.mInstanceRowDetails.setVisibility(View.GONE);

        if (!data.HasChildren)
            instanceHolder.mInstanceRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_label_outline_black_24dp));
        else
            instanceHolder.mInstanceRowImg.setBackground(ContextCompat.getDrawable(mContext, R.drawable.ic_list_black_24dp));

        instanceHolder.mInstanceRowCheckBox.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                instanceHolder.onCheckBoxClick((CheckBox) v);
            }
        });

        instanceHolder.mInstanceRowCheckBox.setChecked(data.Done != null);

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

    private int indexOf(Data data) {
        Assert.assertTrue(data != null);

        if (mDoneInstances.contains(data)) {
            return mDoneInstances.indexOf(data);
        } else {
            Assert.assertTrue(mNotDoneInstances.contains(data));
            return mDoneInstances.size() + mNotDoneInstances.indexOf(data);
        }
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
            Data data = getData(position);
            Assert.assertTrue(data != null);

            boolean isChecked = checkBox.isChecked();

            data.Done = DomainFactory.getDomainFactory(mContext).setInstanceDone(mDataId, data.InstanceKey, isChecked);

            TickService.startService(mContext);

            if (isChecked) {
                Assert.assertTrue(mNotDoneInstances.contains(data));

                int oldPosition = indexOf(data);

                mNotDoneInstances.remove(data);
                mDoneInstances.add(data);
                sort();

                int newPosition = indexOf(data);

                notifyItemMoved(oldPosition, newPosition);
            } else {
                Assert.assertTrue(mDoneInstances.contains(data));

                int oldPosition = indexOf(data);

                mDoneInstances.remove(data);
                mNotDoneInstances.add(data);

                int newPosition = indexOf(data);

                notifyItemMoved(oldPosition, newPosition);
            }
        }

        public void onRowClick() {
            Data data = getData(getAdapterPosition());
            Assert.assertTrue(data != null);

            mContext.startActivity(ShowInstanceActivity.getIntent(mContext, data.InstanceKey));
        }
    }

    public static class Data {
        public ExactTimeStamp Done;
        public final String Name;
        public final boolean HasChildren;
        public final InstanceKey InstanceKey;
        public final String DisplayText;

        public Data(ExactTimeStamp done, String name, boolean hasChildren, InstanceKey instanceKey, String displayText) {
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(instanceKey != null);

            Done = done;
            Name = name;
            HasChildren = hasChildren;
            InstanceKey = instanceKey;
            DisplayText = displayText;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            if (Done != null)
                hashCode += Done.hashCode();
            hashCode += Name.hashCode();
            hashCode += (HasChildren ? 1 : 0);
            hashCode += InstanceKey.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            return hashCode;
        }

        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            return (((Done == null) && (data.Done == null)) || (((Done != null) && (data.Done != null)) && Done.equals(data.Done)) && Name.equals(data.Name) && (HasChildren == data.HasChildren) && InstanceKey.equals(data.InstanceKey) && ((TextUtils.isEmpty(DisplayText) && TextUtils.isEmpty(data.DisplayText)) || ((!TextUtils.isEmpty(DisplayText) && !TextUtils.isEmpty(data.DisplayText)) && DisplayText.equals(data.DisplayText))));
        }
    }
}
