package com.atguigu.gmall.payment.Interceptor;

import com.atguigu.gmall.common.bean.UserInfo;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

@Component
public class LoginInterceptor implements HandlerInterceptor {

    //public static UserInfo userInfo;
    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    /**
     * 前置方法，在handler方法执行之前执行
     * 返回值：true-放行 false-被拦截
     * @param request
     * @param response
     * @param handler
     * @return
     * @throws Exception
     */
    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {

        String userId = request.getHeader("userId");
        if (StringUtils.isNotBlank(userId)){
            UserInfo userInfo = new UserInfo();
            userInfo.setUserId(Long.valueOf(userId));
            THREAD_LOCAL.set(userInfo);
        }

        return true;
    }

    public static UserInfo getUserInfo(){
        return THREAD_LOCAL.get();
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler, Exception ex) throws Exception {
        // 必须手动释放线程局部变量，否则会导致内存泄漏
        // 因为使用的时线程池，请求结束线程没有结束，导致内存无法自动释放
        THREAD_LOCAL.remove();
    }
}
