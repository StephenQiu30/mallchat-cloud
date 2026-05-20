package com.stephen.cloud.file.service.impl;

import com.qcloud.cos.COSClient;
import com.qcloud.cos.model.ObjectMetadata;
import com.qcloud.cos.model.PutObjectRequest;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileVO;
import com.stephen.cloud.file.config.FileStorageConfiguration;
import com.stephen.cloud.file.service.FileService;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.InputStream;
import java.util.UUID;

/**
 * COS 文件服务实现类
 *
 * @author StephenQiu30
 */
@Service
@Slf4j
public class CosFileServiceImpl implements FileService {

    @Resource
    private FileStorageConfiguration fileStorageConfiguration;

    @Resource
    private COSClient cosClient;

    @Resource
    private FileUploadValidator fileUploadValidator;

    @Override
    public FileVO uploadFile(MultipartFile multipartFile, FileUploadBizEnum bizTypeEnum) {
        FileUploadValidator.ValidatedFile validatedFile = fileUploadValidator.validate(multipartFile, bizTypeEnum);
        String fileName = validatedFile.fileName();
        String suffix = validatedFile.suffix();
        String key = String.format("%s/%s_%s.%s", bizTypeEnum.getCode(), UUID.randomUUID(), System.currentTimeMillis(), suffix);

        try (InputStream inputStream = multipartFile.getInputStream()) {
            ObjectMetadata objectMetadata = new ObjectMetadata();
            objectMetadata.setContentLength(validatedFile.size());
            objectMetadata.setContentType(validatedFile.contentType());

            PutObjectRequest putObjectRequest = new PutObjectRequest(fileStorageConfiguration.getBucket(), key, inputStream, objectMetadata);
            cosClient.putObject(putObjectRequest);

            String url = String.format("https://%s.cos.%s.myqcloud.com/%s",
                    fileStorageConfiguration.getBucket(), fileStorageConfiguration.getRegion(), key);

            return FileVO.builder()
                    .key(key)
                    .url(url)
                    .fileName(fileName)
                    .size(validatedFile.size())
                    .build();
        } catch (Exception e) {
            log.error("COS 文件上传失败, key: {}", key, e);
            throw new RuntimeException("文件上传失败");
        }
    }
}
