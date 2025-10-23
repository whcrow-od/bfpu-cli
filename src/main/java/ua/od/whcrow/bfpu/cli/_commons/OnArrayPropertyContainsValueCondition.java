package ua.od.whcrow.bfpu.cli._commons;

import jakarta.annotation.Nonnull;
import org.springframework.boot.autoconfigure.condition.ConditionOutcome;
import org.springframework.boot.autoconfigure.condition.SpringBootCondition;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.context.annotation.ConditionContext;
import org.springframework.core.type.AnnotatedTypeMetadata;

import java.util.Collections;
import java.util.List;
import java.util.Map;

public class OnArrayPropertyContainsValueCondition extends SpringBootCondition {
	
	private static final String PN_NAME = "name";
	private static final String PN_CONTAINS_VALUE = "containsValue";
	
	@Nonnull
	@Override
	public ConditionOutcome getMatchOutcome(@Nonnull ConditionContext context,
			@Nonnull AnnotatedTypeMetadata metadata) {
		String annotationName = ConditionalOnArrayPropertyContains.class.getName();
		Map<String,Object> attributes = metadata.getAnnotationAttributes(annotationName);
		if (attributes == null) {
			return ConditionOutcome.noMatch("Annotation " + annotationName + " not found");
		}
		String propertyName = (String) attributes.get(PN_NAME);
		String propertyContainedValue = (String) attributes.get(PN_CONTAINS_VALUE);
		if (propertyName == null) {
			return ConditionOutcome.noMatch("Annotation " + annotationName + " required property \"" + PN_NAME
					+ "\" is NULL");
		}
		if (propertyContainedValue == null) {
			return ConditionOutcome.noMatch("Annotation " + annotationName + " required property \""
					+ PN_CONTAINS_VALUE + "\" is NULL");
		}
		List<?> actualPropertyValues = Binder.get(context.getEnvironment())
				.bind(propertyName, List.class)
				.orElse(Collections.emptyList());
		if (actualPropertyValues.contains(propertyContainedValue)) {
			return ConditionOutcome.match("Property \"" + propertyName + "\" value contains \""
					+ propertyContainedValue + "\"");
		}
		return ConditionOutcome.noMatch("Property \"" + propertyName + "\" value doesn't contain \""
				+ propertyContainedValue + "\"");
	}
	
}
