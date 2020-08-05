package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.Interceptor.LoginInterceptor;
import com.atguigu.gmall.cart.pojo.UserInfo;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import javax.servlet.http.HttpServletRequest;

@Controller
public class CartController {

    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request){
        //System.out.println(LoginInterceptor.userInfo);
//        System.out.println(request.getAttribute("userId"));
//        System.out.println(request.getAttribute("userKey"));
        UserInfo userInfo = LoginInterceptor.getUserInfo();
        System.out.println(userInfo);
        return "hello....";
    }
}
