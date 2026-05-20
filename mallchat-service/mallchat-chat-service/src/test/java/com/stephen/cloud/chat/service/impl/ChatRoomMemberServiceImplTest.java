package com.stephen.cloud.chat.service.impl;

import com.stephen.cloud.api.chat.model.enums.ChatRoomRoleEnum;
import com.stephen.cloud.chat.model.entity.ChatRoomMember;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ChatRoomMemberServiceImplTest {

    private TestableChatRoomMemberServiceImpl chatRoomMemberService;
    private ChatRoomMember existingMember;
    private ChatRoomMember updatedMember;

    @BeforeEach
    void setUp() {
        chatRoomMemberService = new TestableChatRoomMemberServiceImpl();
    }

    @Test
    void shouldKeepExistingAdminRoleWhenDuplicateInviteAddsMemberRole() {
        existingMember = buildMember(ChatRoomRoleEnum.ADMIN.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.ADMIN.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldKeepExistingOwnerRoleWhenDuplicateInviteAddsMemberRole() {
        existingMember = buildMember(ChatRoomRoleEnum.OWNER.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.OWNER.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldKeepExistingMemberRoleWhenDuplicateInviteAddsMemberRole() {
        existingMember = buildMember(ChatRoomRoleEnum.MEMBER.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.MEMBER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.MEMBER.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldKeepExistingMemberRoleWhenDuplicateAddAttemptsOwnerRole() {
        existingMember = buildMember(ChatRoomRoleEnum.MEMBER.getCode());

        chatRoomMemberService.addMember(10L, 2L, ChatRoomRoleEnum.OWNER.getCode());

        Assertions.assertEquals(ChatRoomRoleEnum.MEMBER.getCode(), existingMember.getRole());
        Assertions.assertNull(updatedMember);
    }

    @Test
    void shouldRejectInvalidAddMemberParams() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomMemberService.addMember(null, 2L, ChatRoomRoleEnum.MEMBER.getCode()));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    @Test
    void shouldRejectNullUserIdWhenAddingMember() {
        BusinessException exception = Assertions.assertThrows(BusinessException.class,
                () -> chatRoomMemberService.addMember(10L, null, ChatRoomRoleEnum.MEMBER.getCode()));

        Assertions.assertEquals(ErrorCode.PARAMS_ERROR.getCode(), exception.getCode());
    }

    private ChatRoomMember buildMember(Integer role) {
        ChatRoomMember member = new ChatRoomMember();
        member.setId(1L);
        member.setRoomId(10L);
        member.setUserId(2L);
        member.setRole(role);
        return member;
    }

    private class TestableChatRoomMemberServiceImpl extends ChatRoomMemberServiceImpl {
        @Override
        public ChatRoomMember getMember(Long roomId, Long userId) {
            return existingMember;
        }

        @Override
        public boolean updateById(ChatRoomMember entity) {
            updatedMember = entity;
            return true;
        }
    }
}
