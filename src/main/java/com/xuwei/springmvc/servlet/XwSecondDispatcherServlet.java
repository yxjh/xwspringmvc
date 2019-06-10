package com.xuwei.springmvc.servlet;

import com.xuwei.springmvc.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * @program: xwspringmvc
 * @author: xuWei
 * @create: 2019/06/09
 * @description: 控制类升级版本
 */
public class XwSecondDispatcherServlet extends HttpServlet {

    //存储配置文件
    private Properties contextConfig=new Properties();
    //存储所有类路径
    private List<String> classNames=new ArrayList<String>();
    //ioc容器
    private Map<String,Object>ioc=new HashMap<>();
    //
    private List<HandlerMapping>handlerMapping=new ArrayList<>();

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
            resp.getWriter().write("500 Exception,Detail:"+ Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispather(HttpServletRequest req, HttpServletResponse resp) throws Exception{
        HandlerMapping handlerMapping=getHandler(req);
        if(handlerMapping==null){
            resp.getWriter().write("404");
            return;
        }
        System.out.println("---->"+handlerMapping);
        //获取方法的形参列表
        Class<?>[] paramTypes = handlerMapping.getParamTypes();
        Object []paramValues=new Object[paramTypes.length];
        Map<String,String[]> params = req.getParameterMap();
        for (Map.Entry<String, String[]> param : params.entrySet()) {
            String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]", "").replaceAll(",\\s", ",");
            //如果找到匹配的对象，则开始填充参数值
            if(!handlerMapping.paramIndexMapping.containsKey(param.getKey())){continue;}
            int index = handlerMapping.paramIndexMapping.get(param.getKey());
            paramValues[index] = convert(paramTypes[index],value);
        }

        //设置方法中的request和response对象
        if(handlerMapping.paramIndexMapping.containsKey(HttpServletRequest.class.getName())){
            int reqIndex = handlerMapping.paramIndexMapping.get(HttpServletRequest.class.getName());
            paramValues[reqIndex] = req;
        }
        if(handlerMapping.paramIndexMapping.containsKey(HttpServletResponse.class.getName())){
            int respIndex = handlerMapping.paramIndexMapping.get(HttpServletResponse.class.getName());
            paramValues[respIndex] = resp;
        }


        handlerMapping.method.invoke(handlerMapping.controller, paramValues);
    }

    /**
     * 获取对应的HandlerMapping
     * @param req
     * @return
     */
    private HandlerMapping getHandler(HttpServletRequest req) {
        if(handlerMapping.isEmpty()){return null;}
        String url = req.getRequestURI();
        String contextPath = req.getContextPath();
        url = url.replaceAll(contextPath,"").replaceAll("/+","/");
        for(HandlerMapping mapping:this.handlerMapping){
            if(mapping.getUrl().equals(url)){
                return mapping;
            }
        }
        return null;
    }

    //数据类型转换
    private Object convert(Class<?>type,String value){
        if(Integer.class==type){
            return Integer.valueOf(value);
        }
        return value;
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
               //简易版本操作 handlerMapping.put(url,method);
                this.handlerMapping.add(new HandlerMapping(url,entry.getValue(),method));
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
    //保存一个url和一个method的关系
    public class HandlerMapping{
        private String url;
        private Method method;
        private Object controller;
        //形参列表 名字作为key,位置作为值
        private Map<String,Integer>paramIndexMapping;
        private Class<?>[]paramTypes;

        public HandlerMapping(String url, Object controller, Method method) {
            this.url = url;
            this.method = method;
            this.controller = controller;
            this.paramTypes=method.getParameterTypes();
            paramIndexMapping=new HashMap<>();
            putParamIndexMapping(method);
        }

        private void putParamIndexMapping(Method method) {
            //提取方法中加了注解的参数，把方法上的注解拿到，拿到一个二维数组【一个字段可以有多个注解，一个方法又可以有多个字段】
            Annotation[] [] pa = method.getParameterAnnotations();
            for (int i = 0; i < pa.length ; i ++) {
                for(Annotation a : pa[i]){
                    if(a instanceof XwRequestParam){
                        String paramName = ((XwRequestParam) a).value();
                        if(!"".equals(paramName.trim())){
                            paramIndexMapping.put(paramName, i);
                        }
                    }
                }
            }
            //提取方法中的request和response参数
            Class<?> [] paramsTypes = method.getParameterTypes();
            for (int i = 0; i < paramsTypes.length ; i ++) {
                Class<?> type = paramsTypes[i];
                if(type == HttpServletRequest.class ||
                        type == HttpServletResponse.class){
                    paramIndexMapping.put(type.getName(),i);
                }
            }
        }

        public String getUrl() {
            return url;
        }

        public void setUrl(String url) {
            this.url = url;
        }

        public Method getMethod() {
            return method;
        }

        public void setMethod(Method method) {
            this.method = method;
        }

        public Object getController() {
            return controller;
        }

        public void setController(Object controller) {
            this.controller = controller;
        }

        public Class<?>[] getParamTypes() {
            return paramTypes;
        }
    }
}

