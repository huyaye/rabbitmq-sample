
package rmqess.ch03;

import java.io.IOException;
import java.util.Map;
import java.util.UUID;

import rmqess.ch02.ChannelCallable;
import rmqess.ch02.UserMessageManagerWithTopics;

import com.rabbitmq.client.AMQP.BasicProperties;
import com.rabbitmq.client.AMQP.Exchange.DeclareOk;
import com.rabbitmq.client.AMQP.Queue.BindOk;
import com.rabbitmq.client.Channel;

public class UserMessageManager extends UserMessageManagerWithTopics
{
    public static final String USER_FANOUT_EXCHANGE = "user-fanout";

    protected RabbitMqManager rabbitMqManager;

    public void setRabbitMqManager(final RabbitMqManager rabbitMqManager)
    {
        super.setRabbitMqManager(rabbitMqManager);
        this.rabbitMqManager = rabbitMqManager;
    }

    @Override
    public void onApplicationStart()
    {
        super.onApplicationStart();

        rabbitMqManager.call(new ChannelCallable<DeclareOk>()
        {
            @Override
            public String getDescription()
            {
                return "Declaring fanout exchange: " + USER_FANOUT_EXCHANGE;
            }

            @Override
            public DeclareOk call(final Channel channel) throws IOException
            {
                final String exchange = USER_FANOUT_EXCHANGE;
                final String type = "fanout";
                // 재시작 후에도 서버는 살아남는다.
                final boolean durable = true;
                // 사용자가 없더라도 유지시킨다.
                final boolean autoDelete = false;
                // 특별한 인자값은 없다.
                final Map<String, Object> arguments = null;

                return channel.exchangeDeclare(exchange, type, durable, autoDelete, arguments);
            }
        });
    }

    @Override
    public void onUserLogin(final long userId)
    {
        super.onUserLogin(userId);

        final String queue = getUserInboxQueue(userId);

        rabbitMqManager.call(new ChannelCallable<BindOk>()
        {
            @Override
            public String getDescription()
            {
                return "Binding user queue: " + queue + " to exchange: " + USER_FANOUT_EXCHANGE;
            }

            @Override
            public BindOk call(final Channel channel) throws IOException
            {
                // 팬아웃 익스체인지에 수신자 큐를 결합한다.
                final String routingKey = "";
                return channel.queueBind(queue, USER_FANOUT_EXCHANGE, routingKey);
            }
        });
    }

    public Subscription subscribeToUserInbox(final long userId, final SubscriptionDeliveryHandler handler)
    {
        final String queue = getUserInboxQueue(userId);
        return rabbitMqManager.createSubscription(queue, handler);
    }

    public String sendUserMessage(final long userId, final String message, final Channel channel)
        throws IOException
    {
        final String queue = getUserInboxQueue(userId);

        // 큐가 존재하지 않으면 선언한다.
        declareUserMessageQueue(queue, channel);

        final String messageId = UUID.randomUUID().toString();

        final BasicProperties props = new BasicProperties.Builder().contentType(MESSAGE_CONTENT_TYPE)
            .contentEncoding(MESSAGE_ENCODING)
            .messageId(messageId)
            .deliveryMode(2)
            .build();

        final String routingKey = queue;

        // 다이렉트 익스체인지에 메시지를 발행한다.
        channel.basicPublish(USER_INBOXES_EXCHANGE, routingKey, props, message.getBytes(MESSAGE_ENCODING));

        return messageId;
    }

    public String sendTopicMessage(final String topic, final String message, final Channel channel)
        throws IOException
    {
        final String messageId = UUID.randomUUID().toString();

        final BasicProperties props = new BasicProperties.Builder().contentType(MESSAGE_CONTENT_TYPE)
            .contentEncoding(MESSAGE_ENCODING)
            .messageId(messageId)
            .deliveryMode(2)
            .build();

        // 토픽 익스체인지에 메시지를 발행한다.
        channel.basicPublish(USER_TOPICS_EXCHANGE, topic, props, message.getBytes(MESSAGE_ENCODING));

        return messageId;
    }

    // 2장에서 리팩토링되었다.

    @Override
    public String sendUserMessage(final long userId, final String message)
    {
        return rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                return "Sending message to user: " + userId;
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                return sendUserMessage(userId, message, channel);
            }
        });
    }

    @Override
    public String sendTopicMessage(final String topic, final String message)
    {
        return rabbitMqManager.call(new ChannelCallable<String>()
        {
            @Override
            public String getDescription()
            {
                return "Sending message to topic: " + topic;
            }

            @Override
            public String call(final Channel channel) throws IOException
            {
                return sendTopicMessage(topic, message, channel);
            }
        });
    }
}
