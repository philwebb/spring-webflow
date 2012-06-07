
package org.springframework.faces.webflow.context.portlet;

import java.util.List;
import java.util.Map;

import javax.faces.application.ViewHandler;
import javax.faces.application.ViewHandlerWrapper;
import javax.faces.context.FacesContext;
import javax.portlet.MimeResponse;
import javax.portlet.ResourceURL;

import org.springframework.web.util.UriComponents;
import org.springframework.web.util.UriComponentsBuilder;

public class PortletViewHandler extends ViewHandlerWrapper {

	private static final String FACES_RESOURCE = "javax.faces.resource";

	private ViewHandler wrapped;

	public PortletViewHandler(ViewHandler wrapped) {
		this.wrapped = wrapped;
	}

	public ViewHandler getWrapped() {
		return wrapped;
	}

	public String getResourceURL(FacesContext context, String path) {
		String uri = super.getResourceURL(context, path);
		int facesResourceIndex = (uri == null ? -1 : uri.indexOf(FACES_RESOURCE));
		if (facesResourceIndex == -1) {
			return uri;
		}
		UriComponents components = UriComponentsBuilder.fromUriString(uri.substring(facesResourceIndex + FACES_RESOURCE.length() + 1)).build();
		MimeResponse response = (MimeResponse) context.getExternalContext().getResponse();
		ResourceURL resourceURL = response.createResourceURL();
		for (Map.Entry<String, List<String>> entry : components.getQueryParams().entrySet()) {
			String name = entry.getKey();
			List<String> value = entry.getValue();
			resourceURL.setParameter(name, value.toArray(new String[value.size()]));
		}
		resourceURL.setParameter(FACES_RESOURCE, components.getPath());
		return resourceURL.toString();
	}

}
