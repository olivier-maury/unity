/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webui.common.i18n;

import com.vaadin.ui.TextField;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.I18nString;

/**
 * Custom field allowing for editing an {@link I18nString}, i.e. a string in several languages.
 * By default the default locale is only shown, but a special button can be clicked to show text fields
 * to enter translations.
 * 
 * @author K. Benedyczak
 */
public class I18nTextField2 extends Abstract18nField2<TextField>
{
	public I18nTextField2(UnityMessageSource msg)
	{
		super(msg);
		initUI();
	}

	public I18nTextField2(UnityMessageSource msg, String caption)
	{
		super(msg, caption);
		initUI();
	}
	
	@Override
	protected TextField makeFieldInstance()
	{
		return new TextField();
	}
}
