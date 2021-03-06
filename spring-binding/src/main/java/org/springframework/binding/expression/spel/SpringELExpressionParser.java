/*
 * Copyright 2004-2010 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.springframework.binding.expression.spel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.binding.convert.ConversionService;
import org.springframework.binding.convert.service.DefaultConversionService;
import org.springframework.binding.expression.Expression;
import org.springframework.binding.expression.ExpressionParser;
import org.springframework.binding.expression.ExpressionVariable;
import org.springframework.binding.expression.ParserContext;
import org.springframework.binding.expression.ParserException;
import org.springframework.binding.expression.support.NullParserContext;
import org.springframework.context.expression.MapAccessor;
import org.springframework.expression.EvaluationContext;
import org.springframework.expression.PropertyAccessor;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.util.Assert;

/**
 * <p>
 * Adapts the Spring EL {@link SpelExpressionParser} to the Spring Binding {@link ExpressionParser} interface.
 * </p>
 * 
 * @author Rossen Stoyanchev
 * @since 2.1.0
 */
public class SpringELExpressionParser implements ExpressionParser {

	private SpelExpressionParser expressionParser;

	private ConversionService conversionService;

	private List<PropertyAccessor> propertyAccessors = new ArrayList<PropertyAccessor>();

	public SpringELExpressionParser(SpelExpressionParser expressionParser) {
		this(expressionParser, new DefaultConversionService());
	}

	public SpringELExpressionParser(SpelExpressionParser expressionParser, ConversionService conversionService) {
		this.expressionParser = expressionParser;
		this.propertyAccessors.add(new MapAccessor());
		this.conversionService = conversionService;
	}

	public ConversionService getConversionService() {
		return conversionService;
	}

	public void addPropertyAccessor(PropertyAccessor propertyAccessor) {
		propertyAccessors.add(propertyAccessor);
	}

	public Expression parseExpression(String expressionString, ParserContext parserContext) throws ParserException {
		Assert.hasText(expressionString, "The expression string to parse is required and must not be empty");
		parserContext = (parserContext == null) ? NullParserContext.INSTANCE : parserContext;
		Map spelExpressionVariables = parseSpelExpressionVariables(parserContext.getExpressionVariables());
		return new SpringELExpression(parseSpelExpression(expressionString, parserContext), spelExpressionVariables,
				parserContext.getExpectedEvaluationResultType(), conversionService.getDelegateConversionService(),
				propertyAccessors);
	}

	private org.springframework.expression.Expression parseSpelExpression(String expression, ParserContext parserContext) {
		return expressionParser.parseExpression(expression, getSpelParserContext(parserContext));
	}

	private org.springframework.expression.ParserContext getSpelParserContext(ParserContext parserContext) {
		return parserContext.isTemplate() ? org.springframework.expression.ParserContext.TEMPLATE_EXPRESSION : null;
	}

	/**
	 * Turns {@link ExpressionVariable}'s (pairs of variable names and string expressions) into a map of variable names
	 * and parsed Spring EL expressions. The map will be saved in a Spring EL {@link EvaluationContext} for later use at
	 * evaluation time.
	 * 
	 * @param expressionVariables an array of ExpressionVariable instances.
	 * @return a Map or null if the input array is empty.
	 */
	private Map parseSpelExpressionVariables(ExpressionVariable[] expressionVariables) {
		if (expressionVariables == null || expressionVariables.length == 0) {
			return null;
		}
		Map spelExpressionVariables = new HashMap(expressionVariables.length);
		for (int i = 0; i < expressionVariables.length; i++) {
			ExpressionVariable var = expressionVariables[i];
			spelExpressionVariables.put(var.getName(),
					parseExpression(var.getValueExpression(), var.getParserContext()));
		}
		return spelExpressionVariables;
	}

}
