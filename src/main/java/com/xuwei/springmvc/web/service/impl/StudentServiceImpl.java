package com.xuwei.springmvc.web.service.impl;

import com.xuwei.springmvc.annotation.XwService;
import com.xuwei.springmvc.web.service.StudentService;

/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/09
 * @description: 实现类
 */
@XwService
public class StudentServiceImpl implements StudentService {

    @Override
    public String hello(String name) {
        return "my name is "+name;
    }

    @Override
    public String findStudent(String name) {
        return "controller封装resp:消息为--->"+name;
    }
}
