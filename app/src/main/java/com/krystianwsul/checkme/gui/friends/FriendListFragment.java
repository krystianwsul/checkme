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
import com.krystianwsul.checkme.gui.FabUser;
import com.krystianwsul.checkme.gui.SelectionCallback;
import com.krystianwsul.checkme.loaders.FriendListLoader;

import junit.framework.Assert;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class FriendListFragment extends AbstractFragment implements LoaderManager.LoaderCallbacks<FriendListLoader.Data>, FabUser {
    private static final String SELECTED_IDS_KEY = "selectedIds";

    private ProgressBar mFriendListProgress;
    private RecyclerView mFriendListRecycler;
    private TextView mEmptyText;

    private FriendListAdapter mFriendListAdapter;

    @Nullable
    private FriendListLoader.Data mData;

    @Nullable
    private List<String> mSelectedIds;

    private final SelectionCallback mSelectionCallback = new SelectionCallback() {
        @Override
        protected void unselect() {
            mFriendListAdapter.unselect();
        }

        @Override
        protected void onMenuClick(MenuItem menuItem) {
            List<String> selectedUserDataEmails = mFriendListAdapter.getSelected();
            Assert.assertTrue(!selectedUserDataEmails.isEmpty());

            switch (menuItem.getItemId()) {
                case R.id.action_custom_times_delete:
                    mFriendListAdapter.removeSelected();

                    updateSelectAll();

                    break;
                default:
                    throw new UnsupportedOperationException();
            }
        }

        @Override
        protected void onFirstAdded() {
            ((AppCompatActivity) getActivity()).startSupportActionMode(this);

            mActionMode.getMenuInflater().inflate(R.menu.menu_custom_times, mActionMode.getMenu());

            updateFabVisibility();

            ((Listener) getActivity()).onCreateUserActionMode(mActionMode);
        }

        @Override
        protected void onSecondAdded() {

        }

        @Override
        protected void onOtherAdded() {

        }

        @Override
        protected void onLastRemoved() {
            updateFabVisibility();

            ((Listener) getActivity()).onDestroyUserActionMode();
        }

        @Override
        protected void onSecondToLastRemoved() {

        }

        @Override
        protected void onOtherRemoved() {

        }
    };

    @Nullable
    private FloatingActionButton mFriendListFab;

    @NonNull
    public static FriendListFragment newInstance() {
        return new FriendListFragment();
    }

    public FriendListFragment() {

    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        Assert.assertTrue(context instanceof Listener);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_friend_list, container, false);
    }

    @Override
    public void onActivityCreated(@Nullable Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        RelativeLayout friendListLayout = (RelativeLayout) getView();
        Assert.assertTrue(friendListLayout != null);

        mFriendListProgress = (ProgressBar) friendListLayout.findViewById(R.id.friend_list_progress);
        Assert.assertTrue(mFriendListProgress != null);

        mFriendListRecycler = (RecyclerView) friendListLayout.findViewById(R.id.friend_list_recycler);
        Assert.assertTrue(mFriendListRecycler != null);

        mFriendListRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        mEmptyText = (TextView) friendListLayout.findViewById(R.id.empty_text);
        Assert.assertTrue(mEmptyText != null);

        if (savedInstanceState != null && savedInstanceState.containsKey(SELECTED_IDS_KEY)) {
            mSelectedIds = savedInstanceState.getStringArrayList(SELECTED_IDS_KEY);
            Assert.assertTrue(mSelectedIds != null);
        }

        getLoaderManager().initLoader(0, null, this);
    }

    @Override
    public Loader<FriendListLoader.Data> onCreateLoader(int id, Bundle args) {
        return new FriendListLoader(getActivity());
    }

    @Override
    public void onLoadFinished(Loader<FriendListLoader.Data> loader, FriendListLoader.Data data) {
        Assert.assertTrue(data != null);

        mData = data;

        if (mFriendListAdapter != null)
            mSelectedIds = mFriendListAdapter.getSelected();
        else if (mSelectedIds == null)
            mSelectedIds = new ArrayList<>();

        mFriendListAdapter = new FriendListAdapter(data.mUserListDatas, mSelectedIds);
        mFriendListRecycler.setAdapter(mFriendListAdapter);

        mSelectionCallback.setSelected(mFriendListAdapter.getSelected().size());

        mFriendListProgress.setVisibility(View.GONE);

        updateFabVisibility();

        if (data.mUserListDatas.isEmpty()) {
            mFriendListRecycler.setVisibility(View.GONE);
            mEmptyText.setVisibility(View.VISIBLE);
            mEmptyText.setText(R.string.friends_empty);
        } else {
            mFriendListRecycler.setVisibility(View.VISIBLE);
            mEmptyText.setVisibility(View.GONE);
        }

        updateSelectAll();
    }

    @Override
    public void onLoaderReset(Loader<FriendListLoader.Data> loader) {
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (mFriendListAdapter != null)
            outState.putStringArrayList(SELECTED_IDS_KEY, new ArrayList<>(mFriendListAdapter.getSelected()));
    }

    private void updateSelectAll() {
        Assert.assertTrue(mFriendListAdapter != null);

        ((Listener) getActivity()).setUserSelectAllVisibility(mFriendListAdapter.getItemCount() != 0);
    }

    public void selectAll() {
        mFriendListAdapter.selectAll();
    }

    @Override
    public void setFab(@NonNull FloatingActionButton floatingActionButton) {
        Assert.assertTrue(mFriendListFab == null);

        mFriendListFab = floatingActionButton;

        mFriendListFab.setOnClickListener(v -> startActivity(FindFriendActivity.newIntent(getActivity())));

        updateFabVisibility();
    }

    private void updateFabVisibility() {
        if (mFriendListFab == null)
            return;

        if (mData != null && !mSelectionCallback.hasActionMode()) {
            mFriendListFab.show();
        } else {
            mFriendListFab.hide();
        }
    }

    @Override
    public void clearFab() {
        if (mFriendListFab == null)
            return;

        mFriendListFab.setOnClickListener(null);

        mFriendListFab.hide();

        mFriendListFab = null;
    }

    public class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {
        @NonNull
        private final List<UserDataWrapper> mUserDataWrappers;

        FriendListAdapter(@NonNull Collection<FriendListLoader.UserListData> userListDatas, @NonNull List<String> selectedIds) {
            Assert.assertTrue(mData != null);

            Map<String, FriendListLoader.UserListData> userListMap = Stream.of(userListDatas)
                    .collect(Collectors.toMap(userListData -> userListData.mId, userListData -> userListData));

            mUserDataWrappers = Stream.of(userListMap.values())
                    .sorted((lhs, rhs) -> lhs.mId.compareTo(rhs.mId))
                    .map(userListData -> new UserDataWrapper(userListData, selectedIds))
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

        @NonNull
        List<String> getSelected() {
            return new ArrayList<>(Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .map(userDataWrapper -> userDataWrapper.mUserListData.mId)
                    .collect(Collectors.toList()));
        }

        void removeSelected() {
            List<UserDataWrapper> selectedUserDataWrappers = Stream.of(mUserDataWrappers)
                    .filter(userDataWrapper -> userDataWrapper.mSelected)
                    .collect(Collectors.toList());

            for (UserDataWrapper userDataWrapper : selectedUserDataWrappers) {
                int position = mUserDataWrappers.indexOf(userDataWrapper);
                mUserDataWrappers.remove(position);
                notifyItemRemoved(position);
            }

            DomainFactory.getDomainFactory(getActivity())
                    .removeFriends(Stream.of(selectedUserDataWrappers)
                            .map(userDataWrapper -> userDataWrapper.mUserListData.mId)
                            .collect(Collectors.toSet()));
        }

        public void selectAll() {
            Assert.assertTrue(!mSelectionCallback.hasActionMode());

            Stream.of(mUserDataWrappers)
                    .filter(customTimeWrapper -> !customTimeWrapper.mSelected)
                    .forEach(UserDataWrapper::toggleSelect);
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

                userDataWrapper.toggleSelect();
            }
        }
    }

    private class UserDataWrapper {
        @NonNull
        final FriendListLoader.UserListData mUserListData;

        boolean mSelected = false;

        UserDataWrapper(@NonNull FriendListLoader.UserListData userListData, @NonNull List<String> selectedIds) {
            mUserListData = userListData;

            mSelected = selectedIds.contains(mUserListData.mId);
        }

        void toggleSelect() {
            mSelected = !mSelected;

            if (mSelected) {
                mSelectionCallback.incrementSelected();
            } else {
                mSelectionCallback.decrementSelected();
            }

            int position = mFriendListAdapter.mUserDataWrappers.indexOf(this);
            Assert.assertTrue(position >= 0);

            mFriendListAdapter.notifyItemChanged(position);
        }
    }

    public interface Listener {
        void onCreateUserActionMode(@NonNull ActionMode actionMode);

        void onDestroyUserActionMode();

        void setUserSelectAllVisibility(boolean selectAllVisible);
    }

}
