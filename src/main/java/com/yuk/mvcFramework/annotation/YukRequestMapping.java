package com.yuk.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * Created by yuk on 2019/3/31.
 */
@Target({ElementType.TYPE,ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YukRequestMapping {

    String value() default "" ;
}
