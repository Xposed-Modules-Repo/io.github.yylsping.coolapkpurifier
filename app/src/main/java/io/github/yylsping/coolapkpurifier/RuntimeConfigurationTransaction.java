package io.github.yylsping.coolapkpurifier;

import java.util.function.BooleanSupplier;

/** Atomically binds runtime configuration publication to the active epoch. */
final class RuntimeConfigurationTransaction {
    interface Publication {
        void publish(long generation, ClassLoader loader,
                     boolean activated, boolean terminal);
    }

    private final ResolutionEpoch epoch;

    RuntimeConfigurationTransaction(ResolutionEpoch epoch) {
        this.epoch = epoch;
    }

    void publish(BooleanSupplier terminalState, Publication publication) {
        epoch.exclusive(() -> publication.publish(
                epoch.generation(), epoch.loader(), epoch.isActivated(),
                terminalState.getAsBoolean()));
    }
}
