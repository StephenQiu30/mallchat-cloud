package com.stephen.cloud.ai.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.stephen.cloud.ai.convert.AiChatRecordConvert;
import com.stephen.cloud.ai.mapper.AiChatRecordMapper;
import com.stephen.cloud.ai.model.entity.AiChatRecord;
import com.stephen.cloud.ai.service.AiChatRecordService;
import com.stephen.cloud.api.ai.model.dto.AiChatRecordQueryRequest;
import com.stephen.cloud.api.ai.model.vo.AiChatRecordVO;
import com.stephen.cloud.common.constants.CommonConstant;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * AI 对话记录服务实现
 *
 * @author StephenQiu30
 */
@Service
public class AiChatRecordServiceImpl extends ServiceImpl<AiChatRecordMapper, AiChatRecord>
        implements AiChatRecordService {

    /**
     * 根据查询请求构造灵活的 MyBatis-Plus 查询条件
     */
    @Override
    public LambdaQueryWrapper<AiChatRecord> getQueryWrapper(AiChatRecordQueryRequest aiChatRecordQueryRequest) {
        LambdaQueryWrapper<AiChatRecord> queryWrapper = new LambdaQueryWrapper<>();
        if (aiChatRecordQueryRequest == null) {
            return queryWrapper;
        }
        String sessionId = aiChatRecordQueryRequest.getSessionId();
        String modelType = aiChatRecordQueryRequest.getModelType();
        String searchText = aiChatRecordQueryRequest.getSearchText();
        String sortField = aiChatRecordQueryRequest.getSortField();
        String sortOrder = aiChatRecordQueryRequest.getSortOrder();

        // 基础字段匹配
        queryWrapper.eq(StringUtils.isNotBlank(sessionId), AiChatRecord::getSessionId, sessionId)
                .eq(StringUtils.isNotBlank(modelType), AiChatRecord::getModelType, modelType);

        // 模糊搜索：匹配提问或回答正文
        if (StringUtils.isNotBlank(searchText)) {
            queryWrapper.and(qw -> qw
                    .like(AiChatRecord::getMessage, searchText)
                    .or()
                    .like(AiChatRecord::getResponse, searchText));
        }

        // 动态排序逻辑
        if (StringUtils.isNotBlank(sortField)) {
            boolean isAsc = CommonConstant.SORT_ORDER_ASC.equalsIgnoreCase(sortOrder);
            if ("createTime".equals(sortField)) {
                queryWrapper.orderBy(true, isAsc, AiChatRecord::getCreateTime);
            }
        }
        return queryWrapper;
    }

    @Override
    public Page<AiChatRecordVO> getAiChatRecordVOPage(Page<AiChatRecord> aiChatRecordPage) {
        List<AiChatRecord> aiChatRecordList = aiChatRecordPage.getRecords();
        Page<AiChatRecordVO> aiChatRecordVOPage = new Page<>(aiChatRecordPage.getCurrent(), aiChatRecordPage.getSize(),
                aiChatRecordPage.getTotal());
        if (aiChatRecordList.isEmpty()) {
            return aiChatRecordVOPage;
        }
        List<AiChatRecordVO> aiChatRecordVOList = aiChatRecordList.stream()
                .map(AiChatRecordConvert::objToVo)
                .toList();
        aiChatRecordVOPage.setRecords(aiChatRecordVOList);
        return aiChatRecordVOPage;
    }
}
