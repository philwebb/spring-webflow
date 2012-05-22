package org.springframework.faces.webflow;

import java.io.IOException;
import java.io.StringWriter;

import javax.faces.context.FacesContext;
import javax.faces.render.ResponseStateManager;

import org.apache.myfaces.renderkit.MyfacesResponseStateManager;
import org.apache.myfaces.test.base.AbstractJsfTestCase;
import org.apache.myfaces.test.mock.MockResponseWriter;

/**
 * Tests for {@link MyFacesFlowResponseStateManager}.
 * 
 * @author Phillip Webb
 */
public class MyFacesFlowResponseStateManagerTests extends AbstractJsfTestCase {

	private MockMyfacesResponseStateManager root;
	private MockFlowResponseStateManager flow;
	private MyFacesFlowResponseStateManager manager;

	public MyFacesFlowResponseStateManagerTests(String name) {
		super(name);
	}

	protected void setUp() throws Exception {
		super.setUp();
		this.root = new MockMyfacesResponseStateManager();
		this.flow = new MockFlowResponseStateManager(this.root);
		this.manager = new MyFacesFlowResponseStateManager(this.flow);
	}

	public void testDelegatesIsWriteStateAfterRenderViewRequired() throws Exception {
		this.manager.isWriteStateAfterRenderViewRequired(this.facesContext);
		assertTrue(this.root.isWriteStateAfterRenderViewRequiredCalled);
	}

	public void testDelegatesWriteState() throws Exception {
		this.facesContext.setResponseWriter(new MockResponseWriter(new StringWriter()));
		this.manager.writeState(this.facesContext, new Object());
		assertTrue(this.root.writeStateCalled);
	}

	public void testTriggersSaveStateInFlowResponseStateManager() throws Exception {
		this.manager.saveState(this.facesContext, new Object());
		assertTrue(this.flow.saveStateCalled);
		assertFalse(this.root.saveStateCalled);
	}

	private static class MockMyfacesResponseStateManager extends MyfacesResponseStateManager {
		private boolean isWriteStateAfterRenderViewRequiredCalled;
		private boolean saveStateCalled;
		private boolean writeStateCalled;

		public boolean isWriteStateAfterRenderViewRequired(FacesContext facesContext) {
			this.isWriteStateAfterRenderViewRequiredCalled = true;
			return true;
		}

		public void saveState(FacesContext facesContext, Object state) {
			this.saveStateCalled = true;
		}

		public void writeState(FacesContext context, Object state) throws IOException {
			this.writeStateCalled = true;
		}
	}

	private static class MockFlowResponseStateManager extends FlowResponseStateManager {

		private boolean saveStateCalled;

		public MockFlowResponseStateManager(ResponseStateManager wrapped) {
			super(wrapped);
		}

		void saveState(Object state) {
			this.saveStateCalled = true;
		}
	}

}
