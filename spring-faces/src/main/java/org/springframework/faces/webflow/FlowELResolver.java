package org.springframework.faces.webflow;

import javax.el.CompositeELResolver;
import javax.el.ELResolver;

import org.springframework.binding.expression.el.MapAdaptableELResolver;
import org.springframework.webflow.expression.el.FlowResourceELResolver;
import org.springframework.webflow.expression.el.ImplicitFlowVariableELResolver;
import org.springframework.webflow.expression.el.RequestContextELResolver;
import org.springframework.webflow.expression.el.ScopeSearchingELResolver;

/**
 * Custom {@link ELResolver} for resolving web flow specific expressions.
 * 
 * @author Jeremy Grelle
 * @author Phillip Webb
 */
public class FlowELResolver extends CompositeELResolver {

	// FIXME PW test case

	public FlowELResolver() {
		add(new RequestContextELResolver());
		add(new ImplicitFlowVariableELResolver());
		add(new FlowResourceELResolver());
		add(new ScopeSearchingELResolver());
		add(new MapAdaptableELResolver());
	}

}
