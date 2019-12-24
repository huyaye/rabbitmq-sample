
package rmqess.ch03;

import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Scanner;

import javax.websocket.CloseReason;
import javax.websocket.CloseReason.CloseCodes;
import javax.websocket.ContainerProvider;
import javax.websocket.Session;
import javax.websocket.WebSocketContainer;

import org.glassfish.tyrus.server.Server;

import com.google.common.collect.Sets;
import com.rabbitmq.client.ConnectionFactory;

public class Main
{
    static UserMessageManager userMessageManager;

    public static void main(final String[] args) throws Exception
    {
        run(new UserMessageManager(), 12);
    }

    protected static void run(final UserMessageManager userMessageManager, final long maxUserId)
        throws Exception
    {
        final RabbitMqManager rabbitMqManager = connectRabbitMqManager();

        Main.userMessageManager = userMessageManager;
        Main.userMessageManager.setRabbitMqManager(rabbitMqManager);
        Main.userMessageManager.onApplicationStart();

        final Server server = new Server("localhost", 8025, "/", null, UserMessageServerEndpoint.class);
        server.start();

        final WebSocketContainer websocketClientContainer = ContainerProvider.getWebSocketContainer();
        final List<Session> websocketClientSessions = new ArrayList<>();

        // 일부 웹 소켓 클라이언트와 시뮬레이션할 사용자를 시작한다.
        System.out.printf("Starting the application with %d simulated users%n", maxUserId);
        for (long userId = 1; userId <= maxUserId; userId++)
        {
            // 웹 소켓을 포함하는 메인 웹 애플리케이션에 의해 이벤트가 발생한다.
            userMessageManager.onUserLogin(userId);
            userMessageManager.onUserTopicInterestChange(userId, Sets.newHashSet("science", "politics"),
                Collections.<String> emptySet());
            System.out.printf("User login: %d%n", userId);

            // 웹 소켓 클라이언트를 시작한다.

            final Session session = websocketClientContainer.connectToServer(new WebsocketClientSimulator(
                userId, maxUserId), new URI("ws://localhost:8025/user-message/" + userId));
            session.setMaxIdleTimeout(0);

            websocketClientSessions.add(session);

            Thread.sleep(1500L);
        }

        // 종료(shutdown)
        waitForEnter();

        for (final Session websocketClientSession : websocketClientSessions)
        {
            if (websocketClientSession.isOpen())
            {
                websocketClientSession.close(new CloseReason(CloseCodes.NORMAL_CLOSURE,
                    "Normal end of simulation"));
            }
        }

        Thread.sleep(1000L);
        server.stop();

        Thread.sleep(1000L);
        shutdownRabbitMqManager(rabbitMqManager);
    }

    protected static RabbitMqManager connectRabbitMqManager()
    {
        final ConnectionFactory factory = new ConnectionFactory();
        factory.setUsername("ccm-dev");
        factory.setPassword("coney123");
        factory.setVirtualHost("ccm-dev-vhost");
        factory.setHost("localhost");
        factory.setPort(5672);

        // 의존성 관리 생성 및 연결을 시뮬레이션한다.
        final RabbitMqManager rabbitMqManager = new RabbitMqManager(factory);
        rabbitMqManager.start();
        return rabbitMqManager;
    }

    protected static void waitForEnter()
    {
        System.out.println("Running, strike ENTER to stop!");
        try (Scanner s = new Scanner(System.in))
        {
            s.nextLine();
        }

        System.out.print("Shutting down...");
    }

    protected static void shutdownRabbitMqManager(final RabbitMqManager rabbitMqManager)
    {
        rabbitMqManager.stop();

        System.out.print("Bye!");
        System.exit(0);
    }
}
