package com.coolapk.market.view.feed;

import androidx.compose.runtime.Composer;
import com.coolapk.market.model.Feed;
import kotlin.Unit;

/** Mirrors FeedBottomViewHolder's dedicated target-row Compose assembler. */
public final class FeedBottomHolder {
    public static Unit composeTargetRow(Feed feed, FeedBottomHolder holder,
                                        Composer composer, int changed) {
        return Unit.INSTANCE;
    }

    public static Unit oldRenderer(Object modifier, Object target, Object viewModel,
                                   Composer composer, int changed) {
        return Unit.INSTANCE;
    }
}
