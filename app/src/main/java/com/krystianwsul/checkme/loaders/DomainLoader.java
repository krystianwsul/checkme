package com.krystianwsul.checkme.loaders;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v4.content.AsyncTaskLoader;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.domainmodel.ObserverHolder;
import com.krystianwsul.checkme.domainmodel.UserInfo;
import com.krystianwsul.checkme.persistencemodel.SaveService;

import junit.framework.Assert;

import java.util.List;

public abstract class DomainLoader<D extends DomainLoader.Data> extends AsyncTaskLoader<D> {
    private D mData;
    private Observer mObserver;

    @NonNull
    private final DomainFactory mDomainFactory;

    @NonNull
    private final FirebaseLevel mFirebaseLevel;

    private final DomainFactory.FirebaseListener mFirebaseListener = domainFactory -> {
        Assert.assertTrue(domainFactory.isConnected());

        if (isStarted())
            forceLoad();
    };

    DomainLoader(@NonNull Context context, @NonNull FirebaseLevel firebaseLevel) {
        super(context);

        mDomainFactory = DomainFactory.getDomainFactory();
        mFirebaseLevel = firebaseLevel;
    }

    @SuppressWarnings("unused")
    abstract String getName();

    @Override
    public final D loadInBackground() {
        if (mFirebaseLevel == FirebaseLevel.NEED && !mDomainFactory.isConnected())
            return null;

        if (mFirebaseLevel == FirebaseLevel.FRIEND && !(mDomainFactory.isConnected() && mDomainFactory.hasFriends()))
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
            switch (mFirebaseLevel) {
                case NOTHING: {
                    forceLoad();

                    break;
                }
                case WANT: {
                    forceLoad();

                    FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                    if (firebaseUser != null && !mDomainFactory.isConnected()) {
                        UserInfo userInfo = new UserInfo(firebaseUser);

                        mDomainFactory.setUserInfo(getContext().getApplicationContext(), SaveService.Source.GUI, userInfo);
                    }

                    break;
                }
                case NEED: {
                    if (mDomainFactory.isConnected()) {
                        forceLoad();
                    } else {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser == null)
                            return;

                        UserInfo userInfo = new UserInfo(firebaseUser);

                        mDomainFactory.setUserInfo(getContext().getApplicationContext(), SaveService.Source.GUI, userInfo);
                        mDomainFactory.addFirebaseListener(mFirebaseListener);
                    }

                    break;
                }
                case FRIEND: {
                    if (mDomainFactory.isConnected() && mDomainFactory.hasFriends()) {
                        forceLoad();
                    } else {
                        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
                        if (firebaseUser != null) {
                            UserInfo userInfo = new UserInfo(firebaseUser);

                            mDomainFactory.setUserInfo(getContext().getApplicationContext(), SaveService.Source.GUI, userInfo);
                            mDomainFactory.addFriendFirebaseListener(mFirebaseListener);
                        }
                    }

                    break;
                }
                default:
                    throw new IndexOutOfBoundsException();
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
        public void onDomainChanged(@NonNull List<Integer> dataIds) {
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

    enum FirebaseLevel {
        NOTHING,
        WANT,
        NEED,
        FRIEND
    }
}
