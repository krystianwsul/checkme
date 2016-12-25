package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.instances.GroupListFragment;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

public class ShowGroupLoader extends DomainLoader<ShowGroupLoader.Data> {
    @NonNull
    private final TimeStamp mTimeStamp;

    public ShowGroupLoader(@NonNull Context context, @NonNull TimeStamp timeStamp) {
        super(context, FirebaseLevel.WANT);

        mTimeStamp = timeStamp;
    }

    @Override
    String getName() {
        return "ShowGroupLoader, timeStamp: " + mTimeStamp;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        return domainFactory.getShowGroupData(getContext(), mTimeStamp);
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final String mDisplayText;

        @Nullable
        public final GroupListFragment.DataWrapper mDataWrapper;


        public Data(@NonNull String displayText, @Nullable GroupListFragment.DataWrapper dataWrapper) {
            Assert.assertTrue(!TextUtils.isEmpty(displayText));

            mDisplayText = displayText;
            mDataWrapper = dataWrapper;
        }

        @Override
        public int hashCode() {
            int hash = mDisplayText.hashCode();
            if (mDataWrapper != null)
                hash += mDataWrapper.hashCode();
            return hash;
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

            if (!mDisplayText.equals(data.mDisplayText))
                return false;

            if (mDataWrapper == null) {
                if (data.mDataWrapper != null)
                    return false;
            } else {
                if (!mDataWrapper.equals(data.mDataWrapper))
                    return false;
            }

            return true;
        }
    }
}
