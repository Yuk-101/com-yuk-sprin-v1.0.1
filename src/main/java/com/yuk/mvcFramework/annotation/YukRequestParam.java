package com.yuk.mvcFramework.annotation;

import java.lang.annotation.*;

/**
 * Created by yuk on 2019/4/1.
 */
@Target(ElementType.PARAMETER)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface YukRequestParam {

    String value() default "" ;
}
