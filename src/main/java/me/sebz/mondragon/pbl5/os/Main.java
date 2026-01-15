package me.sebz.mondragon.pbl5.os;
import java.util.concurrent.CompletableFuture;
import java.util.Random;
import java.security.SecureRandom;
import java.util.List;
import java.util.ArrayList;
import java.util.function.Supplier;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.locks.ReentrantLock;
import java.util.concurrent.locks.Lock;
import java.time.Duration;
import java.util.stream.Stream;
import java.util.Base64;
import java.util.concurrent.TimeUnit;


public class Main implements Runnable {

    private final NodeRedServer nodeRedServer;
    private final List<Phone> phones;
    private static final Random random = new SecureRandom();
    private final Lock mutex = new ReentrantLock();

    public Main() {
        System.out.println("Starting servers...");
        nodeRedServer = new NodeRedServer();
        phones = new ArrayList<>();
        for (int i = 0; i < 3; ++i) {
            addPhone();
        }
    }

    public void addPhone() {
        String password = generatePassword(20);
        CompletableFuture<Integer> userIdFuture = nodeRedServer.signup(password);
        List<String> people = Stream.generate(Main::generateRandomName).limit(random.nextInt(1,4)).toList();
        userIdFuture.thenAccept(userId -> {
            Phone phone = new Phone(nodeRedServer, userId, password, people);
            mutex.lock();
            try {
                phones.add(phone);
            } finally {
                mutex.unlock();
            }

        });
    }

    public static String generatePassword(int length) {
        byte[] randomBytes = new byte[length];
        random.nextBytes(randomBytes);
        return Base64.getEncoder().withoutPadding().encodeToString(randomBytes)
                     .substring(0, length);
    }

    public void run() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println("Shutting down servers...");
            nodeRedServer.shutdown().join();
            System.out.println("Shutdown complete.");
        }));

        Thread manager = new Thread(() -> {
            List<Thread> phoneThreads = new ArrayList<>();
            while (!Thread.currentThread().isInterrupted()) {
                mutex.lock();
                try {
                    while (phoneThreads.size() < phones.size()) {
                        System.out.println("New phone detected, starting thread...");
                        Phone phone = phones.get(phoneThreads.size());
                        Thread thread = new Thread(() -> {
                            while (!Thread.currentThread().isInterrupted()) {
                                phone.pressButton();
                                try {
                                    Thread.sleep(random.nextInt(4000, 8000));
                                } catch (InterruptedException e) {
                                    Thread.currentThread().interrupt();
                                }
                            }
                        });
                        thread.setDaemon(true);
                        thread.start();
                        phoneThreads.add(thread);
                    }
                } finally {
                    mutex.unlock();
                }
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    phoneThreads.forEach(Thread::interrupt);
                    Thread.currentThread().interrupt();
                }
            }
        });
        manager.setDaemon(true);
        manager.start();
    }

    public static void main(String[] args) {
        new Main().run();
    }

    public static String generateRandomName() {
        String vowels = "aeiou";
        String consonants = "bcdfghjklmnpqrstvwxyz";
        StringBuilder name = new StringBuilder();
        int length = random.nextInt(6,11);
        for (int i = 0; i < length; i++) {
            String pool = (i % 2 == 0) ? consonants : vowels;
            char c = pool.charAt(random.nextInt(pool.length()));
            name.append((i == 0) ? Character.toUpperCase(c) : c);
        }
        return name.toString();
    }


    public static <T> CompletableFuture<T> retryUntilSuccess(
            Supplier<CompletableFuture<T>> supplier) {
        return supplier.get().exceptionallyComposeAsync(ex -> retryUntilSuccess(supplier));
    }

    public static <T> CompletableFuture<T> retryUntilSuccess(
            Supplier<CompletableFuture<T>> supplier,
            int maxRetries) {
        return retryUntilSuccess(supplier, ForkJoinPool.commonPool(), maxRetries, Duration.ofMillis(50));
    }

    private static <T> CompletableFuture<T> retryUntilSuccess(
            Supplier<CompletableFuture<T>> supplier,
            Executor executor,
            int maxRetries,
            Duration initialBackoff) {
        return retryUntilSuccess(supplier, executor, maxRetries, initialBackoff, 0);
    }

    private static <T> CompletableFuture<T> retryUntilSuccess(
            Supplier<CompletableFuture<T>> supplier,
            Executor executor,
            int maxRetries,
            Duration initialBackoff,
            int attempt) {

        return supplier.get().exceptionallyComposeAsync(ex -> {
            System.out.println("Attempt " + (attempt + 1) + " failed: " + ex.getMessage());

            if (attempt >= maxRetries) {
                System.out.println("Max retries (" + maxRetries + ") exceeded. Giving up.");
                return CompletableFuture.failedFuture(ex);
            }

            long delayMs = initialBackoff.toMillis() * (1L << attempt); // exponential: 2^attempt
            System.out.println("Retrying in " + delayMs + " ms...");

            Executor delayedExecutor = CompletableFuture.delayedExecutor(delayMs, TimeUnit.MILLISECONDS, executor);

            return CompletableFuture.supplyAsync(() -> null, delayedExecutor)
                    .thenComposeAsync(
                        ignored -> retryUntilSuccess(supplier, executor, maxRetries, initialBackoff, attempt + 1),
                        executor
                    );
        }, executor);
    }

}
