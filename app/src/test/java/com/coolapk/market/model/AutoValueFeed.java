package com.coolapk.market.model;

/** Mirrors the concrete generated getter hooked by the detail sponsor filter. */
public final class AutoValueFeed extends Feed {
    @Override
    public Entity getDetailSponsorCard() {
        return new Entity();
    }

    @Override
    public java.util.List<Entity> getRelatedData() {
        return java.util.Collections.singletonList(new Entity());
    }
}
