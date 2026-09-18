package com.mixininject.api;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface Inject {
   String method() default "";

   String desc() default "";

   String at() default "HEAD";

   boolean captureArgs() default false;

   boolean captureReturn() default false;

   boolean cancellable() default false;
}
