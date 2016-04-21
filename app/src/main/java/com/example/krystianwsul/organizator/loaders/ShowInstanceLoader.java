package com.example.krystianwsul.organizator.loaders;

import android.content.Context;
import android.text.TextUtils;

import com.example.krystianwsul.organizator.domainmodel.DomainFactory;
import com.example.krystianwsul.organizator.utils.InstanceKey;

import junit.framework.Assert;

public class ShowInstanceLoader extends DomainLoader<ShowInstanceLoader.Data> {
    private final InstanceKey mInstanceKey;

    public ShowInstanceLoader(Context context, InstanceKey instanceKey) {
        super(context);

        Assert.assertTrue(instanceKey != null);

        mInstanceKey = instanceKey;
    }

    @Override
    public Data loadInBackground() {
        return DomainFactory.getDomainFactory(getContext()).getShowInstanceData(getContext(), mInstanceKey);
    }

    public static class Data extends DomainLoader.Data {
        public final InstanceKey InstanceKey;
        public final String Name;
        public final String DisplayText;
        public boolean Done;
        public final boolean Editable;
        public final Boolean IsRoot;

        public Data(InstanceKey instanceKey, String name, String displayText, boolean done, boolean editable, Boolean isRoot) {
            Assert.assertTrue(instanceKey != null);
            Assert.assertTrue(!TextUtils.isEmpty(name));

            InstanceKey = instanceKey;
            Name = name;
            DisplayText = displayText;
            Done = done;
            Editable = editable;
            IsRoot = isRoot;
        }

        @Override
        public int hashCode() {
            int hashCode = 0;
            hashCode += InstanceKey.hashCode();
            hashCode += Name.hashCode();
            if (!TextUtils.isEmpty(DisplayText))
                hashCode += DisplayText.hashCode();
            hashCode += (Done ? 1 : 0);
            hashCode += (Editable ? 1 : 0);
            if (IsRoot != null)
                hashCode += (IsRoot ? 2 : 1);
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

            return (InstanceKey.equals(data.InstanceKey) && Name.equals(data.Name) && ((TextUtils.isEmpty(DisplayText) && TextUtils.isEmpty(data.DisplayText)) || ((!TextUtils.isEmpty(DisplayText) && !TextUtils.isEmpty(data.DisplayText)) && DisplayText.equals(data.DisplayText))) && (Done == data.Done) && (Editable == data.Editable) && (IsRoot == data.IsRoot));
        }
    }
}
