package com.stephen.cloud.file.service;

import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.api.file.model.vo.FileVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务接口
 *
 * @author StephenQiu30
 */
public interface FileService {

    /**
     * 上传文件
     *
     * @param multipartFile 文件
     * @param bizTypeEnum   业务类型
     * @return 文件信息
     */
    FileVO uploadFile(MultipartFile multipartFile, FileUploadBizEnum bizTypeEnum);
}
