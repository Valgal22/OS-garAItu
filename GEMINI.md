# Project Context: OS-GARAITU

## Overview
This project (`me.sebz.mondragon.pbl5.os`) appears to be a backend system for a distributed application involving mobile devices ("Phones"), face detection services, and NodeRed integration. It serves as a simulation or prototype, likely for an Operating Systems or Concurrency course (PBL5).

The system mimics a microservices or multi-server architecture using Java threads and concurrent primitives rather than separate processes or network services.

## Architecture

The application is composed of several "Server" and "Manager" components running within a single JVM, orchestrated by `Main.java`.

### Key Components
*   **Main (`Main.java`):** The entry point. It initializes the `NodeRedServer` and `PhoneManager`, and starts a raw TCP server (default port 8888) to listen for commands like `ADD_PHONE`.
*   **NodeRedServer (`NodeRedServer.java`):** A facade that acts as the primary controller for business logic. It orchestrates sub-services:
    *   **FaceDetectServer (`FaceDetectServer.java`):** Simulates face detection analysis (threaded, with simulated delays and random results).
    *   **FaceEmbeddingServer (`FaceEmbeddingServer.java`):** Simulates face embedding generation.
    *   **RestServer (`RestServer.java`):** Provides high-level administrative operations (Create Group, Add Person, Identify) and manages the data layer.
*   **PhoneManager (`PhoneManager.java`):** Manages connected "Phone" entities.
*   **Database (`Database.java`):** An **in-memory** data store using thread-safe structures (`LinkedHashSet`, `HashMap`) to store `Group`, `Person`, and `Session` data. **Data is not persistent.**

### Concurrency Model
The project heavily emphasizes concurrency control and asynchronous programming:
*   **`CompletableFuture`**: Used extensively for asynchronous request handling and chaining operations.
*   **`ExecutorService`**: Dedicated executors for different components (e.g., database serialization).
*   **Synchronization**: Uses `ReentrantLock`, `Condition`, and `volatile` flags to manage shared state and simulated task queues.

## Building and Running

### Prerequisites
*   Java 17 (defined in `pom.xml`)
*   Maven

### Build Commands
*   **Compile:** `mvn compile`
*   **Test:** `mvn test`
*   **Package:** `mvn package` (This will likely generate a jar file in `target/`)

### Running the Application
The `exec-maven-plugin` is configured to run the `Main` class.
*   **Run:** `mvn exec:java`
*   **Run (Jar):** `java -cp target/OS-GARAITU-1.0-SNAPSHOT.jar me.sebz.mondragon.pbl5.os.Main [IP] [PORT]`
    *   Default IP: `0.0.0.0`
    *   Default Port: `8888`

## Development Conventions

*   **Style:** Standard Java naming conventions.
*   **Testing:**
    *   Frameworks: JUnit 5 (`junit-jupiter`), Hamcrest, EasyMock.
    *   Coverage: Jacoco plugin is configured for coverage reports (target `target/site/jacoco`).
*   **Dependencies:** Minimal external dependencies. `jbcrypt` is used for hashing, but core networking and logic use standard JDK libraries.
