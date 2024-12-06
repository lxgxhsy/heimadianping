package com.example.heimadianping.service.impl;

import cn.hutool.core.bean.BeanUtil;
import cn.hutool.core.bean.copier.CopyOptions;
import cn.hutool.core.lang.UUID;
import cn.hutool.core.util.RandomUtil;
import cn.hutool.jwt.JWT;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.heimadianping.dto.LoginFormDTO;
import com.example.heimadianping.dto.Result;
import com.example.heimadianping.dto.UserDTO;
import com.example.heimadianping.entity.User;
import com.example.heimadianping.mapper.UserMapper;
import com.example.heimadianping.service.IUserService;
import com.example.heimadianping.utils.RegexUtils;
import com.example.heimadianping.utils.UserHolder;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cglib.beans.BeanMap;
import org.springframework.data.redis.connection.BitFieldSubCommands;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.servlet.http.HttpSession;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static com.example.heimadianping.utils.RedisConstants.*;
import static com.example.heimadianping.utils.SystemConstants.USER_NICK_NAME_PREFIX;

/**
 * <p>
 * 服务实现类
 * </p>
 *
 * @author sy
 * @since 2021-12-22
 */
@Slf4j
@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements IUserService {

    @Resource
    private StringRedisTemplate stringRedisTemplate;


    @Override
    public Result sendcode(String phone, HttpSession session) {
        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(phone)) {
            //2.如果不符合，返回报错信息
          return Result.fail("手机号格式错误");
        }

        //3.符合，生成验证码
        String code = RandomUtil.randomNumbers(6);

        //4.保存验证码先到session
//        session.setAttribute("code",code);
        //存到redis里面而已 没什么大惊小怪的
        stringRedisTemplate.opsForValue().set(LOGIN_CODE_KEY + phone,code,LOGIN_CODE_TTL, TimeUnit.MINUTES);

        //5.发送验证码
        log.info("发送短信验证码成功，验证码：{}",code);
        return Result.ok(code);
    }

    @Override
    public Result login(LoginFormDTO loginForm, HttpSession session) {

        String phone = loginForm.getPhone();
        String code = loginForm.getCode();

        //1.校验手机号
        if(RegexUtils.isPhoneInvalid(loginForm.getPhone())) {

                return Result.fail("手机号格式错误");
        }
        String codecache =  stringRedisTemplate.opsForValue().get(LOGIN_CODE_KEY + phone);

        //2.校验验证码
        if(codecache == null || !codecache.toString().equals(code)){
            //3.不一致，报错
            return Result.fail("验证码错误");
        }


        //4.一致 是否存在
        User user = query().eq("phone", phone).one();
        //5.存在，直接登录，返回用户信息
        if(user == null){
            //6.不存在，创建新用户
            user = createUserWithPhone(phone);
        }
        //6.
        UserDTO userDTO = BeanUtil.copyProperties(user, UserDTO.class);
        //保存用户到redis
        // 7.保存用户信息到 redis中
        // 7.1.随机生成token，作为登录令牌
        String token = UUID.randomUUID().toString(true);
        Map<String, Object> userMap = BeanUtil.beanToMap(userDTO, new HashMap<>(),
                CopyOptions.create()
                        .setIgnoreNullValue(true)
                        .setFieldValueEditor((fieldName, fieldValue) -> fieldValue.toString()));

        String tokenKey = LOGIN_USER_KEY + token;
        stringRedisTemplate.opsForHash().putAll(tokenKey, userMap);
        //设置token有效期
        stringRedisTemplate.expire(tokenKey, LOGIN_USER_TTL, TimeUnit.MINUTES);
        return Result.ok(token);
    }

    private User createUserWithPhone(String phone) {
        User user = new User();
        user.setPhone(phone);
        user.setNickName(USER_NICK_NAME_PREFIX + RandomUtil.randomString(10));
        //保存用户
        save(user);
        return user;
    }
}
