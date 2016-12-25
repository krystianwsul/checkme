package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;

import com.annimon.stream.Stream;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.utils.InstanceKey;
import com.krystianwsul.checkme.utils.TaskKey;

import junit.framework.Assert;

import java.util.Set;

public class ShowNotificationGroupLoader extends DomainLoader<ShowNotificationGroupLoader.Data> {
    @NonNull
    private final Set<InstanceKey> mInstanceKeys;

    public ShowNotificationGroupLoader(@NonNull Context context, @NonNull Set<InstanceKey> instanceKeys) {
        super(context, needsFirebase(instanceKeys));

        Assert.assertTrue(!instanceKeys.isEmpty());

        mInstanceKeys = instanceKeys;
    }

    @NonNull
    private static FirebaseLevel needsFirebase(@NonNull Set<InstanceKey> instanceKeys) {
        return (Stream.of(instanceKeys)
                .map(InstanceKey::getType)
                .anyMatch(type -> type == TaskKey.Type.REMOTE) ? FirebaseLevel.NEED : FirebaseLevel.NOTHING);
    }

    @Override
    String getName() {
        return "ShowNotificationGroupLoader, instanceKeys: " + mInstanceKeys;
    }

    @Override
    public Data loadDomain(@NonNull DomainFactory domainFactory) {
        Assert.assertTrue(!mInstanceKeys.isEmpty());

        return domainFactory.getShowNotificationGroupData(getContext(), mInstanceKeys);
    }

    public static class Data extends DomainLoader.Data {
        @NonNull
        public final GroupListLoader.DataWrapper mDataWrapper;

        public Data(@NonNull GroupListLoader.DataWrapper dataWrapper) {
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
