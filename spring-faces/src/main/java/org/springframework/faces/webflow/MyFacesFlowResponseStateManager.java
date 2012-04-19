package org.springframework.faces.webflow;

import java.io.IOException;

import javax.faces.FacesWrapper;
import javax.faces.application.StateManager.SerializedView;
import javax.faces.context.FacesContext;
import javax.faces.render.ResponseStateManager;

import org.apache.myfaces.renderkit.MyfacesResponseStateManager;
import org.apache.myfaces.renderkit.StateCacheUtils;

public class MyFacesFlowResponseStateManager extends MyfacesResponseStateManager implements
		FacesWrapper<ResponseStateManager> {

	private FlowResponseStateManager flowResponseStateManager;

	public MyFacesFlowResponseStateManager(FlowResponseStateManager flowResponseStateManager) {
		this.flowResponseStateManager = flowResponseStateManager;
	}

	public ResponseStateManager getWrapped() {
		return flowResponseStateManager;
	}

	private MyfacesResponseStateManager getWrappedMyfacesResponseStateManager() {
		return StateCacheUtils.getMyFacesResponseStateManager(flowResponseStateManager);
	}

	public boolean isWriteStateAfterRenderViewRequired(FacesContext facesContext) {
		MyfacesResponseStateManager wrapped = getWrappedMyfacesResponseStateManager();
		if (wrapped != null) {
			return wrapped.isWriteStateAfterRenderViewRequired(facesContext);
		}
		return super.isWriteStateAfterRenderViewRequired(facesContext);
	}

	public void saveState(FacesContext facesContext, Object state) {
		flowResponseStateManager.saveState(state);
	}

	@Deprecated
	public void writeStateAsUrlParams(FacesContext facesContext, SerializedView serializedview) {
		MyfacesResponseStateManager wrapped = getWrappedMyfacesResponseStateManager();
		if (wrapped != null) {
			wrapped.writeStateAsUrlParams(facesContext, serializedview);
		}
		super.writeStateAsUrlParams(facesContext, serializedview);
	}

	@Deprecated
	public Object getComponentStateToRestore(FacesContext context) {
		return getWrapped().getComponentStateToRestore(context);
	}

	public Object getState(FacesContext context, String viewId) {
		return getWrapped().getState(context, viewId);
	}

	@Deprecated
	public Object getTreeStructureToRestore(FacesContext context, String viewId) {
		return getWrapped().getTreeStructureToRestore(context, viewId);
	}

	public String getViewState(FacesContext context, Object state) {
		return getWrapped().getViewState(context, state);
	}

	public boolean isPostback(FacesContext context) {
		return getWrapped().isPostback(context);
	}

	public void writeState(FacesContext arg0, Object arg1) throws IOException {
		getWrapped().writeState(arg0, arg1);
	}

	@Deprecated
	public void writeState(FacesContext context, SerializedView state) throws IOException {
		getWrapped().writeState(context, state);
	}

}
