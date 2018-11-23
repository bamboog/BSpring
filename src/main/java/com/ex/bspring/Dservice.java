package com.ex.bspring;

import com.ex.bspring.annotation.BService;

/**
 * Created by IntelliJ IDEA.
 *
 * @author gangwen.xu
 * Date  : 2018/11/15
 * Time  : 下午11:46
 * 类描述 :
 */
@BService(value = "sservice")
public class Dservice {
    public String getTest(String te){
        return "xge::"+te;
    }
}
