package com.stephen.cloud.log.controller;

import cn.dev33.satoken.annotation.SaCheckRole;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.stephen.cloud.api.log.model.dto.LogIdRequest;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordQueryRequest;
import com.stephen.cloud.api.log.model.vo.FileUploadRecordVO;
import com.stephen.cloud.api.log.model.vo.LogOperationResultVO;
import com.stephen.cloud.common.auth.annotation.InternalAuth;
import com.stephen.cloud.common.common.*;
import com.stephen.cloud.common.constants.UserConstant;
import com.stephen.cloud.log.convert.LogConvert;
import com.stephen.cloud.log.model.entity.FileUploadRecord;
import com.stephen.cloud.log.service.FileUploadRecordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 文件上传记录接口
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/log/file/upload")
@Slf4j
@Tag(name = "FileUploadRecordController", description = "文件上传记录接口")
public class FileUploadRecordController {

    @Resource
    private FileUploadRecordService fileUploadRecordService;

    /**
     * 创建文件上传记录
     *
     * @param request 创建请求
     * @return 是否创建成功
     */
    @PostMapping("/add")
    @InternalAuth
    @Operation(summary = "创建文件上传记录", description = "记录文件上传信息")
    public BaseResponse<LogOperationResultVO> addFileUploadRecord(@RequestBody FileUploadRecordAddRequest request) {
        ThrowUtils.throwIf(request == null, ErrorCode.PARAMS_ERROR);
        boolean result = fileUploadRecordService.addRecord(request);
        return ResultUtils.success(LogOperationResultVO.of(result));
    }

    /**
     * 删除文件上传记录
     *
     * @param request 删除请求
     * @return 是否删除成功
     */
    @PostMapping("/delete")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "删除文件上传记录", description = "删除指定文件上传记录（仅管理员）")
    public BaseResponse<LogOperationResultVO> deleteFileUploadRecord(@Validated @RequestBody LogIdRequest request) {
        ThrowUtils.throwIf(request == null || request.getId() == null || request.getId() <= 0,
                ErrorCode.PARAMS_ERROR);
        long id = request.getId();
        FileUploadRecord oldRecord = fileUploadRecordService.getById(id);
        ThrowUtils.throwIf(oldRecord == null, ErrorCode.NOT_FOUND_ERROR);
        boolean result = fileUploadRecordService.removeById(id);
        return ResultUtils.success(LogOperationResultVO.of(result));
    }

    /**
     * 分页获取文件上传记录列表（仅管理员）
     *
     * @param queryRequest 查询请求
     * @return 分页记录列表
     */
    @PostMapping("/list/page")
    @SaCheckRole(UserConstant.ADMIN_ROLE)
    @Operation(summary = "分页获取文件上传记录列表", description = "分页查询文件上传记录（仅管理员）")
    public BaseResponse<Page<FileUploadRecordVO>> listRecordByPage(
            @RequestBody FileUploadRecordQueryRequest queryRequest) {
        ThrowUtils.throwIf(queryRequest == null, ErrorCode.PARAMS_ERROR);
        long current = queryRequest.getCurrent();
        long size = queryRequest.getPageSize();
        Page<FileUploadRecord> recordPage = fileUploadRecordService.page(new Page<>(current, size),
                fileUploadRecordService.getQueryWrapper(queryRequest));
        Page<FileUploadRecordVO> voPage = new Page<>(current, size, recordPage.getTotal());
        List<FileUploadRecordVO> voList = recordPage.getRecords().stream()
                .map(LogConvert::fileUploadRecordToVO)
                .collect(Collectors.toList());
        voPage.setRecords(voList);
        return ResultUtils.success(voPage);
    }
}
