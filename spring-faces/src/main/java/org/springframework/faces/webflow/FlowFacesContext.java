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
package org.springframework.faces.webflow;

import java.io.IOException;
import java.lang.reflect.Method;
import java.util.Iterator;
import java.util.List;

import javax.el.ELContext;
import javax.faces.FactoryFinder;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.ExternalContextWrapper;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.context.FacesContextWrapper;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.context.MessageSource;
import org.springframework.faces.webflow.context.portlet.PortletFacesContextImpl;
import org.springframework.util.ClassUtils;
import org.springframework.webflow.execution.RequestContext;

/**
 * Custom {@link FacesContext} implementation that delegates all standard FacesContext messaging functionality to a
 * Spring {@link MessageSource} made accessible as part of the current Web Flow request. Additionally, it manages the
 * {@code renderResponse} flag in flash scope so that the execution of the JSF {@link Lifecycle} may span multiple
 * requests in the case of the POST+REDIRECT+GET pattern being enabled.
 * 
 * @author Jeremy Grelle
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 */
public class FlowFacesContext extends FacesContextWrapper {

	/**
	 * The key for storing the renderResponse flag
	 */
	static final String RENDER_RESPONSE_KEY = "flowRenderResponse";

	/**
	 * The key for storing the renderResponse flag
	 */
	private RequestContext context;

	private FlowFacesContextMessageDelegate messageDelegate;

	private ExternalContext externalContext;

	/**
	 * The wrapped FacesContext
	 */
	private FacesContext wrapped;

	private PartialViewContext partialViewContext;

	public static FlowFacesContext newInstance(RequestContext context, Lifecycle lifecycle) {
		FacesContext defaultFacesContext = null;
		if (JsfRuntimeInformation.isPortletRequest(context)) {
			defaultFacesContext = new PortletFacesContextImpl((PortletContext) context.getExternalContext()
					.getNativeContext(), (PortletRequest) context.getExternalContext().getNativeRequest(),
					(PortletResponse) context.getExternalContext().getNativeResponse());
		} else {
			FacesContextFactory facesContextFactory = (FacesContextFactory) FactoryFinder
					.getFactory(FactoryFinder.FACES_CONTEXT_FACTORY);
			defaultFacesContext = facesContextFactory.getFacesContext(context.getExternalContext().getNativeContext(),
					context.getExternalContext().getNativeRequest(), context.getExternalContext().getNativeResponse(),
					lifecycle);
		}
		return new FlowFacesContext(context, defaultFacesContext);
	}

	public FlowFacesContext(RequestContext context, FacesContext wrapped) {
		this.context = context;
		this.wrapped = wrapped;
		this.messageDelegate = new FlowFacesContextMessageDelegate(context);
		this.externalContext = new FlowExternalContext(wrapped.getExternalContext());
		setCurrentInstance(this);
		PartialViewContextFactory factory = (PartialViewContextFactory) FactoryFinder
				.getFactory(FactoryFinder.PARTIAL_VIEW_CONTEXT_FACTORY);
		PartialViewContext partialViewContextDelegate = factory.getPartialViewContext(this);
		this.partialViewContext = new FlowPartialViewContext(partialViewContextDelegate);
	}

	public FacesContext getWrapped() {
		return wrapped;
	}

	/**
	 * Translates a FacesMessage to a Spring Web Flow message and adds it to the current MessageContext
	 */
	public void addMessage(String clientId, FacesMessage message) {
		messageDelegate.addToFlowMessageContext(clientId, message);
	}

	/**
	 * Returns an Iterator for all component clientId's for which messages have been added.
	 */
	public Iterator<String> getClientIdsWithMessages() {
		return messageDelegate.getClientIdsWithMessages();
	}

	// FIXME PW revist
	public ELContext getELContext() {
		Method delegateMethod = ClassUtils.getMethodIfAvailable(wrapped.getClass(), "getELContext");
		if (delegateMethod != null) {
			try {
				ELContext context = (ELContext) delegateMethod.invoke(wrapped);
				context.putContext(FacesContext.class, this);
				return context;
			} catch (Exception e) {
				return null;
			}
		} else {
			return null;
		}
	}

	/**
	 * Return the maximum severity level recorded on any FacesMessages that has been queued, whether or not they are
	 * associated with any specific UIComponent. If no such messages have been queued, return null.
	 */
	public FacesMessage.Severity getMaximumSeverity() {
		return messageDelegate.getMaximumSeverity();
	}

	/**
	 * Returns an Iterator for all Messages in the current MessageContext that does translation to FacesMessages.
	 */
	public Iterator<FacesMessage> getMessages() {
		return messageDelegate.getMessages();
	}

	/**
	 * Returns an Iterator for all Messages with the given clientId in the current MessageContext that does translation
	 * to FacesMessages.
	 */
	public Iterator<FacesMessage> getMessages(String clientId) {
		return messageDelegate.getMessages(clientId);
	}

	public boolean getRenderResponse() {
		Boolean renderResponse = context.getFlashScope().getBoolean(RENDER_RESPONSE_KEY);
		return (renderResponse == null ? false : renderResponse);
	}

	public boolean getResponseComplete() {
		return context.getExternalContext().isResponseComplete();
	}

	public void renderResponse() {
		// stored in flash scope to survive a redirect when transitioning from one view to another
		context.getFlashScope().put(RENDER_RESPONSE_KEY, true);
	}

	public void responseComplete() {
		context.getExternalContext().recordResponseComplete();
	}

	public void release() {
		super.release();
		// FIXME PW revist
		setCurrentInstance(null);
	}

	// FIXME PW remove the message delegate, makes less sense now
	protected FlowFacesContextMessageDelegate getMessageDelegate() {
		return messageDelegate;
	}

	public ExternalContext getExternalContext() {
		return externalContext;
	}

	public PartialViewContext getPartialViewContext() {
		return partialViewContext;
	}

	/**
	 * Returns a List for all Messages in the current MessageContext that does translation to FacesMessages.
	 */
	public List<FacesMessage> getMessageList() {
		return getMessageDelegate().getMessageList();
	}

	/**
	 * Returns a List for all Messages with the given clientId in the current MessageContext that does translation to
	 * FacesMessages.
	 */
	public List<FacesMessage> getMessageList(String clientId) {
		return getMessageDelegate().getMessageList(clientId);
	}

	public boolean isValidationFailed() {
		if (getMessageDelegate().hasErrorMessages()) {
			return true;
		} else {
			return super.isValidationFailed();
		}
	}

	// FIXME PW make top level class

	protected class FlowExternalContext extends ExternalContextWrapper {

		Log logger = LogFactory.getLog(FlowExternalContext.class);

		private static final String CUSTOM_RESPONSE = "customResponse";

		private ExternalContext wrapped;

		public FlowExternalContext(ExternalContext wrapped) {
			this.wrapped = wrapped;
		}

		public ExternalContext getWrapped() {
			return wrapped;
		}

		public Object getResponse() {
			if (context.getRequestScope().contains(CUSTOM_RESPONSE)) {
				return context.getRequestScope().get(CUSTOM_RESPONSE);
			}
			return super.getResponse();
		}

		/**
		 * Store the native response object to be used for the duration of the Faces Request
		 */
		public void setResponse(Object response) {
			context.getRequestScope().put(CUSTOM_RESPONSE, response);
			super.setResponse(response);
		}

		public void responseSendError(int statusCode, String message) throws IOException {
			logger.debug("Sending error HTTP status code " + statusCode + " with message '" + message + "'");
			super.responseSendError(statusCode, message);
		}

	}

}
