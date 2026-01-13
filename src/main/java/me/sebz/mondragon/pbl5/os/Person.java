package me.sebz.mondragon.pbl5.os;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Person {
    private int id;
    private final AtomicReference<float[]> faceEmbedding = new AtomicReference<float[]>(null);
    private final AtomicReference<String> info = new AtomicReference<String>(null);
    private static int lastId = 0;
    private final static Lock staticMutex = new ReentrantLock();

    public Person() {
        staticMutex.lock();
        try {
            id = ++lastId;
        } finally {
            staticMutex.unlock();
        }
    }

    public void setFaceEmbedding(float[] embedding) {
        faceEmbedding.set(embedding == null ? null : embedding.clone());
    }

    public float[] getFaceEmbedding() {
        float[] e = faceEmbedding.get();
        return e == null ? null : e.clone();
    }


    public String getInfo() {
        return info.get();
    }

    public void setInfo(String info) {
        this.info.set(info);
    }

    public int getId() {
        return id;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o instanceof Person other) return other.id == this.id;
        return false;
    }

    @Override
    public int hashCode() {
        return Integer.hashCode(id);
    }

}
