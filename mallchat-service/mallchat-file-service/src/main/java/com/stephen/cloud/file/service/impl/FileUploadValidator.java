package com.stephen.cloud.file.service.impl;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.util.StrUtil;
import com.stephen.cloud.api.file.model.enums.FileUploadBizEnum;
import com.stephen.cloud.common.common.ErrorCode;
import com.stephen.cloud.common.exception.BusinessException;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import java.util.Set;

/**
 * 文件上传安全边界校验。
 */
@Component
public class FileUploadValidator {

    private static final long MB = 1024L * 1024L;
    private static final int MAX_FILE_NAME_LENGTH = 128;

    private static final Set<String> IMAGE_SUFFIXES = Set.of("jpg", "jpeg", "png", "webp", "gif");
    private static final Set<String> IMAGE_CONTENT_TYPES = Set.of("image/jpeg", "image/png", "image/webp", "image/gif");
    private static final Set<String> CHAT_FILE_SUFFIXES = Set.of("pdf", "txt", "doc", "docx", "xls", "xlsx", "ppt", "pptx", "zip");
    private static final Set<String> CHAT_FILE_CONTENT_TYPES = Set.of(
            "application/pdf",
            "text/plain",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
            "application/vnd.ms-excel",
            "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
            "application/vnd.ms-powerpoint",
            "application/vnd.openxmlformats-officedocument.presentationml.presentation",
            "application/zip");
    private static final Set<String> VOICE_SUFFIXES = Set.of("mp3", "m4a", "aac", "amr", "ogg");
    private static final Set<String> VOICE_CONTENT_TYPES = Set.of(
            "audio/mpeg",
            "audio/mp4",
            "audio/aac",
            "audio/amr",
            "audio/ogg",
            "application/ogg");
    private static final Set<String> VIDEO_SUFFIXES = Set.of("mp4", "webm", "mov");
    private static final Set<String> VIDEO_CONTENT_TYPES = Set.of("video/mp4", "video/webm", "video/quicktime");
    private static final Map<FileUploadBizEnum, Long> MAX_SIZE_MAP = Map.of(
            FileUploadBizEnum.USER_AVATAR, 5 * MB,
            FileUploadBizEnum.CHAT_IMAGE, 10 * MB,
            FileUploadBizEnum.CHAT_FILE, 10 * MB,
            FileUploadBizEnum.CHAT_VOICE, 20 * MB,
            FileUploadBizEnum.CHAT_VIDEO, 100 * MB);

    public ValidatedFile validate(MultipartFile file, FileUploadBizEnum bizTypeEnum) {
        if (file == null || bizTypeEnum == null) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件参数错误");
        }
        String fileName = file.getOriginalFilename();
        validateFileName(fileName);
        long size = file.getSize();
        if (size <= 0) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件不能为空");
        }
        if (size > MAX_SIZE_MAP.getOrDefault(bizTypeEnum, 10 * MB)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件大小超出限制");
        }

        String suffix = normalize(FileUtil.getSuffix(fileName));
        if (StrUtil.isBlank(suffix)) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件后缀不能为空");
        }
        String contentType = normalize(file.getContentType());
        validateBusinessType(file, bizTypeEnum, suffix, contentType);
        return new ValidatedFile(fileName, suffix, contentType, size);
    }

    private void validateBusinessType(MultipartFile file, FileUploadBizEnum bizTypeEnum, String suffix, String contentType) {
        if (FileUploadBizEnum.USER_AVATAR.equals(bizTypeEnum) || FileUploadBizEnum.CHAT_IMAGE.equals(bizTypeEnum)) {
            if (!IMAGE_SUFFIXES.contains(suffix) || !IMAGE_CONTENT_TYPES.contains(contentType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片类型不支持");
            }
            if (!hasImageMagic(file, suffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "图片内容不合法");
            }
            return;
        }
        if (FileUploadBizEnum.CHAT_FILE.equals(bizTypeEnum)) {
            if (!CHAT_FILE_SUFFIXES.contains(suffix) || !CHAT_FILE_CONTENT_TYPES.contains(contentType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件类型不支持");
            }
            if (!hasChatFileMagic(file, suffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件内容不合法");
            }
            return;
        }
        if (FileUploadBizEnum.CHAT_VOICE.equals(bizTypeEnum)) {
            if (!VOICE_SUFFIXES.contains(suffix) || !VOICE_CONTENT_TYPES.contains(contentType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "语音类型不支持");
            }
            if (!hasVoiceMagic(file, suffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "语音内容不合法");
            }
            return;
        }
        if (FileUploadBizEnum.CHAT_VIDEO.equals(bizTypeEnum)) {
            if (!VIDEO_SUFFIXES.contains(suffix) || !VIDEO_CONTENT_TYPES.contains(contentType)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "视频类型不支持");
            }
            if (!hasVideoMagic(file, suffix)) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "视频内容不合法");
            }
            return;
        }
        throw new BusinessException(ErrorCode.PARAMS_ERROR, "业务类型错误");
    }

    private void validateFileName(String fileName) {
        if (StrUtil.isBlank(fileName) || fileName.length() > MAX_FILE_NAME_LENGTH) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不合法");
        }
        if (fileName.contains("/") || fileName.contains("\\") || fileName.contains("..")) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不合法");
        }
        for (int i = 0; i < fileName.length(); i++) {
            if (Character.isISOControl(fileName.charAt(i))) {
                throw new BusinessException(ErrorCode.PARAMS_ERROR, "文件名不合法");
            }
        }
    }

    private boolean hasImageMagic(MultipartFile file, String suffix) {
        byte[] header = new byte[12];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "读取文件失败");
        }
        if (read < 4) {
            return false;
        }
        return switch (suffix) {
            case "png" -> (header[0] & 0xFF) == 0x89 && header[1] == 0x50 && header[2] == 0x4E && header[3] == 0x47;
            case "jpg", "jpeg" -> (header[0] & 0xFF) == 0xFF && (header[1] & 0xFF) == 0xD8 && (header[2] & 0xFF) == 0xFF;
            case "gif" -> header[0] == 'G' && header[1] == 'I' && header[2] == 'F';
            case "webp" -> read >= 12 && header[0] == 'R' && header[1] == 'I' && header[2] == 'F' && header[3] == 'F'
                    && header[8] == 'W' && header[9] == 'E' && header[10] == 'B' && header[11] == 'P';
            default -> false;
        };
    }

    private boolean hasChatFileMagic(MultipartFile file, String suffix) {
        if ("txt".equals(suffix)) {
            return true;
        }
        byte[] header = new byte[8];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "读取文件失败");
        }
        if (read < 4) {
            return false;
        }
        boolean zip = header[0] == 'P' && header[1] == 'K'
                && ((header[2] == 0x03 && header[3] == 0x04)
                || (header[2] == 0x05 && header[3] == 0x06)
                || (header[2] == 0x07 && header[3] == 0x08));
        boolean office = (header[0] & 0xFF) == 0xD0 && (header[1] & 0xFF) == 0xCF
                && (header[2] & 0xFF) == 0x11 && (header[3] & 0xFF) == 0xE0;
        return switch (suffix) {
            case "pdf" -> header[0] == '%' && header[1] == 'P' && header[2] == 'D' && header[3] == 'F';
            case "zip", "docx", "xlsx", "pptx" -> zip;
            case "doc", "xls", "ppt" -> office;
            default -> false;
        };
    }

    private boolean hasVoiceMagic(MultipartFile file, String suffix) {
        byte[] header = readHeader(file, 12);
        if (header.length < 4) {
            return false;
        }
        return switch (suffix) {
            case "mp3" -> (header[0] == 'I' && header[1] == 'D' && header[2] == '3')
                    || ((header[0] & 0xFF) == 0xFF && (header[1] & 0xE0) == 0xE0);
            case "m4a" -> hasIsoBaseMediaMagic(header);
            case "aac" -> (header[0] & 0xFF) == 0xFF && ((header[1] & 0xF6) == 0xF0);
            case "amr" -> header[0] == '#' && header[1] == '!' && header[2] == 'A' && header[3] == 'M';
            case "ogg" -> header[0] == 'O' && header[1] == 'g' && header[2] == 'g' && header[3] == 'S';
            default -> false;
        };
    }

    private boolean hasVideoMagic(MultipartFile file, String suffix) {
        byte[] header = readHeader(file, 12);
        if (header.length < 4) {
            return false;
        }
        return switch (suffix) {
            case "mp4", "mov" -> hasIsoBaseMediaMagic(header);
            case "webm" -> (header[0] & 0xFF) == 0x1A && header[1] == 0x45 && (header[2] & 0xFF) == 0xDF
                    && (header[3] & 0xFF) == 0xA3;
            default -> false;
        };
    }

    private byte[] readHeader(MultipartFile file, int length) {
        byte[] header = new byte[length];
        int read;
        try (InputStream inputStream = file.getInputStream()) {
            read = inputStream.read(header);
        } catch (IOException e) {
            throw new BusinessException(ErrorCode.PARAMS_ERROR, "读取文件失败");
        }
        if (read <= 0) {
            return new byte[0];
        }
        if (read == header.length) {
            return header;
        }
        return java.util.Arrays.copyOf(header, read);
    }

    private boolean hasIsoBaseMediaMagic(byte[] header) {
        return header.length >= 8 && header[4] == 'f' && header[5] == 't' && header[6] == 'y' && header[7] == 'p';
    }

    private String normalize(String value) {
        return StrUtil.blankToDefault(value, "").toLowerCase();
    }

    public record ValidatedFile(String fileName, String suffix, String contentType, long size) {
    }
}
