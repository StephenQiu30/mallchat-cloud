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
 * 关注处理器
 *
 * @author stephen
 **/
@Component
public class SubscribeHandler implements WxMpMessageHandler {

    @Resource
    private UserService userService;

    @Resource
    private CacheUtils cacheUtils;

    @Override
    public WxMpXmlOutMessage handle(WxMpXmlMessage wxMpXmlMessage, Map<String, Object> map,
                                    WxMpService wxMpService, WxSessionManager wxSessionManager) throws WxErrorException {
        String openId = wxMpXmlMessage.getFromUser();
        String eventKey = wxMpXmlMessage.getEventKey();

        // 1. 执行登录/注册逻辑
        LoginUserVO loginUserVO = userService.userLoginByWxOpenId(openId);

        // 2. 如果是通过扫码关注（扫码登录），则将登录结果存入 Redis
        if (eventKey != null && eventKey.startsWith("qrscene_")) {
            String sceneId = eventKey.replace("qrscene_", "");
            String redisKey = "user:login:wx:" + sceneId;
            cacheUtils.put(redisKey, loginUserVO, 60 * 5);
        }

        final String content = "感谢关注";
        return WxMpXmlOutMessage.TEXT().content(content)
                .fromUser(wxMpXmlMessage.getToUser())
                .toUser(wxMpXmlMessage.getFromUser())
                .build();
    }
}
