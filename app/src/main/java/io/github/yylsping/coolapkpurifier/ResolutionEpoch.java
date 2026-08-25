package io.github.yylsping.coolapkpurifier;

import android.content.Context;

/**
 * Atomic transaction boundary shared by loader transitions and resolver
 * commit points. A commit action runs while the epoch monitor is held, so a
 * generation switch cannot slip between validation and its external effect.
 */
final class ResolutionEpoch {
    static final class Transition {
        final boolean changed;
        final long previousLoaderIdentity;
        final long generation;
        final ClassLoader loader;

        Transition(boolean changed, long previousLoaderIdentity,
                   long generation, ClassLoader loader) {
            this.changed = changed;
            this.previousLoaderIdentity = previousLoaderIdentity;
            this.generation = generation;
            this.loader = loader;
        }
    }

    private final ClassLoader fallbackLoader;
    private ClassLoader activeLoader;
    private long generation;
    private boolean activated;
    private ResolutionSessionContext activeSession;

    ResolutionEpoch(ClassLoader fallbackLoader) {
        if (fallbackLoader == null) {
            throw new IllegalArgumentException("fallbackLoader == null");
        }
        this.fallbackLoader = fallbackLoader;
    }

    synchronized Transition transition(ClassLoader nextLoader) {
        ClassLoader next = nextLoader == null ? fallbackLoader : nextLoader;
        ClassLoader previous = activated ? activeLoader : null;
        long previousIdentity = previous == null
                ? -1L : System.identityHashCode(previous);
        if (activated && next == activeLoader) {
            return new Transition(false, previousIdentity, generation, activeLoader);
        }
        if (activeSession != null) {
            activeSession.supersede();
        }
        activated = true;
        activeLoader = next;
        generation++;
        return new Transition(true, previousIdentity, generation, next);
    }

    synchronized ResolutionSessionContext capture(long sessionId) {
        return capture(sessionId, null);
    }

    synchronized ResolutionSessionContext capture(long sessionId, Context appContext) {
        ResolutionSessionContext context = new ResolutionSessionContext(
                sessionId, generation, loader(), appContext, null);
        activeSession = context;
        return context;
    }

    synchronized ResolutionSessionContext captureForTest(
            long sessionId, ResolutionSessionContext.OwnedResource resource) {
        ResolutionSessionContext context = new ResolutionSessionContext(
                sessionId, generation, loader(), null, resource);
        activeSession = context;
        return context;
    }

    synchronized boolean isCurrent(ResolutionSessionContext context) {
        return context != null
                && activeSession == context
                && !context.isInvalidated()
                && context.generation == generation
                && context.loader == loader();
    }

    synchronized boolean commit(ResolutionSessionContext context, Runnable action) {
        if (!isCurrent(context)) {
            return false;
        }
        action.run();
        return true;
    }

    synchronized void exclusive(Runnable action) {
        action.run();
    }

    synchronized void terminalizeActive() {
        if (activeSession != null) {
            activeSession.terminalize();
        }
    }

    synchronized void finish(ResolutionSessionContext context) {
        if (activeSession == context) {
            activeSession = null;
        }
    }

    synchronized long generation() {
        return generation;
    }

    synchronized ClassLoader loader() {
        return activated ? activeLoader : fallbackLoader;
    }

    synchronized boolean isActivated() {
        return activated;
    }
}
