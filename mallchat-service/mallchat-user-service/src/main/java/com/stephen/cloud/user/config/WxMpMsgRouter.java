package com.stephen.cloud.user.config;

import com.stephen.cloud.user.constant.WxMpConstant;
import com.stephen.cloud.user.wxmp.handler.EventHandler;
import com.stephen.cloud.user.wxmp.handler.MessageHandler;
import com.stephen.cloud.user.wxmp.handler.ScanHandler;
import com.stephen.cloud.user.wxmp.handler.SubscribeHandler;
import jakarta.annotation.Resource;
import me.chanjar.weixin.common.api.WxConsts.EventType;
import me.chanjar.weixin.common.api.WxConsts.XmlMsgType;
import me.chanjar.weixin.mp.api.WxMpMessageRouter;
import me.chanjar.weixin.mp.api.WxMpService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * 微信公众号路由配置
 *
 * @author stephen
 */
@Configuration
public class WxMpMsgRouter {

    @Resource
    private WxMpService wxMpService;

    @Resource
    private EventHandler eventHandler;

    @Resource
    private MessageHandler messageHandler;

    @Resource
    private SubscribeHandler subscribeHandler;

    @Resource
    private ScanHandler scanHandler;

    @Bean(name = "wxMpMessageRouter")
    public WxMpMessageRouter wxMpMessageRouter() {
        WxMpMessageRouter router = new WxMpMessageRouter(wxMpService);
        // 消息
        router.rule()
                .async(false)
                .msgType(XmlMsgType.TEXT)
                .handler(messageHandler)
                .end();
        // 关注
        router.rule()
                .async(false)
                .msgType(XmlMsgType.EVENT)
                .event(EventType.SUBSCRIBE)
                .handler(subscribeHandler)
                .end();
        // 点击按钮
        router.rule()
                .async(false)
                .msgType(XmlMsgType.EVENT)
                .event(EventType.CLICK)
                .eventKey(WxMpConstant.CLICK_MENU_KEY)
                .handler(eventHandler)
                .end();
        // 扫码
        router.rule()
                .async(false)
                .msgType(XmlMsgType.EVENT)
                .event(EventType.SCAN)
                .handler(scanHandler)
                .end();
        return router;
    }
}
