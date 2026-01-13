package me.sebz.mondragon.pbl5.os;

import java.util.LinkedHashSet;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class Group {
    private final int id;
    private static final Lock staticMutex = new ReentrantLock();
    private static int lastId = 0;
    private final Lock mutex = new ReentrantLock();
    private final Set<Person> members = new LinkedHashSet<>();
    private final AtomicReference<String> passwordHash = new AtomicReference<String>(null);

    public Group() {
        staticMutex.lock();
        try {
            id = ++lastId;
        } finally {
            staticMutex.unlock();
        }
    }

    public Person[] getMembers() {
        mutex.lock();
        try {
            return members.toArray(new Person[0]);
        } finally {
            mutex.unlock();
        }
    }

    public void addMember(Person person) {
        mutex.lock();
        try {
            members.add(person);
        } finally {
            mutex.unlock();
        }
    }

    public void removeMember(Person person) {
        mutex.lock();
        try {
            members.remove(person);
        } finally {
            mutex.unlock();
        }
    }

    public void removeMember(int personId) {
        mutex.lock();
        try {
            members.removeIf(person -> personId == person.getId());
        } finally {
            mutex.unlock();
        }
    }

    public int getId() {
        return id;
    }

    public Person getMemberById(int personId) {
        mutex.lock();
        try {
            return members.stream()
                    .filter(person -> person.getId() == personId)
                    .findFirst()
                    .orElse(null);
        } finally {
            mutex.unlock();
        }
    }

    public void setPassword(String password) {
        passwordHash.set(BCrypt.hashpw(password, BCrypt.gensalt()));
    }

    public boolean checkPassword(String password) {
        String hash = passwordHash.get();
        return hash != null && BCrypt.checkpw(password, hash);
    }

    public Person getClosestMember(float[] embedding) {
        Person[] snapshot = getMembers(); // already locks
        Person best = null;
        float bestScore = -Float.MAX_VALUE;

        for (Person person : snapshot) {
            float[] fe = person.getFaceEmbedding();
            if (fe == null) continue;
            float score = cosineSimilarity(embedding, fe);
            if (score > bestScore) { bestScore = score; best = person; }
        }
        return best;
    }

    private float cosineSimilarity(float[] a, float[] b) {
        if (a == null || b == null) return -Float.MAX_VALUE;
        if (a.length != b.length) return -Float.MAX_VALUE;

        float dot = 0f, normA = 0f, normB = 0f;
        for (int i = 0; i < a.length; i++) {
            dot += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }

        if (normA == 0f || normB == 0f) return -Float.MAX_VALUE;
        return (float)(dot / (Math.sqrt(normA) * Math.sqrt(normB)));
    }

}