package com.stephen.cloud.file.controller;

import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileVO;
import com.stephen.cloud.common.common.BaseResponse;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.common.ResultUtils;
import com.stephen.cloud.common.common.ThrowUtils;
import com.stephen.cloud.file.service.FileService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
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

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizType 业务类型
     * @return 文件信息
     */
    @PostMapping("/upload")
    @Operation(summary = "上传文件", description = "上传文件到腾讯云 COS，支持用户头像、聊天图片、聊天文件等业务类型")
    public BaseResponse<FileVO> uploadFile(
            @Parameter(description = "上传的文件", required = true) @RequestPart("file") MultipartFile file,
            @Parameter(description = "业务类型：user_avatar(用户头像)、chat_image(聊天图片)、chat_file(聊天文件)", required = true, example = "user_avatar") @RequestParam("bizType") String bizType) {
        FileUploadBizEnum bizTypeEnum = FileUploadBizEnum.getEnumByCode(bizType);
        ThrowUtils.throwIf(bizTypeEnum == null, ErrorCode.PARAMS_ERROR, "业务类型错误");
        FileVO fileVO = fileService.uploadFile(file, bizTypeEnum);
        return ResultUtils.success(fileVO);
    }
}
