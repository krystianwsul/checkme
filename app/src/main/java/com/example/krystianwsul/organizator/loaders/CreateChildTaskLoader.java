package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;

import junit.framework.Assert;

public class CreateChildTaskLoader extends DomainLoader<CreateChildTaskLoader.Data, CreateChildTaskLoader.Observer> {
    private final int mChildTaskId;

    public CreateChildTaskLoader(Context context, int childTaskId) {
        super(context);

        mChildTaskId = childTaskId;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getCreateChildTaskData(mChildTaskId);
    }

    @Override
    protected CreateChildTaskLoader.Observer newObserver() {
        return new Observer();
    }

    public class Observer implements DomainFactory.Observer {
        @Override
        public void onDomainChanged(int dataId) {
            if (mData != null && dataId == mData.DataId)
                return;

            Data newData = loadInBackground();

            if (mData.equals(newData))
                return;

            onContentChanged();
        }
    }

    public static class Data extends DomainLoader.Data {
        public final String Name;

        public Data(String name) {
            Assert.assertTrue(!TextUtils.isEmpty(name));

            Name = name;
        }

        @Override
        public int hashCode() {
            return Name.hashCode();
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

            return Name.equals(data.Name);
        }
    }
}
