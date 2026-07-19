package ir.ozyrox.ctcommand.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface SubCommand {
    String value();
    String permission() default "";
    String usage() default "";
    int minArgs() default 0;
    boolean playerOnly() default false;
    int cooldown() default 0;
}
