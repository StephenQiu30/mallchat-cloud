package com.stephen.cloud.chat.ops;

import java.util.List;

/**
 * IM 核心表一致性断言目录（与 scripts/verify-im-core-data-recovery.sh 对齐）。
 */
public final class ImCoreConsistencyAssertions {

    private ImCoreConsistencyAssertions() {
    }

    public static final List<ImCoreConsistencyAssertion> ALL = List.of(
            new ImCoreConsistencyAssertion("friend", "user_friend user relation",
                    "SELECT COUNT(*) FROM user_friend uf LEFT JOIN `user` u ON u.id = uf.user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("friend", "user_friend friend relation",
                    "SELECT COUNT(*) FROM user_friend uf LEFT JOIN `user` u ON u.id = uf.friend_user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("friend", "user_friend_apply user relation",
                    "SELECT COUNT(*) FROM user_friend_apply ufa LEFT JOIN `user` u ON u.id = ufa.user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("friend", "user_friend_apply target relation",
                    "SELECT COUNT(*) FROM user_friend_apply ufa LEFT JOIN `user` u ON u.id = ufa.target_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("room", "chat_room creator relation",
                    "SELECT COUNT(*) FROM chat_room r LEFT JOIN `user` u ON u.id = r.create_user WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("room", "chat_room_member user relation",
                    "SELECT COUNT(*) FROM chat_room_member rm LEFT JOIN `user` u ON u.id = rm.user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("room", "chat_room_member room relation",
                    "SELECT COUNT(*) FROM chat_room_member rm LEFT JOIN chat_room r ON r.id = rm.room_id WHERE r.id IS NULL"),
            new ImCoreConsistencyAssertion("room", "chat_private_room room relation",
                    "SELECT COUNT(*) FROM chat_private_room pr LEFT JOIN chat_room r ON r.id = pr.room_id WHERE r.id IS NULL"),
            new ImCoreConsistencyAssertion("room", "chat_group_info room relation",
                    "SELECT COUNT(*) FROM chat_group_info gi LEFT JOIN chat_room r ON r.id = gi.room_id WHERE r.id IS NULL"),
            new ImCoreConsistencyAssertion("message", "chat_message room relation",
                    "SELECT COUNT(*) FROM chat_message m LEFT JOIN chat_room r ON r.id = m.room_id WHERE r.id IS NULL"),
            new ImCoreConsistencyAssertion("message", "chat_message sender relation",
                    "SELECT COUNT(*) FROM chat_message m LEFT JOIN `user` u ON u.id = m.from_user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("session", "chat_session message relation",
                    "SELECT COUNT(*) FROM chat_session s LEFT JOIN chat_message m ON m.id = s.last_message_id WHERE s.last_message_id IS NOT NULL AND m.id IS NULL"),
            new ImCoreConsistencyAssertion("session", "chat_session room relation",
                    "SELECT COUNT(*) FROM chat_session s LEFT JOIN chat_room r ON r.id = s.room_id WHERE r.id IS NULL"),
            new ImCoreConsistencyAssertion("session", "chat_session user relation",
                    "SELECT COUNT(*) FROM chat_session s LEFT JOIN `user` u ON u.id = s.user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("moment", "chat_moment user relation",
                    "SELECT COUNT(*) FROM chat_moment m LEFT JOIN `user` u ON u.id = m.user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("moment", "chat_moment_media relation",
                    "SELECT COUNT(*) FROM chat_moment_media mm LEFT JOIN chat_moment m ON m.id = mm.moment_id WHERE m.id IS NULL"),
            new ImCoreConsistencyAssertion("moment", "chat_moment_like relation",
                    "SELECT COUNT(*) FROM chat_moment_like ml LEFT JOIN chat_moment m ON m.id = ml.moment_id WHERE m.id IS NULL"),
            new ImCoreConsistencyAssertion("moment", "chat_moment_like user relation",
                    "SELECT COUNT(*) FROM chat_moment_like ml LEFT JOIN `user` u ON u.id = ml.user_id WHERE u.id IS NULL"),
            new ImCoreConsistencyAssertion("moment", "chat_moment_comment relation",
                    "SELECT COUNT(*) FROM chat_moment_comment mc LEFT JOIN chat_moment m ON m.id = mc.moment_id WHERE m.id IS NULL"),
            new ImCoreConsistencyAssertion("moment", "chat_moment_comment user relation",
                    "SELECT COUNT(*) FROM chat_moment_comment mc LEFT JOIN `user` u ON u.id = mc.user_id WHERE u.id IS NULL")
    );
}
