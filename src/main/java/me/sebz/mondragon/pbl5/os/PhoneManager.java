package me.sebz.mondragon.pbl5.os;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.security.SecureRandom;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

public class PhoneManager implements Runnable {

    private final List<Phone> phones;
    private final Lock mutex;
    private final NodeRedServer nodeRedServer;
    private final Random random = new SecureRandom();

    public PhoneManager(List<Phone> phones, Lock mutex, NodeRedServer nodeRedServer) {
        this.phones = phones;
        this.mutex = mutex;
        this.nodeRedServer = nodeRedServer;
    }

    public void addPhones(int amount) {
        for (int i = 0; i < amount; i++) {
            String password = Main.generatePassword(20);
            CompletableFuture<Integer> userIdFuture = nodeRedServer.signup(password);
            List<String> people = Stream.generate(Main::generateRandomName).limit(random.nextInt(1, 4)).toList();
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
    }

    public void removePhones(int amount) {
        mutex.lock();
        try {
            int toRemove = Math.min(amount, phones.size());
            for (int i = 0; i < toRemove; i++) {
                phones.remove(phones.size() - 1);
            }
        } finally {
            mutex.unlock();
        }
    }

    public int getPhoneCount() {
        mutex.lock();
        try {
            return phones.size();
        } finally {
            mutex.unlock();
        }
    }

    @Override
    public void run() {
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
                                Thread.sleep(random.nextInt(5000, 5500));
                            } catch (InterruptedException e) {
                                Thread.currentThread().interrupt();
                            }
                        }
                    });
                    thread.setDaemon(true);
                    thread.start();
                    phoneThreads.add(thread);
                }
                while (phoneThreads.size() > phones.size()) {
                    System.out.println("Phone removed, stopping thread...");
                    Thread thread = phoneThreads.remove(phoneThreads.size() - 1);
                    thread.interrupt();
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
    }
}
