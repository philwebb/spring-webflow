package org.springframework.faces.webflow.context.portlet;

import java.io.IOException;
import java.util.Map;

import javax.faces.application.Resource;
import javax.faces.application.ResourceHandler;
import javax.faces.context.FacesContext;
import javax.portlet.ActionRequest;
import javax.portlet.ActionResponse;
import javax.portlet.EventRequest;
import javax.portlet.EventResponse;
import javax.portlet.PortletRequest;
import javax.portlet.RenderRequest;
import javax.portlet.RenderResponse;
import javax.portlet.ResourceRequest;
import javax.portlet.ResourceResponse;

import org.springframework.core.Ordered;
import org.springframework.faces.webflow.FacesContextHelper;
import org.springframework.util.FileCopyUtils;
import org.springframework.web.portlet.HandlerAdapter;
import org.springframework.web.portlet.HandlerExecutionChain;
import org.springframework.web.portlet.HandlerMapping;
import org.springframework.web.portlet.ModelAndView;
import org.springframework.web.portlet.handler.PortletContentGenerator;

public class JsfResourceRequestHandler extends PortletContentGenerator implements HandlerAdapter, HandlerMapping,
		Ordered {
	private static final String FACES_RESOURCE = "javax.faces.resource";

	private int order = Ordered.HIGHEST_PRECEDENCE;

	public HandlerExecutionChain getHandler(PortletRequest request) throws Exception {
		if (request instanceof ResourceRequest && request.getParameter(FACES_RESOURCE) != null) {
			return new HandlerExecutionChain(new JsfResourceRequest());
		}
		return null;
	}

	public boolean supports(Object handler) {
		return handler instanceof JsfResourceRequest;
	}

	public ModelAndView handleResource(ResourceRequest request, ResourceResponse response, Object handler)
			throws IOException {
		FacesContextHelper helper = new FacesContextHelper();
		try {
			FacesContext facesContext = helper.getFacesContext(getPortletContext(), request, response);
			handleResourceRequest(facesContext, request, response);
		} finally {
			helper.releaseIfNecessary();
		}
		return null;
	}

	private void handleResourceRequest(FacesContext facesContext, ResourceRequest request, ResourceResponse response)
			throws IOException {
		ResourceHandler resourceHandler = facesContext.getApplication().getResourceHandler();
		String resourceName = request.getParameter(FACES_RESOURCE);
		String libraryName = request.getParameter("ln");
		if (isResourceIdentifierExcluded(resourceName)) {
			// FIXME set status
			// HttpStatus.NOT_FOUND;
			return;
		}
		Resource resource = createResource(resourceHandler, resourceName, libraryName);
		if (resource == null) {
			// FIXME SC_NOT_FOUND
			return;
		}
		for (Map.Entry<String, String> entry : resource.getResponseHeaders().entrySet()) {
			response.setProperty(entry.getKey(), entry.getValue());
		}
		response.setContentType(resource.getContentType());
		FileCopyUtils.copy(resource.getInputStream(), response.getPortletOutputStream());
	}

	private boolean isResourceIdentifierExcluded(String resourceName) {
		// FIXME
		return false;
	}

	private Resource createResource(ResourceHandler resourceHandler, String resourceName, String libraryName) {
		if (libraryName != null) {
			return resourceHandler.createResource(resourceName, libraryName);
		}
		return resourceHandler.createResource(resourceName);
	}

	public void handleAction(ActionRequest request, ActionResponse response, Object handler) throws Exception {
	}

	public ModelAndView handleRender(RenderRequest request, RenderResponse response, Object handler) throws Exception {
		return null;
	}

	public void handleEvent(EventRequest request, EventResponse response, Object handler) throws Exception {
	}

	public int getOrder() {
		return order;
	}

	public void setOrder(int order) {
		this.order = order;
	}

	private static class JsfResourceRequest {
	}

}