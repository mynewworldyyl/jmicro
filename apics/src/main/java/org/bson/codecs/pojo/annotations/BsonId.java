package org.bson.codecs.pojo.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * An annotation that configures the property as the id property for a {@link  org.bson.codecs.pojo.ClassModel}.
 *
 * <p>Note: Requires the {@link org.bson.codecs.pojo.Conventions#ANNOTATION_CONVENTION}</p>
 *
 * @since 3.5
 * @see org.bson.codecs.pojo.Conventions#ANNOTATION_CONVENTION
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.PARAMETER})
public @interface BsonId {
}
