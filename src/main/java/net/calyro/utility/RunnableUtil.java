package net.calyro.utility;

import net.calyro.Core;
import net.md_5.bungee.api.ProxyServer;

import java.util.concurrent.TimeUnit;

public class RunnableUtil {

    public static void runLater(Runnable runnable, long delayTicks) {
        ProxyServer.getInstance().getScheduler().schedule(
                Core.getInstance(),
                runnable,
                delayTicks * 50L,
                TimeUnit.MILLISECONDS);
    }

    public static void runRepeating(Runnable runnable, long initialDelay, long interval) {
        ProxyServer.getInstance().getScheduler().schedule(
                Core.getInstance(),
                runnable,
                initialDelay * 50L,
                interval * 50L,
                TimeUnit.MILLISECONDS);
    }

    public static void runNow(Runnable runnable) {
        runnable.run();
    }
}
