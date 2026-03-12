package com.stephen.cloud.api.file.client;

import com.stephen.cloud.api.file.model.vo.FileVO;
import com.stephen.cloud.common.common.BaseResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.multipart.MultipartFile;

/**
 * 文件服务 Feign 客户端
 *
 * @author StephenQiu30
 */
@FeignClient(name = "mallchat-file-service", path = "/api/file")
public interface FileFeignClient {

    /**
     * 上传文件
     *
     * @param file    文件
     * @param bizType 业务类型
     * @return 文件信息
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    BaseResponse<FileVO> uploadFile(@RequestPart("file") MultipartFile file, @RequestParam("bizType") String bizType);
}
