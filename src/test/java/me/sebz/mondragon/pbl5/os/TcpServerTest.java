package me.sebz.mondragon.pbl5.os;

import static org.junit.jupiter.api.Assertions.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class TcpServerTest {

    private final int port = 9999;
    private final String ip = "127.0.0.1";

    @BeforeEach
    void setUp() throws InterruptedException {
        // Start Main in a separate thread
        new Thread(() -> {
            Main.main(new String[]{ip, String.valueOf(port)});
        }).start();
        // Give it a moment to start the server
        TimeUnit.SECONDS.sleep(1);
    }

    @Test
    void testTcpCommands() {
        try (Socket socket = new Socket(ip, port);
             PrintWriter out = new PrintWriter(socket.getOutputStream(), true);
             BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()))) {

            // 0. Test initial count
            out.println("GET_PHONE_COUNT");
            String response = in.readLine();
            assertEquals("0", response);

            // 1. Test ADD_PHONE (default)
            out.println("ADD_PHONE");
            response = in.readLine();
            assertEquals("PHONE(S)_ADDED", response);

            // 2. Test ADD_PHONE with parameter
            out.println("ADD_PHONE 5");
            response = in.readLine();
            assertEquals("PHONE(S)_ADDED", response);

            // Wait for async adds to complete
            try {
                TimeUnit.SECONDS.sleep(2);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }

            // 3. Test count after adds (1 + 5 = 6)
            out.println("GET_PHONE_COUNT");
            response = in.readLine();
            assertEquals("6", response);

            // 4. Test REMOVE_PHONE (default)
            out.println("REMOVE_PHONE");
            response = in.readLine();
            assertEquals("PHONE(S)_REMOVED", response);

            // 5. Test REMOVE_PHONE with parameter
            out.println("REMOVE_PHONE 3");
            response = in.readLine();
            assertEquals("PHONE(S)_REMOVED", response);

            // 6. Test count after removes (6 - 1 - 3 = 2)
            out.println("GET_PHONE_COUNT");
            response = in.readLine();
            assertEquals("2", response);

            // 7. Send invalid command
            out.println("INVALID_CMD");
            response = in.readLine();
            assertEquals("UNKNOWN_COMMAND", response);

        } catch (IOException e) {
            fail("Should have connected to the TCP server: " + e.getMessage());
        }
    }
}
