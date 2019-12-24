
package rmqess.ch05;

import java.util.logging.Logger;

public class UserManager
{
    protected final static Logger LOGGER = Logger.getLogger(UserManager.class.getName());

    public void handleDeadMessage(final long userId, final String jsonMessage)
    {
        // 사용자 정보를 기반으로 이메일을 보내거나 메시지를 버린다.
        LOGGER.info("Handling dead message for user: " + userId);
    }
}
