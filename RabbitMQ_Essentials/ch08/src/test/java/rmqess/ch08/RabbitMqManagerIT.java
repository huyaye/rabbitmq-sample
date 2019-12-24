
package rmqess.ch08;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import rmqess.ch02.ChannelCallable;
import rmqess.ch03.Subscription;
import rmqess.ch03.SubscriptionDeliveryHandler;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Queue.DeclareOk;
import com.rabbitmq.client.Address;
import com.rabbitmq.client.Channel;
import com.rabbitmq.client.ConnectionFactory;
import com.rabbitmq.client.Envelope;

public class RabbitMqManagerIT
{
    private RabbitMqManager rabbitMqManager;

    @Before
    public void configureAndStart() throws Exception
    {
        final ConnectionFactory connectionFactory = new ConnectionFactory();
        connectionFactory.setUsername(System.getProperty("test.rmq.username", "ccm-dev"));
        connectionFactory.setPassword(System.getProperty("test.rmq.password", "coney123"));
        connectionFactory.setVirtualHost(System.getProperty("test.rmq.vhost", "ccm-dev-vhost"));

        final String addresses = System.getProperty("test.rmq.addresses", "localhost:5672");

        rabbitMqManager = new RabbitMqManager(connectionFactory, Address.parseAddresses(addresses));

        System.out.printf("%nRunning integration tests on %s%n%n", addresses);

        rabbitMqManager.start();
    }

    @After
    public void stop() throws Exception
    {
        rabbitMqManager.stop();
    }

    @Test
    public void subscriptionTest() throws Exception
    {
        final String queue = rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                return "subscription test setup";
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                final DeclareOk declareOk = channel.queueDeclare(
                    "", // 자동으로 이름을 만든다.
                    false, // 내구성은 없도록 설정한다.
                    true,  // 배타적 성질을 갖는다.
                    true,  // 자동 삭제 가능하도록 설정한다.
                    null); // 인자값은 없다.
                return declareOk.getQueue();
            }
        });

        final AtomicReference<byte[]> delivered = new AtomicReference<byte[]>();
        final CountDownLatch latch = new CountDownLatch(1);

        final Subscription subscription = rabbitMqManager.createSubscription(queue,
            new SubscriptionDeliveryHandler()
            {
                @Override
                public void handleDelivery(final Channel channel,
                                           final Envelope envelope,
                                           final BasicProperties properties,
                                           final byte[] body)
                {
                    delivered.set(body);
                    latch.countDown();
                }
            });

        assertThat(subscription.getChannel().isOpen(), is(true));

        final byte[] body = rabbitMqManager.call(new ChannelCallable<byte[]>()
        {
            @Override
            public String getDescription()
            {
                return "publish test message";
            }

            @Override
            public byte[] call(final Channel channel) throws IOException
            {
                final byte[] body = UUID.randomUUID().toString().getBytes();

                channel.basicPublish(
                    "", // 기본 익스체인지
                    queue, // 라우팅 키로 큐를 설정
                    null, // 속성은 없다
                    body);
                return body;
            }
        });

        if (!latch.await(1, TimeUnit.MINUTES))
        {
            fail("Handler not called on time");
        }

        assertThat(delivered.get(), is(body));

        subscription.stop();
        assertThat(subscription.getChannel(), is(nullValue()));
    }
}
