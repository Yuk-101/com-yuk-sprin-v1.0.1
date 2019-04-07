package com.yuk.springv101.mvc;

import com.yuk.springv101.service.IdemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

/**
 * Created by yuk on 2019/4/6.
 */
public class Action {

    private IdemoService idemoService;

    private void edit(HttpServletRequest req, HttpServletResponse resp,String userName){
        String name = idemoService.get(userName);
        try {
            resp.getWriter().write(name);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
