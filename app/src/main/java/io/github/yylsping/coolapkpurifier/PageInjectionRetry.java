package io.github.yylsping.coolapkpurifier;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Predicate;

/** Main-thread, page-scoped retries for views populated after onResume. */
final class PageInjectionRetry<A> {
    private static final long[] DELAYS = {0L, 100L, 250L, 500L, 1000L};
    private final PageStateRegistry<A, Pending> pending = new PageStateRegistry<>();
    private final BiConsumer<Runnable, Long> post;
    private final Consumer<Runnable> remove;
    private final Predicate<A> attempt;
    private final Consumer<A> exhausted;

    PageInjectionRetry(BiConsumer<Runnable, Long> post, Consumer<Runnable> remove,
                       Predicate<A> attempt, Consumer<A> exhausted) {
        this.post = post;
        this.remove = remove;
        this.attempt = attempt;
        this.exhausted = exhausted;
    }

    void start(A page) {
        cancel(page);
        Pending job = new Pending(page);
        pending.put(page, job);
        post.accept(job, DELAYS[0]);
    }

    void cancel(A page) {
        Pending job = pending.remove(page);
        if (job != null) {
            remove.accept(job);
            job.page = null;
        }
    }

    private final class Pending implements Runnable {
        private A page;
        private int attempts;

        Pending(A page) { this.page = page; }

        @Override public void run() {
            A current = page;
            if (current == null || pending.get(current) != this) return;
            try {
                attempts++;
                if (attempt.test(current)) {
                    cancel(current);
                } else if (attempts == DELAYS.length) {
                    cancel(current);
                    exhausted.accept(current);
                } else {
                    post.accept(this, DELAYS[attempts]);
                }
            } catch (RuntimeException | Error failure) {
                cancel(current);
                throw failure;
            }
        }
    }
}
