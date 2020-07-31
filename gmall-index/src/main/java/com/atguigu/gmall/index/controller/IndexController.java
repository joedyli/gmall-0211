package com.atguigu.gmall.index.controller;

import com.atguigu.gmall.common.bean.ResponseVo;
import com.atguigu.gmall.index.service.IndexService;
import com.atguigu.gmall.pms.entity.CategoryEntity;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ResponseBody;

import java.util.List;

@Controller
public class IndexController {

    @Autowired
    private IndexService indexService;

    @GetMapping
    public String toIndex(Model model){
        // 查询一级分类
        List<CategoryEntity> categoryEntityList = this.indexService.queryLv1lCategories();
        model.addAttribute("categories", categoryEntityList);
        return "index";
    }

    @GetMapping("index/cates/{pid}")
    @ResponseBody
    public ResponseVo<List<CategoryEntity>> queryCategoriesWithSubByPid(@PathVariable("pid")Long pid){
        List<CategoryEntity> categoryEntities = this.indexService.queryCategoriesWithSubByPid(pid);
        return ResponseVo.ok(categoryEntities);
    }

    @GetMapping("index/testLock")
    @ResponseBody
    public ResponseVo<Object> testLock(){
        this.indexService.testLock();
        return ResponseVo.ok();
    }
}
