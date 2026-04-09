package com.stephen.cloud.common.utils;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.io.InputStream;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * IP 工具类
 *
 * @author StephenQiu30
 */
@Slf4j
public class IpUtils {

    private static final String XDB_PATH = "/ip2region.xdb";
    private static byte[] cBuff;

    static {
        try (InputStream inputStream = IpUtils.class.getResourceAsStream(XDB_PATH)) {
            if (inputStream != null) {
                cBuff = inputStream.readAllBytes();
            } else {
                log.error("IP database file not found at {}", XDB_PATH);
            }
        } catch (Exception e) {
            log.error("Failed to load IP database: {}", e.getMessage());
        }
    }

    private IpUtils() {
    }

    /**
     * 获取客户端 IP
     *
     * @param request request
     * @return client ip
     */
    public static String getClientIp(HttpServletRequest request) {
        if (request == null) {
            return null;
        }

        // 1. 尝试从负载均衡常用的 X-Forwarded-For 头部获取 (通常包含代理链)
        String ip = request.getHeader("X-Forwarded-For");
        // 2. 如果为空或 unknown，则依次尝试其他常见的代理头
        if (StringUtils.isBlank(ip) || isUnknown(ip)) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || isUnknown(ip)) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (StringUtils.isBlank(ip) || isUnknown(ip)) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (StringUtils.isBlank(ip) || isUnknown(ip)) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (StringUtils.isBlank(ip) || isUnknown(ip)) {
            ip = request.getHeader("X-Real-IP");
        }
        // 3. 最终兜底方案：通过 getRemoteAddr 获取直连 IP
        if (StringUtils.isBlank(ip) || isUnknown(ip)) {
            ip = request.getRemoteAddr();
        }

        // 4. 特殊处理：如果是本地回环地址，则尝试解析服务器本机真实 IP
        if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) {
            try {
                InetAddress localhost = InetAddress.getLocalHost();
                ip = localhost.getHostAddress();
            } catch (UnknownHostException e) {
                log.warn("Failed to resolve localhost address: {}", e.getMessage());
            }
        }

        // 5. 处理代理链情况，提取第一个且有效的非 unknown IP
        return extractFirstIp(ip);
    }

    /**
     * 根据 IP 获取归属地解析器
     *
     * @param ip IP 地址
     * @return 归属地 (格式: 中国 广东省 广州市 电信)
     */
    public static String getRegion(String ip) {
        if (StringUtils.isBlank(ip) || cBuff == null) {
            return null;
        }
        try {
            org.lionsoul.ip2region.xdb.Searcher searcher = org.lionsoul.ip2region.xdb.Searcher.newWithBuffer(cBuff);
            String region = searcher.search(ip);
            searcher.close();
            return formatRegion(region);
        } catch (Exception e) {
            log.error("IP region search failed: {}", e.getMessage());
            return "未知地址";
        }
    }

    /**
     * 格式化归属地信息 (去除 0 和 |)
     */
    private static String formatRegion(String region) {
        if (StringUtils.isBlank(region)) {
            return region;
        }
        return Arrays.stream(region.split("\\|"))
                .filter(s -> !"0".equals(s))
                .collect(Collectors.joining(" "));
    }

    private static String extractFirstIp(String headerValue) {
        if (StringUtils.isBlank(headerValue)) {
            return null;
        }
        String first = headerValue.split(",")[0].trim();
        return isUnknown(first) ? null : first;
    }

    private static boolean isUnknown(String value) {
        return "unknown".equalsIgnoreCase(value);
    }
}
