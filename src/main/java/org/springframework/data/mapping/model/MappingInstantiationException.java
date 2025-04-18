/*
 * Copyright 2011-2025 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.data.mapping.model;

import kotlin.reflect.KFunction;
import kotlin.reflect.jvm.ReflectJvmMapping;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.core.KotlinDetector;
import org.springframework.data.mapping.FactoryMethod;
import org.springframework.data.mapping.InstanceCreatorMetadata;
import org.springframework.data.mapping.PersistentEntity;
import org.springframework.data.mapping.PreferredConstructor;
import org.springframework.data.util.KotlinReflectionUtils;
import org.springframework.lang.Nullable;
import org.springframework.util.ObjectUtils;

/**
 * Exception being thrown in case an entity could not be instantiated in the process of a to-object-mapping.
 *
 * @author Oliver Gierke
 * @author Jon Brisbin
 * @author Christoph Strobl
 * @author Mark Paluch
 */
public class MappingInstantiationException extends RuntimeException {

	private static final long serialVersionUID = 822211065035487628L;
	private static final String TEXT_TEMPLATE = "Failed to instantiate %s using constructor %s with arguments %s";

	private final Class<?> entityType;
	private final InstanceCreatorMetadata<?> entityCreator;
	private final List<Object> constructorArguments;

	/**
	 * Creates a new {@link MappingInstantiationException} for the given {@link PersistentEntity}, constructor arguments
	 * and the causing exception.
	 *
	 * @param entity
	 * @param arguments
	 * @param cause
	 */
	public MappingInstantiationException(PersistentEntity<?, ?> entity, List<Object> arguments, Exception cause) {
		this(Optional.ofNullable(entity), arguments, null, cause);
	}

	/**
	 * Creates a new {@link MappingInstantiationException} for the given constructor arguments and the causing exception.
	 *
	 * @param arguments
	 * @param cause
	 */
	public MappingInstantiationException(List<Object> arguments, Exception cause) {
		this(Optional.empty(), arguments, null, cause);
	}

	private MappingInstantiationException(Optional<PersistentEntity<?, ?>> entity, List<Object> arguments,
			@Nullable String message, Exception cause) {

		super(buildExceptionMessage(entity, arguments, message), cause);

		this.entityType = entity.map(PersistentEntity::getType).orElse(null);
		this.entityCreator = entity.map(PersistentEntity::getInstanceCreatorMetadata).orElse(null);
		this.constructorArguments = arguments;
	}

	private static String buildExceptionMessage(Optional<PersistentEntity<?, ?>> entity, List<Object> arguments,
			@Nullable String defaultMessage) {

		return entity.map(it -> {

			Optional<? extends InstanceCreatorMetadata<?>> constructor = Optional.ofNullable(it.getInstanceCreatorMetadata());
			List<String> toStringArgs = new ArrayList<>(arguments.size());

			for (Object o : arguments) {
				toStringArgs.add(ObjectUtils.nullSafeToString(o));
			}

			return String.format(TEXT_TEMPLATE, it.getType().getName(),
					constructor.map(MappingInstantiationException::toString).orElse("NO_CONSTRUCTOR"), //
					String.join(",", toStringArgs));

		}).orElse(defaultMessage);
	}

	private static String toString(InstanceCreatorMetadata<?> creator) {

		if (creator instanceof PreferredConstructor<?, ?> c) {
			return toString(c);
		}

		if (creator instanceof FactoryMethod<?, ?> m) {
			return toString(m);
		}

		return creator.toString();
	}

	private static String toString(PreferredConstructor<?, ?> preferredConstructor) {

		Constructor<?> constructor = preferredConstructor.getConstructor();

		if (KotlinDetector.isKotlinPresent() && KotlinReflectionUtils.isSupportedKotlinClass(constructor.getDeclaringClass())) {

			KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(constructor);

			if (kotlinFunction != null) {
				return kotlinFunction.toString();
			}
		}

		return constructor.toString();
	}

	private static String toString(FactoryMethod<?, ?> factoryMethod) {

		Method constructor = factoryMethod.getFactoryMethod();

		if (KotlinDetector.isKotlinPresent() && KotlinReflectionUtils.isSupportedKotlinClass(constructor.getDeclaringClass())) {

			KFunction<?> kotlinFunction = ReflectJvmMapping.getKotlinFunction(constructor);

			if (kotlinFunction != null) {
				return kotlinFunction.toString();
			}
		}

		return constructor.toString();
	}

	/**
	 * Returns the type of the entity that was attempted to instantiate.
	 *
	 * @return the entityType
	 */
	public Optional<Class<?>> getEntityType() {
		return Optional.ofNullable(entityType);
	}

	/**
	 * The constructor used during the instantiation attempt.
	 *
	 * @return the constructor
	 * @deprecated since 3.0, use {@link #getEntityCreator()} instead.
	 */
	@Deprecated
	public Optional<Constructor<?>> getConstructor() {
		return getEntityCreator().filter(PreferredConstructor.class::isInstance).map(PreferredConstructor.class::cast)
				.map(PreferredConstructor::getConstructor);
	}

	/**
	 * The entity creator used during the instantiation attempt.
	 *
	 * @return the entity creator
	 * @since 3.0
	 */
	public Optional<InstanceCreatorMetadata<?>> getEntityCreator() {
		return Optional.ofNullable(entityCreator);
	}

	/**
	 * The constructor arguments used to invoke the constructor.
	 *
	 * @return the constructorArguments
	 */
	public List<Object> getConstructorArguments() {
		return constructorArguments;
	}
}
