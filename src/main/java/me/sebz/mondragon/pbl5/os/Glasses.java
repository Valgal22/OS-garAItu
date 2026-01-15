package me.sebz.mondragon.pbl5.os;

import java.util.concurrent.atomic.AtomicInteger;

public class Glasses {

    private int id;
    private static AtomicInteger lastPhotoId = new AtomicInteger(0);
    private static AtomicInteger lastId = new AtomicInteger(0);

    public Glasses() {
        id = lastId.incrementAndGet();
    }

    public int takePhoto() {
        int photoId = lastPhotoId.incrementAndGet();
        System.out.println("Glasses " + id + " took photo " + photoId);
        return photoId;
    }

    public String speak(String message) {
        return "Glasses " + id + " says: " + message;
    }
}
