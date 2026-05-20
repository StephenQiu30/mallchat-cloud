package com.stephen.cloud.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.chat.model.entity.ChatMomentLike;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

/**
 * 动态点赞映射器
 *
 * @author StephenQiu30
 */
public interface ChatMomentLikeMapper extends BaseMapper<ChatMomentLike> {

    @Select("SELECT * FROM chat_moment_like WHERE moment_id = #{momentId} AND user_id = #{userId} LIMIT 1")
    ChatMomentLike selectByMomentIdAndUserIdIncludingDeleted(Long momentId, Long userId);

    @Update("UPDATE chat_moment_like SET is_delete = 0 WHERE id = #{id} AND is_delete = 1")
    int restoreByIdIncludingDeleted(Long id);
}
