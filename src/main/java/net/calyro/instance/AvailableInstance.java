package net.calyro.instance;

public class AvailableInstance {

    private String name;
    private String template;
    private String nodeId;
    private String host;
    private int port;
    private int maxPlayers;
    private int players;
    private InstanceState state;
    private long lastHeartbeat;
    private long startedAt;
    
    public AvailableInstance() {
    }

    public String getName() {
        return name;
    }

    public String getTemplate() {
        return template;
    }

    public String getNodeId() {
        return nodeId;
    }

    public String getHost() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public int getMaxPlayers() {
        return maxPlayers;
    }

    public int getPlayers() {
        return players;
    }

    public InstanceState getState() {
        return state;
    }

    public long getLastHeartbeat() {
        return lastHeartbeat;
    }

    public long getStartedAt() {
        return startedAt;
    }

    public boolean isRunning() {
        return state == InstanceState.RUNNING;
    }

    public boolean isFull() {
        return players >= maxPlayers;
    }

    public boolean isJoinable() {
        return isRunning() && !isFull();
    }

    public boolean isAlive(long timeoutMs) {
        return System.currentTimeMillis() - lastHeartbeat <= timeoutMs;
    }

    public String getAddress() {
        return host + ":" + port;
    }

    public double getLoadFactor() {
        if (maxPlayers <= 0) return 0.0;
        return (double) players / (double) maxPlayers;
    }

    @Override
    public String toString() {
        return name + " [" + players + "/" + maxPlayers + "] " + state;
    }
}