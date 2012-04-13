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

import java.lang.reflect.Method;

import javax.faces.application.StateManager;
import javax.faces.application.StateManagerWrapper;
import javax.faces.context.FacesContext;
import javax.faces.render.ResponseStateManager;
import javax.faces.view.ViewDeclarationLanguage;

import org.springframework.util.ReflectionUtils;
import org.springframework.webflow.execution.RequestContext;
import org.springframework.webflow.execution.RequestContextHolder;

/**
 * Custom {@link StateManager} that manages ensures web flow's state is always stored server side.
 * 
 * @author Jeremy Grelle
 * @author Rossen Stoyanchev
 * @author Phillip Webb
 */
public class FlowViewStateManager extends StateManagerWrapper {

	private static final String MYFACES_STATE_CACHE_UTILS = "org.apache.myfaces.renderkit.StateCacheUtils";

	private StateManager wrapped;

	private Method isMyFacesResponseStateManagerMethod;

	public FlowViewStateManager(StateManager wrapped) {
		this.wrapped = wrapped;
		this.isMyFacesResponseStateManagerMethod = findIsMyFacesResponseStateManagerMethod();
	}

	private Method findIsMyFacesResponseStateManagerMethod() {
		try {
			return ReflectionUtils.findMethod(Class.forName(MYFACES_STATE_CACHE_UTILS),
					"isMyFacesResponseStateManager", ResponseStateManager.class);
		} catch (ClassNotFoundException e) {
			return null;
		}
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

	public Object saveView(FacesContext context) {
		Object state = super.saveView(context);
		if (wouldCallMyfacesResponseStateManagerSaveState(context)) {
			RequestContext requestContext = RequestContextHolder.getRequestContext();
			requestContext.getViewScope().put(FlowViewResponseStateManager.FACES_VIEW_STATE, state);
		}
		return state;
	}

	private boolean wouldCallMyfacesResponseStateManagerSaveState(FacesContext context) {
		ResponseStateManager responseStateManager = context.getRenderKit().getResponseStateManager();
		if (!isMyFacesResponseStateManager(responseStateManager)) {
			return false;
		}

		String viewId = context.getViewRoot().getViewId();
		ViewDeclarationLanguage viewDeclarationLanguage = context.getApplication().getViewHandler()
				.getViewDeclarationLanguage(context, viewId);
		if (viewDeclarationLanguage == null) {
			return false;
		}
		return (viewDeclarationLanguage.getStateManagementStrategy(context, viewId) != null);
	}

	private boolean isMyFacesResponseStateManager(ResponseStateManager responseStateManager) {
		if (isMyFacesResponseStateManagerMethod == null) {
			return false;
		}
		return (Boolean) ReflectionUtils.invokeMethod(isMyFacesResponseStateManagerMethod, null, responseStateManager);
	}
}
