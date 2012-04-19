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
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import javax.el.ELContext;
import javax.faces.application.FacesMessage;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.context.FacesContextFactory;
import javax.faces.context.FacesContextWrapper;
import javax.faces.context.PartialViewContext;
import javax.faces.context.PartialViewContextFactory;
import javax.faces.lifecycle.Lifecycle;
import javax.portlet.PortletContext;
import javax.portlet.PortletRequest;
import javax.portlet.PortletResponse;

import org.springframework.binding.message.Message;
import org.springframework.binding.message.MessageCriteria;
import org.springframework.binding.message.MessageResolver;
import org.springframework.binding.message.Severity;
import org.springframework.context.MessageSource;
import org.springframework.core.style.ToStringCreator;
import org.springframework.faces.webflow.context.portlet.PortletFacesContextImpl;
import org.springframework.util.StringUtils;
import org.springframework.webflow.execution.RequestContext;

/**
 * Custom {@link FacesContext} implementation that delegates all standard FacesContext messaging functionality to a
 * Spring {@link MessageSource} made accessible as part of the current Web Flow request. Additionally, it manages the
 * {@code renderResponse} flag in flash scope so that the execution of the JSF {@link Lifecycle} may span multiple
 * requests in the case of the POST+REDIRECT+GET pattern being enabled.
 * 
 * @see FlowExternalContext
 * 
 * @author Jeremy Grelle
 * @author Phillip Webb
 * @author Rossen Stoyanchev
 */
public class FlowFacesContext extends FacesContextWrapper {

	/**
	 * The key for storing the renderResponse flag
	 */
	private static final String RENDER_RESPONSE_KEY = "flowRenderResponse";

	/**
	 * Key for identifying summary messages
	 */
	private static final String SUMMARY_MESSAGE_KEY = "_summary";

	/**
	 * Key for identifying detail messages
	 */
	private static final String DETAIL_MESSAGE_KEY = "_detail";

	/**
	 * Mappings between {@link FacesMessage} and {@link Severity}.
	 */
	private static final Map<FacesMessage.Severity, Severity> SEVERITY_MAPPING;
	static {
		SEVERITY_MAPPING = new LinkedHashMap<FacesMessage.Severity, Severity>();
		SEVERITY_MAPPING.put(FacesMessage.SEVERITY_INFO, Severity.INFO);
		SEVERITY_MAPPING.put(FacesMessage.SEVERITY_WARN, Severity.WARNING);
		SEVERITY_MAPPING.put(FacesMessage.SEVERITY_ERROR, Severity.ERROR);
		SEVERITY_MAPPING.put(FacesMessage.SEVERITY_FATAL, Severity.FATAL);
	}

	private static MessageCriteria SUMMARY_MESSAGES = new MessagesEnding(SUMMARY_MESSAGE_KEY);
	private static MessageCriteria DETAIL_MESSAGES = new MessagesEnding(DETAIL_MESSAGE_KEY);
	private static MessageCriteria USER_MESSAGES = new MessageCriteria() {
		public boolean test(Message message) {
			return !(SUMMARY_MESSAGES.test(message) || DETAIL_MESSAGES.test(message));
		}
	};

	private FacesContext wrapped;

	private RequestContext context;

	private ExternalContext externalContext;

	private PartialViewContext partialViewContext;

	public FlowFacesContext(RequestContext context, FacesContext wrapped) {
		this.context = context;
		this.wrapped = wrapped;
		this.externalContext = new FlowExternalContext(context, wrapped.getExternalContext());
		PartialViewContextFactory factory = JsfUtils.findFactory(PartialViewContextFactory.class);
		PartialViewContext partialViewContextDelegate = factory.getPartialViewContext(this);
		this.partialViewContext = new FlowPartialViewContext(partialViewContextDelegate);
		setCurrentInstance(this);
	}

	public FacesContext getWrapped() {
		return wrapped;
	}

	public void release() {
		super.release();
		setCurrentInstance(null);
	}

	public ExternalContext getExternalContext() {
		return externalContext;
	}

	public PartialViewContext getPartialViewContext() {
		return partialViewContext;
	}

	public ELContext getELContext() {
		ELContext elContext = super.getELContext();
		// Ensure that our wrapper is used over the stock FacesContextImpl
		elContext.putContext(FacesContext.class, this);
		return elContext;
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

	public boolean isValidationFailed() {
		if (context.getMessageContext().hasErrorMessages()) {
			return true;
		} else {
			return super.isValidationFailed();
		}
	}

	/**
	 * Translates a FacesMessage to a Spring Web Flow message and adds it to the current MessageContext
	 */
	public void addMessage(String clientId, FacesMessage message) {
		String source = null;
		if (StringUtils.hasText(clientId)) {
			source = clientId;
		}
		context.getMessageContext().addMessage(new FlowFacesMessageAdapter(source, SUMMARY_MESSAGE_KEY, message));
		context.getMessageContext().addMessage(new FlowFacesMessageAdapter(source, DETAIL_MESSAGE_KEY, message));
	}

	/**
	 * Returns an Iterator for all component clientId's for which messages have been added.
	 */
	public Iterator<String> getClientIdsWithMessages() {
		ClientIdCollector clientIdCollector = new ClientIdCollector();
		context.getMessageContext().getMessagesByCriteria(new ClientIdCollector());
		// FIXME make unmodifiable
		return clientIdCollector.clientIds.iterator();
	}

	/**
	 * Return the maximum severity level recorded on any FacesMessages that has been queued, whether or not they are
	 * associated with any specific UIComponent. If no such messages have been queued, return null.
	 */
	public FacesMessage.Severity getMaximumSeverity() {
		if (context.getMessageContext().getAllMessages().length == 0) {
			return null;
		}
		FacesMessage.Severity max = FacesMessage.SEVERITY_INFO;
		Iterator<FacesMessage> messages = getMessages();
		while (messages.hasNext()) {
			FacesMessage message = messages.next();
			if (message.getSeverity().getOrdinal() > max.getOrdinal()) {
				max = message.getSeverity();
			}
			if (max.getOrdinal() == FacesMessage.SEVERITY_FATAL.getOrdinal()) {
				break;
			}
		}
		return max;
	}

	/**
	 * Returns an Iterator for all Messages in the current MessageContext that does translation to FacesMessages.
	 */
	public Iterator<FacesMessage> getMessages() {
		return getMessageList().iterator();
	}

	/**
	 * Returns a List for all Messages in the current MessageContext that does translation to FacesMessages.
	 */
	public List<FacesMessage> getMessageList() {
		Message[] summaryMessages = context.getMessageContext().getMessagesByCriteria(SUMMARY_MESSAGES);
		Message[] detailMessages = context.getMessageContext().getMessagesByCriteria(DETAIL_MESSAGES);
		Message[] userMessages = context.getMessageContext().getMessagesByCriteria(USER_MESSAGES);
		return buildMessages(summaryMessages, detailMessages, userMessages);
	}

	/**
	 * Returns an Iterator for all Messages with the given clientId in the current MessageContext that does translation
	 * to FacesMessages.
	 */
	public Iterator<FacesMessage> getMessages(String clientId) {
		return getMessageList(clientId).iterator();
	}

	/**
	 * Returns a List for all Messages with the given clientId in the current MessageContext that does translation to
	 * FacesMessages.
	 */
	public List<FacesMessage> getMessageList(String clientId) {
		Message[] summaryMessages = context.getMessageContext().getMessagesBySource(clientId + SUMMARY_MESSAGE_KEY);
		Message[] detailMessages = context.getMessageContext().getMessagesBySource(clientId + DETAIL_MESSAGE_KEY);
		Message[] userMessages = context.getMessageContext().getMessagesBySource(clientId);
		return buildMessages(summaryMessages, detailMessages, userMessages);
	}

	private FacesMessage getFacesMessage(Message summary, Message detail) {
		// If we can return the actual message instance.
		if (summary instanceof FlowFacesMessageAdapter) {
			return ((FlowFacesMessageAdapter) summary).getFacesMessage();
		}
		if (detail instanceof FlowFacesMessageAdapter) {
			return ((FlowFacesMessageAdapter) detail).getFacesMessage();
		}

		for (Map.Entry<FacesMessage.Severity, Severity> mapping : SEVERITY_MAPPING.entrySet()) {
			if (summary.getSeverity() == mapping.getValue()) {
				return new FacesMessage(mapping.getKey(), summary.getText(), detail.getText());
			}
		}
		return new FacesMessage(FacesMessage.SEVERITY_FATAL, summary.getText(), detail.getText());
	}

	private List<FacesMessage> buildMessages(Message[] summaryMessages, Message[] detailMessages, Message[] userMessages) {
		List<FacesMessage> messages = new ArrayList<FacesMessage>();
		for (int i = 0; i < summaryMessages.length; i++) {
			messages.add(getFacesMessage(summaryMessages[i], detailMessages[i]));
		}
		for (Message userMessage : userMessages) {
			messages.add(getFacesMessage(userMessage, userMessage));
		}
		return Collections.unmodifiableList(messages);
	}

	public static FlowFacesContext newInstance(RequestContext context, Lifecycle lifecycle) {
		FacesContext defaultFacesContext = newDefaultInstance(context, lifecycle);
		return new FlowFacesContext(context, defaultFacesContext);
	}

	public static FacesContext newDefaultInstance(RequestContext context, Lifecycle lifecycle) {
		Object nativeContext = context.getExternalContext().getNativeContext();
		Object nativeRequest = context.getExternalContext().getNativeRequest();
		Object nativeResponse = context.getExternalContext().getNativeResponse();
		if (JsfRuntimeInformation.isPortletRequest(context)) {
			return new PortletFacesContextImpl((PortletContext) nativeContext, (PortletRequest) nativeRequest,
					(PortletResponse) nativeResponse);
		}
		FacesContextFactory facesContextFactory = JsfUtils.findFactory(FacesContextFactory.class);
		return facesContextFactory.getFacesContext(nativeContext, nativeRequest, nativeResponse, lifecycle);
	}

	private static class MessagesEnding implements MessageCriteria {

		private String ending;

		public MessagesEnding(String ending) {
			this.ending = ending;
		}

		public boolean test(Message message) {
			return message.getSource() != null && message.getSource().toString().endsWith(ending);
		}
	}

	private static class ClientIdCollector implements MessageCriteria {

		private static final String NULL_SUMMARY_ID = null + SUMMARY_MESSAGE_KEY;

		private Set<String> clientIds = new HashSet<String>();

		public boolean test(Message message) {
			if (DETAIL_MESSAGES.equals(message)) {
				return false;
			}
			Object source = message.getSource();
			if ("".equals(source) || NULL_SUMMARY_ID.equals(source)) {
				// From getClientIdsWithMessages docs:
				// If any messages have been queued that were not associated with
				// any specific client identifier, a null value will be included in the iterated values.
				source = null;
			}
			String clientId = null;
			if (source != null) {
				clientId = source.toString();
				if (clientId.endsWith(SUMMARY_MESSAGE_KEY)) {
					clientId = clientId.replaceAll(SUMMARY_MESSAGE_KEY, "");
				}
			}
			if (!clientIds.contains(clientId)) {
				clientIds.add(clientId);
				return true;
			}
			return false;
		}
	}

	/**
	 * Adapter class to convert a {@link FacesMessage} to a Spring {@link Message}. This adapter is required to allow
	 * <tt>FacesMessages</tt> to be registered with Spring whilst still retaining their mutable nature. It is not
	 * uncommon for <tt>FacesMessages</tt> to be changed after they have been added to a <tt>FacesContext</tt>, for
	 * example, from a <tt>PhaseListener</tt>.
	 * <p>
	 * NOTE: Only {@link javax.faces.application.FacesMessage} instances are directly adapted, any subclasses will be
	 * converted to the standard FacesMessage implementation. This is to protect against bugs such as SWF-1073.
	 * 
	 * For convenience this class also implements the {@link MessageResolver} interface.
	 */
	private static class FlowFacesMessageAdapter extends Message implements MessageResolver {

		private String key;
		private String source;
		private transient FacesMessage facesMessage;

		public FlowFacesMessageAdapter(String source, String key, FacesMessage message) {
			super(null, null, null);
			this.source = source;
			this.key = key;
			this.facesMessage = asStandardFacesMessageInstance(message);
		}

		// Custom serialization to work around myfaces bug MYFACES-1347

		private void writeObject(ObjectOutputStream oos) throws IOException {
			oos.defaultWriteObject();
			oos.writeObject(facesMessage.getSummary());
			oos.writeObject(facesMessage.getDetail());
			oos.writeInt(facesMessage.getSeverity().getOrdinal());
		}

		private void readObject(ObjectInputStream ois) throws ClassNotFoundException, IOException {
			ois.defaultReadObject();
			String summary = (String) ois.readObject();
			String detail = (String) ois.readObject();
			int severityOrdinal = ois.readInt();
			FacesMessage.Severity severity = FacesMessage.SEVERITY_INFO;
			for (Iterator<?> iterator = FacesMessage.VALUES.iterator(); iterator.hasNext();) {
				FacesMessage.Severity value = (FacesMessage.Severity) iterator.next();
				if (value.getOrdinal() == severityOrdinal) {
					severity = value;
				}
			}
			facesMessage = new FacesMessage(severity, summary, detail);
		}

		/**
		 * Use standard faces message as required to protect against bugs such as SWF-1073.
		 * 
		 * @param message {@link javax.faces.application.FacesMessage} or subclass.
		 * @return {@link javax.faces.application.FacesMessage} instance
		 */
		private FacesMessage asStandardFacesMessageInstance(FacesMessage message) {
			if (FacesMessage.class.equals(message.getClass())) {
				return message;
			}
			return new FacesMessage(message.getSeverity(), message.getSummary(), message.getDetail());
		}

		public Object getSource() {
			return source + key;
		}

		public String getText() {
			String text = null;
			if (DETAIL_MESSAGE_KEY.equals(key)) {
				text = facesMessage.getDetail();
			} else if (SUMMARY_MESSAGE_KEY.equals(key)) {
				text = facesMessage.getSummary();
			} else {
				throw new RuntimeException("Unknown faces message type key");
			}

			if (StringUtils.hasText(text)) {
				return text;
			}
			return "";
		}

		public Severity getSeverity() {
			Severity severity = null;
			if (facesMessage.getSeverity() != null) {
				severity = SEVERITY_MAPPING.get(facesMessage.getSeverity());
			}
			return (severity == null ? Severity.INFO : severity);
		}

		public String toString() {
			ToStringCreator rtn = new ToStringCreator(this);
			rtn.append("severity", getSeverity());
			if (FacesContext.getCurrentInstance() != null) {
				// Only append text if running within a faces context
				rtn.append("text", getText());
			}
			return rtn.toString();
		}

		public Message resolveMessage(MessageSource messageSource, Locale locale) {
			return this;
		}

		/**
		 * @return The original {@link FacesMessage} adapted by this class.
		 */
		public FacesMessage getFacesMessage() {
			return facesMessage;
		}
	}

}
