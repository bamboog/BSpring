package com.ex.bspring;

import com.ex.bspring.annotation.BController;
import com.ex.bspring.annotation.BRequestMapping;
import com.ex.bspring.annotation.BResource;
import com.ex.bspring.annotation.BService;

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
 * Created by IntelliJ IDEA.
 *
 * @author gangwen.xu
 * Date  : 2018/11/15
 * Time  : 下午5:37
 * 类描述 :
 */
public class BDispatcherServlet extends HttpServlet {
    private Properties properties = new Properties();
    private List<String> calassNames = new ArrayList<>();
    private Map<String, Object> ioc = new HashMap<>();
    private Map<String, Method> handlerMap = new HashMap<>();

    @Override
    public void init(ServletConfig config) throws ServletException {
        //加载配置文件
        doLoadConf(config.getInitParameter("contextConfigLocation"));
        // 扫描类
        doScanPackage(properties.getProperty("sanPackage"));
        // 初始化实例
        doInstance();
        // 注入
        doResourced();
        // 构造handlerMap
        initHandlerMap();
        // 匹配uri 定位方法  回调
    }

    private void initHandlerMap() {
        for (Map.Entry entry : ioc.entrySet()) {
            Class<?> classe = entry.getValue().getClass();
            if (!classe.isAnnotationPresent(BController.class)) {
                continue;
            }
            String baseUrl = "";
            if (classe.isAnnotationPresent(BRequestMapping.class)) {
                BRequestMapping requerstMapping = classe.getAnnotation(BRequestMapping.class);
                baseUrl = requerstMapping.value();
            }
            Method[] methods = classe.getMethods();
            for (Method method : methods) {
                if (!method.isAnnotationPresent(BRequestMapping.class)) {
                    continue;
                }
                BRequestMapping methodMapping = method.getAnnotation(BRequestMapping.class);
                String url = ("/" + baseUrl + "/" + methodMapping.value()).replaceAll("/+", "/");
                handlerMap.put(url, method);
            }
        }
    }

    private void doResourced() {

        for (Map.Entry entry : ioc.entrySet()) {
            Field[] fields = entry.getValue().getClass().getDeclaredFields();
            for (Field field : fields) {
                if (!field.isAnnotationPresent(BResource.class)) {
                    continue;
                }
                BResource bResource = field.getAnnotation(BResource.class);
                String beanName = bResource.value();
                if ("".equals(beanName)) {
                    beanName = field.getType().getName();
                }
                field.setAccessible(true);
                try {
                    field.set(entry.getValue(), ioc.get(beanName));
                } catch (IllegalAccessException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private void doInstance() {
        try {
            for (String className : calassNames) {
                Class<?> reCla = Class.forName(className);
                if (reCla.isAnnotationPresent(BController.class)) {
                    String beanName = lowerFirst(reCla.getSimpleName());
                    ioc.put(beanName, reCla.newInstance());
                } else if (reCla.isAnnotationPresent(BService.class)) {
                    BService service = reCla.getAnnotation(BService.class);
                    String beanName = service.value();
                    if (!"".equals(beanName)) {
                        ioc.put(beanName, reCla.newInstance());
                        continue;
                    }
                    Class<?>[] interfaces = reCla.getInterfaces();
                    for (Class<?> inter : interfaces) {
                        ioc.put(inter.getName(), reCla.newInstance());
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    private String lowerFirst(String string) {
        char[] chars = string.toCharArray();
        chars[0] += 32;
        return String.valueOf(chars);
    }

    private void doScanPackage(String sanPackage) {
        URL uri = this.getClass().getClassLoader().getResource("/" + sanPackage.replaceAll("\\.", "/"));
        File dir = new File(uri.getFile());
        for (File file : dir.listFiles()) {
            if (file.isDirectory()) {
                doScanPackage(sanPackage + "." + file.getName());
            } else {
                calassNames.add(sanPackage + "." + file.getName().replace(".class", "").trim());
            }
        }
    }

    private void doLoadConf(String contextConfigLocation) {
        InputStream inputStream;
        inputStream = this.getClass().getClassLoader().getResourceAsStream(contextConfigLocation);
        try {
            properties.load(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (null != inputStream) {
                try {
                    inputStream.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    @Override
    protected void doGet(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doDisptch(req, resp);
    }

    @Override
    protected void doPost(HttpServletRequest req, HttpServletResponse resp)
            throws ServletException, IOException {
        doDisptch(req, resp);
    }

    private void doDisptch(HttpServletRequest req, HttpServletResponse resp) {
        String url = req.getRequestURI();
        String contextPa = req.getContextPath();
        url = url.replace(contextPa, "").replaceAll("/+", "/");
        Map<String, String[]> params = req.getParameterMap();
        Method method = handlerMap.get(url);
        Class<?>[] paramTypes = method.getParameterTypes();
        Object[] paramVals = new Object[paramTypes.length];
        for (int i = 0; i < paramTypes.length; i++) {
            Class paramType = paramTypes[i];
            if (paramType == HttpServletRequest.class) {
                paramVals[i] = req;
            } else if (paramType == HttpServletResponse.class) {
                paramVals[i] = resp;
            } else if (paramType == String.class) {
                for (Map.Entry<String, String[]> stringEntry : params.entrySet()) {
                    String val = Arrays.toString(stringEntry.getValue()).replaceAll("\\[|\\]", "").
                            replaceAll(",\\s", "");
                    paramVals[i] = val;
                }
            }
        }
        String beanName = lowerFirst(method.getDeclaringClass().getSimpleName());
        try {
            method.invoke(ioc.get(beanName), paramVals);
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (InvocationTargetException e) {
            e.printStackTrace();
        }
    }
}
