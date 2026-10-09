package net.foundations.pl4.compat.scenarios;
@java.lang.annotation.Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
public @interface GameTest {String template() default "empty";String templateNamespace() default "foundations_pl4";int timeoutTicks() default 100;}
