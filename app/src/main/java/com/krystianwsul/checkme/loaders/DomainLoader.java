package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.ObserverHolder;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.notifications.InstanceDoneService;

import junit.framework.Assert;

import java.util.ArrayList;

public abstract class DomainLoader<D extends DomainLoader.Data> extends AsyncTaskLoader<D> {
    private D mData;
    private Observer mObserver;

    private final DomainFactory mDomainFactory;
    private final boolean mNeedsFirebase;

    private final DomainFactory.FirebaseListener mFirebaseListener = new DomainFactory.FirebaseListener() {
        @Override
        public void onFirebaseResult(@NonNull DomainFactory domainFactory) {
            Assert.assertTrue(domainFactory.isConnected());

            if (isStarted()) {
                Log.e("asdf", "forceLoad b " + getName());
                forceLoad();
            }
        }

        @NonNull
        @Override
        public String getSource() {
            return getName();
        }
    };

    DomainLoader(Context context, boolean needsFirebase) {
        super(context);

        mDomainFactory = DomainFactory.getDomainFactory(getContext());
        mNeedsFirebase = needsFirebase;
    }

    abstract String getName();

    @Override
    public final D loadInBackground() {
        if (mNeedsFirebase && !mDomainFactory.isConnected())
            return null;

        return loadDomain(mDomainFactory);
    }

    protected abstract D loadDomain(@NonNull DomainFactory domainFactory);

    // main thread
    @Override
    public void deliverResult(D data) {
        if (data == null)
            return;

        if (isReset())
            return;

        if ((mData == null) || !mData.equals(data)) {
            mData = data;

            if (isStarted())
                super.deliverResult(data);
        }
    }

    // main thread
    @Override
    protected void onStartLoading() {
        if (mData != null)
            deliverResult(mData);

        if (mObserver == null) {
            mObserver = new Observer();
            ObserverHolder.getObserverHolder().addDomainObserver(mObserver);
        }

        if (takeContentChanged() || mData == null) {
            Log.e("asdf", "needs firebase? " + mNeedsFirebase + " " + getName());

            if (!mNeedsFirebase) {
                Log.e("asdf", "forceLoad a " + getName());
                forceLoad();
            } else {
                if (mDomainFactory.isConnected()) {
                    Log.e("asdf", "forceLoad c " + getName());
                    forceLoad();
                } else {
                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null) {
                        UserData userData = new UserData(firebaseUser);

                        mDomainFactory.setUserData(getContext().getApplicationContext(), userData);
                        mDomainFactory.addFirebaseListener(mFirebaseListener);
                    } else {
                        throw new InstanceDoneService.NeedsFirebaseException();
                    }
                }
            }
        }
    }

    @Override
    protected void onStopLoading() {
        mDomainFactory.removeFirebaseListener(mFirebaseListener);

        cancelLoad();
    }

    @Override
    protected void onReset() {
        onStopLoading();

        if (mData != null)
            mData = null;

        if (mObserver != null) {
            //DomainFactory.getDomainFactory(getContext()).removeDomainObserver(mObserver);
            mObserver = null;
        }
    }

    public class Observer {
        public void onDomainChanged(ArrayList<Integer> dataIds) {
            if (mData != null && dataIds.contains(mData.DataId))
                return;

            onContentChanged();
        }
    }

    public abstract static class Data {
        private static int sDataId = 1;

        public final int DataId;

        private static int getNextId() {
            return sDataId++;
        }

        public Data() {
            DataId = getNextId();
        }
    }
}
