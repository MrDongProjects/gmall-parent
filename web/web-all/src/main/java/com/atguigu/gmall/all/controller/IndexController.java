package com.atguigu.gmall.all.controller;


import com.alibaba.fastjson.JSONObject;
import com.atguigu.gmall.common.result.Result;
import com.atguigu.gmall.product.client.ProductFeignClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ResponseBody;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import javax.servlet.http.HttpServletRequest;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;


@Controller
@SuppressWarnings("all")
public class IndexController {
    @Autowired
    private ProductFeignClient productFeignClient;

    @Autowired
    private TemplateEngine templateEngine;

    /**
     * 获取首页分类数据
     * @param request
     * @return
     */
    @GetMapping({"/","index.html"})
    public String index(HttpServletRequest request){
        List<JSONObject> baseCategoryList = productFeignClient.getBaseCategoryList();
        request.setAttribute("list",baseCategoryList);
        return "index/index";
    }

    @GetMapping("createIndex")
    @ResponseBody()
    public Result createIndex(){
        //  获取后台存储的数据
        List<JSONObject> baseCategoryList = productFeignClient.getBaseCategoryList();
        //  设置模板显示的内容
        Context context = new Context();
        context.setVariable("list",baseCategoryList);
        FileWriter fileWriter = null;
        try {
            fileWriter = new FileWriter("E:\\gmall\\index.html");
        } catch (IOException e) {
            e.printStackTrace();
        }

        templateEngine.process("index/index.html",context, fileWriter);
        return Result.ok();
    }


}
