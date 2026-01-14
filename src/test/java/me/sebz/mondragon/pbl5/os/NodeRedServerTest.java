package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import static org.easymock.EasyMock.*;

import java.lang.reflect.Field;
import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

class NodeRedServerTest {

    @Test
    void testSignupAndLogin() throws Exception {
        NodeRedServer server = new NodeRedServer();

        RestServer restMock = mock(RestServer.class);

        expect(restMock.createGroup("pw"))
                .andReturn(CompletableFuture.completedFuture(1));
        expect(restMock.newSession(1, "pw"))
                .andReturn(CompletableFuture.completedFuture(10L));

        replay(restMock);

        inject(server, "restServer", restMock);

        assertEquals(1, server.signup("pw").join());
        assertEquals(10L, server.login(1, "pw").join());

        verify(restMock);
    }

    @Test
    void testAddPersonFlow() throws Exception {
        NodeRedServer server = new NodeRedServer();

        RestServer restMock = mock(RestServer.class);
        FaceDetectServer detectMock = mock(FaceDetectServer.class);
        FaceEmbeddingServer embedMock = mock(FaceEmbeddingServer.class);

        // fire-and-forget, result ignored
        expect(restMock.getValidatedGroup(1L))
                .andReturn(CompletableFuture.completedFuture(new Group()));

        expect(detectMock.analyzePhoto(5))
                .andReturn(CompletableFuture.completedFuture(true));

        expect(embedMock.analyzePhoto(5))
                .andReturn(CompletableFuture.completedFuture(new float[]{1f, 0f}));

        expect(restMock.createPerson(eq(1L), eq("info"), aryEq(new float[]{1f, 0f})))
                .andReturn(CompletableFuture.completedFuture(7));

        replay(restMock, detectMock, embedMock);

        inject(server, "restServer", restMock);
        inject(server, "detectServer", detectMock);
        inject(server, "embeddingServer", embedMock);

        int id = server.addPerson(1L, "info", 5).join();
        assertEquals(7, id);

        verify(restMock, detectMock, embedMock);
    }

    @Test
    void testDeletePerson() throws Exception {
        NodeRedServer server = new NodeRedServer();

        RestServer restMock = mock(RestServer.class);

        expect(restMock.deletePerson(1L, 2))
                .andReturn(CompletableFuture.completedFuture(null));

        replay(restMock);

        inject(server, "restServer", restMock);

        server.deletePerson(1L, 2).join();

        verify(restMock);
    }

    private static void inject(Object target, String field, Object value) throws Exception {
        Field f = target.getClass().getDeclaredField(field);
        f.setAccessible(true);
        f.set(target, value);
    }
}
