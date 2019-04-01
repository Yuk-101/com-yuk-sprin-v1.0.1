package com.yuk.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * Created by yuk on 2019/3/31.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YukAutowired {

    String value() default "" ;
}
