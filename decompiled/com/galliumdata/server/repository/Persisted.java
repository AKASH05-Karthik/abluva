/*
 * Decompiled with CFR 0.152.
 */
package com.galliumdata.server.repository;

import com.galliumdata.server.repository.NullClass;
import com.galliumdata.server.repository.RepositoryObject;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(value=RetentionPolicy.RUNTIME)
@Target(value={ElementType.FIELD})
public @interface Persisted {
    public String JSONName() default "";

    public String[] allowedValues() default {};

    public String fileName() default "";

    public String directoryName() default "";

    public Class<? extends RepositoryObject> memberClass() default NullClass.class;
}
