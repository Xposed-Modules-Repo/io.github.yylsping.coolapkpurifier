package com.coolapk.market.view.cardlist;

/** Mirrors the unique feedRecommendListCard predicate shape. */
public final class MainV8ListFragment extends EntityListFragment {
    public static boolean isRecommendCard(Object entity) {
        return entity != null;
    }

    public static boolean unsafeBroadPredicate(String value) {
        return value != null;
    }
}
