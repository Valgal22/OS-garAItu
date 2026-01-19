package me.sebz.mondragon.pbl5.os;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.security.SecureRandom;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Base64;
import java.util.List;
import java.util.Random;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

public class Main implements Runnable {

    private final NodeRedServer nodeRedServer;
    private final List<Phone> phones;
    private final PhoneManager phoneManager;
    private static final Random random = new SecureRandom();
    private final Lock mutex = new ReentrantLock();
    private final String ip;
    private final int port;

    public Main() {
        this("0.0.0.0", 8888);
    }

    public Main(String ip, int port) {
        this.ip = ip;
        this.port = port;
        System.out.println("Starting servers...");
        nodeRedServer = new NodeRedServer();
        phones = new ArrayList<>();
        phoneManager = new PhoneManager(phones, mutex, nodeRedServer);
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

        Thread manager = new Thread(phoneManager);
        manager.setDaemon(true);
        manager.start();

        Thread tcpServer = new Thread(this::startTcpServer);
        tcpServer.start();
    }

    private void startTcpServer() {
        try (ServerSocket serverSocket = new ServerSocket(port, 50, InetAddress.getByName(ip))) {
            System.out.println("TCP Server listening on " + ip + ":" + port);
            while (!Thread.currentThread().isInterrupted()) {
                try {
                    Socket clientSocket = serverSocket.accept();
                    // Handle client in a new thread to avoid blocking the accept loop
                    new Thread(() -> handleClient(clientSocket)).start();
                } catch (IOException e) {
                    System.err.println("Accept failed: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            System.err.println("Could not listen on " + ip + ":" + port);
            e.printStackTrace();
        }
    }

    private void handleClient(Socket clientSocket) {
        try (clientSocket;
             PrintWriter out = new PrintWriter(clientSocket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(clientSocket.getInputStream()))) {

            String inputLine;
            while ((inputLine = in.readLine()) != null) {
                String[] parts = inputLine.trim().split("\\s+");
                String command = parts[0].toUpperCase();

                if ("ADD_PHONE".equals(command)) {
                    int amount = 1;
                    if (parts.length > 1) {
                        try {
                            amount = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            // ignore, use default
                        }
                    }
                    System.out.println("Received ADD_PHONE command via TCP (amount: " + amount + ")");
                    phoneManager.addPhones(amount);
                    out.println("PHONE(S)_ADDED");
                } else if ("REMOVE_PHONE".equals(command)) {
                    int amount = 1;
                    if (parts.length > 1) {
                        try {
                            amount = Integer.parseInt(parts[1]);
                        } catch (NumberFormatException e) {
                            // ignore
                        }
                    }
                    System.out.println("Received REMOVE_PHONE command via TCP (amount: " + amount + ")");
                    phoneManager.removePhones(amount);
                    out.println("PHONE(S)_REMOVED");
                } else if ("GET_PHONE_COUNT".equals(command)) {
                    System.out.println("Received GET_PHONE_COUNT command via TCP");
                    out.println(phoneManager.getPhoneCount());
                } else {
                    out.println("UNKNOWN_COMMAND");
                }
            }
        } catch (IOException e) {
            System.err.println("Error handling client: " + e.getMessage());
        }
    }

    public static void main(String[] args) {
        String ip = "0.0.0.0";
        int port = 8888;
        if (args.length >= 2) {
            ip = args[0];
            try {
                port = Integer.parseInt(args[1]);
            } catch (NumberFormatException e) {
                System.err.println("Invalid port number. Using default 8888.");
            }
        }
        new Main(ip, port).run();
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
