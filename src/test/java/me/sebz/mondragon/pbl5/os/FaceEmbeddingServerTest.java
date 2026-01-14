package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;

class FaceEmbeddingServerTest {

    @Test
    void testInnerAnalyzeViaReflection() throws Exception {
        FaceEmbeddingServer s = new FaceEmbeddingServer();

        Method m = FaceEmbeddingServer.class
                .getDeclaredMethod("innerAnalyzePhoto", int.class);
        m.setAccessible(true);

        float[] emb = (float[]) m.invoke(s, 1);

        assertNotNull(emb);
        assertEquals(128, emb.length);
    }

    @Test
    void testAnalyzeAndStopWithTask() throws Exception {
        FaceEmbeddingServer s = new FaceEmbeddingServer();
        Thread t = new Thread(s);
        t.start();

        CompletableFuture<float[]> f = s.analyzePhoto(1);
        float[] emb = f.get();

        assertNotNull(emb);
        assertEquals(128, emb.length);

        s.stop();
        t.join();
    }

    @Test
    void testStopWithoutTasks() throws Exception {
        FaceEmbeddingServer s = new FaceEmbeddingServer();
        Thread t = new Thread(s);
        t.start();

        // no analyzePhoto() -> hilo bloqueado en take()
        s.stop();
        t.join();

        assertTrue(true); // cobertura explícita del camino POISON
    }
}
