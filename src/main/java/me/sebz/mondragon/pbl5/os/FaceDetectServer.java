package me.sebz.mondragon.pbl5.os;

import java.security.SecureRandom;
import java.util.Random;
import java.util.ArrayDeque;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class FaceDetectServer implements Runnable {

    private class Task {
        int id;
        CompletableFuture<Boolean> future;

        Task(int id) {
            this.id = id;
            this.future = new CompletableFuture<>();
        }
    }

    private static Random random = new SecureRandom();
    private volatile boolean running = true;
    private final Lock mutex = new ReentrantLock();
    private final Condition hasTasks = mutex.newCondition();
    private final Queue<Task> queue = new ArrayDeque<>();
    private final CompletableFuture<Void> terminationFuture = new CompletableFuture<>();

    public void run() {
        try {
            while (running) {
                Task task;

                mutex.lock();
                try {
                    while (queue.isEmpty() && running) {
                        hasTasks.await();
                    }
                    if (!running) break;

                    task = queue.poll();
                } catch (InterruptedException e) {
                    running = false;
                    Thread.currentThread().interrupt();
                    continue;
                } finally {
                    mutex.unlock();
                }

                if (task == null) continue;

                // Execute outside the lock
                try {
                    boolean result = innerAnalyzePhoto(task.id);
                    task.future.complete(result);
                } catch (Exception e) {
                    task.future.completeExceptionally(e);
                }
            }
            terminationFuture.complete(null);
        } catch (Exception e) {
            terminationFuture.completeExceptionally(e);
        }
    }

    public CompletableFuture<Void> stop() {
        running = false;
        mutex.lock();
        try {
            hasTasks.signalAll();
        } finally {
            mutex.unlock();
        }
        return terminationFuture;
    }

    public CompletableFuture<Boolean> analyzePhoto(int photoId) {
        Task task = new Task(photoId);

        mutex.lock();
        try {
            queue.add(task);
            hasTasks.signal();
        } finally {
            mutex.unlock();
        }

        return task.future;
    }

    private boolean innerAnalyzePhoto(int photoId) {
        boolean faceDetected = random.nextBoolean();
        StringBuilder analysis = new StringBuilder();
        System.out.println("FaceDetectServer started analyzing photo " + photoId);
        try {
            Thread.sleep(random.nextInt(100, 200));
        } catch (InterruptedException e) {
            // Auto-generated catch block
            e.printStackTrace();
        } // Simulate processing time
        analysis.append("Analysis result: ");
        if (faceDetected) {
            analysis.append("Face detected in photo ");
        } else {
            analysis.append("No face detected in photo ");
        }
        analysis.append(photoId).append(".");
        System.out.println("FaceDetectServer analyzed photo " + photoId);
        System.out.println(analysis.toString());
        if (!faceDetected) {
            throw new IllegalArgumentException("No Face");
        }
        return faceDetected;
    }
}
