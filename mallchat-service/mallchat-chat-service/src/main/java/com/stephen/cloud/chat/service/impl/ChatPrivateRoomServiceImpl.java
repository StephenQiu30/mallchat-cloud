package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.chat.mapper.ChatPrivateRoomMapper;
import com.stephen.cloud.chat.model.entity.ChatPrivateRoom;
import com.stephen.cloud.chat.service.ChatPrivateRoomService;
import org.springframework.stereotype.Service;

/**
 * 私聊房间映射服务实现
 *
 * @author StephenQiu30
 */
@Service
public class ChatPrivateRoomServiceImpl extends ServiceImpl<ChatPrivateRoomMapper, ChatPrivateRoom>
    implements ChatPrivateRoomService {
}
