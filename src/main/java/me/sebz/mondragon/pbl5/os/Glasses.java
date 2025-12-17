package me.sebz.mondragon.pbl5.os;

public class Glasses {

    private int id;
    private static int lastPhotoId = 0;
    private static int lastId = 0;

    public Glasses() {
        id = ++lastId;
    }

    public int takePhoto() {
        int photoId = ++lastPhotoId;
        System.out.println("Glasses " + id + " took photo " + photoId);
        return photoId;
    }

    public String speak(String message) {
        return "Glasses " + id + " says: " + message;
    }
}
