/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile;

import com.vaadin.v7.data.util.converter.StringToIntegerConverter;
import com.vaadin.v7.data.validator.IntegerRangeValidator;

import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.types.translation.ActionParameterDefinition;

/**
 * Action component allowing to select a number of days
 * @author K. Benedyczak
 */
public class DaysActionParameterComponent extends DefaultActionParameterComponent
{	
	public DaysActionParameterComponent(ActionParameterDefinition desc, UnityMessageSource msg)
	{
		super(desc, msg);
		setColumns(4);
		addValidator(new IntegerRangeValidator(msg.getMessage("DaysActionParameterComponent.notANumber"), 
				1, 365*20));
		setConverter(new StringToIntegerConverter());
		setNullRepresentation("");
	}
	
	@Override
	public String getActionValue()
	{
		return String.valueOf(getValue());
	}
}
