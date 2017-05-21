package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.instances.tree.GroupListFragment;
import com.krystianwsul.checkme.utils.time.ExactTimeStamp;

public class DayLoader extends DomainLoader<DayLoader.Data> {
    private final int mPosition;

    @NonNull
    private final MainActivity.TimeRange mTimeRange;

    public DayLoader(@NonNull Context context, int position, @NonNull MainActivity.TimeRange timeRange) {
        super(context, FirebaseLevel.WANT);

        mPosition = position;
        mTimeRange = timeRange;
    }

    @Override
    String getName() {
        return "DayLoader, position: " + mPosition + ", timeRange: " + mTimeRange;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getGroupListData(getContext(), ExactTimeStamp.getNow(), mPosition, mTimeRange);
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final GroupListFragment.DataWrapper mDataWrapper;

        public Data(@NonNull GroupListFragment.DataWrapper dataWrapper) {
            mDataWrapper = dataWrapper;
        }

        @Override
        public int hashCode() {
            return mDataWrapper.hashCode();
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof Data))
                return false;

            Data data = (Data) object;

            if (!mDataWrapper.equals(data.mDataWrapper))
                return false;

            return true;
        }
    }
}
