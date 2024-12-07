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

	@Override
	public Result sign() {
        //获取当前登录用户
       Long userId = UserHolder.getUser().getId();
        //获取时间
       LocalDateTime now = LocalDateTime.now();
        //处理一下时间格式
       String timePrefix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
       String key = USER_SIGN_KEY + userId + timePrefix;
        //获取今天是本月的第几天
       int dayOfMonth = now.getDayOfMonth();
       //写入redis  setbit key offset 1
        stringRedisTemplate.opsForValue().setBit(key, dayOfMonth - 1, true);
		return Result.ok();
	}

    @Override
    public Result signCount() {
        //获取当前登录用户
        Long userId = UserHolder.getUser().getId();
        //获取时间
        LocalDateTime now = LocalDateTime.now();
        //处理一下时间格式
        String timePrefix = now.format(DateTimeFormatter.ofPattern(":yyyyMM"));
        String key = USER_SIGN_KEY + userId + timePrefix;
        //获取今天是本月的第几天
        int dayOfMonth = now.getDayOfMonth();
        //截至本月所有的签到记录 返回的是一个十进制的数字 BITFIELD sign:5:202412 GET u14 0
        List<Long> result = stringRedisTemplate.opsForValue().bitField(
                key,
                BitFieldSubCommands.create()
                        .get(BitFieldSubCommands.BitFieldType.unsigned(dayOfMonth)).valueAt(0)
        );
       if(result == null || result.isEmpty()){
           return Result.ok(0);
       }
        Long num = result.get(0);
       if(num == null || num == 0){
           return Result.ok(0);
       }
       //循环遍历
       int count = 0;
       while(true){
           //这个数字与1 得到最后一个bit位置
           if((num & 1) == 0){
               break;
           }else{
               count++;
           }
           num >>= 1;
       }

        return Result.ok(count);
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
