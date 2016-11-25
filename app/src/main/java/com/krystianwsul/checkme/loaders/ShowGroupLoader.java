package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.time.TimeStamp;

import junit.framework.Assert;

public class ShowGroupLoader extends DomainLoader<ShowGroupLoader.Data> {
    @NonNull
    private final TimeStamp mTimeStamp;

    public ShowGroupLoader(@NonNull Context context, @NonNull TimeStamp timeStamp) {
        super(context);

        mTimeStamp = timeStamp;
    }

    @Override
    String getName() {
        return "ShowGroupLoader, timeStamp: " + mTimeStamp;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowGroupData(getContext(), mTimeStamp);
    }

    public static class Data extends DomainLoader.Data {
        public final String DisplayText;
        public final boolean HasInstances;

        public Data(String displayText, boolean hasInstances) {
            Assert.assertTrue(!TextUtils.isEmpty(displayText));

            DisplayText = displayText;
            HasInstances = hasInstances;
        }

        @Override
        public int hashCode() {
            return (DisplayText.hashCode() + (HasInstances ? 1 : 0));
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

            return (DisplayText.equals(data.DisplayText) && (HasInstances == data.HasInstances));
        }
    }
}
