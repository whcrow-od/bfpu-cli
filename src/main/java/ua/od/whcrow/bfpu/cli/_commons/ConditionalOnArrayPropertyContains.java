package ua.od.whcrow.bfpu.cli._commons;

import org.springframework.context.annotation.Conditional;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.TYPE, ElementType.METHOD})
@Documented
@Conditional({OnArrayPropertyContainsValueCondition.class})
public @interface ConditionalOnArrayPropertyContains {
	
	String name();
	
	String containsValue();
	
}
