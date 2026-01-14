package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class PersonTest {

    @Test
    void testConstructorAndGetId() {
        Person p1 = new Person();
        Person p2 = new Person();

        assertNotEquals(p1.getId(), p2.getId());
    }

    @Test
    void testFaceEmbeddingIsDefensiveCopy() {
        Person p = new Person();
        assertNull(p.getFaceEmbedding());

        float[] emb = new float[]{1f, 0f};
        p.setFaceEmbedding(emb);

        float[] fromPerson = p.getFaceEmbedding();
        assertArrayEquals(emb, fromPerson);

        // modificar array original NO debe afectar
        emb[0] = 99f;
        assertNotEquals(99f, p.getFaceEmbedding()[0]);

        // modificar el devuelto tampoco
        fromPerson[1] = 99f;
        assertNotEquals(99f, p.getFaceEmbedding()[1]);
    }

    @Test
    void testInfo() {
        Person p = new Person();
        p.setInfo("info");

        assertEquals("info", p.getInfo());
    }

    @Test
    void testEqualsAndHashCode() {
        Person p1 = new Person();
        Person p2 = new Person();

        assertNotEquals(p1, p2);
        assertNotEquals(p1.hashCode(), p2.hashCode());

        // mismo objeto
        assertEquals(p1, p1);
        assertEquals(p1.hashCode(), p1.hashCode());
    }

    @Test
    void testEqualsWithNonPerson() {
        Person p = new Person();
        assertNotEquals(p, "not a person");
        assertNotEquals(p, null);
    }
}
