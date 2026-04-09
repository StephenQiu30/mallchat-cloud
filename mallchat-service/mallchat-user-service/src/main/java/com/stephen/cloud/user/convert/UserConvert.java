package com.stephen.cloud.user.convert;

import cn.hutool.core.util.DesensitizedUtil;
import com.stephen.cloud.api.user.model.dto.UserAddRequest;
import com.stephen.cloud.api.user.model.dto.UserEditRequest;
import com.stephen.cloud.api.user.model.dto.UserUpdateRequest;
import com.stephen.cloud.api.user.model.vo.LoginUserVO;
import com.stephen.cloud.api.user.model.vo.UserVO;
import com.stephen.cloud.user.model.entity.User;
import org.springframework.beans.BeanUtils;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 用户转换器
 *
 * @author StephenQiu30
 */
public class UserConvert {

    /**
     * 对象转视图
     *
     * @param user 用户实体
     * @return 用户视图
     */
    public static UserVO objToVo(User user) {
        if (user == null) {
            return null;
        }
        UserVO userVO = new UserVO();
        // 批量属性拷贝，简化赋值操作
        BeanUtils.copyProperties(user, userVO);
        // 针对敏感字段手机号进行脱敏处理，返回中段脱敏后的字符串
        userVO.setUserPhone(DesensitizedUtil.mobilePhone(userVO.getUserPhone()));
        return userVO;
    }

    /**
     * 对象列表转视图列表
     *
     * @param userList 用户对象列表
     * @return 用户视图列表
     */
    public static List<UserVO> getUserVO(List<User> userList) {
        // 利用 Java 8 Stream 执行批量对象转换
        return userList.stream().map(UserConvert::objToVo).collect(Collectors.toList());
    }


    /**
     * 对象转登录视图
     *
     * @param user 用户实体
     * @return 登录用户视图
     */
    public static LoginUserVO objToLoginVo(User user) {
        if (user == null) {
            return null;
        }
        LoginUserVO loginUserVO = new LoginUserVO();
        // 拷贝用户基础信息至登录视图类
        BeanUtils.copyProperties(user, loginUserVO);
        return loginUserVO;
    }

    /**
     * 创建请求转实体对象
     *
     * @param userAddRequest 创建请求
     * @return 用户实体
     */
    public static User addRequestToObj(UserAddRequest userAddRequest) {
        if (userAddRequest == null) {
            return null;
        }
        User user = new User();
        // 将 API 请求参数映射至数据库实体
        BeanUtils.copyProperties(userAddRequest, user);
        return user;
    }

    /**
     * 更新请求转实体对象
     *
     * @param userUpdateRequest 更新请求
     * @return 用户实体
     */
    public static User updateRequestToObj(UserUpdateRequest userUpdateRequest) {
        if (userUpdateRequest == null) {
            return null;
        }
        User user = new User();
        // 将后台更新请求映射至实体，以便入库
        BeanUtils.copyProperties(userUpdateRequest, user);
        return user;
    }

    /**
     * 编辑请求转实体对象
     *
     * @param userEditRequest 编辑请求
     * @return 用户实体
     */
    public static User editRequestToObj(UserEditRequest userEditRequest) {
        if (userEditRequest == null) {
            return null;
        }
        User user = new User();
        // 处理用户自主编辑资料请求的对象转换
        BeanUtils.copyProperties(userEditRequest, user);
        return user;
    }
}
