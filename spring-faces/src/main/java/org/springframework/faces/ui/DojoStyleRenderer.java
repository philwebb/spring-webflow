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
package org.springframework.faces.ui;

import java.io.IOException;

import javax.faces.component.UIComponent;
import javax.faces.context.FacesContext;
import javax.faces.render.Renderer;

import org.springframework.faces.ui.resource.ResourceHelper;

/**
 * {@link Renderer} implementation that renders the CSS resources required by Dojo's widget system.
 * 
 * @author Jeremy Grelle
 * 
 */
public class DojoStyleRenderer extends Renderer {

	private static final String THEME_PATH_ATTR = "themePath";

	private static final String THEME_ATTR = "theme";

	public void encodeBegin(FacesContext context, UIComponent component) throws IOException {

		String themePath = DojoConstants.DIJIT_THEME_PATH;
		String theme = DojoConstants.DEFAULT_DIJIT_THEME;

		if (component.getAttributes().containsKey(THEME_PATH_ATTR)) {
			themePath = (String) component.getAttributes().get(THEME_PATH_ATTR);
			context.getViewRoot().getAttributes().put(DojoConstants.CUSTOM_THEME_PATH_SET, Boolean.TRUE);
		}

		if (component.getAttributes().containsKey(THEME_ATTR)) {
			theme = (String) component.getAttributes().get(THEME_ATTR);
			context.getViewRoot().getAttributes().put(DojoConstants.CUSTOM_THEME_SET, Boolean.TRUE);
		}

		ResourceHelper.renderStyleLink(context, themePath + theme + "/" + theme + ".css");
	}
}
