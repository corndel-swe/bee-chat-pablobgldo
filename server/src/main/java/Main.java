import com.fasterxml.jackson.databind.ObjectMapper;
import io.javalin.Javalin;
import io.javalin.websocket.WsContext;

import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

public class Main {

    private static final List<WsContext> allUsers = new ArrayList<>();

    public static void main(String[] args) {

        Javalin app = Javalin.create(javalinConfig -> {
            // Set WebSocket idle timeout to 120 seconds
            javalinConfig.jetty.modifyWebSocketServletFactory(jettyWebSocketServletFactory ->
                    jettyWebSocketServletFactory.setIdleTimeout(Duration.ofSeconds(120))
            );
        });

        app.ws("/", wsConfig -> {

            wsConfig.onConnect(connectContext -> {
                System.out.println("Connected: " + connectContext.sessionId());
                allUsers.add(connectContext);
            });

            wsConfig.onMessage(messageContext -> {
                String json = messageContext.message();
                ObjectMapper objectMapper = new ObjectMapper();
                Map<String, String> message = objectMapper.readValue(json, Map.class);
                String recipient = message.get("recipientId");

                if (recipient.isEmpty()) {
                    for (WsContext user : allUsers) {
                        if (!user.equals(messageContext)) {
                            user.send(message);
                        }
                    }
                } else {
                    for (WsContext user : allUsers) {
                        if (user.sessionId().equals(recipient)) {
                            user.send(message);
                        }
                    }
                }
            });

            wsConfig.onClose(closeContext -> {
                System.out.println("Closed: " + closeContext.sessionId());
                allUsers.remove(closeContext);
            });

            wsConfig.onError(errorContext -> {
                System.out.println("Error: " + errorContext.sessionId());
            });
        });

        app.start(5001);
    }
}
