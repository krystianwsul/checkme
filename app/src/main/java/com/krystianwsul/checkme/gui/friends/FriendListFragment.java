package com.krystianwsul.checkme.gui.friends;


import android.content.Context;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.FriendListLoader;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class FriendListFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<List<UserData>> {
    private static final String VISIBILITY_KEY = "visibilityKey";
    private static final String SELECTED_USER_DATA_EMAILS_KEY = "selectedUserDataEmails";

    private RelativeLayout mFriendListLayout;
    private RecyclerView mFriendListRecycler;
    private FloatingActionButton mFriendListFab;
    private TextView mEmptyText;

    private FriendListAdapter mFriendListAdapter;

    private ArrayList<String> mSelectedUserDataEmails;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mFriendListAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            ArrayList<String> selectedUserDataEmails = mFriendListAdapter.getSelected();
            Assert.assertTrue(selectedUserDataEmails != null);
            Assert.assertTrue(!selectedUserDataEmails.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_custom_times_delete:
                    mFriendListAdapter.removeSelected();
                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mActionMode.getMenuInflater().inflate(R.menu.menu_custom_times, mActionMode.getMenu());

            mFriendListFab.setVisibility(View.GONE);

            ((ShowCustomTimesFragment.CustomTimesListListener) getActivity()).onCreateCustomTimesActionMode(mActionMode);
        }

        @Override
        protected void onSecondAdded() {

        }

        @Override
        protected void onOtherAdded() {

        }

        @Override
        protected void onLastRemoved() {
            mFriendListFab.setVisibility(View.VISIBLE);

            ((ShowCustomTimesFragment.CustomTimesListListener) getActivity()).onDestroyCustomTimesActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {

        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    private boolean mVisible = false;

    public static FriendListFragment newInstance() {
        return new FriendListFragment();
    }

    public FriendListFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Log.e("asdf", "context: " + context);

        Assert.assertTrue(context instanceof ShowCustomTimesFragment.CustomTimesListListener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFriendListLayout = (RelativeLayout) getView();
        Assert.assertTrue(mFriendListLayout != null);

        mFriendListRecycler = (RecyclerView) mFriendListLayout.findViewById(R.id.friend_list_recycler);
        Assert.assertTrue(mFriendListRecycler != null);

        mFriendListRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        mFriendListFab = (FloatingActionButton) mFriendListLayout.findViewById(R.id.friend_list_fab);
        Assert.assertTrue(mFriendListFab != null);

        mFriendListFab.setOnClickListener(v -> startActivity(FindFriendActivity.newIntent(getActivity())));

        mEmptyText = (TextView) mFriendListLayout.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(VISIBILITY_KEY));
            mVisible = savedInstanceState.getBoolean(VISIBILITY_KEY);

            if (savedInstanceState.containsKey(SELECTED_USER_DATA_EMAILS_KEY)) {
                mSelectedUserDataEmails = savedInstanceState.getStringArrayList(SELECTED_USER_DATA_EMAILS_KEY);
                Assert.assertTrue(mSelectedUserDataEmails != null);
                Assert.assertTrue(!mSelectedUserDataEmails.isEmpty());
            }
        }

        updateVisibility();

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<List<UserData>> onCreateLoader(int id, Bundle args) {
        return new FriendListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<List<UserData>> loader, List<UserData> userDatas) {
        Assert.assertTrue(userDatas != null);

        if (mFriendListAdapter != null) {
            ArrayList<String> selectedUserDataKeys = mFriendListAdapter.getSelected();
            if (selectedUserDataKeys.isEmpty())
                mSelectedUserDataEmails = null;
            else
                mSelectedUserDataEmails = selectedUserDataKeys;
        }

        mFriendListAdapter = new FriendListAdapter(userDatas, this, mSelectedUserDataEmails);
        mFriendListRecycler.setAdapter(mFriendListAdapter);

        mSelectionCallback.setSelected(mFriendListAdapter.getSelected().size());

        mFriendListFab.setVisibility(View.VISIBLE);

        if (userDatas.isEmpty()) {
            mFriendListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.friends_empty);
        } else {
            mFriendListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<List<UserData>> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(VISIBILITY_KEY, mVisible);

        if (mFriendListAdapter != null) {
            ArrayList<String> selectedUserDataEmails = mFriendListAdapter.getSelected();
            if (!selectedUserDataEmails.isEmpty())
                outState.putStringArrayList(SELECTED_USER_DATA_EMAILS_KEY, selectedUserDataEmails);
        }
    }

    public void show() {
        mVisible = true;

        updateVisibility();
    }

    public void hide() {
        mVisible = false;

        updateVisibility();
    }

    private void updateVisibility() {
        if (mFriendListLayout == null)
            return;

        mFriendListLayout.setVisibility(mVisible ? View.VISIBLE : View.GONE);
    }

    public static class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.CustomTimeHolder> {
        private final List<UserDataWrapper> mUserDataWrappers;

        @NonNull
        private final FriendListFragment mShowCustomTimesFragment;

        FriendListAdapter(@NonNull List<UserData> userDatas, @NonNull FriendListFragment showCustomTimesFragment, @Nullable ArrayList<String> selectedUserDataIds) {
            mUserDataWrappers = Stream.of(userDatas)
                    .map(userData -> new UserDataWrapper(userData, selectedUserDataIds))
                    .collect(Collectors.toList());

            mShowCustomTimesFragment = showCustomTimesFragment;
        }

        @Override
        public int getItemCount() {
            return mUserDataWrappers.size();
        }

        void unselect() {
            Stream.of(mUserDataWrappers)
                    .filter(customTimeWrapper -> customTimeWrapper.mSelected)
                    .forEach(customTimeWrapper -> {
                        customTimeWrapper.mSelected = false;
                        notifyItemChanged(mUserDataWrappers.indexOf(customTimeWrapper));
                    });
        }

        @Override
        public CustomTimeHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(mShowCustomTimesFragment.getActivity());
            View showCustomTimesRow = layoutInflater.inflate(R.layout.row_show_custom_times, parent, false);

            TextView timesRowName = (TextView) showCustomTimesRow.findViewById(R.id.times_row_name);
            Assert.assertTrue(timesRowName != null);

            return new CustomTimeHolder(showCustomTimesRow, timesRowName);
        }

        @Override
        public void onBindViewHolder(final CustomTimeHolder customTimeHolder, int position) {
            UserDataWrapper userDataWrapper = mUserDataWrappers.get(position);
            Assert.assertTrue(userDataWrapper != null);

            customTimeHolder.mTimesRowName.setText(userDataWrapper.mUserData.displayName);

            if (userDataWrapper.mSelected)
                customTimeHolder.mShowCustomTimeRow.setBackgroundColor(ContextCompat.getColor(mShowCustomTimesFragment.getActivity(), R.color.selected));
            else
                customTimeHolder.mShowCustomTimeRow.setBackgroundColor(Color.TRANSPARENT);

            customTimeHolder.mShowCustomTimeRow.setOnLongClickListener(v -> {
                customTimeHolder.onLongClick();
                return true;
            });

            customTimeHolder.mShowCustomTimeRow.setOnClickListener(v -> {
                if (mShowCustomTimesFragment.mSelectionCallback.hasActionMode())
                    customTimeHolder.onLongClick();
                else
                    customTimeHolder.onRowClick();
            });
        }

        ArrayList<String> getSelected() {
            return new ArrayList<>(Stream.of(mUserDataWrappers)
                    .filter(customTimeWrapper -> customTimeWrapper.mSelected)
                    .map(customTimeWrapper -> customTimeWrapper.mUserData.email)
                    .collect(Collectors.toList()));
        }

        void removeSelected() {
            List<UserDataWrapper> selectedUserDataWrappers = Stream.of(mUserDataWrappers)
                    .filter(customTimeWrapper -> customTimeWrapper.mSelected)
                    .collect(Collectors.toList());

            for (UserDataWrapper userDataWrapper : selectedUserDataWrappers) {
                int position = mUserDataWrappers.indexOf(userDataWrapper);
                mUserDataWrappers.remove(position);
                notifyItemRemoved(position);
            }

            List<String> selectedUserDataEmails = Stream.of(selectedUserDataWrappers)
                    .map(customTimeWrapper -> customTimeWrapper.mUserData.email)
                    .collect(Collectors.toList());

            //DomainFactory.getDomainFactory(mShowCustomTimesFragment.getActivity()).setCustomTimeCurrent(mShowCustomTimesFragment.getActivity(), mDataId, selectedUserDataEmails);
        }

        class CustomTimeHolder extends RecyclerView.ViewHolder {
            final View mShowCustomTimeRow;
            final TextView mTimesRowName;

            CustomTimeHolder(View showCustomTimesRow, TextView timesRowName) {
                super(showCustomTimesRow);

                Assert.assertTrue(timesRowName != null);

                mShowCustomTimeRow = showCustomTimesRow;
                mTimesRowName = timesRowName;
            }

            void onRowClick() {
                UserDataWrapper userDataWrapper = mUserDataWrappers.get(getAdapterPosition());
                Assert.assertTrue(userDataWrapper != null);

                //mShowCustomTimesFragment.getActivity().startActivity(ShowCustomTimeActivity.getEditIntent(userDataWrapper.mUserData.Id, mShowCustomTimesFragment.getActivity()));
            }

            void onLongClick() {
                int position = getAdapterPosition();

                UserDataWrapper userDataWrapper = mUserDataWrappers.get(position);
                Assert.assertTrue(userDataWrapper != null);

                userDataWrapper.mSelected = !userDataWrapper.mSelected;

                if (userDataWrapper.mSelected) {
                    mShowCustomTimesFragment.mSelectionCallback.incrementSelected();
                } else {
                    mShowCustomTimesFragment.mSelectionCallback.decrementSelected();
                }

                notifyItemChanged(position);
            }
        }
    }

    private static class UserDataWrapper {
        @NonNull
        final UserData mUserData;

        boolean mSelected = false;

        UserDataWrapper(@NonNull UserData userData, @Nullable ArrayList<String> selectedUserDataEmails) {
            mUserData = userData;

            if (selectedUserDataEmails != null) {
                Assert.assertTrue(!selectedUserDataEmails.isEmpty());

                mSelected = selectedUserDataEmails.contains(mUserData.email);
            }
        }
    }
}
