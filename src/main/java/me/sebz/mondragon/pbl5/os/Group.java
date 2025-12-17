package me.sebz.mondragon.pbl5.os;

import java.util.LinkedHashSet;
import org.mindrot.jbcrypt.BCrypt;
import java.util.Set;

public class Group {
    private int id;
    private static int lastId = 0;
    private Set<Person> members;
    private String passwordHash;

    public Group() {
        id = ++lastId;
        members = new LinkedHashSet<>();
    }

    public Person[] getMembers() {
        return members.toArray(new Person[0]);
    }

    public void addMember(Person person) {
        members.add(person);
    }

    public void removeMember(Person person) {
        members.remove(person);
    }

    public void removeMember(int personId) {
        members.removeIf(person -> personId == person.getId());
    }

    public int getId() {
        return id;
    }

    public Person getMemberById(int personId) {
        return members.stream()
                .filter(person -> person.getId() == personId)
                .findFirst()
                .orElse(null);
    }

    public void setPassword(String password) {
        passwordHash = BCrypt.hashpw(password, BCrypt.gensalt());
    }

    public boolean checkPassword(String password) {
        if (passwordHash == null) {
            return false;
        }
        return BCrypt.checkpw(password, passwordHash);
    }

    public Person getClosestMember(float[] embedding) {
        return members.stream()
                .filter(person -> person.getFaceEmbedding() != null)
                .max((p1, p2) -> {
                    float sim1 = cosineSimilarity(embedding, p1.getFaceEmbedding());
                    float sim2 = cosineSimilarity(embedding, p2.getFaceEmbedding());
                    return Float.compare(sim1, sim2);
                })
                .orElse(null);
    }

    private float cosineSimilarity(float[] a, float[] b) {
        float dotProduct = 0.0f;
        float normA = 0.0f;
        float normB = 0.0f;
        for (int i = 0; i < a.length; i++) {
            dotProduct += a[i] * b[i];
            normA += a[i] * a[i];
            normB += b[i] * b[i];
        }
        return dotProduct / ((float) Math.sqrt(normA) * (float) Math.sqrt(normB));
    }
}