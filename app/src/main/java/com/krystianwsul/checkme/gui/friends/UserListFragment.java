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
import android.support.v7.view.ActionMode;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.domainmodel.DomainFactory;
import com.krystianwsul.checkme.gui.AbstractFragment;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.loaders.UserListLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class UserListFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<UserListLoader.Data> {
    private static final String PROJECT_ID_KEY = "projectId";

    private static final String SELECTED_USER_DATA_EMAILS_KEY = "selectedUserDataEmails";

    private RelativeLayout mFriendListLayout;
    private ProgressBar mFriendListProgress;
    private RecyclerView mFriendListRecycler;
    private FloatingActionButton mFriendListFab;
    private TextView mEmptyText;

    @Nullable
    private String mProjectId;

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

            ((Listener) getActivity()).onCreateUsersActionMode(mActionMode);
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

            ((Listener) getActivity()).onDestroyUsersActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {

        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    @NonNull
    public static UserListFragment newFriendInstance() {
        UserListFragment userListFragment = new UserListFragment();

        Bundle args = new Bundle();
        args.putString(PROJECT_ID_KEY, null);
        userListFragment.setArguments(args);

        return userListFragment;
    }

    @NonNull
    public static UserListFragment newProjectInstance(@NonNull String projectId) {
        Assert.assertTrue(!TextUtils.isEmpty(projectId));

        UserListFragment userListFragment = new UserListFragment();

        Bundle args = new Bundle();
        args.putString(PROJECT_ID_KEY, null);
        userListFragment.setArguments(args);

        return userListFragment;
    }

    public UserListFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof Listener);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Bundle args = getArguments();
        Assert.assertTrue(args != null);
        Assert.assertTrue(args.containsKey(PROJECT_ID_KEY));

        mProjectId = args.getString(PROJECT_ID_KEY);
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

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_USER_DATA_EMAILS_KEY)) {
            mSelectedUserDataEmails = savedInstanceState.getStringArrayList(SELECTED_USER_DATA_EMAILS_KEY);
            Assert.assertTrue(mSelectedUserDataEmails != null);
            Assert.assertTrue(!mSelectedUserDataEmails.isEmpty());
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<UserListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new UserListLoader(getActivity(), mProjectId);
    }

    @Override
    public void onLoadFinished(Loader<UserListLoader.Data> loader, @NonNull UserListLoader.Data data) {
        if (mFriendListAdapter != null) {
            ArrayList<String> selectedUserDataKeys = mFriendListAdapter.getSelected();
            if (selectedUserDataKeys.isEmpty())
                mSelectedUserDataEmails = null;
            else
                mSelectedUserDataEmails = selectedUserDataKeys;
        }

        mFriendListAdapter = new FriendListAdapter(data.mUserListDatas, mSelectedUserDataEmails);
        mFriendListRecycler.setAdapter(mFriendListAdapter);

        mSelectionCallback.setSelected(mFriendListAdapter.getSelected().size());

        mFriendListProgress.setVisibility(View.GONE);
        mFriendListFab.setVisibility(View.VISIBLE);

        if (data.mUserListDatas.isEmpty()) {
            mFriendListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.friends_empty);
        } else {
            mFriendListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }
    }

    @Override
    public void onLoaderReset(Loader<UserListLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFriendListAdapter != null) {
            ArrayList<String> selectedUserDataEmails = mFriendListAdapter.getSelected();
            if (!selectedUserDataEmails.isEmpty())
                outState.putStringArrayList(SELECTED_USER_DATA_EMAILS_KEY, selectedUserDataEmails);
        }
    }

    public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {
        @NonNull
        private final List<UserListLoader.UserListData> mUserListDatas;

        @NonNull
        private final List<UserDataWrapper> mUserDataWrappers;

        FriendListAdapter(@NonNull Collection<UserListLoader.UserListData> userListDatas, @Nullable ArrayList<String> selectedUserDataEmails) {
            mUserListDatas = new ArrayList<>(userListDatas);
            mUserDataWrappers = Stream.of(mUserListDatas)
                    .map(userData -> new UserDataWrapper(userData, selectedUserDataEmails))
                    .collect(Collectors.toList());
        }

        @Override
        public int getItemCount() {
            return mUserDataWrappers.size();
        }

        void unselect() {
            Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .forEach(userDataWrapper -> {
                        userDataWrapper.mSelected = false;
                        notifyItemChanged(mUserDataWrappers.indexOf(userDataWrapper));
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

            friendHolder.mFriendName.setText(userDataWrapper.mUserListData.mName);
            friendHolder.mFriendEmail.setText(userDataWrapper.mUserListData.mEmail);

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
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .map(userDataWrapper -> userDataWrapper.mUserListData.mEmail)
                    .collect(Collectors.toList()));
        }

        void removeSelected() {
            List<UserDataWrapper> selectedUserDataWrappers = Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .collect(Collectors.toList());

            for (UserDataWrapper userDataWrapper : selectedUserDataWrappers) {
                mUserListDatas.remove(userDataWrapper.mUserListData);

                int position = mUserDataWrappers.indexOf(userDataWrapper);
                mUserDataWrappers.remove(position);
                notifyItemRemoved(position);
            }

            DomainFactory.getDomainFactory(getActivity())
                    .removeFriends(Stream.of(selectedUserDataWrappers)
                            .map(userDataWrapper -> userDataWrapper.mUserListData.mKey)
                            .collect(Collectors.toSet()));
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
        final UserListLoader.UserListData mUserListData;

        boolean mSelected = false;

        UserDataWrapper(@NonNull UserListLoader.UserListData userListData, @Nullable ArrayList<String> selectedUserDataEmails) {
            mUserListData = userListData;

            if (selectedUserDataEmails != null) {
                Assert.assertTrue(!selectedUserDataEmails.isEmpty());

                mSelected = selectedUserDataEmails.contains(mUserListData.mEmail);
            }
        }
    }

    public interface Listener {
        void onCreateUsersActionMode(@NonNull ActionMode actionMode);

        void onDestroyUsersActionMode();
    }
}
