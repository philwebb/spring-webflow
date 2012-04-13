/*
 * Copyright 2004-2011 the original author or authors.
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

import javax.faces.application.StateManager;
import javax.faces.application.StateManagerWrapper;
import javax.faces.context.FacesContext;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

/**
 * Custom {@link StateManager} that manages the JSF component state in web flow's view scope.
 * 
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 */
public class FlowViewStateManager extends StateManagerWrapper {

	private static final Log logger = LogFactory.getLog(FlowViewStateManager.class);

	// FIXME PW rename and check usage
	protected static final String SERIALIZED_VIEW_STATE = "flowSerializedViewState";

	private StateManager wrapped;

	public FlowViewStateManager(StateManager wrapped) {
		this.wrapped = wrapped;
	}

	public StateManager getWrapped() {
		return wrapped;
	}

	public boolean isSavingStateInClient(FacesContext context) {
		if (!JsfUtils.isFlowRequest()) {
			return super.isSavingStateInClient(context);
		} else {
			return false;
		}
	}

	/**
	 * <p>
	 * JSF 1.2 (or higher) version of state saving.
	 * 
	 * <p>
	 * In JSF 2, if partial state saving is enabled this method delegates in order to obtain the serialized view state.
	 * During rendering JSF calls this method to prepare the state and then calls {@link FlowViewResponseStateManager}
	 * which writes it to Web Flow's view scope.
	 * 
	 * <p>
	 * Nevertheless this method always writes the serialized state to Web Flow's view scope to ensure it is up-to-date
	 * for cases outside of rendering (e.g. ViewState.updateHistory()) or when the render phase doesn't call
	 * {@link FlowViewResponseStateManager} such as when processing a partial request.
	 */
	public Object saveView(FacesContext context) {
		if (context.getViewRoot().isTransient()) {
			return null;
		}
		if (!JsfUtils.isFlowRequest()) {
			return super.saveView(context);
		}
		Object state = super.saveView(context);
		RequestContext requestContext = RequestContextHolder.getRequestContext();
		if (logger.isDebugEnabled()) {
			logger.debug("Saving view root '" + context.getViewRoot().getViewId() + "' in view scope");
		}
		requestContext.getViewScope().put(SERIALIZED_VIEW_STATE, state);
		return state;
	}

	// FIXME PW revisit, is this save
	// @Override
	// public String getViewState(FacesContext context) {
	// if (!JsfUtils.isFlowRequest()) {
	// return super.getViewState(context);
	// }
	// /*
	// * Mojarra 2: PartialRequestContextImpl.renderState() invokes this method during Ajax request rendering. We
	// * overridde it to convert FlowSerializedView state to an array before calling the
	// * ResponseStateManager.getViewState(), which in turn calls the ServerSideStateHelper and expects state to be an
	// * array.
	// */
	// Object state = saveView(context);
	// if (state != null) {
	// if (state instanceof FlowSerializedView) {
	// FlowSerializedView view = (FlowSerializedView) state;
	// state = view.asTreeStructAndCompStateArray();
	// }
	// return context.getRenderKit().getResponseStateManager().getViewState(context, state);
	// }
	// return null;
	// }

}
