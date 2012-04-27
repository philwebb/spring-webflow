package org.springframework.faces.webflow.context.portlet;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;

public class PortletViewHandler extends ViewHandlerWrapper {

	private ViewHandler wrapped;

	public PortletViewHandler(ViewHandler wrapped) {
		this.wrapped = wrapped;
	}

	public ViewHandler getWrapped() {
		return wrapped;
	}

	public String getResourceURL(FacesContext context, String path) {
		String url = super.getResourceURL(context, path);
		url = url.replace("/booking-portlet-faces/", "/booking-portlet-faces/spring/");
		return url;
		// System.out.println(url);
		// MimeResponse response = (MimeResponse) context.getExternalContext().getResponse();
		// ResourceURL resourceURL = response.createResourceURL();
		// resourceURL.setResourceID("test");
		// return resourceURL.toString();
	}
}
