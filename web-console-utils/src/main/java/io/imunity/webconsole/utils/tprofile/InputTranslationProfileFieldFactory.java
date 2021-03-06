/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package io.imunity.webconsole.utils.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.vaadin.data.Binder;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webadmin.tprofile.ActionParameterComponentProvider;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.translation.TranslationActionFactory;
import pl.edu.icm.unity.engine.api.translation.in.InputTranslationActionsRegistry;
import pl.edu.icm.unity.engine.api.utils.TypesRegistryBase;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * Factory for {@link TranslationProfileField}. 
 * @author P.Piernik
 *
 */
@Component
public class InputTranslationProfileFieldFactory
{
	private UnityMessageSource msg;
	private TypesRegistryBase<? extends TranslationActionFactory<?>> registry;
	private ActionParameterComponentProvider actionComponentProvider;

	@Autowired
	InputTranslationProfileFieldFactory(UnityMessageSource msg,
			InputTranslationActionsRegistry inputActionsRegistry,
			ActionParameterComponentProvider actionComponentProvider)
	{

		this.msg = msg;
		this.registry = inputActionsRegistry;
		this.actionComponentProvider = actionComponentProvider;
	}

	public TranslationProfileField getInstance(SubViewSwitcher subViewSwitcher)
	{
		return new TranslationProfileField(msg, registry, actionComponentProvider, subViewSwitcher);
	}

	public CollapsibleLayout getWrappedFieldInstance(SubViewSwitcher subViewSwitcher, Binder<?> binder,
			String fieldName)
	{
		TranslationProfileField field = getInstance(subViewSwitcher);
		binder.forField(field).bind(fieldName);

		VerticalLayout layout = new VerticalLayout();
		layout.setMargin(false);
		layout.setSpacing(false);
		layout.addComponent(field);

		return new CollapsibleLayout(msg.getMessage("TranslationProfileSection.caption"), layout);
	}	
}
