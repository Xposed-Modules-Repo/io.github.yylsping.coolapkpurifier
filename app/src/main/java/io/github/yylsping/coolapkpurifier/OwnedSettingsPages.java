package io.github.yylsping.coolapkpurifier;

/** Main-thread ownership: closing a child page never removes its native owner. */
final class OwnedSettingsPages<A, P> {
    private final PageStateRegistry<A, P> pages = new PageStateRegistry<>();

    boolean contains(A owner) { return pages.contains(owner); }

    boolean open(A owner, P page) {
        if (pages.contains(owner)) return false;
        pages.put(owner, page);
        return true;
    }

    boolean close(A owner, P page) {
        if (pages.get(owner) != page) return false;
        pages.remove(owner);
        return true;
    }

    P removeOwner(A owner) { return pages.remove(owner); }
}
