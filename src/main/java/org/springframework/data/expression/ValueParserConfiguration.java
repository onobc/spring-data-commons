/*
 * Copyright 2024-2025 the original author or authors.
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
package org.springframework.data.expression;

import org.springframework.expression.ExpressionParser;

/**
 * Configuration for {@link ValueExpressionParser}.
 *
 * @author Christoph Strobl
 * @author Mark Paluch
 * @since 3.3
 */
public interface ValueParserConfiguration {

	/**
	 * Parser for {@link org.springframework.expression.Expression SpEL expressions}.
	 *
	 * @return for {@link org.springframework.expression.Expression SpEL expressions}.
	 */
	ExpressionParser getExpressionParser();

}
