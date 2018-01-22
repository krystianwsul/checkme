package com.krystianwsul.checkme.gui.tasks;


import android.app.Dialog;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.MaterialDialog;
import com.krystianwsul.checkme.R;
import com.krystianwsul.checkme.gui.AbstractDialogFragment;

import junit.framework.Assert;

import java.util.List;

public class FriendPickerFragment extends AbstractDialogFragment {
    private ProgressBar mFriendPickerProgress;
    private RecyclerView mFriendPickerRecycler;

    @Nullable
    private List<FriendData> mFriendDatas;

    private Listener mListener;

    @NonNull
    public static FriendPickerFragment newInstance() {
        return new FriendPickerFragment();
    }

    public FriendPickerFragment() {

    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        MaterialDialog.Builder builder = new MaterialDialog.Builder(getActivity())
                .title(R.string.friend_dialog_title)
                .customView(R.layout.fragment_friend_picker, false)
                .negativeText(android.R.string.cancel)
                .onNegative((dialog, which) -> dialog.cancel());

        MaterialDialog materialDialog = builder.build();

        LinearLayout linearLayout = (LinearLayout) materialDialog.getCustomView();
        Assert.assertTrue(linearLayout != null);

        mFriendPickerProgress = (ProgressBar) linearLayout.findViewById(R.id.friend_picker_progress);
        Assert.assertTrue(mFriendPickerProgress != null);

        mFriendPickerRecycler = (RecyclerView) linearLayout.findViewById(R.id.friend_picker_recycler);
        Assert.assertTrue(mFriendPickerRecycler != null);

        mFriendPickerRecycler.setLayoutManager(new LinearLayoutManager(getActivity()));

        return materialDialog;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mFriendPickerProgress.setVisibility(View.GONE);
        mFriendPickerRecycler.setVisibility(View.VISIBLE);

        if (mFriendDatas != null)
            initialize();
    }

    public void initialize(@NonNull List<FriendData> friendDatas, @NonNull Listener listener) {
        mFriendDatas = friendDatas;
        mListener = listener;

        if (getActivity() != null)
            initialize();
    }

    private void initialize() {
        Assert.assertTrue(getActivity() != null);
        Assert.assertTrue(mFriendDatas != null);
        Assert.assertTrue(mListener != null);

        mFriendPickerRecycler.setAdapter(new FriendListAdapter());
    }

    public interface Listener {
        void onFriendSelected(@NonNull String friendId);
    }

    class FriendListAdapter extends RecyclerView.Adapter<FriendListAdapter.FriendHolder> {
        @Override
        public int getItemCount() {
            Assert.assertTrue(mFriendDatas != null);

            return mFriendDatas.size();
        }

        @Override
        public FriendListAdapter.FriendHolder onCreateViewHolder(ViewGroup parent, int viewType) {
            LayoutInflater layoutInflater = LayoutInflater.from(getActivity());
            View friendRow = layoutInflater.inflate(R.layout.row_friend, parent, false);

            TextView friendName = (TextView) friendRow.findViewById(R.id.friendName);
            Assert.assertTrue(friendName != null);

            TextView friendEmail = (TextView) friendRow.findViewById(R.id.friendEmail);
            Assert.assertTrue(friendEmail != null);

            return new FriendListAdapter.FriendHolder(friendRow, friendName, friendEmail);
        }

        @Override
        public void onBindViewHolder(final FriendListAdapter.FriendHolder friendHolder, int position) {
            Assert.assertTrue(mFriendDatas != null);

            FriendData friendData = mFriendDatas.get(position);
            Assert.assertTrue(friendData != null);

            friendHolder.mFriendName.setText(friendData.mName);
            friendHolder.mFriendEmail.setText(friendData.mEmail);

            friendHolder.mFriendRow.setOnClickListener(v -> friendHolder.onRowClick());
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
                Assert.assertTrue(mFriendDatas != null);

                FriendData friendData = mFriendDatas.get(getAdapterPosition());
                Assert.assertTrue(friendData != null);

                dismiss();

                mListener.onFriendSelected(friendData.mId);
            }
        }
    }

    public static class FriendData {
        @NonNull
        final String mId;

        @NonNull
        final String mName;

        @NonNull
        final String mEmail;

        public FriendData(@NonNull String id, @NonNull String name, @NonNull String email) {
            Assert.assertTrue(!TextUtils.isEmpty(id));
            Assert.assertTrue(!TextUtils.isEmpty(name));
            Assert.assertTrue(!TextUtils.isEmpty(email));

            mId = id;
            mName = name;
            mEmail = email;
        }
    }
}
