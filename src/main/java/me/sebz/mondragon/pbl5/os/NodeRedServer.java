package me.sebz.mondragon.pbl5.os;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class NodeRedServer {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private FaceDetectServer detectServer = new FaceDetectServer();
    private FaceEmbeddingServer embeddingServer = new FaceEmbeddingServer();
    private RestServer restServer = new RestServer();
    private Thread detectServerThread;
    private Thread embeddingServerThread;

    public CompletableFuture<Long> login(int groupId, String password) {
        return CompletableFuture.completedFuture(null).thenComposeAsync(ignored -> {
            return restServer.newSession(groupId, password);
        }, executor);
    }

    public CompletableFuture<Integer> signup(String password) {
        return CompletableFuture.completedFuture(null).thenComposeAsync(ignored -> {
            return restServer.createGroup(password);
        }, executor);
    }

    public CompletableFuture<Void> deleteAccount(Long sessionId) {
        return CompletableFuture.completedFuture(null).thenComposeAsync(ignored -> {
            return restServer.deleteGroup(sessionId);
        }, executor);
    }

    public CompletableFuture<Integer> addPerson(Long sessionId, String info, int photoId) {
        return CompletableFuture.supplyAsync(() -> {
            return restServer.getValidatedGroup(sessionId);
        }, executor).thenComposeAsync(ignored -> {
            return detectServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(ignored -> {
            return embeddingServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(embedding -> {
            return restServer.createPerson(sessionId, info, embedding);
        }, executor);
    }

    public CompletableFuture<Void> deletePerson(Long sessionId, int personId) {
        return CompletableFuture.completedFuture(null).thenComposeAsync(ignored -> {
            return restServer.deletePerson(sessionId, personId);
        }, executor);
    }

    public CompletableFuture<Void> editPersonInfo(Long sessionId, int personId, String info) {
        return CompletableFuture.completedFuture(null).thenComposeAsync(ignored -> {
            return restServer.editPersonInfo(sessionId, personId, info);
        }, executor);
    }

    public CompletableFuture<Void> editPersonFace(Long sessionId, int personId, int photoId) {
        return CompletableFuture.supplyAsync(() -> {
            return restServer.getValidatedGroup(sessionId);
        }, executor).thenComposeAsync(ignored -> {
            return detectServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(ignored -> {
            return embeddingServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(embedding -> {
            return restServer.editPersonEmbedding(sessionId, personId, embedding);
        }, executor);
    }

    public CompletableFuture<String> identify(Long sessionId, int photoId) {
        return CompletableFuture.supplyAsync(() -> {
            return restServer.getValidatedGroup(sessionId);
        }, executor).thenComposeAsync(ignored -> {
            return detectServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(ignored -> {
            return embeddingServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(embedding -> {
            return restServer.identifyPerson(sessionId, embedding);
        }, executor);
    }

    public NodeRedServer() {
        detectServerThread = new Thread(detectServer, "FaceDetectServerThread");
        embeddingServerThread = new Thread(embeddingServer, "FaceEmbeddingServerThread");
        detectServerThread.start();
        embeddingServerThread.start();
    }

    public CompletableFuture<Void> shutdown() {
        CompletableFuture<Void> restFuture = restServer.shutdown();
        CompletableFuture<Void> detectFuture = detectServer.stop();
        CompletableFuture<Void> embeddingFuture = embeddingServer.stop();

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

        return CompletableFuture.allOf(restFuture, detectFuture, embeddingFuture, selfShutdown);
    }
}
