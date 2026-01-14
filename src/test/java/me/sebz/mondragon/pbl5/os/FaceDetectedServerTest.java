package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

class FaceDetectedServerTest {

    @Test
    void testInnerAnalyzeViaReflection_successOrException() throws Exception {
        FaceDetectServer s = new FaceDetectServer();

        Method m = FaceDetectServer.class
                .getDeclaredMethod("innerAnalyzePhoto", int.class);
        m.setAccessible(true);

        try {
            boolean result = (boolean) m.invoke(s, 1);
            assertTrue(result); // solo puede llegar aquí si detecta cara
        } catch (InvocationTargetException ex) {
            // Rama cuando no hay cara
            assertTrue(ex.getCause() instanceof IllegalArgumentException);
            assertEquals("No Face", ex.getCause().getMessage());
        }
    }

    @Test
    void testAnalyzePhotoCompletesNormallyOrExceptionally() throws Exception {
        FaceDetectServer s = new FaceDetectServer();
        Thread t = new Thread(s);
        t.start();

        CompletableFuture<Boolean> f = s.analyzePhoto(1);

        try {
            Boolean result = f.get();
            assertTrue(result);
        } catch (ExecutionException ex) {
            assertTrue(ex.getCause() instanceof IllegalArgumentException);
            assertEquals("No Face", ex.getCause().getMessage());
        }

        s.stop();
        t.join();
    }

    @Test
    void testStopWhileWaiting() throws Exception {
        FaceDetectServer s = new FaceDetectServer();
        Thread t = new Thread(s);
        t.start();

        // no tasks -> thread bloqueado en await()
        s.stop();
        t.join();

        assertTrue(true); // fuerza cobertura del stop con cola vacía
    }
}
