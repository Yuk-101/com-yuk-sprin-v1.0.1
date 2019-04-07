package com.yuk.mvcFramework.v1.servlet;

import com.yuk.mvcFramework.annotation.*;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.net.URL;
import java.util.*;

/**
 * Created by yuk on 2019/3/31.
 */
public class YukDispatchServlet extends HttpServlet{

    //保存application.properties 配置的文件内容
    private Properties properties = new Properties();

    //保存扫描到的所有类名
    private List<String> classNames = new ArrayList<>();

    //IOC容器实际就是一个map集合
    private Map<String,Object> ioc = new HashMap<>();

    //保存url和method的对应关系
    private Map<String,Method> handlerMapping = new HashMap<>();

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {
        this.doPost(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp) throws ServletException, IOException {

        //6、调试阶段
        try {
            doDispatcher(req,resp);
        }catch (Exception e){
            e.printStackTrace();
            resp.getWriter().write("500 Exception,Detail :" + Arrays.toString(e.getStackTrace()));
        }
    }

    private void doDispatcher(HttpServletRequest req, HttpServletResponse resp) throws IOException, InvocationTargetException, IllegalAccessException {

        //绝对路径
        String url = req.getRequestURI();
        //处理成相对路径
        String contestPath = req.getContextPath();
        url = url.replaceAll(contestPath, "").replaceAll("/+", "/");

        if(!handlerMapping.containsKey(url)){
            resp.getWriter().write("404 Not Found !");
            return;
        }

        Method method = this.handlerMapping.get(url);
        //从Request获取传过来的参数
        Map<String,String []> params = req.getParameterMap();

        //获取方法的形参列表
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] paramValues = new Object[parameterTypes.length];

        for(int i=0;i<parameterTypes.length;i++){
            Class parameterType = parameterTypes[i];
            //不能用instanceof,parameterType不是实参而是形参
            if(parameterType == HttpServletRequest.class){
                paramValues[i] = req;
                continue;
            }else if(parameterType == HttpServletResponse.class){
                paramValues[i] = resp;
                continue;
            }else if(parameterType == String.class){
                YukRequestParam yukRequestParam = (YukRequestParam) parameterType.getAnnotation(YukRequestParam.class);
                if(params.containsKey(yukRequestParam.value())){
                    for (Map.Entry<String,String []> param : params.entrySet()) {
                        String value = Arrays.toString(param.getValue()).replaceAll("\\[|\\]","").replaceAll("\\s",",");
                        paramValues [i] = value;
                    }
                }
            }

        }
        //通过反射拿到method所在的class，拿到class 后还要拿到class名称  ，再调用ToLowerFirstCase获得beanName
        String beanName = toLowerFirstCase(method.getDeclaringClass().getSimpleName());
        method.invoke(ioc.get(beanName),paramValues);
    }

    //url传过来的参数都是String 类型的，HTTP是基于字符串协议
    //将String 转化为任意类型
    private Object convert(Class<?> type,String value){
        //如果是int
        if(Integer.class == type){
            return Integer.valueOf(value);
        }
        //如果还double 类型 继续加if 这是可以用策略模式
        return value;
    }


    //初始化阶段
    @Override
    public void init(ServletConfig config) throws ServletException {

        //1、加载配置文件
        doLoadConfig(config.getInitParameter("contextConfigLocation"));

        //2、扫描相关类
        doScanner(properties.getProperty("scanPackage"));

        //3、初始化扫描到的类，并将他们放入IOC容器中
        doInstance();

        //4、完成依赖注入
        doAutowired();

        //5、初始化HandlerMapping
        initHandlerMapping();

        System.out.println("GP Spring framework is init.");
    }

    //初始化url和Method的一对一对应关系
    private void initHandlerMapping() {

        if(ioc.isEmpty()){return;}

        for(Map.Entry<String,Object> entry : ioc.entrySet()){
            Class<?> clazz = entry.getValue().getClass();

            if(!clazz.isAnnotationPresent(YukController.class)){continue;}

            //保存写在类上面的@YukRequestMapping("/demo")
            String baseUrl = "";
            if(clazz.isAnnotationPresent(YukRequestMapping.class)){
                YukRequestMapping yukRequestMapping = clazz.getAnnotation(YukRequestMapping.class);
                baseUrl = yukRequestMapping.value();
            }

            //默认获取所有的public 方法
            for(Method method : clazz.getMethods()){
                if(!method.isAnnotationPresent(YukRequestMapping.class)){continue;}

                YukRequestMapping yukRequestMapping = method.getAnnotation(YukRequestMapping.class);

                String url = (baseUrl + yukRequestMapping.value().replaceAll("/+","/"));
                handlerMapping.put(url,method);
                System.out.println("Mapped  :" + url +"," + method);
            }


        }
    }

    private void doAutowired() {
        //自动依赖注入
        if(ioc.isEmpty()){return;}

        for(Map.Entry<String,Object> entry : ioc.entrySet() ){
            //Declared 所有的 ，特定的字段，包括 private 、protected 、default
            //正常来说普通的oop 只能拿到public 属性
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for(Field field : fields){
                if(!field.isAnnotationPresent(YukAutowired.class)){continue;}
                YukAutowired yukAutowired = field.getAnnotation(YukAutowired.class);

                //如果用户没有自定义beanName，默认就根据类型注入
                //这个地方省去了对类名首字母小写的情况的判断
                String beanName = yukAutowired.value().trim();
                if("".equals(beanName)){
                    //获得接口的类型，作为key待会儿根据这个key到ioc容器中去取值
                    beanName = field.getType().getName();
                }

                //如果是public 以外的修饰符，只要加了@Autowired注解，都要强制赋值
                //反射中叫做暴力访问，俗称 强吻
                field.setAccessible(true);

                try {
                    field.set(entry.getValue(),ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }

        }
    }

    private void doInstance() {
        //初始化，为DI做准备
        if(classNames.isEmpty()){return;}

        try{
            for(String className : classNames){
                Class<?> clazz = Class.forName(className);

                //什么样的类需要初始化？
                //加了注解的类才需要初始化，怎样判断？
                if(clazz.isAnnotationPresent(YukController.class)){
                    Object instance = clazz.newInstance();
                    //Spring 默认类名首字母小写
                    String beanName = toLowerFirstCase(clazz.getSimpleName());
                    ioc.put(beanName,instance);
                }else if(clazz.isAnnotationPresent(YukService.class)){
                    //1、自定义的beanName
                    YukService yukService = clazz.getAnnotation(YukService.class);
                    String beanName = yukService.value();
                    //2、默认类名首字母小写
                    if("".equals(beanName.trim())){
                        beanName = toLowerFirstCase(clazz.getSimpleName());
                    }

                    Object instance = clazz.newInstance();
                    ioc.put(beanName,instance);

                    //3、根据类型自动复制
                    for(Class<?> i : clazz.getInterfaces()){
                        if(ioc.containsKey(i.getName())){
                            throw new Exception("The “" + i.getName() + "” is exists !" );
                        }
                        //把接口类型直接当成key了
                        ioc.put(i.getName(),instance);
                    }
                }else{
                    continue;
                }
            }
        }catch (Exception e){
            e.printStackTrace();
        }
    }

    //如果类名本身是小写字母，确实会出问题
    //默认传入的值，存在首字母小写的情况，也不可能出现非字母的情况
    private String toLowerFirstCase(String simpleName) {
        char[] chars = simpleName.toCharArray();

        //之所以加，是因为大小写字母的ASCII码相差32，
        // 而且大写字母的ASCII码要小于小写字母的ASCII码
        //在Java中，对char做算学运算，实际上就是对ASCII码做算学运算
        chars[0] += 32;
        return String.valueOf(chars);
    }

    //扫描出相关的类
    private void doScanner(String scanPackage) {
        //scanPackage = com.yuk.springv101 , 存储的是包路径
        //转换为文件路径，实际就是将 . 替换为 / 就好了
        //classPath
        //获取文件url路径
        URL url = this.getClass().getClassLoader().getResource("/" + scanPackage.replaceAll("\\.","/"));
        //获取里面所有的文件路径
        File classPath = new File(url.getFile());
        for(File file : classPath.listFiles()){
            if(file.isDirectory()){
                //如果还是文件夹那就递归获取
                doScanner(scanPackage + "." + file.getName());
            }else{
                if(!file.getName().endsWith(".class")){continue;}
                //w为什么是要.class文件而不是.java 文件
                String className = scanPackage + "." + file.getName().replace(".class","");
                classNames.add(className);
            }
        }
    }

    private void doLoadConfig(String contextConfigLocation) {
        //直接从类路径下找到Spring主配置文件所在路径,并得到输入流
        //并且将其读取出来放入properties对象中
        //相当于Scanpakage = com.yuk.springv101
        InputStream input = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(input);
        } catch (IOException e) {
            e.printStackTrace();
        }finally {
            if(input != null){
                try {
                    input.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }

    }

  }
