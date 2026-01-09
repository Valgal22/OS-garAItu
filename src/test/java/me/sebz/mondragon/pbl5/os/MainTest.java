package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import org.junit.jupiter.api.Test;

public class MainTest {

    @Test
    void testMainDoesNotThrowException() {
        assertDoesNotThrow(() -> {
            Main.main(new String[]{});
        });
    }
}
