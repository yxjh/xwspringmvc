package com.xuwei.springmvc.web.controller;

import com.xuwei.springmvc.annotation.XwAutowired;
import com.xuwei.springmvc.annotation.XwController;
import com.xuwei.springmvc.annotation.XwRequestMapping;
import com.xuwei.springmvc.annotation.XwRequestParam;
import com.xuwei.springmvc.web.service.StudentService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/09
 * @description: web测试类
 */
@XwController
@XwRequestMapping("/student")
public class StudentController {

    @XwAutowired
    public StudentService studentService;

    @XwRequestMapping("hello")
    public String hello(HttpServletRequest req, HttpServletResponse resp, @XwRequestParam("name")String name){
        String message=studentService.hello(name);
        try {
            resp.getWriter().write(message);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return null;
    }

    @XwRequestMapping("add")
    public void add(HttpServletRequest req, HttpServletResponse resp, @XwRequestParam("a")Integer a,@XwRequestParam("b")Integer b){
        try {
            resp.getWriter().write(a+"+"+b+"="+(a+b));
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @XwRequestMapping("findStudent")
    public String findStudent(@XwRequestParam("name")String name){
        return studentService.findStudent(name);
    }

    /**
     * 路径支持正则
     */
    @XwRequestMapping("regx.*")
    public String regx(@XwRequestParam("name")String name){
        return studentService.hello(name);
    }
}
