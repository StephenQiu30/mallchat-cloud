package com.stephen.cloud.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.chat.model.entity.ChatMoment;
import org.apache.ibatis.annotations.Update;
import org.apache.ibatis.annotations.Select;

/**
 * 动态主体映射器
 *
 * @author StephenQiu30
 */
public interface ChatMomentMapper extends BaseMapper<ChatMoment> {

    @Select("SELECT * FROM chat_moment WHERE id = #{id} LIMIT 1")
    ChatMoment selectByIdIncludingDeleted(Long id);

    @Update("UPDATE chat_moment SET like_count = like_count + 1 WHERE id = #{id} AND status = 0 AND is_delete = 0")
    int increaseLikeCount(Long id);

    @Update("UPDATE chat_moment SET like_count = CASE WHEN like_count > 0 THEN like_count - 1 ELSE 0 END WHERE id = #{id} AND status = 0 AND is_delete = 0")
    int decreaseLikeCount(Long id);

    @Update("UPDATE chat_moment SET comment_count = comment_count + 1 WHERE id = #{id} AND status = 0 AND is_delete = 0")
    int increaseCommentCount(Long id);
}
