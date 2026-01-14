package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.*;

class DatabaseTest {

    Database db;

    @BeforeEach
    void setup() {
        db = new Database();
    }

    @AfterEach
    void shutdown() {
        db.shutdown();
    }

    @Test
    void testAddAndGetGroup() {
        Group g = new Group();
        db.addGroup(g).join();

        Group[] groups = db.getGroups().join();
        assertEquals(1, groups.length);
    }

    @Test
    void testRemoveGroupById() {
        Group g = new Group();
        db.addGroup(g).join();

        db.removeGroup(g.getId()).join();
        assertEquals(0, db.getGroups().join().length);
    }

    @Test
    void testGetGroupById() {
        Group g = new Group();
        db.addGroup(g).join();

        assertEquals(g, db.getGroupById(g.getId()).join());
        assertNull(db.getGroupById(999).join());
    }

    @Test
    void testSignInAndSession() {
        Group g = new Group();
        g.setPassword("pw");
        db.addGroup(g).join();

        Long session = db.signIn(g.getId(), "pw").join();
        assertNotNull(session);

        assertEquals(g, db.getGroupFromSession(session).join());
    }

    @Test
    void testSignInFailsWithWrongPassword() {
        Group g = new Group();
        g.setPassword("pw");
        db.addGroup(g).join();

        Long session = db.signIn(g.getId(), "wrong").join();
        assertNull(session);
    }

    @Test
    void testSignOut() {
        Group g = new Group();
        g.setPassword("pw");
        db.addGroup(g).join();

        Long session = db.signIn(g.getId(), "pw").join();
        assertNotNull(session);

        assertNotNull(db.getGroupFromSession(session).join());

        db.signOut(session).join();

        assertNull(db.getGroupFromSession(session).join());
    }

    @Test
    void testSignOutEverywhere() {
        Group g = new Group();
        g.setPassword("pw");
        db.addGroup(g).join();

        Long session1 = db.signIn(g.getId(), "pw").join();
        Long session2 = db.signIn(g.getId(), "pw").join();

        assertNotNull(session1);
        assertNotNull(session2);

        assertEquals(g, db.getGroupFromSession(session1).join());
        assertEquals(g, db.getGroupFromSession(session2).join());

        db.signOutEverywhere(g.getId()).join();

        assertNull(db.getGroupFromSession(session1).join());
        assertNull(db.getGroupFromSession(session2).join());
    }

    @Test
    void testGetPersonById() {
        Group g = new Group();
        Person p = new Person();
        g.addMember(p);

        db.addGroup(g).join();

        assertEquals(p, db.getPersonById(p.getId()).join());
        assertNull(db.getPersonById(999).join());
    }
}
