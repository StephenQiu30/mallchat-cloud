package com.stephen.cloud.chat.convert;

import com.stephen.cloud.api.chat.model.vo.ChatSessionVO;
import com.stephen.cloud.chat.model.entity.ChatSession;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.Date;

class ChatSessionConvertTest {

    @Test
    void shouldExposeMessageCursorsOnSessionVO() {
        ChatSession session = new ChatSession();
        session.setRoomId(1L);
        session.setLastMessageId(10L);
        session.setLastReadMessageId(8L);
        session.setUnreadCount(2);
        session.setActiveTime(new Date());

        ChatSessionVO vo = ChatSessionConvert.objToVo(session);

        Assertions.assertEquals(1L, vo.getRoomId());
        Assertions.assertEquals(10L, vo.getLastMessageId());
        Assertions.assertEquals(8L, vo.getLastReadMessageId());
        Assertions.assertEquals(2, vo.getUnreadCount());
    }
}
