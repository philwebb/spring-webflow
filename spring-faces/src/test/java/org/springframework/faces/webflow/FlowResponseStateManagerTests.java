package org.springframework.faces.webflow;

import junit.framework.TestCase;

import org.easymock.EasyMock;
import org.springframework.web.context.support.StaticWebApplicationContext;
import org.springframework.webflow.core.collection.LocalAttributeMap;
import org.springframework.webflow.execution.FlowExecutionContext;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;
import org.springframework.webflow.test.MockFlowExecutionKey;

public class FlowResponseStateManagerTests extends TestCase {

	private final JSFMockHelper jsfMock = new JSFMockHelper();

	private FlowResponseStateManager responseStateManager;

	private RequestContext requestContext;
	private FlowExecutionContext flowExecutionContext;

	protected void setUp() throws Exception {
		jsfMock.setUp();
		StaticWebApplicationContext webappContext = new StaticWebApplicationContext();
		webappContext.setServletContext(jsfMock.servletContext());

		requestContext = EasyMock.createMock(RequestContext.class);
		RequestContextHolder.setRequestContext(requestContext);
		flowExecutionContext = EasyMock.createMock(FlowExecutionContext.class);

		responseStateManager = new FlowResponseStateManager(null);
	}

	protected void tearDown() throws Exception {
		super.tearDown();
		jsfMock.tearDown();
		RequestContextHolder.setRequestContext(null);
	}

	public void testname() throws Exception {

	}

	public void testWriteFlowSerializedView() throws Exception {
		EasyMock.expect(flowExecutionContext.getKey()).andReturn(new MockFlowExecutionKey("e1s1"));
		LocalAttributeMap<Object> viewMap = new LocalAttributeMap<Object>();
		EasyMock.expect(requestContext.getViewScope()).andStubReturn(viewMap);
		EasyMock.expect(requestContext.getFlowExecutionContext()).andReturn(flowExecutionContext);
		EasyMock.replay(requestContext, flowExecutionContext);

		Object state = new Object();
		responseStateManager.writeState(jsfMock.facesContext(), state);

		assertEquals(state, viewMap.get(FlowResponseStateManager.FACES_VIEW_STATE));
		assertEquals(
				"<input type=\"hidden\" name=\"javax.faces.ViewState\" id=\"javax.faces.ViewState\" value=\"e1s1\" />",
				jsfMock.contentAsString());
		EasyMock.verify(flowExecutionContext, requestContext);
	}

	public void testGetState() throws Exception {
		Object state = new Object();

		LocalAttributeMap<Object> viewMap = new LocalAttributeMap<Object>();
		viewMap.put(FlowResponseStateManager.FACES_VIEW_STATE, state);
		EasyMock.expect(requestContext.getViewScope()).andStubReturn(viewMap);
		EasyMock.replay(requestContext);

		Object actual = responseStateManager.getState(jsfMock.facesContext(), "viewId");

		assertSame(state, actual);
		EasyMock.verify(requestContext);
	}
}
