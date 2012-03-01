/*
 * Copyright 2004-2008 the original author or authors.
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

import javax.faces.FacesException;
import javax.faces.component.UIComponent;
import javax.faces.component.UIViewRoot;
import javax.faces.component.visit.VisitCallback;
import javax.faces.component.visit.VisitContext;
import javax.faces.component.visit.VisitResult;
import javax.faces.context.FacesContext;
import javax.faces.event.AbortProcessingException;
import javax.faces.event.ComponentSystemEvent;
import javax.faces.event.ExceptionQueuedEvent;
import javax.faces.event.ExceptionQueuedEventContext;
import javax.faces.event.PhaseEvent;
import javax.faces.event.PhaseId;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

import org.springframework.webflow.execution.RequestContextHolder;

/**
 * Common support for the JSF integration with Spring Web Flow.
 * 
 * @author Jeremy Grelle
 */
public class JsfUtils {

	public static void notifyAfterListeners(PhaseId phaseId, Lifecycle lifecycle, FacesContext context) {
		PhaseEvent afterPhaseEvent = new PhaseEvent(context, phaseId, lifecycle);
		for (int i = (lifecycle.getPhaseListeners().length - 1); i >= 0; i--) {
			PhaseListener listener = lifecycle.getPhaseListeners()[i];
			if (listener.getPhaseId() == phaseId || listener.getPhaseId() == PhaseId.ANY_PHASE) {
				listener.afterPhase(afterPhaseEvent);
			}
		}
	}

	public static void notifyBeforeListeners(PhaseId phaseId, Lifecycle lifecycle, FacesContext context) {
		PhaseEvent beforePhaseEvent = new PhaseEvent(context, phaseId, lifecycle);
		for (int i = 0; i < lifecycle.getPhaseListeners().length; i++) {
			PhaseListener listener = lifecycle.getPhaseListeners()[i];
			if (listener.getPhaseId() == phaseId || listener.getPhaseId() == PhaseId.ANY_PHASE) {
				listener.beforePhase(beforePhaseEvent);
			}
		}
	}

	public static boolean isFlowRequest() {
		if (RequestContextHolder.getRequestContext() != null) {
			return true;
		} else {
			return false;
		}
	}

	public static boolean isAsynchronousFlowRequest() {
		if (isFlowRequest() && RequestContextHolder.getRequestContext().getExternalContext().isAjaxRequest()) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Deliver a {@link ComponentSystemEvent} to the all components in the {@link FacesContext#getViewRoot() view root}
	 * dealing with any raised exceptions.
	 * @param facesContext the faces context
	 * @param eventFactory a factory class used to create and configure the event
	 * @throws FacesException
	 */
	public static <T extends ComponentSystemEvent> void deliverComponentSystemEvent(FacesContext facesContext,
			final ComponentEventFactory<T> eventFactory) throws FacesException {
		UIViewRoot root = facesContext.getViewRoot();
		final T event = eventFactory.createEvent(root);
		try {
			root.visitTree(VisitContext.createVisitContext(facesContext), new VisitCallback() {
				public VisitResult visit(VisitContext context, UIComponent target) {
					eventFactory.beforeVisit(event, context, target);
					target.processEvent(event);
					return VisitResult.ACCEPT;
				}
			});
		} catch (AbortProcessingException e) {
			facesContext.getApplication().publishEvent(facesContext, ExceptionQueuedEvent.class,
					new ExceptionQueuedEventContext(facesContext, e, null, facesContext.getCurrentPhaseId()));
		}
	}

	/**
	 * Factory class used with {@link JsfUtils#deliverComponentSystemEvent(FacesContext, ComponentEventFactory)}.
	 * @param <T> the type of event created
	 */
	public static abstract class ComponentEventFactory<T extends ComponentSystemEvent> {
		/**
		 * Create a new {@link ComponentSystemEvent} instance.
		 * @param root the current view root
		 * @return the newly created event
		 */
		public abstract T createEvent(UIViewRoot root);

		/**
		 * Called before every component visit. This method can be overridden to update the event before the target
		 * component is visited.
		 * @param event the event
		 * @param context the visit context
		 * @param target the target component
		 */
		public void beforeVisit(T event, VisitContext context, UIComponent target) {
		}
	}
}
