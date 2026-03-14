package com.stephen.cloud.chat.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.chat.mapper.ChatRoomMemberMapper;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.chat.service.ChatRoomMemberService;
import org.springframework.stereotype.Service;

/**
 * 聊天室成员服务实现
 *
 * @author StephenQiu30
 */
@Service
public class ChatRoomMemberServiceImpl extends ServiceImpl<ChatRoomMemberMapper, ChatRoomMember>
    implements ChatRoomMemberService {

}
