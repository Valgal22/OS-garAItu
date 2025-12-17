package me.sebz.mondragon.pbl5.os;

import java.security.SecureRandom;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class Database {
    private Set<Group> groups;
    private Map<Long, Group> sessions;
    private Random random = new SecureRandom();

    // single-threaded executor to serialize DB operations
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public Database() {
        groups = new LinkedHashSet<>();
        sessions = new java.util.HashMap<>();
    }

    public CompletableFuture<Group[]> getGroups() {
        return CompletableFuture.supplyAsync(() -> groups.toArray(new Group[0]), executor);
    }

    public CompletableFuture<Void> addGroup(Group group) {
        return CompletableFuture.runAsync(() -> groups.add(group), executor);
    }

    public CompletableFuture<Void> removeGroup(Group group) {
        return CompletableFuture.runAsync(() -> groups.remove(group), executor);
    }

    public CompletableFuture<Void> removeGroup(int groupId) {
        return CompletableFuture.runAsync(() -> groups.removeIf(group -> groupId == group.getId()), executor);
    }

    public CompletableFuture<Group> getGroupById(int groupId) {
        return CompletableFuture.supplyAsync(() -> {
            for (Group group : groups) {
                if (group.getId() == groupId) {
                    return group;
                }
            }
            return null;
        }, executor);
    }

    public CompletableFuture<Long> signIn(int id, String password) {
        return CompletableFuture.supplyAsync(() -> {
            for (Group group : groups) {
                if (group.getId() == id && group.checkPassword(password)) {
                    long sessionId = random.nextLong();
                    sessions.put(sessionId, group);
                    return sessionId;
                }
            }
            return null;
        }, executor);
    }

    public CompletableFuture<Group> getGroupFromSession(Long sessionId) {
        return CompletableFuture.supplyAsync(() -> sessions.get(sessionId), executor);
    }

    public CompletableFuture<Person> getPersonById(int personId) {
        return CompletableFuture.supplyAsync(() ->
            groups.stream()
                  .map(group -> group.getMemberById(personId))
                  .filter(person -> person != null)
                  .findFirst()
                  .orElse(null)
        , executor);
    }

    // Graceful shutdown for the executor
    public void shutdown() {
        executor.shutdown();
        try {
            if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                executor.shutdownNow();
            }
        } catch (InterruptedException e) {
            executor.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}