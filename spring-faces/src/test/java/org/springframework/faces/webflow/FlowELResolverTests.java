package org.springframework.faces.webflow;

import javax.el.ELContext;

import junit.framework.TestCase;

import org.apache.myfaces.test.el.MockELContext;
import org.springframework.context.ApplicationContext;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.engine.Flow;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;
import org.springframework.webflow.test.MockRequestContext;

/**
 * Tests for {@link FlowELResolver}.
 * 
 * @author Phillip Webb
 */
public class FlowELResolverTests extends TestCase {

	private final FlowELResolver resolver = new FlowELResolver();

	private final RequestContext requestContext = new MockRequestContext();

	private final ELContext elContext = new MockELContext();

	protected void setUp() throws Exception {
		RequestContextHolder.setRequestContext(requestContext);
	}

	protected void tearDown() throws Exception {
		RequestContextHolder.setRequestContext(null);
	}

	public void testRequestContextResolve() throws Exception {
		Object actual = resolver.getValue(elContext, null, "flowRequestContext");
		assertTrue(elContext.isPropertyResolved());
		assertNotNull(actual);
		assertSame(requestContext, actual);
	}

	public void testImplicitFlowResolve() throws Exception {
		Object actual = resolver.getValue(elContext, null, "flowScope");
		assertTrue(elContext.isPropertyResolved());
		assertNotNull(actual);
		assertSame(requestContext.getFlowScope(), actual);
	}

	public void testFlowResourceResolve() throws Exception {
		ApplicationContext applicationContext = new StaticWebApplicationContext();
		((Flow) requestContext.getActiveFlow()).setApplicationContext(applicationContext);
		Object actual = resolver.getValue(elContext, null, "resourceBundle");
		assertTrue(elContext.isPropertyResolved());
		assertNotNull(actual);
		assertSame(applicationContext, actual);
	}

	public void testScopeResolve() throws Exception {
		requestContext.getFlowScope().put("test", "test");
		Object actual = resolver.getValue(elContext, null, "test");
		assertTrue(elContext.isPropertyResolved());
		assertEquals("test", actual);
	}

	public void testMapAdaptableResolve() throws Exception {
		LocalAttributeMap<String> base = new LocalAttributeMap<String>();
		base.put("test", "test");
		Object actual = resolver.getValue(elContext, base, "test");
		assertTrue(elContext.isPropertyResolved());
		assertEquals("test", actual);
	}

	public void testBeanResolveWithRequestContext() throws Exception {
		StaticWebApplicationContext applicationContext = new StaticWebApplicationContext();
		((Flow) requestContext.getActiveFlow()).setApplicationContext(applicationContext);
		applicationContext.registerSingleton("test", Bean.class);
		Object actual = resolver.getValue(elContext, null, "test");
		assertTrue(elContext.isPropertyResolved());
		assertNotNull(actual);
		assertTrue(actual instanceof Bean);
	}

	public void testBeanResolveWithoutRequestContext() throws Exception {
		RequestContextHolder.setRequestContext(null);
		Object actual = resolver.getValue(elContext, null, "test");
		assertFalse(elContext.isPropertyResolved());
		assertNull(actual);
	}

	public static class Bean {
	}
}
