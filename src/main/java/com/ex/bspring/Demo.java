package com.ex.bspring;

import com.ex.bspring.annotation.BController;
import com.ex.bspring.annotation.BRequestMapping;
import com.ex.bspring.annotation.BRequestParam;
import com.ex.bspring.annotation.BResource;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by IntelliJ IDEA.
 *
 * @author gangwen.xu
 * Date  : 2018/11/15
 * Time  : 下午11:31
 * 类描述 :
 */
@BController
@BRequestMapping(value = "/demo")
public class Demo {

    @BResource(value = "sservice")
    private Dservice dservice;

    @BRequestMapping(value = "/test")
    public void  test(HttpServletRequest request,HttpServletResponse response,
                      @BRequestParam(name = "test") String test){
        try {
            String re = dservice.getTest(test);
            response.getWriter().write(re);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
