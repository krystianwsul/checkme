package androidx.recyclerview.widget;

import android.content.Context;

import androidx.annotation.NonNull;

public class TreeLinearLayoutManager extends LinearLayoutManager {

    private boolean freezeTopPosition = false;

    public TreeLinearLayoutManager(@NonNull Context context) {
        super(context);
    }

    @Override
    void onAnchorReady(RecyclerView.Recycler recycler, RecyclerView.State state, AnchorInfo anchorInfo, int firstLayoutItemDirection) {
        if (freezeTopPosition && state.mLayoutStep == 2) {
            anchorInfo.mPosition = 0;
            freezeTopPosition = false;
        }

        super.onAnchorReady(recycler, state, anchorInfo, firstLayoutItemDirection);
    }

    public void freezeTopPosition() {
        if (findFirstCompletelyVisibleItemPosition() == 0) {
            freezeTopPosition = true;
        }
    }
}
