package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

class GroupTest {

    @Test
    void testAddAndRemoveMembersByObject() {
        Group g = new Group();
        Person p = new Person();

        g.addMember(p);
        assertEquals(1, g.getMembers().length);

        g.removeMember(p);
        assertEquals(0, g.getMembers().length);
    }

    @Test
    void testRemoveMemberById() {
        Group g = new Group();
        Person p = new Person();
        g.addMember(p);

        assertNotNull(g.getMemberById(p.getId()));

        g.removeMember(p.getId());
        assertNull(g.getMemberById(p.getId()));
    }

    @Test
    void testGetMemberByIdNotFound() {
        Group g = new Group();
        assertNull(g.getMemberById(999));
    }

    @Test
    void testGetMembersSnapshot() {
        Group g = new Group();
        Person p1 = new Person();
        Person p2 = new Person();

        g.addMember(p1);
        g.addMember(p2);

        Person[] members = g.getMembers();
        assertEquals(2, members.length);
    }

    @Test
    void testPasswordLifecycle() {
        Group g = new Group();

        // hash == null
        assertFalse(g.checkPassword("anything"));

        g.setPassword("secret");
        assertTrue(g.checkPassword("secret"));
        assertFalse(g.checkPassword("wrong"));
    }

    @Test
    void testClosestMemberNormalCase() {
        Group g = new Group();

        Person p1 = new Person();
        Person p2 = new Person();

        p1.setFaceEmbedding(new float[]{1f, 0f});
        p2.setFaceEmbedding(new float[]{0f, 1f});

        g.addMember(p1);
        g.addMember(p2);

        Person closest = g.getClosestMember(new float[]{1f, 0f});
        assertEquals(p1, closest);
    }

    @Test
    void testClosestMemberSkipsNullEmbeddings() {
        Group g = new Group();
        Person p = new Person();
        g.addMember(p);

        assertNull(g.getClosestMember(new float[]{1f, 0f}));
    }

    @Test
    void testClosestMemberWithDifferentVectorLengths() {
        Group g = new Group();

        Person p = new Person();
        p.setFaceEmbedding(new float[]{1f, 2f, 3f});
        g.addMember(p);

        assertNull(g.getClosestMember(new float[]{1f, 2f}));
    }

    @Test
    void testClosestMemberWithZeroNormVector() {
        Group g = new Group();

        Person p = new Person();
        p.setFaceEmbedding(new float[]{0f, 0f});
        g.addMember(p);

        assertNull(g.getClosestMember(new float[]{1f, 1f}));
    }

    @Test
    void testCosineSimilarityViaReflectionAllBranches() throws Exception {
        Group g = new Group();

        Method m = Group.class.getDeclaredMethod(
                "cosineSimilarity", float[].class, float[].class);
        m.setAccessible(true);

        // null vectors
        float r1 = (float) m.invoke(g, null, new float[]{1});
        float r2 = (float) m.invoke(g, new float[]{1}, null);
        assertEquals(-Float.MAX_VALUE, r1);
        assertEquals(-Float.MAX_VALUE, r2);

        // different length
        float r3 = (float) m.invoke(g, new float[]{1}, new float[]{1, 2});
        assertEquals(-Float.MAX_VALUE, r3);

        // zero norm
        float r4 = (float) m.invoke(g, new float[]{0, 0}, new float[]{1, 1});
        assertEquals(-Float.MAX_VALUE, r4);

        // valid vectors
        float r5 = (float) m.invoke(g, new float[]{1, 0}, new float[]{1, 0});
        assertEquals(1.0f, r5, 0.0001);
    }
}
