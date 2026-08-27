package io.github.yylsping.coolapkpurifier;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/** Process-local feature health; never changes persisted user choices or BootstrapState. */
final class FeatureRuntimeHealth {
    enum Status { DISABLED, INSTALLED, DEFERRED, UNAVAILABLE }
    private Status splash = Status.DISABLED;
    private Status feed = Status.DISABLED;
    private Status reply = Status.DISABLED;
    private String replyProblem;
    private final List<Runnable> listeners = new CopyOnWriteArrayList<>();

    synchronized void configure(boolean splashSelected, boolean feedSelected,
                                boolean replySelected) {
        splash = splashSelected ? Status.DEFERRED : Status.DISABLED;
        feed = feedSelected ? Status.DEFERRED : Status.DISABLED;
        reply = replySelected ? Status.DEFERRED : Status.DISABLED;
        replyProblem = null;
    }

    synchronized void updateCore(boolean splashInstalled, boolean feedInstalled) {
        if (splash != Status.DISABLED) {
            splash = splashInstalled ? Status.INSTALLED : Status.UNAVAILABLE;
        }
        if (feed != Status.DISABLED) {
            feed = feedInstalled ? Status.INSTALLED : Status.UNAVAILABLE;
        }
    }

    void replyInstalled() { updateReply(Status.INSTALLED, null); }
    void replyUnavailable(String reason) { updateReply(Status.UNAVAILABLE, reason); }

    private void updateReply(Status status, String reason) {
        synchronized (this) {
            if (reply == Status.DISABLED || reply == Status.INSTALLED) {
                return;
            }
            reply = status;
            replyProblem = reason;
        }
        for (Runnable listener : listeners) {
            listener.run();
        }
    }

    synchronized Status replyStatus() { return reply; }

    synchronized String replyMessage() {
        switch (reply) {
            case UNAVAILABLE: return "本次启动未找到适配目标，当前未生效";
            case DEFERRED: return "本次启动正在查找适配目标，尚未生效";
            case INSTALLED: return "本次启动已安装适配目标";
            default: return "本次启动未启用；修改开关后需重启酷安";
        }
    }

    synchronized String summary() {
        return "selectedFeatureStatus={splash:" + splash + ", feedSponsor:" + feed
                + ", replySponsor:" + reply + "} selectedFeatureProblems=" + problems();
    }

    synchronized List<String> problems() {
        List<String> result = new ArrayList<>();
        if (splash == Status.UNAVAILABLE) result.add("splash:targetUnavailable");
        if (feed == Status.UNAVAILABLE) result.add("feedSponsor:targetUnavailable");
        if (reply == Status.UNAVAILABLE) result.add("replySponsor:" + replyProblem);
        return result;
    }

    void addListener(Runnable listener) { listeners.add(listener); }
    void removeListener(Runnable listener) { listeners.remove(listener); }
}
