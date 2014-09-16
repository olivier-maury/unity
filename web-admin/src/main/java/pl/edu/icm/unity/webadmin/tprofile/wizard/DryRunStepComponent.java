/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.webadmin.tprofile.wizard;

import pl.edu.icm.unity.sandbox.SandboxAuthnEvent;
import pl.edu.icm.unity.sandbox.SandboxUI;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.common.Styles;

import com.vaadin.annotations.AutoGenerated;
import com.vaadin.server.ExternalResource;
import com.vaadin.shared.ui.label.ContentMode;
import com.vaadin.ui.Button;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.VerticalSplitPanel;
import com.vaadin.ui.themes.Reindeer;

/**
 * UI Component used by {@link DryRunStep}.
 * 
 * @author Roman Krysinski
 */
public class DryRunStepComponent extends CustomComponent 
{

	/*- VaadinEditorProperties={"grid":"RegularGrid,20","showGrid":true,"snapToGrid":true,"snapToObject":true,"movingGuides":false,"snappingDistance":10} */

	@AutoGenerated
	private VerticalLayout mainLayout;
	@AutoGenerated
	private VerticalSplitPanel splitPanel;
	@AutoGenerated
	private VerticalLayout buttomWraper;
	@AutoGenerated
	private Label capturedLogs;
	@AutoGenerated
	private Label logsLabel;
	@AutoGenerated
	private VerticalLayout topWrapper;
	@AutoGenerated
	private Label authnResultLabel;
	@AutoGenerated
	private Button popupButton;
	@AutoGenerated
	private Label infoLabel;
	private UnityMessageSource msg;
	/**
	 * The constructor should first build the main layout, set the
	 * composition root and then do any custom initialization.
	 *
	 * The constructor will not be automatically regenerated by the
	 * visual editor.
	 * @param msg 
	 * @param sandboxURL 
	 */
	public DryRunStepComponent(UnityMessageSource msg, String sandboxURL) 
	{
		buildMainLayout();
		setCompositionRoot(mainLayout);

		this.msg = msg;
		
		capturedLogs.setContentMode(ContentMode.PREFORMATTED);
		capturedLogs.setValue("");
		
		logsLabel.setValue("");
		logsLabel.setContentMode(ContentMode.HTML);
		
		infoLabel.setValue(msg.getMessage("Wizard.DryRunStepComponent.infoLabel"));
		
		
		popupButton.setCaption(msg.getMessage("Wizard.DryRunStepComponent.popupButton"));
		SandboxPopup popup = new SandboxPopup(
				new ExternalResource(sandboxURL + "?" + SandboxUI.PROFILE_VALIDATION + "=true"));
		authnResultLabel.setValue("");
		popup.attachButton(popupButton);		
		
		splitPanel.setSplitPosition(25);
		splitPanel.addStyleName(Reindeer.SPLITPANEL_SMALL);
		splitPanel.setLocked(true);
	}

	public void handle(SandboxAuthnEvent event) 
	{
		if (event.getAuthnResult().getStatus() == Status.success)
		{
			authnResultLabel.setValue(msg.getMessage("Wizard.DryRunStepComponent.authnResultLabel.success"));
			authnResultLabel.setStyleName(Styles.success.toString());
		} else
		{
			authnResultLabel.setValue(msg.getMessage("Wizard.DryRunStepComponent.authnResultLabel.error"));
			authnResultLabel.setStyleName(Styles.error.toString());
		}
		logsLabel.setValue(msg.getMessage("Wizard.DryRunStepComponent.logsLabel"));
		capturedLogs.setValue(event.getCapturedLogs().toString());
	}

	@AutoGenerated
	private VerticalLayout buildMainLayout() {
		// common part: create layout
		mainLayout = new VerticalLayout();
		mainLayout.setImmediate(false);
		mainLayout.setWidth("100%");
		mainLayout.setHeight("100%");
		mainLayout.setMargin(true);
		mainLayout.setSpacing(true);
		
		// top-level component properties
		setWidth("100.0%");
		setHeight("100.0%");
		
		// splitPanel
		splitPanel = buildSplitPanel();
		mainLayout.addComponent(splitPanel);
		
		return mainLayout;
	}

	@AutoGenerated
	private VerticalSplitPanel buildSplitPanel() {
		// common part: create layout
		splitPanel = new VerticalSplitPanel();
		splitPanel.setImmediate(false);
		splitPanel.setWidth("100.0%");
		splitPanel.setHeight("100.0%");
		
		// topWrapper
		topWrapper = buildTopWrapper();
		splitPanel.addComponent(topWrapper);
		
		// buttomWraper
		buttomWraper = buildButtomWraper();
		splitPanel.addComponent(buttomWraper);
		
		return splitPanel;
	}

	@AutoGenerated
	private VerticalLayout buildTopWrapper() {
		// common part: create layout
		topWrapper = new VerticalLayout();
		topWrapper.setImmediate(false);
		topWrapper.setWidth("-1px");
		topWrapper.setHeight("-1px");
		topWrapper.setMargin(false);
		topWrapper.setSpacing(true);
		
		// infoLabel
		infoLabel = new Label();
		infoLabel.setImmediate(false);
		infoLabel.setWidth("-1px");
		infoLabel.setHeight("-1px");
		infoLabel.setValue("Label");
		topWrapper.addComponent(infoLabel);
		
		// popupButton
		popupButton = new Button();
		popupButton.setCaption("Button");
		popupButton.setImmediate(true);
		popupButton.setWidth("-1px");
		popupButton.setHeight("-1px");
		topWrapper.addComponent(popupButton);
		
		// authnResultLabel
		authnResultLabel = new Label();
		authnResultLabel.setImmediate(false);
		authnResultLabel.setWidth("-1px");
		authnResultLabel.setHeight("-1px");
		authnResultLabel.setValue("Label");
		topWrapper.addComponent(authnResultLabel);
		
		return topWrapper;
	}

	@AutoGenerated
	private VerticalLayout buildButtomWraper() {
		// common part: create layout
		buttomWraper = new VerticalLayout();
		buttomWraper.setImmediate(false);
		buttomWraper.setWidth("-1px");
		buttomWraper.setHeight("-1px");
		buttomWraper.setMargin(false);
		buttomWraper.setSpacing(true);
		
		// logsLabel
		logsLabel = new Label();
		logsLabel.setImmediate(false);
		logsLabel.setWidth("-1px");
		logsLabel.setHeight("-1px");
		logsLabel.setValue("Label");
		buttomWraper.addComponent(logsLabel);
		
		// capturedLogs
		capturedLogs = new Label();
		capturedLogs.setImmediate(false);
		capturedLogs.setWidth("-1px");
		capturedLogs.setHeight("-1px");
		capturedLogs.setValue("Label");
		buttomWraper.addComponent(capturedLogs);
		
		return buttomWraper;
	}

}
