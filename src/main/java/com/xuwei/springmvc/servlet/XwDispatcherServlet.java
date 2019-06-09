package com.xuwei.springmvc.servlet;

import com.xuwei.springmvc.annotation.XwAutowired;
import com.xuwei.springmvc.annotation.XwController;
import com.xuwei.springmvc.annotation.XwRequestMapping;
import com.xuwei.springmvc.annotation.XwService;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/09
 * @description: 自定义的控制转发器
 */
public class XwDispatcherServlet extends HttpServlet {

    private Properties contextConfig=new Properties();

    private List<String> classNames=new ArrayList<String>();
    //ioc容器
    private Map<String,Object>ioc=new HashMap<>();
    //url---method
    private Map<String,Method>handlerMapping=new HashMap<>();
    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req,resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        //运行阶段，执行
        try {
            doDispather(req,resp);
        } catch (Exception e) {
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Detail:"+Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispather(HttpServletRequest req, HttpServletResponse resp) throws Exception {
                String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");

        if(!this.handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found!!");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //第一个参数：方法所在的实例
        //第二个参数：调用时所需要的实参

        Map<String,String[]> params = req.getParameterMap();

        //投机取巧的方式
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        String name = params.get("name")[0];
        method.invoke(ioc.get(beanName),new Object[]{req,resp,name});
    }

    @Override
    public void init(ServletConfig config) throws ServletException {
        //1加载配置文件
        doLoadConnfig(config.getInitParameter("contextConfigLocation"));
        //2扫描相关的类
        doScanner(contextConfig.getProperty("scanPackage"));
        //3初始化扫描到的类，并将他们放入到IOC容器之中
        doInstance();
        //4完成依赖注入
        doAutoWired();
        //5初始化HandlerMappering
        initHandlerMappering();
        System.out.println("初始化完成");
    }

    //路径映射
    private void initHandlerMappering() {
        if(ioc.isEmpty()){return;}
        for(Map.Entry<String,Object>entry:ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();
            if(!clazz.isAnnotationPresent(XwController.class)){continue;}
            String baseurl="";
            if(clazz.isAnnotationPresent(XwRequestMapping.class)){
                XwRequestMapping requestMapping = clazz.getAnnotation(XwRequestMapping.class);
                baseurl=requestMapping.value();
            }
            //默认获取所有的public方法
            Method[] methods = clazz.getMethods();
            for (Method method:methods){
                if(!method.isAnnotationPresent(XwRequestMapping.class)){continue;}
                XwRequestMapping methodAnnotation = method.getAnnotation(XwRequestMapping.class);
                String url = ("/" + baseurl + "/" + methodAnnotation.value())
                        .replaceAll("/+", "/");
                //声明一个HandlerMappeing
                handlerMapping.put(url,method);
                System.out.println("Mapped:"+url+"---"+method);
            }
        }
    }

    //自动依赖注入
    private void doAutoWired() {
        if(ioc.isEmpty()){return;}
        for(Map.Entry<String,Object>entry:ioc.entrySet()){
            Field[] declaredFields = entry.getValue().getClass().getDeclaredFields();
            for(Field field:declaredFields){
                if(!field.isAnnotationPresent(XwAutowired.class)){continue;}
                XwAutowired  xwAutowired= field.getAnnotation(XwAutowired.class);
                String beanName=xwAutowired.value().trim();
                if("".equals(beanName)){
                    beanName=field.getType().getName();
                }
                //如果是public以外的修饰符，只要加了该注解，都要强制赋值
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                }catch (Exception e){
                    e.printStackTrace();
                }
            }


        }
    }

    private void doInstance(){
        if(classNames.isEmpty()){return;}
        try {
            for(String className:classNames){
                Class<?>clazz=Class.forName(className);
                //加了注解的类，进行初始化
                if(clazz.isAnnotationPresent(XwController.class)){
                    Object instance=clazz.newInstance();
                    //key-->className首字母小写
                    String beanName=toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,instance);
                }else if(clazz.isAnnotationPresent(XwService.class)){
                    //默认类名字母小写
                    String beanName=toLowerFirstCase(clazz.getSimpleName());
                    //自定义的beanname
                    XwService xwService = clazz.getAnnotation(XwService.class);
                    if(!"".equals(xwService.value())){
                        beanName=xwService.value();
                    }
                    Object instance=clazz.newInstance();
                    ioc.put(beanName,instance);
                    //根据类型自动赋值
                    for(Class<?> i:clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("the "+i.getName()+ "is exists!!!");
                        }
                        ioc.put(i.getName(),instance);
                    }
                }else {continue;}
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }



    //扫描出相关的类
    private void doScanner(String scanPackage) {
        URL url= this.getClass().getClassLoader().getResource("/"+scanPackage.replaceAll("\\.","/"));
        File classPath=new File(url.getFile());
        for (File file:classPath.listFiles()){
            if(file.isDirectory()){
                doScanner(scanPackage+"."+file.getName());
            }else {
                if(!file.getName().endsWith(".class")){
                    continue;
                }
                String className=(scanPackage+"."+file.getName().replaceAll(".class",""));
                classNames.add(className);
            }
        }
    }

    //加载配置文件
    private void doLoadConnfig(String contextConfigLocation) {
        InputStream fis=null;
        fis = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            contextConfig.load(fis);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(null!=fis){
                try {
                    fis.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    //首字母小写
    private String toLowerFirstCase(String simpleName) {
        char[]chars=simpleName.toCharArray();
        chars[0]+=32;
        return String.valueOf(chars);
    }
}
