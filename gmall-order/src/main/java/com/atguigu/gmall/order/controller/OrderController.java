package com.atguigu.gmall.order.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.oms.entity.OrderEntity;
import com.atguigu.gmall.oms.vo.OrderSubmitVo;
import com.atguigu.gmall.order.service.OrderService;
import com.atguigu.gmall.order.vo.OrderConfirmVo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class OrderController {

    @Autowired
    private OrderService orderService;


    @GetMapping("confirm")
    public String confirm(Model model){
        OrderConfirmVo confirmVo = this.orderService.confirm();
        model.addAttribute("confirmVo", confirmVo);
        return "trade";
    }

    @GetMapping("confirm2")
    @ResponseBody
    public ResponseVo<OrderConfirmVo> confirm2(Model model){
        OrderConfirmVo confirmVo = this.orderService.confirm();
        return ResponseVo.ok(confirmVo);
    }

    @PostMapping("submit")
    @ResponseBody
    public ResponseVo<Object> submit(@RequestBody OrderSubmitVo submitVo){

        OrderEntity orderEntity = this.orderService.submit(submitVo);
        return ResponseVo.ok(orderEntity.getOrderSn());
    }
}