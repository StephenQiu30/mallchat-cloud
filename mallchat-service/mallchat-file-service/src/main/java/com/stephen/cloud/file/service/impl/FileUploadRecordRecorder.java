package com.stephen.cloud.file.service.impl;

import cn.hutool.core.io.FileUtil;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileVO;
import com.stephen.cloud.api.log.client.LogFeignClient;
import com.stephen.cloud.api.log.model.dto.file.FileUploadRecordAddRequest;
import com.stephen.cloud.file.config.FileStorageConfiguration;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件上传记录器
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class FileUploadRecordRecorder {

    private static final String STORAGE_TYPE_COS = "COS";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_FAIL = "FAIL";
    private static final String EMPTY_OBJECT_VALUE = "-";

    @Resource
    private LogFeignClient logFeignClient;

    @Resource
    private FileStorageConfiguration fileStorageConfiguration;

    @Async
    public void recordSuccess(FileUploadMetadata metadata, FileUploadBizEnum bizTypeEnum, FileVO fileVO, Long userId, String clientIp) {
        try {
            FileUploadRecordAddRequest request = buildBaseRequest(metadata, bizTypeEnum, userId, clientIp);
            request.setObjectKey(fileVO.getKey());
            request.setUrl(fileVO.getUrl());
            request.setStatus(STATUS_SUCCESS);
            logFeignClient.addFileUploadRecord(request);
        } catch (Exception e) {
            log.warn("记录文件上传成功日志失败: {}", e.getMessage());
        }
    }

    @Async
    public void recordFailure(FileUploadMetadata metadata, FileUploadBizEnum bizTypeEnum, Long userId, String clientIp, String errorMessage) {
        try {
            FileUploadRecordAddRequest request = buildBaseRequest(metadata, bizTypeEnum, userId, clientIp);
            request.setObjectKey(EMPTY_OBJECT_VALUE);
            request.setUrl(EMPTY_OBJECT_VALUE);
            request.setStatus(STATUS_FAIL);
            request.setErrorMessage(errorMessage);
            logFeignClient.addFileUploadRecord(request);
        } catch (Exception e) {
            log.warn("记录文件上传失败日志失败: {}", e.getMessage());
        }
    }

    private FileUploadRecordAddRequest buildBaseRequest(FileUploadMetadata metadata, FileUploadBizEnum bizTypeEnum, Long userId, String clientIp) {
        FileUploadRecordAddRequest request = new FileUploadRecordAddRequest();
        request.setUserId(userId);
        request.setBizType(bizTypeEnum.getCode());
        request.setFileName(metadata.fileName());
        request.setFileSize(metadata.fileSize());
        request.setFileSuffix(metadata.fileSuffix());
        request.setContentType(metadata.contentType());
        request.setStorageType(STORAGE_TYPE_COS);
        if (fileStorageConfiguration != null) {
            request.setBucket(fileStorageConfiguration.getBucket());
        }
        request.setClientIp(clientIp);
        return request;
    }

    public record FileUploadMetadata(String fileName, Long fileSize, String fileSuffix, String contentType) {

        public static FileUploadMetadata from(MultipartFile file) {
            String fileName = file.getOriginalFilename();
            return new FileUploadMetadata(fileName, file.getSize(), FileUtil.getSuffix(fileName), file.getContentType());
        }
    }
}
