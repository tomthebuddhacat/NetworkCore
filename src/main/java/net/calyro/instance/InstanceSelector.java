package net.calyro.instance;

import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

public class InstanceSelector {

    private static final Random RANDOM = new Random();

    public static AvailableInstance pickRandomJoinable(List<AvailableInstance> instances) {
        if (instances == null || instances.isEmpty()) return null;

        List<AvailableInstance> joinable = instances.stream()
                .filter(AvailableInstance::isJoinable)
                .collect(Collectors.toList());

        if (joinable.isEmpty()) return null;

        return joinable.get(RANDOM.nextInt(joinable.size()));
    }
}