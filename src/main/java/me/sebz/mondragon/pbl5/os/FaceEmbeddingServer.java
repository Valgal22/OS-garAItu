package me.sebz.mondragon.pbl5.os;

import java.security.SecureRandom;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class FaceEmbeddingServer implements Runnable {

	private class Task {
		int id;
		CompletableFuture<float[]> future;
		final boolean poison;

		Task(int id) {
			this(id, false);
		}

		Task(int id, boolean poison) {
			this.id = id;
			this.poison = poison;
			this.future = poison ? null : new CompletableFuture<>();
		}
	}

	private static Random random = new SecureRandom();
	private final BlockingQueue<Task> queue = new LinkedBlockingQueue<>();
	private final Task POISON = new Task(-1, true);

	public void run() {
		try {
			while (true) {
				Task task = queue.take();
				if (task.poison)
					break;

				try {
					float[] embedding = innerAnalyzePhoto(task.id);
					task.future.complete(embedding);
				} catch (Exception e) {
					task.future.completeExceptionally(e);
				}
			}
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public void stop() {
		try {
			queue.put(POISON);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
	}

	public CompletableFuture<float[]> analyzePhoto(int photoId) {
		Task task = new Task(photoId);
		try {
			queue.put(task);
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
			CompletableFuture<float[]> f = new CompletableFuture<>();
			f.completeExceptionally(e);
			return f;
		}
		return task.future;
	}

	private float[] innerAnalyzePhoto(int photoId) {
		float[] embedding = new float[128]; // Simulate a 128-dimensional embedding
        System.out.println("FaceEmbeddingServer started analyzing photo " + photoId);
		try {
			Thread.sleep(random.nextInt(500, 1500));
		} catch (InterruptedException e) {
			Thread.currentThread().interrupt();
		}
		for (int i = 0; i < 128; i++) {
			embedding[i] = random.nextFloat();
		}
		System.out.println("FaceEmbeddingServer analyzed photo " + photoId);
		return embedding;
	}
}
