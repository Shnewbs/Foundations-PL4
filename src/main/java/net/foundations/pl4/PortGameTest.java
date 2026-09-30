package net.foundations.pl4;
import java.lang.annotation.*;
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.METHOD)
public @interface PortGameTest {
    String template();
    String templateNamespace();
    int timeoutTicks() default 100;
}
