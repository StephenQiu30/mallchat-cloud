package com.stephen.cloud.api.file.model.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serial;
import java.io.Serializable;

/**
 * 文件信息视图
 *
 * @author StephenQiu30
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "文件信息视图")
public class FileVO implements Serializable {

    @Serial
    private static final long serialVersionUID = 1L;

    @Schema(description = "文件访问链接")
    private String url;

    @Schema(description = "文件对象Key")
    private String key;

    @Schema(description = "文件名")
    private String fileName;

    @Schema(description = "文件大小 (bytes)")
    private Long size;
}
