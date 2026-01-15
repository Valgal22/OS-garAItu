package me.sebz.mondragon.pbl5.os;
import java.util.Map;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.concurrent.CompletableFuture;

public class Phone {
    private final NodeRedServer server;
    private final CompletableFuture<Long> sessionIdFuture;
    private final Glasses glasses;

    public Phone(NodeRedServer server, Integer userId, String password, List<String> people) {
        this.server = server;
        sessionIdFuture = server.login(userId, password);
        glasses = new Glasses();
        sessionIdFuture.thenAcceptAsync(session -> {
            List<CompletableFuture<Map.Entry<Integer, String>>> futures =
                people.stream().map(string -> Main.retryUntilSuccess(() -> {
                    return server.addPerson(session, string, glasses.takePhoto());
                }, 10)
                .thenApply(futureInt -> Map.entry(futureInt, string))
                .exceptionally(ex -> null))
                .toList();

            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                .thenApply(ignored -> futures.stream()
                    .map(CompletableFuture::join)
                    .filter(Objects::nonNull)
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        });
    }

    public void pressButton() {
        sessionIdFuture.thenApply(sessionId -> server.identify(sessionId, glasses.takePhoto()));
    }
}
