package com.stephen.cloud.file.controller;

import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileVO;
import com.stephen.cloud.common.auth.utils.SecurityUtils;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.common.utils.IpUtils;
import com.stephen.cloud.file.service.FileService;
import com.stephen.cloud.file.service.impl.FileUploadRecordRecorder;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件控制器
 *
 * @author StephenQiu30
 */
@RestController
@RequestMapping("/file")
@Tag(name = "FileController", description = "文件上传与访问接口")
@Slf4j
public class FileController {

    @Resource
    private FileService fileService;

    @Resource
    private FileUploadRecordRecorder fileUploadRecordRecorder;

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizType 业务类型
     * @return 文件信息
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传文件", description = "上传文件到腾讯云 COS，支持用户头像、聊天图片、聊天文件、聊天语音、聊天视频等业务类型")
    public BaseResponse<FileVO> uploadFile(
            @Parameter(description = "上传的文件", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "业务类型：user_avatar(用户头像)、chat_image(聊天图片)、chat_file(聊天文件)、chat_voice(聊天语音)、chat_video(聊天视频)", required = true, example = "user_avatar") @RequestParam("bizType") String bizType,
            HttpServletRequest request) {
        FileUploadBizEnum bizTypeEnum = FileUploadBizEnum.getEnumByCode(bizType);
        ThrowUtils.throwIf(bizTypeEnum == null, ErrorCode.PARAMS_ERROR, "业务类型错误");
        Long userId = SecurityUtils.getLoginUserId();
        String clientIp = IpUtils.getClientIp(request);
        FileUploadRecordRecorder.FileUploadMetadata metadata = FileUploadRecordRecorder.FileUploadMetadata.from(file);
        try {
            FileVO fileVO = fileService.uploadFile(file, bizTypeEnum);
            fileUploadRecordRecorder.recordSuccess(metadata, bizTypeEnum, fileVO, userId, clientIp);
            return ResultUtils.success(fileVO);
        } catch (Exception e) {
            fileUploadRecordRecorder.recordFailure(metadata, bizTypeEnum, userId, clientIp, e.getMessage());
            throw e;
        }
    }
}
