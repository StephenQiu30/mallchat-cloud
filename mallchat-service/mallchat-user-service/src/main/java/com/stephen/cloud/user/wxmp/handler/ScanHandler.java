package com.stephen.cloud.user.wxmp.handler;

import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.common.cache.utils.CacheUtils;
import com.stephen.cloud.user.service.UserService;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.error.WxErrorException;
import me.chanjar.weixin.common.session.WxSessionManager;
import me.chanjar.weixin.mp.api.WxMpMessageHandler;
import me.chanjar.weixin.mp.api.WxMpService;
import me.chanjar.weixin.mp.bean.message.WxMpXmlMessage;
import me.chanjar.weixin.mp.bean.message.WxMpXmlOutMessage;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 扫码处理器
 *
 * @author stephen
 **/
@Component
public class ScanHandler implements WxMpMessageHandler {

    @Resource
    private UserService userService;

    @Resource
    private CacheUtils cacheUtils;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map, WxMpService wxMpService,
                                    WxSessionManager wxSessionManager) throws WxErrorException {
        String openId = wxMpXmlMessage.getFromUser();
        String sceneId = wxMpXmlMessage.getEventKey();

        // 1. 执行扫码登录/关联逻辑
        LoginUserVO loginUserVO = userService.userLoginByWxOpenId(openId);

        // 2. 将登录结果存入 Redis，供前端轮询
        // key 为 user:login:wx:sceneId
        String redisKey = "user:login:wx:" + sceneId;
        cacheUtils.put(redisKey, loginUserVO, 60 * 5); // 5 分钟有效期

        // 3. 返回给用户扫码成功的提示
        String content = "扫码登录成功，正在为您跳转...";
        return WxMpXmlOutMessage.TEXT().content(content)
                .fromUser(wxMpXmlMessage.getToUser())
                .toUser(wxMpXmlMessage.getFromUser())
                .build();
    }
}
