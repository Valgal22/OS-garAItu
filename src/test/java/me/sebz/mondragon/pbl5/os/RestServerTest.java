package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import static org.easymock.EasyMock.*;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.Test;

class RestServerTest {

    // ---------- tests existentes (correctos) ----------

    @Test
    void testCreateGroupAndLogin() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        Group g = new Group();
        g.setPassword("pw");

        expect(dbMock.signIn(g.getId(), "pw"))
                .andReturn(CompletableFuture.completedFuture(5L));

        replay(dbMock);
        inject(rs, "database", dbMock);

        Long session = rs.newSession(g.getId(), "pw").join();
        assertEquals(5L, session);

        verify(dbMock);
    }

    @Test
    void testInvalidSessionThrowsIllegalArgumentExceptionWrapped() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        expect(dbMock.getGroupFromSession(99L))
                .andReturn(CompletableFuture.completedFuture(null));

        replay(dbMock);
        inject(rs, "database", dbMock);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> rs.getValidatedGroup(99L).join()
        );

        assertTrue(ex.getCause() instanceof IllegalArgumentException);

        verify(dbMock);
    }

    @Test
    void testCreateAndDeletePerson() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        Group g = new Group();

        expect(dbMock.getGroupFromSession(1L))
                .andReturn(CompletableFuture.completedFuture(g))
                .anyTimes();

        replay(dbMock);
        inject(rs, "database", dbMock);

        int id = rs.createPerson(1L, "info", new float[]{1f}).join();
        assertTrue(id > 0);

        rs.deletePerson(1L, id).join();

        verify(dbMock);
    }

    @Test
    void testEditPersonInvalidIdThrowsWrappedException() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        Group g = new Group();

        expect(dbMock.getGroupFromSession(1L))
                .andReturn(CompletableFuture.completedFuture(g));

        replay(dbMock);
        inject(rs, "database", dbMock);

        CompletionException ex = assertThrows(
                CompletionException.class,
                () -> rs.editPersonInfo(1L, 999, "x").join()
        );

        assertTrue(ex.getCause() instanceof IllegalArgumentException);

        verify(dbMock);
    }

    @Test
    void testIdentifyPerson() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        Group g = new Group();
        Person p = new Person();
        p.setInfo("Alice");
        p.setFaceEmbedding(new float[]{1f, 0f});
        g.addMember(p);

        expect(dbMock.getGroupFromSession(1L))
                .andReturn(CompletableFuture.completedFuture(g))
                .anyTimes();

        replay(dbMock);
        inject(rs, "database", dbMock);

        String info = rs.identifyPerson(1L, new float[]{1f, 0f}).join();
        assertEquals("Alice", info);

        verify(dbMock);
    }

    // ---------- NUEVOS TESTS PARA SUBIR COVERAGE ----------

    @Test
    void testCreateGroup() {
        RestServer rs = new RestServer();
        int id = rs.createGroup("pw").join();
        assertTrue(id > 0);
    }

    @Test
    void testDeleteGroupValidSession() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        Group g = new Group();

        expect(dbMock.getGroupFromSession(1L))
                .andReturn(CompletableFuture.completedFuture(g));
        expect(dbMock.removeGroup(g))
                .andReturn(CompletableFuture.completedFuture(null));

        replay(dbMock);
        inject(rs, "database", dbMock);

        rs.deleteGroup(1L).join();

        verify(dbMock);
    }

    @Test
    void testNewSessionFailure() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        expect(dbMock.signIn(1, "bad"))
                .andReturn(CompletableFuture.completedFuture(null));

        replay(dbMock);
        inject(rs, "database", dbMock);

        Long session = rs.newSession(1, "bad").join();
        assertNull(session);

        verify(dbMock);
    }

    @Test
    void testEditPersonEmbedding() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        Group g = new Group();
        Person p = new Person();
        g.addMember(p);

        expect(dbMock.getGroupFromSession(1L))
                .andReturn(CompletableFuture.completedFuture(g))
                .anyTimes();

        replay(dbMock);
        inject(rs, "database", dbMock);

        rs.editPersonEmbedding(1L, p.getId(), new float[]{1f}).join();

        assertNotNull(p.getFaceEmbedding());
        verify(dbMock);
    }

    @Test
    void testIdentifyPersonNoMatch() throws Exception {
        RestServer rs = new RestServer();
        Database dbMock = mock(Database.class);

        Group g = new Group(); // sin embeddings

        expect(dbMock.getGroupFromSession(1L))
                .andReturn(CompletableFuture.completedFuture(g))
                .anyTimes();

        replay(dbMock);
        inject(rs, "database", dbMock);

        assertNull(rs.identifyPerson(1L, new float[]{1f, 0f}).join());

        verify(dbMock);
    }

    // ---------- helper ----------

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
