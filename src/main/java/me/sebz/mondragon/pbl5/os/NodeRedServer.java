package me.sebz.mondragon.pbl5.os;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class NodeRedServer {

    private ExecutorService executor = Executors.newSingleThreadExecutor();
    private FaceDetectServer detectServer = new FaceDetectServer();
    private FaceEmbeddingServer embeddingServer = new FaceEmbeddingServer();
    private RestServer restServer = new RestServer();

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
        return CompletableFuture.completedFuture(null).thenRunAsync(() -> {
            restServer.getValidatedGroup(sessionId);
        }, executor).thenRunAsync(() -> {
            detectServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(ignored -> {
            return embeddingServer.analyzePhoto(photoId);
        }, executor).thenComposeAsync(embedding -> {
            return restServer.createPerson(sessionId, info, embedding);
        }, executor);
    }

    public CompletableFuture<Void> deletePerson(Long sessionId, int personId) {
        return CompletableFuture.completedFuture(null).thenRunAsync(() -> {
            restServer.deletePerson(sessionId, personId);
        }, executor);
    }
}
