package me.sebz.mondragon.pbl5.os;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.RejectedExecutionHandler;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class RestServer {

    private Database database;
    private ExecutorService executor;

    public RestServer() {
        int corePoolSize = Runtime.getRuntime().availableProcessors();
        int maximumPoolSize = corePoolSize;
        long keepAliveTime = 30L;
        TimeUnit unit = TimeUnit.SECONDS;
        BlockingQueue<Runnable> queue = new ArrayBlockingQueue<>(100);
        RejectedExecutionHandler rejectionHandler = new ThreadPoolExecutor.CallerRunsPolicy();
        executor = new ThreadPoolExecutor(corePoolSize, maximumPoolSize, keepAliveTime, unit, queue, rejectionHandler);
        database = new Database();
    }

    public CompletableFuture<Group> getValidatedGroup(Long sessionId) {
        return CompletableFuture.completedFuture(null).thenComposeAsync(ignored -> {
            return database.getGroupFromSession(sessionId);
        }, executor).thenApplyAsync(group -> {
            if (group == null) {
                System.out.println("Invalid session ID: " + sessionId);
                throw new IllegalArgumentException("invalid session ID");
            }
            return group;
        }, executor);
    }

    private CompletableFuture<Person> getValidatedPerson(Long sessionId, int personId) {
        return getValidatedGroup(sessionId).thenApplyAsync(group -> {
            Person person = group.getMemberById(personId);
            if (person == null) {
                System.out.println("Person ID: " + personId + " not found in group ID: " + group.getId());
                throw new IllegalArgumentException("invalid personId");
            }
            return person;
        }, executor);
    }

    public CompletableFuture<Long> newSession(int groupId, String password) {
        return CompletableFuture.completedFuture(null).thenComposeAsync(ignored -> {
            return database.signIn(groupId, password);
        }, executor).thenApplyAsync(sessionId -> {
            if (sessionId != null) {
                System.out.println("New session created with ID: " + sessionId);
            } else {
                System.out.println("Failed to create session for group ID: " + groupId);
            }
            return sessionId;
        }, executor);
    }

    public CompletableFuture<Integer> createGroup(String password) {
        return CompletableFuture.supplyAsync(() -> {
            Group group = new Group();
            group.setPassword(password);
            database.addGroup(group);
            System.out.println("Created new group: with ID: " + group.getId());
            return group.getId();
        }, executor);
    }

    public CompletableFuture<Void> deleteGroup(Long sessionId) {
        return getValidatedGroup(sessionId).thenAcceptAsync(group -> {
            if (group == null) {
                return;
            }
            database.removeGroup(group);
            System.out.println("Deleted group with ID: " + group.getId());
        }, executor);
    }

    public CompletableFuture<Integer> createPerson(Long sessionId, String info, float[] faceEmbedding) {
        return getValidatedGroup(sessionId).thenApplyAsync(group -> {
            if (group == null) {
                return -1;
            }
            Person person = new Person();
            person.setInfo(info);
            person.setFaceEmbedding(faceEmbedding);
            group.addMember(person);
            System.out.println("Created new person with ID: " + person.getId() + " in group ID: " + group.getId());
            return person.getId();
        }, executor);
    }

    public CompletableFuture<Void> deletePerson(Long sessionId, int personId) {
        return getValidatedGroup(sessionId).thenAcceptAsync(group -> {
            if (group == null) {
                return;
            }
            group.removeMember(personId);
            System.out.println("Deleted person with ID: " + personId + " from group ID: " + group.getId());
        }, executor);
    }

    public CompletableFuture<Void> editPersonInfo(Long sessionId, int personId, String newInfo) {
        return getValidatedPerson(sessionId, personId).thenAcceptAsync(person -> {
            if (person == null) {
                return;
            }
            person.setInfo(newInfo);
            System.out.println("Updated info for person ID: " + personId);
        }, executor);
    }

    public CompletableFuture<Void> editPersonEmbedding(Long sessionId, int personId, float[] newEmbedding) {
        return getValidatedPerson(sessionId, personId).thenAcceptAsync(person -> {
            if (person == null) {
                return;
            }
            person.setFaceEmbedding(newEmbedding);
            System.out.println("Updated face embedding for person ID: " + personId);
        }, executor);
    }

    private CompletableFuture<Person> findClosestPerson(Long sessionId, float[] embedding) {
        return getValidatedGroup(sessionId).thenApplyAsync(group -> {
            if (group == null) {
                return null;
            }
            Person closestPerson = group.getClosestMember(embedding);
            if (closestPerson != null) {
                System.out.println("Found closest person with ID: " + closestPerson.getId() + " in group ID: " + group.getId());
            } else {
                System.out.println("No members with face embeddings found in group ID: " + group.getId());
            }
            return closestPerson;
        }, executor);
    }

    public CompletableFuture<String> identifyPerson(Long sessionId, float[] embedding) {
        return findClosestPerson(sessionId, embedding).thenApplyAsync(closestPerson -> {
            if (closestPerson != null) {
                return closestPerson.getInfo();
            }
            return null;
        }, executor);
    }

    public CompletableFuture<Void> shutdown() {
        CompletableFuture<Void> selfShutdown = CompletableFuture.runAsync(() -> {
            executor.shutdown();
            try {
                if (!executor.awaitTermination(1, TimeUnit.SECONDS)) {
                    executor.shutdownNow();
                }
            } catch (InterruptedException e) {
                executor.shutdownNow();
                Thread.currentThread().interrupt();
            }
        });

        CompletableFuture<Void> dbShutdown = database.shutdown();

        return CompletableFuture.allOf(selfShutdown, dbShutdown);
    }
}
