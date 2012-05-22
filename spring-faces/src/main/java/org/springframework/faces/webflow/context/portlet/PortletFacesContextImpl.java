/*
 * Copyright 2004-2012 the original author or authors.
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
package org.springframework.faces.webflow.context.portlet;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;

import javax.el.ELContext;
import javax.el.ELContextEvent;
import javax.el.ELContextListener;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;
import javax.faces.application.Application;
import javax.faces.application.ApplicationFactory;
import javax.faces.application.FacesMessage;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.ResponseStream;
import javax.faces.context.ResponseWriter;
import javax.faces.render.RenderKit;
import javax.faces.render.RenderKitFactory;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.springframework.faces.webflow.JsfUtils;
import org.springframework.util.Assert;
import org.springframework.util.ClassUtils;

/**
 * The default FacesContext implementation in Mojarra and in Apache MyFaces depends on the Servlet API. This
 * implementation provides an alternative that accepts Portlet request and response structures and creates a
 * {@link PortletExternalContextImpl} in its constructor. The rest of the method implementations mimic the equivalent
 * methods in the default FacesContext implementation.
 * 
 * @author Rossen Stoyanchev
 * @since 2.2.0
 */
public class PortletFacesContextImpl extends FacesContext {

	private Application application;

	private ELContext elContext;

	private ExternalContext externalContext;

	private FacesMessage.Severity maximumSeverity;

	private List<String> messageClientIds;

	private List<FacesMessage> messages;

	private boolean released = false;

	private RenderKitFactory renderKitFactory;

	private boolean renderResponse = false;

	private boolean responseComplete = false;

	private ResponseStream responseStream;

	private ResponseWriter responseWriter;

	private UIViewRoot viewRoot;

	public PortletFacesContextImpl(PortletContext portletContext, PortletRequest portletRequest,
			PortletResponse portletResponse) {
		this.application = JsfUtils.findFactory(ApplicationFactory.class).getApplication();
		this.renderKitFactory = JsfUtils.findFactory(RenderKitFactory.class);
		this.externalContext = new PortletExternalContextImpl(portletContext, portletRequest, portletResponse);
		FacesContext.setCurrentInstance(this);
	}

	public PortletFacesContextImpl(ExternalContext externalContext) {
		this.externalContext = externalContext;
	}

	public ExternalContext getExternalContext() {
		assertFacesContextIsNotReleased();
		return this.externalContext;
	}

	public FacesMessage.Severity getMaximumSeverity() {
		assertFacesContextIsNotReleased();
		return this.maximumSeverity;
	}

	@SuppressWarnings("unchecked")
	public Iterator<FacesMessage> getMessages() {
		assertFacesContextIsNotReleased();
		return (this.messages != null) ? this.messages.iterator() : Collections.EMPTY_LIST.iterator();
	}

	public Application getApplication() {
		assertFacesContextIsNotReleased();
		return this.application;
	}

	public Iterator<String> getClientIdsWithMessages() {
		assertFacesContextIsNotReleased();
		if (this.messages == null || this.messages.isEmpty()) {
			return new ArrayList<String>().iterator();
		}

		return new LinkedHashSet<String>(this.messageClientIds).iterator();
	}

	@SuppressWarnings("unchecked")
	public Iterator<FacesMessage> getMessages(String clientId) {
		assertFacesContextIsNotReleased();
		if (this.messages == null) {
			return Collections.EMPTY_LIST.iterator();
		}

		List<FacesMessage> list = new ArrayList<FacesMessage>();
		for (int i = 0; i < this.messages.size(); i++) {
			Object current = this.messageClientIds.get(i);
			if (clientId == null) {
				if (current == null) {
					list.add(this.messages.get(i));
				}
			} else {
				if (clientId.equals(current)) {
					list.add(this.messages.get(i));
				}
			}
		}
		return list.iterator();
	}

	public RenderKit getRenderKit() {
		if (getViewRoot() == null) {
			return null;
		}

		String renderKitId = getViewRoot().getRenderKitId();

		if (renderKitId == null) {
			return null;
		}

		return this.renderKitFactory.getRenderKit(this, renderKitId);
	}

	public boolean getRenderResponse() {
		assertFacesContextIsNotReleased();
		return this.renderResponse;
	}

	public boolean getResponseComplete() {
		assertFacesContextIsNotReleased();
		return this.responseComplete;
	}

	public ResponseStream getResponseStream() {
		assertFacesContextIsNotReleased();
		return this.responseStream;
	}

	public void setResponseStream(ResponseStream responseStream) {
		assertFacesContextIsNotReleased();
		if (responseStream == null) {
			throw new NullPointerException("responseStream");
		}
		this.responseStream = responseStream;
	}

	public ResponseWriter getResponseWriter() {
		assertFacesContextIsNotReleased();
		return this.responseWriter;
	}

	public void setResponseWriter(ResponseWriter responseWriter) {
		assertFacesContextIsNotReleased();
		if (responseWriter == null) {
			throw new NullPointerException("responseWriter");
		}
		this.responseWriter = responseWriter;
	}

	public UIViewRoot getViewRoot() {
		assertFacesContextIsNotReleased();
		return this.viewRoot;
	}

	public void setViewRoot(UIViewRoot viewRoot) {
		assertFacesContextIsNotReleased();
		if (viewRoot == null) {
			throw new NullPointerException("viewRoot");
		}
		this.viewRoot = viewRoot;
	}

	public void addMessage(String clientId, FacesMessage message) {
		assertFacesContextIsNotReleased();
		if (message == null) {
			throw new NullPointerException("message");
		}

		if (this.messages == null) {
			this.messages = new ArrayList<FacesMessage>();
			this.messageClientIds = new ArrayList<String>();
		}
		this.messages.add(message);
		this.messageClientIds.add((clientId != null) ? clientId : null);
		FacesMessage.Severity severity = message.getSeverity();
		if (severity != null) {
			if (this.maximumSeverity == null) {
				this.maximumSeverity = severity;
			} else if (severity.compareTo(this.maximumSeverity) > 0) {
				this.maximumSeverity = severity;
			}
		}
	}

	public void release() {
		assertFacesContextIsNotReleased();
		if (this.externalContext != null) {
			Method delegateMethod = ClassUtils.getMethodIfAvailable(this.externalContext.getClass(), "release");
			if (delegateMethod != null) {
				try {
					delegateMethod.invoke(this.externalContext);
				} catch (Exception e) {
					this.externalContext.log("Failed to release external context", e);
				}
				this.externalContext = null;
			}
		}
		this.messageClientIds = null;
		this.messages = null;
		this.application = null;
		this.responseStream = null;
		this.responseWriter = null;
		this.viewRoot = null;

		this.released = true;
		FacesContext.setCurrentInstance(null);
	}

	public void renderResponse() {
		assertFacesContextIsNotReleased();
		this.renderResponse = true;
	}

	public void responseComplete() {
		assertFacesContextIsNotReleased();
		this.responseComplete = true;
	}

	public ELContext getELContext() {
		if (this.elContext == null) {
			Application application = getApplication();
			this.elContext = new PortletELContextImpl(application.getELResolver());
			this.elContext.putContext(FacesContext.class, FacesContext.getCurrentInstance());
			UIViewRoot root = getViewRoot();
			if (null != root) {
				this.elContext.setLocale(root.getLocale());
			}
			ELContextListener[] listeners = application.getELContextListeners();
			if (listeners.length > 0) {
				ELContextEvent event = new ELContextEvent(this.elContext);
				for (ELContextListener listener : listeners) {
					listener.contextCreated(event);
				}
			}
		}
		return this.elContext;
	}

	private void assertFacesContextIsNotReleased() {
		Assert.isTrue(!this.released, "FacesContext already released");
	}

	private class PortletELContextImpl extends ELContext {

		private FunctionMapper functionMapper;
		private VariableMapper variableMapper;
		private final ELResolver resolver;

		public PortletELContextImpl(ELResolver resolver) {
			this.resolver = resolver;
		}

		@Override
		public FunctionMapper getFunctionMapper() {
			return this.functionMapper;
		}

		@Override
		public VariableMapper getVariableMapper() {
			return this.variableMapper;
		}

		@Override
		public ELResolver getELResolver() {
			return this.resolver;
		}

	}

}
