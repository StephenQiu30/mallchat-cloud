package com.stephen.cloud.chat.e2e;

import com.stephen.cloud.api.notification.client.NotificationFeignClient;
import com.stephen.cloud.api.notification.model.vo.NotificationIdVO;
import com.stephen.cloud.api.user.client.UserFeignClient;
import com.stephen.cloud.api.user.model.dto.UserIdsRequest;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.chat.mq.producer.ChatMqProducer;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;

/**
 * E2E Test Base Configuration
 *
 * Uses H2 in-memory database for E2E testing.
 * Mocks external Feign clients and MQ producer to isolate chat-service logic.
 *
 * @author StephenQiu30
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("e2e")
@Transactional
public abstract class E2eBaseTest {

    @MockBean
    protected UserFeignClient userFeignClient;

    @MockBean
    protected NotificationFeignClient notificationFeignClient;

    @MockBean
    protected ChatMqProducer chatMqProducer;

    @Autowired
    protected JdbcTemplate jdbcTemplate;

    @BeforeEach
    void setUpMocks() {
        // Mock UserFeignClient.getUserVOById to return test user
        when(userFeignClient.getUserVOById(anyLong())).thenAnswer(invocation -> {
            Long userId = invocation.getArgument(0);
            UserVO userVO = new UserVO();
            userVO.setId(userId);
            userVO.setUserName("TestUser" + userId);
            userVO.setUserAvatar("https://example.com/avatar/" + userId + ".png");
            return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), userVO, "ok");
        });

        // Mock UserFeignClient.getUserVOByIds (List<Long> overload)
        when(userFeignClient.getUserVOByIds(anyList())).thenAnswer(invocation -> {
            List<Long> userIds = invocation.getArgument(0);
            List<UserVO> userVOs = userIds.stream().map(id -> {
                UserVO userVO = new UserVO();
                userVO.setId(id);
                userVO.setUserName("TestUser" + id);
                userVO.setUserAvatar("https://example.com/avatar/" + id + ".png");
                return userVO;
            }).toList();
            return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), userVOs, "ok");
        });

        // Mock UserFeignClient.getUserVOByIds (UserIdsRequest overload)
        when(userFeignClient.getUserVOByIds(any(UserIdsRequest.class))).thenAnswer(invocation -> {
            UserIdsRequest request = invocation.getArgument(0);
            List<Long> userIds = request.getIds();
            List<UserVO> userVOs = userIds.stream().map(id -> {
                UserVO userVO = new UserVO();
                userVO.setId(id);
                userVO.setUserName("TestUser" + id);
                userVO.setUserAvatar("https://example.com/avatar/" + id + ".png");
                return userVO;
            }).toList();
            return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), userVOs, "ok");
        });

        // Mock NotificationFeignClient to avoid notification calls
        when(notificationFeignClient.addBusinessNotification(any())).thenAnswer(invocation -> {
            NotificationIdVO idVO = new NotificationIdVO(1L);
            return new BaseResponse<>(ErrorCode.SUCCESS.getCode(), idVO, "ok");
        });

        // Mock ChatMqProducer to avoid RabbitMQ dependency
        doNothing().when(chatMqProducer).sendChatMessageGroupPush(anyLong(), any(), any());
        doNothing().when(chatMqProducer).sendFriendApply(anyLong(), any(), any());
        doNothing().when(chatMqProducer).sendFriendApprove(anyLong(), any(), any());
    }

    /**
     * Insert test user directly into the database.
     *
     * @param userId    user ID
     * @param userName  user name
     */
    protected void insertTestUser(Long userId, String userName) {
        jdbcTemplate.update(
                "INSERT INTO `user` (id, user_name, user_role) VALUES (?, ?, 'user')",
                userId, userName
        );
    }

    /**
     * Insert friend relationship directly into the database.
     *
     * @param userId       user ID
     * @param friendUserId friend user ID
     */
    protected void insertFriendRelation(Long userId, Long friendUserId) {
        jdbcTemplate.update(
                "INSERT INTO `user_friend` (user_id, friend_user_id, friend_group_name) VALUES (?, ?, '默认分组')",
                userId, friendUserId
        );
    }
}
