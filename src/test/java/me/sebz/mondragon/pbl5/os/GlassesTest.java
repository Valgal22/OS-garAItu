package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Test;

class GlassesTest {

    @Test
    void testTakePhotoIncrementsGlobally() {
        Glasses g = new Glasses();

        int p1 = g.takePhoto();
        int p2 = g.takePhoto();

        assertEquals(p1 + 1, p2);
    }

    @Test
    void testSpeak() {
        Glasses g = new Glasses();
        String msg = g.speak("hello");

        assertTrue(msg.contains("hello"));
        assertTrue(msg.contains("Glasses"));
        assertTrue(msg.contains("says"));
    }
}
