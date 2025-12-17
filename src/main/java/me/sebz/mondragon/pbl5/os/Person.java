package me.sebz.mondragon.pbl5.os;

public class Person {
    private int id;
    private float[] faceEmbedding;
    private String info;
    private static int lastId = 0;

    public Person() {
        id = ++lastId;
        faceEmbedding = null;
    }

    public void setFaceEmbedding(float[] embedding) {
        this.faceEmbedding = embedding;
    }

    public float[] getFaceEmbedding() {
        return faceEmbedding;
    }

    public String getInfo() {
        return info;
    }

    public void setInfo(String info) {
        this.info = info;
    }

    public int getId() {
        return id;
    }
}
