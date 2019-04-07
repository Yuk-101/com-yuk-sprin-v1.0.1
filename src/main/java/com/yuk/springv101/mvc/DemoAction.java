package com.yuk.springv101.mvc;


import com.yuk.mvcFramework.annotation.YukAutowired;
import com.yuk.mvcFramework.annotation.YukController;
import com.yuk.mvcFramework.annotation.YukRequestMapping;
import com.yuk.mvcFramework.annotation.YukRequestParam;
import com.yuk.springv101.service.IdemoService;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;


//虽然，用法一样，但是没有功能
@YukController
@YukRequestMapping("/demo")
public class DemoAction {

  	@YukAutowired
	private IdemoService demoService;

	@YukRequestMapping("/query")
	public void query(HttpServletRequest req, HttpServletResponse resp,
                      @YukRequestParam("name") String name){
//		String result = demoService.get(name);
		String result = "My name is " + name;
		try {
			resp.getWriter().write(result);
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@YukRequestMapping("/add")
	public void add(HttpServletRequest req, HttpServletResponse resp,
                    @YukRequestParam("a") Integer a, @YukRequestParam("b") Integer b){
		try {
			resp.getWriter().write(a + "+" + b + "=" + (a + b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@YukRequestMapping("/sub")
	public void add(HttpServletRequest req, HttpServletResponse resp,
                    @YukRequestParam("a") Double a, @YukRequestParam("b") Double b){
		try {
			resp.getWriter().write(a + "-" + b + "=" + (a - b));
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

	@YukRequestMapping("/remove")
	public String  remove(@YukRequestParam("id") Integer id){
		return "" + id;
	}

}
