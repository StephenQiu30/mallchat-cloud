package com.stephen.cloud.chat.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.stephen.cloud.chat.model.entity.ChatMoment;
import org.apache.ibatis.annotations.Select;

/**
 * 动态主体映射器
 *
 * @author StephenQiu30
 */
public interface ChatMomentMapper extends BaseMapper<ChatMoment> {

    @Select("SELECT * FROM chat_moment WHERE id = #{id} LIMIT 1")
    ChatMoment selectByIdIncludingDeleted(Long id);
}
