/*
 * Copyright 2018-2025 the original author or authors.
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
package org.springframework.data.repository.query;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.data.domain.Range;
import org.springframework.data.domain.Range.Bound;
import org.springframework.lang.Nullable;
import org.springframework.util.Assert;

/**
 * A {@literal SpelQueryContext} is able to find SpEL expressions in a query string and to replace them with bind
 * variables.
 * <p>
 * Result o the parse process is a {@link SpelExtractor} which offers the transformed query string. Alternatively and
 * preferred one may provide a {@link QueryMethodEvaluationContextProvider} via
 * {@link #withEvaluationContextProvider(QueryMethodEvaluationContextProvider)} which will yield the more powerful
 * {@link EvaluatingSpelQueryContext}.
 * <p>
 * Typical usage looks like
 *
 * <pre>
 * <code>
     SpelQueryContext.EvaluatingSpelQueryContext queryContext = SpelQueryContext
         .of((counter, expression) -> String.format("__$synthetic$__%d", counter), String::concat)
         .withEvaluationContextProvider(evaluationContextProvider);

     SpelEvaluator spelEvaluator = queryContext.parse(query, queryMethod.getParameters());

     spelEvaluator.evaluate(objects).forEach(parameterMap::addValue);
 * </code>
 * </pre>
 *
 * @author Jens Schauder
 * @author Gerrit Meier
 * @author Mark Paluch
 * @since 2.1
 * @deprecated since 3.3, use {@link ValueExpressionQueryRewriter} instead.
 */
@SuppressWarnings("removal")
@Deprecated(since = "3.3", forRemoval = true)
public class SpelQueryContext {

	private static final String SPEL_PATTERN_STRING = "([:?])#\\{([^}]+)}";
	private static final Pattern SPEL_PATTERN = Pattern.compile(SPEL_PATTERN_STRING);

	/**
	 * A function from the index of a SpEL expression in a query and the actual SpEL expression to the parameter name to
	 * be used in place of the SpEL expression. A typical implementation is expected to look like
	 * <code>(index, spel) -> "__some_placeholder_" + index</code>
	 */
	private final BiFunction<Integer, String, String> parameterNameSource;

	/**
	 * A function from a prefix used to demarcate a SpEL expression in a query and a parameter name as returned from
	 * {@link #parameterNameSource} to a {@literal String} to be used as a replacement of the SpEL in the query. The
	 * returned value should normally be interpretable as a bind parameter by the underlying persistence mechanism. A
	 * typical implementation is expected to look like <code>(prefix, name) -> prefix + name</code> or
	 * <code>(prefix, name) -> "{" + name + "}"</code>
	 */
	private final BiFunction<String, String, String> replacementSource;

	private SpelQueryContext(BiFunction<Integer, String, String> parameterNameSource,
			BiFunction<String, String, String> replacementSource) {

		Assert.notNull(parameterNameSource, "Parameter name source must not be null");
		Assert.notNull(replacementSource, "Replacement source must not be null");

		this.parameterNameSource = parameterNameSource;
		this.replacementSource = replacementSource;
	}

	public static SpelQueryContext of(BiFunction<Integer, String, String> parameterNameSource,
			BiFunction<String, String, String> replacementSource) {
		return new SpelQueryContext(parameterNameSource, replacementSource);
	}

	/**
	 * Parses the query for SpEL expressions using the pattern:
	 *
	 * <pre>
	 * &lt;prefix&gt;#{&lt;spel&gt;}
	 * </pre>
	 * <p>
	 * with prefix being the character ':' or '?'. Parsing honors quoted {@literal String}s enclosed in single or double
	 * quotation marks.
	 *
	 * @param query a query containing SpEL expressions in the format described above. Must not be {@literal null}.
	 * @return A {@link SpelExtractor} which makes the query with SpEL expressions replaced by bind parameters and a map
	 *         from bind parameter to SpEL expression available. Guaranteed to be not {@literal null}.
	 */
	public SpelExtractor parse(String query) {
		return new SpelExtractor(query);
	}

	/**
	 * Createsa {@link EvaluatingSpelQueryContext} from the current one and the given
	 * {@link QueryMethodEvaluationContextProvider}.
	 *
	 * @param provider must not be {@literal null}.
	 * @return
	 */
	public EvaluatingSpelQueryContext withEvaluationContextProvider(QueryMethodEvaluationContextProvider provider) {

		Assert.notNull(provider, "QueryMethodEvaluationContextProvider must not be null");

		return new EvaluatingSpelQueryContext(provider, parameterNameSource, replacementSource);
	}

	/**
	 * An extension of {@link SpelQueryContext} that can create {@link SpelEvaluator} instances as it also knows about a
	 * {@link QueryMethodEvaluationContextProvider}.
	 *
	 * @author Oliver Gierke
	 * @since 2.1
	 */
	public static class EvaluatingSpelQueryContext extends SpelQueryContext {

		private final QueryMethodEvaluationContextProvider evaluationContextProvider;

		/**
		 * Creates a new {@link EvaluatingSpelQueryContext} for the given {@link QueryMethodEvaluationContextProvider},
		 * parameter name source and replacement source.
		 *
		 * @param evaluationContextProvider must not be {@literal null}.
		 * @param parameterNameSource must not be {@literal null}.
		 * @param replacementSource must not be {@literal null}.
		 */
		private EvaluatingSpelQueryContext(QueryMethodEvaluationContextProvider evaluationContextProvider,
				BiFunction<Integer, String, String> parameterNameSource, BiFunction<String, String, String> replacementSource) {

			super(parameterNameSource, replacementSource);

			this.evaluationContextProvider = evaluationContextProvider;
		}

		/**
		 * Parses the query for SpEL expressions using the pattern:
		 *
		 * <pre>
		 * &lt;prefix&gt;#{&lt;spel&gt;}
		 * </pre>
		 * <p>
		 * with prefix being the character ':' or '?'. Parsing honors quoted {@literal String}s enclosed in single or double
		 * quotation marks.
		 *
		 * @param query a query containing SpEL expressions in the format described above. Must not be {@literal null}.
		 * @param parameters a {@link Parameters} instance describing query method parameters
		 * @return A {@link SpelEvaluator} which allows to evaluate the SpEL expressions. Will never be {@literal null}.
		 */
		public SpelEvaluator parse(String query, Parameters<?, ?> parameters) {
			return new SpelEvaluator(evaluationContextProvider, parameters, parse(query));
		}
	}

	/**
	 * Parses a query string, identifies the contained SpEL expressions, replaces them with bind parameters and offers a
	 * {@link Map} from those bind parameters to the SpEL expression.
	 * <p>
	 * The parser detects quoted parts of the query string and does not detect SpEL expressions inside such quoted parts
	 * of the query.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @author Mark Paluch
	 * @since 2.1
	 */
	public class SpelExtractor {

		private static final int PREFIX_GROUP_INDEX = 1;
		private static final int EXPRESSION_GROUP_INDEX = 2;

		private final String query;
		private final Map<String, String> expressions;
		private final QuotationMap quotations;

		/**
		 * Creates a SpelExtractor from a query String.
		 *
		 * @param query must not be {@literal null}.
		 */
		SpelExtractor(String query) {

			Assert.notNull(query, "Query must not be null");

			Map<String, String> expressions = new HashMap<>();
			Matcher matcher = SPEL_PATTERN.matcher(query);
			StringBuilder resultQuery = new StringBuilder();
			QuotationMap quotedAreas = new QuotationMap(query);

			int expressionCounter = 0;
			int matchedUntil = 0;

			while (matcher.find()) {

				if (quotedAreas.isQuoted(matcher.start())) {

					resultQuery.append(query, matchedUntil, matcher.end());

				} else {

					String spelExpression = matcher.group(EXPRESSION_GROUP_INDEX);
					String prefix = matcher.group(PREFIX_GROUP_INDEX);

					String parameterName = parameterNameSource.apply(expressionCounter, spelExpression);
					String replacement = replacementSource.apply(prefix, parameterName);

					resultQuery.append(query, matchedUntil, matcher.start());
					resultQuery.append(replacement);

					expressions.put(parameterName, spelExpression);
					expressionCounter++;
				}

				matchedUntil = matcher.end();
			}

			resultQuery.append(query.substring(matchedUntil));

			this.expressions = Collections.unmodifiableMap(expressions);
			this.query = resultQuery.toString();

			// recreate quotation map based on rewritten query.
			this.quotations = new QuotationMap(this.query);
		}

		/**
		 * The query with all the SpEL expressions replaced with bind parameters.
		 *
		 * @return Guaranteed to be not {@literal null}.
		 */
		public String getQueryString() {
			return query;
		}

		/**
		 * Return whether the {@link #getQueryString() query} at {@code index} is quoted.
		 *
		 * @param index
		 * @return {@literal true} if quoted; {@literal false} otherwise.
		 */
		public boolean isQuoted(int index) {
			return quotations.isQuoted(index);
		}

		public String getParameter(String name) {
			return expressions.get(name);
		}

		/**
		 * Returns the number of expressions in this extractor.
		 *
		 * @return the number of expressions in this extractor.
		 * @since 3.1.3
		 */
		public int size() {
			return expressions.size();
		}

		/**
		 * A {@literal Map} from parameter name to SpEL expression.
		 *
		 * @return Guaranteed to be not {@literal null}.
		 */
		Map<String, String> getParameterMap() {
			return expressions;
		}

	}

	/**
	 * Value object to analyze a {@link String} to determine the parts of the {@link String} that are quoted and offers an
	 * API to query that information.
	 *
	 * @author Jens Schauder
	 * @author Oliver Gierke
	 * @since 2.1
	 */
	static class QuotationMap {

		private static final Collection<Character> QUOTING_CHARACTERS = List.of('"', '\'');

		private final List<Range<Integer>> quotedRanges = new ArrayList<>();

		/**
		 * Creates a new {@link QuotationMap} for the query.
		 *
		 * @param query can be {@literal null}.
		 */
		public QuotationMap(@Nullable String query) {

			if (query == null) {
				return;
			}

			Character inQuotation = null;
			int start = 0;

			for (int i = 0; i < query.length(); i++) {

				char currentChar = query.charAt(i);

				if (QUOTING_CHARACTERS.contains(currentChar)) {

					if (inQuotation == null) {

						inQuotation = currentChar;
						start = i;

					} else if (currentChar == inQuotation) {

						inQuotation = null;

						quotedRanges.add(Range.from(Bound.inclusive(start)).to(Bound.inclusive(i)));
					}
				}
			}

			if (inQuotation != null) {
				throw new IllegalArgumentException(
						String.format("The string <%s> starts a quoted range at %d, but never ends it.", query, start));
			}
		}

		/**
		 * Checks if a given index is within a quoted range.
		 *
		 * @param index to check if it is part of a quoted range.
		 * @return whether the query contains a quoted range at {@literal index}.
		 */
		public boolean isQuoted(int index) {
			return quotedRanges.stream().anyMatch(r -> r.contains(index));
		}
	}
}
