/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.attributetype;

import java.util.SortedSet;
import java.util.TreeSet;

import com.vaadin.v7.data.Property.ValueChangeEvent;
import com.vaadin.v7.data.Property.ValueChangeListener;
import com.vaadin.v7.data.util.converter.StringToIntegerConverter;
import com.vaadin.v7.data.validator.IntegerRangeValidator;
import com.vaadin.v7.ui.AbstractTextField;
import com.vaadin.v7.ui.CheckBox;
import com.vaadin.v7.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.Panel;
import com.vaadin.v7.ui.TextField;
import com.vaadin.v7.ui.VerticalLayout;

import pl.edu.icm.unity.engine.api.attributes.AttributeTypeSupport;
import pl.edu.icm.unity.engine.api.attributes.AttributeValueSyntax;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.IllegalAttributeTypeException;
import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.basic.AttributeType;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.FormValidator;
import pl.edu.icm.unity.webui.common.RequiredTextField;
import pl.edu.icm.unity.webui.common.attributes.AttributeHandlerRegistry;
import pl.edu.icm.unity.webui.common.attributes.AttributeSyntaxEditor;
import pl.edu.icm.unity.webui.common.attrmetadata.AttributeMetadataHandlerRegistry;
import pl.edu.icm.unity.webui.common.boundededitors.IntegerBoundEditor;
import pl.edu.icm.unity.webui.common.i18n.I18nTextArea;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.safehtml.SafePanel;

/**
 * Allows to edit an attribute type. Can be configured to edit an existing attribute (name is fixed)
 * or to create a new one (name can be chosen).
 * 
 * @author K. Benedyczak
 */
public class RegularAttributeTypeEditor extends FormLayout implements AttributeTypeEditor
{
	private UnityMessageSource msg;
	private AttributeHandlerRegistry registry;
	private AttributeMetadataHandlerRegistry attrMetaHandlerReg;
	
	private AbstractTextField name;
	private I18nTextField displayedName;
	private I18nTextArea typeDescription;
	private TextField min;
	private IntegerBoundEditor max;
	private CheckBox uniqueVals;
	private CheckBox selfModificable;
	private ComboBox syntax;
	private VerticalLayout syntaxPanel;
	private AttributeSyntaxEditor<?> editor;
	private MetadataEditor metaEditor;
	private FormValidator validator;
	private AttributeTypeSupport atSupport;
	
	public RegularAttributeTypeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, 
			AttributeMetadataHandlerRegistry attrMetaHandlerReg, AttributeTypeSupport atSupport)
	{
		this(msg, registry, null, attrMetaHandlerReg, atSupport);
	}

	public RegularAttributeTypeEditor(UnityMessageSource msg, AttributeHandlerRegistry registry, AttributeType toEdit, 
			AttributeMetadataHandlerRegistry attrMetaHandlerReg, AttributeTypeSupport atSupport)
	{
		super();
		this.msg = msg;
		this.registry = registry;
		this.attrMetaHandlerReg = attrMetaHandlerReg;
		this.atSupport = atSupport;
		
		initUI(toEdit);
	}

	private void initUI(AttributeType toEdit)
	{
		setWidth(100, Unit.PERCENTAGE);

		name = new RequiredTextField(msg);
		if (toEdit != null)
		{
			name.setValue(toEdit.getName());
			name.setReadOnly(true);
		} else
			name.setValue(msg.getMessage("AttributeType.defaultName"));
		name.setCaption(msg.getMessage("AttributeType.name"));
		addComponent(name);
		
		displayedName = new I18nTextField(msg, msg.getMessage("AttributeType.displayedName"));
		addComponent(displayedName);
		
		typeDescription = new I18nTextArea(msg, msg.getMessage("AttributeType.description"));
		addComponent(typeDescription);
		
		min = new RequiredTextField(msg);
		min.setCaption(msg.getMessage("AttributeType.min"));
		min.setConverter(new StringToIntegerConverter());
		min.setNullRepresentation("");
		min.addValidator(new IntegerRangeValidator(msg.getMessage("AttributeType.invalidNumber"), 
				0, Integer.MAX_VALUE));
		addComponent(min);

		max = new IntegerBoundEditor(msg, msg.getMessage("AttributeType.maxUnlimited"), 
				msg.getMessage("AttributeType.max"), Integer.MAX_VALUE);
		max.setMin(0);
		addComponent(max);
		
		uniqueVals = new CheckBox(msg.getMessage("AttributeType.uniqueValuesCheck"));
		addComponent(uniqueVals);
		
		selfModificable = new CheckBox(msg.getMessage("AttributeType.selfModificableCheck"));
		addComponent(selfModificable);
		
		syntax = new ComboBox(msg.getMessage("AttributeType.type"));
		syntax.setNullSelectionAllowed(false);
		syntax.setImmediate(true);
		SortedSet<String> syntaxes = new TreeSet<String>(registry.getSupportedSyntaxes());
		for (String syntaxId: syntaxes)
			syntax.addItem(syntaxId);
		addComponent(syntax);
		
		Panel syntaxPanelP = new SafePanel();
		syntaxPanel = new VerticalLayout();
		syntaxPanel.setMargin(true);
		syntaxPanelP.setContent(syntaxPanel);
		
		addComponent(syntaxPanelP);
		
		syntax.addValueChangeListener(new ValueChangeListener()
		{
			@Override
			public void valueChange(ValueChangeEvent event)
			{
				String syntaxId = (String)syntax.getValue();
				editor = registry.getSyntaxEditor(syntaxId, null);
				syntaxPanel.removeAllComponents();
				syntaxPanel.addComponent(editor.getEditor());
			}
		});
		
		metaEditor = new MetadataEditor(msg, attrMetaHandlerReg);
		metaEditor.setMargin(true);
		Panel metaPanel = new SafePanel(msg.getMessage("AttributeType.metadata"), metaEditor);
		addComponent(metaPanel);
		
		if (toEdit != null)
			setInitialValues(toEdit);
		else
		{
			min.setValue("1");
			max.setValue(1);
			syntax.setValue(syntaxes.first());
		}
		
		validator = new FormValidator(this);
	}
	
	private void setInitialValues(AttributeType aType)
	{
		typeDescription.setValue(aType.getDescription());
		min.setValue(aType.getMinElements()+"");
		max.setValue(aType.getMaxElements());
		uniqueVals.setValue(aType.isUniqueValues());
		selfModificable.setValue(aType.isSelfModificable());
		String syntaxId = aType.getValueSyntax();
		syntax.setValue(syntaxId);
		AttributeValueSyntax<?> syntaxObj = atSupport.getSyntax(aType);
		editor = registry.getSyntaxEditor(syntaxId, syntaxObj);
		syntaxPanel.removeAllComponents();
		syntaxPanel.addComponent(editor.getEditor());
		metaEditor.setInput(aType.getMetadata());
		displayedName.setValue(aType.getDisplayedName());
	}
	
	@Override
	public AttributeType getAttributeType() throws IllegalAttributeTypeException
	{
		try
		{
			validator.validate();
		} catch (FormValidationException e)
		{
			throw new IllegalAttributeTypeException("");
		}
		
		AttributeValueSyntax<?> syntax = editor.getCurrentValue();
		AttributeType ret = new AttributeType();
		ret.setDescription(typeDescription.getValue());
		ret.setName(name.getValue());
		ret.setMaxElements(max.getValue());
		ret.setMinElements((Integer)min.getConvertedValue());
		ret.setSelfModificable(selfModificable.getValue());
		ret.setUniqueValues(uniqueVals.getValue());
		ret.setValueSyntax(syntax.getValueSyntaxId());
		ret.setValueSyntaxConfiguration(syntax.getSerializedConfiguration());
		ret.setMetadata(metaEditor.getValue());
		I18nString displayedNameS = displayedName.getValue();
		displayedNameS.setDefaultValue(ret.getName());
		ret.setDisplayedName(displayedNameS);
		return ret;
	}

	@Override
	public Component getComponent()
	{
		return this;
	}
}
