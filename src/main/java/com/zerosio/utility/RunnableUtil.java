package com.zerosio.utility;

import com.zerosio.Core;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

public class RunnableUtil {

    public static void runLater(Runnable runnable, long delayTicks) {
        Core.getInstance().getProxy().getScheduler().buildTask(
                Core.getInstance(), runnable)
                .delay(delayTicks * 50L, TimeUnit.MILLISECONDS).schedule();
    }

    public static void runRepeating(Runnable runnable, long initialDelay, long interval) {
        Core.getInstance().getProxy().getScheduler().buildTask(
                Core.getInstance(), runnable)
                .delay(Duration.ofMillis(initialDelay * 50L))
                .repeat(Duration.ofMillis(interval * 50L))
                .schedule();
    }

    public static void runNow(Runnable runnable) {
        runnable.run();
    }
}
