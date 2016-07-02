package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.krystianwsul.checkme.domainmodel.DomainFactory;

import junit.framework.Assert;

public class CreateChildTaskLoader extends DomainLoader<CreateChildTaskLoader.Data> {
    private final Integer mChildTaskId;

    public CreateChildTaskLoader(Context context, Integer childTaskId) {
        super(context);

        mChildTaskId = childTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateChildTaskData(mChildTaskId, getContext());
    }

    public static class Data extends DomainLoader.Data {
        public final ChildTaskData ChildTaskData;

        public Data(ChildTaskData childTaskData) {
            ChildTaskData = childTaskData;
        }

        @Override
        public int hashCode() {
            int hash = 0;
            if (ChildTaskData != null)
                hash += ChildTaskData.hashCode();
            return hash;
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

            if ((ChildTaskData == null) != (data.ChildTaskData == null))
                return false;

            if ((ChildTaskData != null) && !ChildTaskData.equals(data.ChildTaskData))
                return false;

            return true;
        }
    }

    public static class ChildTaskData {
        public final String Name;
        public final int ParentTaskId;

        public ChildTaskData(String name, int parentTaskId) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
            ParentTaskId = parentTaskId;
        }

        @Override
        public int hashCode() {
            return Name.hashCode() + ParentTaskId;
        }

        @SuppressWarnings("RedundantIfStatement")
        @Override
        public boolean equals(Object object) {
            if (object == null)
                return false;

            if (object == this)
                return true;

            if (!(object instanceof ChildTaskData))
                return false;

            ChildTaskData childTaskData = (ChildTaskData) object;

            if (!Name.equals(childTaskData.Name))
                return false;

            if (ParentTaskId != childTaskData.ParentTaskId)
                return false;

            return true;
        }
    }
}
