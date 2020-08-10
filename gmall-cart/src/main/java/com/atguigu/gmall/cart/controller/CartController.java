package com.atguigu.gmall.cart.controller;

import com.atguigu.gmall.cart.pojo.Cart;
import com.atguigu.gmall.cart.service.CartService;
import com.atguigu.gmall.common.bean.ResponseVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.concurrent.ExecutionException;

@Controller
public class CartController {

    @Autowired
    private CartService cartService;

    @GetMapping
    public String addCart(Cart cart){
        if (cart == null || cart.getSkuId() == null){
            throw new RuntimeException("你没有选中的任何商品！！");
        }

        this.cartService.saveCart(cart);
        return "redirect:http://cart.gmall.com/addCart?skuId=" + cart.getSkuId();
    }

    @GetMapping("addCart")
    public String queryCartBySkuId(@RequestParam("skuId")Long skuId, Model model){
        Cart cart = this.cartService.queryCartBySkuId(skuId);
        model.addAttribute("cart", cart);
        return "addCart";
    }

    @GetMapping("cart.html")
    public String queryCarts(Model model){
        List<Cart> carts = this.cartService.queryCarts();
        model.addAttribute("carts", carts);
        return "cart";
    }

    @PostMapping("updateNum")
    @ResponseBody
    public ResponseVo<Object> updateNum(@RequestBody Cart cart){

        this.cartService.updateNum(cart);
        return ResponseVo.ok();
    }

    @PostMapping("deleteCart")
    @ResponseBody
    public ResponseVo<Object> deleteCart(@RequestParam("skuId")Long skuId){
        this.cartService.deleteCart(skuId);
        return ResponseVo.ok();
    }

    @GetMapping("user/{userId}")
    @ResponseBody
    public ResponseVo<List<Cart>> queryCheckedCartByUserId(@PathVariable("userId")Long userId){
        List<Cart> carts = this.cartService.queryCheckedCartByUserId(userId);
        return ResponseVo.ok(carts);
    }

    @GetMapping("test")
    @ResponseBody
    public String test(HttpServletRequest request) throws ExecutionException, InterruptedException {

        long now = System.currentTimeMillis();
        System.out.println("这是controller的test方法开始执行。。。。。。。。。");
        this.cartService.executor1();
        this.cartService.executor2();
//        this.cartService.executor1().addCallback(
//                t -> System.out.println("异步成功回调executor1：" + t),
//                ex -> System.out.println("异步失败回调executor1：" + ex.getMessage()));
//        this.cartService.executor2().addCallback(
//                t -> System.out.println("异步成功回调executor2：" + t),
//                ex -> System.out.println("异步失败回调executor2：" + ex.getMessage()));
//        System.out.println(future1.get());
//        System.out.println(future2.get());
        System.out.println("这是controller的test方法结束执行。。。。。。。。。" + (System.currentTimeMillis() - now));

        //System.out.println(LoginInterceptor.userInfo);
//        System.out.println(request.getAttribute("userId"));
//        System.out.println(request.getAttribute("userKey"));
//        UserInfo userInfo = LoginInterceptor.getUserInfo();
//        System.out.println(userInfo);
        return "hello....";
    }
}
