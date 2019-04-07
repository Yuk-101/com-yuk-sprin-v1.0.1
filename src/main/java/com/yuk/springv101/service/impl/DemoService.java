package com.yuk.springv101.service.impl;

import com.yuk.springv101.service.IdemoService;

/**
 * Created by yuk on 2019/4/6.
 */
public class DemoService implements IdemoService{
    @Override
    public String get(String UserName) {
        return "My name is " + UserName + "!";
    }
}
