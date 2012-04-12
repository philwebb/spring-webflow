package org.springframework.faces.util;

import javax.faces.FacesException;
import javax.faces.FacesWrapper;
import javax.faces.context.FacesContext;
import javax.faces.event.PhaseListener;
import javax.faces.lifecycle.Lifecycle;

public abstract class LifecycleWrapper extends Lifecycle implements FacesWrapper<Lifecycle> {

	// FIXME PW DC + Test

	public abstract Lifecycle getWrapped();

	public void addPhaseListener(PhaseListener listener) {
		getWrapped().addPhaseListener(listener);
	}

	public void execute(FacesContext context) throws FacesException {
		getWrapped().execute(context);
	}

	public PhaseListener[] getPhaseListeners() {
		return getWrapped().getPhaseListeners();
	}

	public void removePhaseListener(PhaseListener listener) {
		getWrapped().removePhaseListener(listener);
	}

	public void render(FacesContext context) throws FacesException {
		getWrapped().render(context);
	}
}
