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
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.firebase.DatabaseWrapper;
import com.krystianwsul.checkme.firebase.FriendListLoader;
import com.krystianwsul.checkme.firebase.UserData;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.MainActivity;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.gui.customtimes.ShowCustomTimesFragment;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.List;

public class FriendListFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<List<UserData>> {
    private static final String USER_DATA_KEY = "userData";
    private static final String SELECTED_USER_DATA_EMAILS_KEY = "selectedUserDataEmails";
    private static final String ERROR_KEY = "error";

    private RelativeLayout mFriendListLayout;
    private ProgressBar mFriendListProgress;
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

    @Nullable
    private UserData mUserData = null;

    private boolean mError = false;

    public FriendListFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

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

        mFriendListProgress = (ProgressBar) mFriendListLayout.findViewById(R.id.friend_list_progress);
        Assert.assertTrue(mFriendListProgress != null);

        mFriendListRecycler = (RecyclerView) mFriendListLayout.findViewById(R.id.friend_list_recycler);
        Assert.assertTrue(mFriendListRecycler != null);

        mFriendListRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        mFriendListFab = (FloatingActionButton) mFriendListLayout.findViewById(R.id.friend_list_fab);
        Assert.assertTrue(mFriendListFab != null);

        mFriendListFab.setOnClickListener(v -> startActivity(FindFriendActivity.newIntent(getActivity())));

        mEmptyText = (TextView) mFriendListLayout.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        if (savedInstanceState != null) {
            Assert.assertTrue(savedInstanceState.containsKey(ERROR_KEY));
            mError = savedInstanceState.getBoolean(ERROR_KEY);

            if (savedInstanceState.containsKey(USER_DATA_KEY)) {
                mUserData = savedInstanceState.getParcelable(USER_DATA_KEY);
                Assert.assertTrue(mUserData != null);
            }

            if (savedInstanceState.containsKey(SELECTED_USER_DATA_EMAILS_KEY)) {
                mSelectedUserDataEmails = savedInstanceState.getStringArrayList(SELECTED_USER_DATA_EMAILS_KEY);
                Assert.assertTrue(mSelectedUserDataEmails != null);
                Assert.assertTrue(!mSelectedUserDataEmails.isEmpty());
            }
        }

        updateVisibility();
    }

    @Override
    public Loader<List<UserData>> onCreateLoader(int id, Bundle args) {
        Assert.assertTrue(mUserData != null);

        return new FriendListLoader(getActivity(), mUserData);
    }

    @Override
    public void onLoadFinished(Loader<List<UserData>> loader, @Nullable List<UserData> userDatas) {
        if (userDatas == null) {
            mFriendListProgress.setVisibility(View.GONE);
            mFriendListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.GONE);
            mFriendListFab.setVisibility(View.GONE);

            if (!mError) {
                mError = true;
                Toast.makeText(getActivity(), R.string.connectionError, Toast.LENGTH_SHORT).show();
            }
        } else {
            mError = false;

            if (mFriendListAdapter != null) {
                ArrayList<String> selectedUserDataKeys = mFriendListAdapter.getSelected();
                if (selectedUserDataKeys.isEmpty())
                    mSelectedUserDataEmails = null;
                else
                    mSelectedUserDataEmails = selectedUserDataKeys;
            }

            mFriendListAdapter = new FriendListAdapter(userDatas, mSelectedUserDataEmails);
            mFriendListRecycler.setAdapter(mFriendListAdapter);

            mSelectionCallback.setSelected(mFriendListAdapter.getSelected().size());

            mFriendListProgress.setVisibility(View.GONE);
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
    }

    @Override
    public void onLoaderReset(Loader<List<UserData>> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean(ERROR_KEY, mError);

        if (mUserData != null)
            outState.putParcelable(USER_DATA_KEY, mUserData);

        if (mFriendListAdapter != null) {
            ArrayList<String> selectedUserDataEmails = mFriendListAdapter.getSelected();
            if (!selectedUserDataEmails.isEmpty())
                outState.putStringArrayList(SELECTED_USER_DATA_EMAILS_KEY, selectedUserDataEmails);
        }
    }

    public void show(@NonNull UserData userData) {
        mUserData = userData;
        mError = false;

        updateVisibility();
    }

    public void hide() {
        mUserData = null;

        updateVisibility();
    }

    private void updateVisibility() {
        if (mFriendListLayout == null)
            return;

        if (mUserData == null) {
            mFriendListLayout.setVisibility(View.GONE);

            getLoaderManager().destroyLoader(0);
        } else {
            mFriendListLayout.setVisibility(View.VISIBLE);
            mFriendListProgress.setVisibility(View.VISIBLE);
            mFriendListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.GONE);
            mFriendListFab.setVisibility(View.GONE);

            getLoaderManager().initLoader(0, null, this);
        }
    }

    public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {
        @NonNull
        private final List<UserData> mUserDatas;

        @NonNull
        private final List<UserDataWrapper> mUserDataWrappers;

        FriendListAdapter(@NonNull List<UserData> userDatas, @Nullable ArrayList<String> selectedUserDataEmails) {
            mUserDatas = userDatas;
            mUserDataWrappers = Stream.of(mUserDatas)
                    .map(userData -> new UserDataWrapper(userData, selectedUserDataEmails))
                    .collect(Collectors.toList());
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
        public FriendHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View friendRow = layoutInflater.inflate(R.layout.row_friend, parent, false);

            TextView friendName = (TextView) friendRow.findViewById(R.id.friend_name);
            Assert.assertTrue(friendName != null);

            TextView friendEmail = (TextView) friendRow.findViewById(R.id.friend_email);
            Assert.assertTrue(friendEmail != null);

            return new FriendHolder(friendRow, friendName, friendEmail);
        }

        @Override
        public void onBindViewHolder(final FriendHolder friendHolder, int position) {
            UserDataWrapper userDataWrapper = mUserDataWrappers.get(position);
            Assert.assertTrue(userDataWrapper != null);

            friendHolder.mFriendName.setText(userDataWrapper.mUserData.displayName);
            friendHolder.mFriendEmail.setText(userDataWrapper.mUserData.email);

            if (userDataWrapper.mSelected)
                friendHolder.mFriendRow.setBackgroundColor(ContextCompat.getColor(getActivity(), R.color.selected));
            else
                friendHolder.mFriendRow.setBackgroundColor(Color.TRANSPARENT);

            friendHolder.mFriendRow.setOnLongClickListener(v -> {
                friendHolder.onLongClick();
                return true;
            });

            friendHolder.mFriendRow.setOnClickListener(v -> {
                if (mSelectionCallback.hasActionMode())
                    friendHolder.onLongClick();
                else
                    friendHolder.onRowClick();
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
                mUserDatas.remove(userDataWrapper.mUserData);

                int position = mUserDataWrappers.indexOf(userDataWrapper);
                mUserDataWrappers.remove(position);
                notifyItemRemoved(position);
            }

            UserData userData = MainActivity.getUserData();
            Assert.assertTrue(userData != null);

            Stream.of(selectedUserDataWrappers).forEach(userDataWrapper -> DatabaseWrapper.removeFriend(userData, userDataWrapper.mUserData));
        }

        class FriendHolder extends RecyclerView.ViewHolder {
            final View mFriendRow;
            final TextView mFriendName;
            final TextView mFriendEmail;

            FriendHolder(@NonNull View friendRow, @NonNull TextView friendName, @NonNull TextView friendEmail) {
                super(friendRow);

                mFriendRow = friendRow;
                mFriendName = friendName;
                mFriendEmail = friendEmail;
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
                    mSelectionCallback.incrementSelected();
                } else {
                    mSelectionCallback.decrementSelected();
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
