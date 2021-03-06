/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.saml.sp.web.authnEditor;

import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.CustomComponent;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import io.imunity.webelements.helpers.StandardButtonsHelper;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.saml.SamlProperties.Binding;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.chips.ChipsWithDropdown;
import pl.edu.icm.unity.webui.common.chips.ChipsWithFreeText;
import pl.edu.icm.unity.webui.common.i18n.I18nTextField;
import pl.edu.icm.unity.webui.common.validators.NoSpaceValidator;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;
import pl.edu.icm.unity.webui.common.webElements.UnitySubView;

/**
 * View for edit SAML individual trusted idp
 * 
 * @author P.Piernik
 *
 */
class EditIndividualTrustedIdpSubView extends CustomComponent implements UnitySubView
{

	private UnityMessageSource msg;
	private Binder<IndividualTrustedSamlIdpConfiguration> configBinder;
	private boolean editMode = false;

	private Set<String> certificates;
	private Set<String> registrationForms;
	private Set<String> usedNames;

	EditIndividualTrustedIdpSubView(UnityMessageSource msg,
			InputTranslationProfileFieldFactory profileFieldFactory,
			IndividualTrustedSamlIdpConfiguration toEdit, SubViewSwitcher subViewSwitcher,
			Set<String> usedNames, Set<String> certificates, Set<String> registrationForms,
			Consumer<IndividualTrustedSamlIdpConfiguration> onConfirm, Runnable onCancel)
	{
		this.msg = msg;
		this.certificates = certificates;
		this.registrationForms = registrationForms;
		this.usedNames = usedNames;
		editMode = toEdit != null;

		configBinder = new Binder<>(IndividualTrustedSamlIdpConfiguration.class);

		FormLayout header = buildHeaderSection();
		CollapsibleLayout singleLogout = buildSingleLogoutSection();

		CollapsibleLayout remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");

		configBinder.setBean(editMode ? toEdit.clone() : new IndividualTrustedSamlIdpConfiguration());

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(singleLogout);
		mainView.addComponent(remoteDataMapping);

		Runnable onConfirmR = () -> {
			try
			{
				onConfirm.accept(getTrustedFederation());
			} catch (FormValidationException e)
			{
				NotificationPopup.showError(msg, msg.getMessage(
						"EditIndividualTrustedIdpSubView.invalidConfiguration"), e);
			}
		};

		mainView.addComponent(editMode
				? StandardButtonsHelper.buildConfirmEditButtonsBar(msg, onConfirmR, onCancel)
				: StandardButtonsHelper.buildConfirmNewButtonsBar(msg, onConfirmR, onCancel));

		setCompositionRoot(mainView);
	}

	private FormLayout buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);

		TextField name = new TextField(msg.getMessage("EditIndividualTrustedIdpSubView.name"));
		name.focus();
		configBinder.forField(name).asRequired(msg.getMessage("fieldRequired"))
				.withValidator(new NoSpaceValidator()).withValidator((s, c) -> {
					if (usedNames.contains(s))
					{
						return ValidationResult.error(msg.getMessage(
								"EditIndividualTrustedIdpSubView.nameExists"));
					} else
					{
						return ValidationResult.ok();
					}

				}).bind("name");
		header.addComponent(name);

		I18nTextField displayedName = new I18nTextField(msg,
				msg.getMessage("EditIndividualTrustedIdpSubView.displayedName"));
		configBinder.forField(displayedName).asRequired(msg.getMessage("fieldRequired")).bind("displayedName");
		header.addComponent(displayedName);

		TextField logo = new TextField(msg.getMessage("EditIndividualTrustedIdpSubView.logo"));
		logo.setCaption(msg.getMessage("EditIndividualTrustedIdpSubView.logo"));
		configBinder.forField(logo).bind("logo");
		header.addComponent(logo);

		TextField id = new TextField(msg.getMessage("EditIndividualTrustedIdpSubView.id"));
		id.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(id).asRequired(msg.getMessage("fieldRequired")).bind("id");
		header.addComponent(id);

		TextField address = new TextField(msg.getMessage("EditIndividualTrustedIdpSubView.address"));
		address.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(address).bind("address");
		header.addComponent(address);

		ChipsWithFreeText requestedNameFormats = new ChipsWithFreeText();
		requestedNameFormats.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		requestedNameFormats.setCaption(msg.getMessage("EditIndividualTrustedIdpSubView.requestedNameFormats"));
		requestedNameFormats.setItems(SAMLAuthenticatorEditor.STANDART_NAME_FORMATS);
		header.addComponent(requestedNameFormats);
		configBinder.forField(requestedNameFormats).bind("requestedNameFormats");

		CheckBox signRequest = new CheckBox(msg.getMessage("EditIndividualTrustedIdpSubView.signRequest"));

		ComboBox<Binding> binding = new ComboBox<>(msg.getMessage("EditIndividualTrustedIdpSubView.binding"));
		binding.setItems(Binding.values());
		binding.setEmptySelectionAllowed(false);
		binding.addValueChangeListener(e -> {
			boolean v = e.getValue().equals(Binding.HTTP_POST);
			signRequest.setEnabled(v);
			if (!v)
			{
				signRequest.setValue(false);
			}

		});

		configBinder.forField(binding).bind("binding");
		configBinder.forField(signRequest).withValidator((v, c) -> {
			if (v != null && v && (binding.getValue().equals(Binding.HTTP_REDIRECT)
					|| binding.getValue().equals(Binding.SOAP)))
			{
				return ValidationResult.error(msg.getMessage(
						"EditIndividualTrustedIdpSubView.signRequestValidationError"));
			}
			return ValidationResult.ok();

		}).bind("signRequest");

		header.addComponent(binding);
		header.addComponent(signRequest);

		ChipsWithDropdown<String> certificatesCombo = new ChipsWithDropdown<>();
		certificatesCombo.setCaption(msg.getMessage("EditIndividualTrustedIdpSubView.certificates"));
		certificatesCombo.setItems(certificates.stream().collect(Collectors.toList()));
		configBinder.forField(certificatesCombo).bind("certificates");
		header.addComponent(certificatesCombo);

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("EditIndividualTrustedIdpSubView.registrationForm"));
		registrationForm.setItems(registrationForms);
		header.addComponent(registrationForm);
		configBinder.forField(registrationForm).bind("registrationForm");

		CheckBox accountAssociation = new CheckBox(
				msg.getMessage("EditIndividualTrustedIdpSubView.accountAssociation"));
		configBinder.forField(accountAssociation).bind("accountAssociation");
		header.addComponent(accountAssociation);

		return header;
	}

	private CollapsibleLayout buildSingleLogoutSection()
	{
		FormLayoutWithFixedCaptionWidth singleLogout = new FormLayoutWithFixedCaptionWidth();
		singleLogout.setMargin(false);

		TextField postLogoutEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedIdpSubView.postLogoutEndpoint"));
		postLogoutEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(postLogoutEndpoint).bind("postLogoutEndpoint");
		singleLogout.addComponent(postLogoutEndpoint);

		TextField postLogoutResponseEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedIdpSubView.postLogoutResponseEndpoint"));
		postLogoutResponseEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(postLogoutResponseEndpoint).bind("postLogoutResponseEndpoint");
		singleLogout.addComponent(postLogoutResponseEndpoint);

		TextField redirectLogoutEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedIdpSubView.redirectLogoutEndpoint"));
		redirectLogoutEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(redirectLogoutEndpoint).bind("redirectLogoutEndpoint");
		singleLogout.addComponent(redirectLogoutEndpoint);

		TextField redirectLogoutResponseEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedIdpSubView.redirectLogoutResponseEndpoint"));
		redirectLogoutResponseEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(redirectLogoutResponseEndpoint).bind("redirectLogoutResponseEndpoint");
		singleLogout.addComponent(redirectLogoutResponseEndpoint);

		TextField soapLogoutEndpoint = new TextField(
				msg.getMessage("EditIndividualTrustedIdpSubView.soapLogoutEndpoint"));
		soapLogoutEndpoint.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(soapLogoutEndpoint).bind("soapLogoutEndpoint");
		singleLogout.addComponent(soapLogoutEndpoint);

		return new CollapsibleLayout(msg.getMessage("EditIndividualTrustedIdpSubView.singleLogout"),
				singleLogout);
	}

	@Override
	public List<String> getBredcrumbs()
	{
		return Arrays.asList(msg.getMessage("EditIndividualTrustedIdpSubView.breadcrumbs"),
				editMode ? configBinder.getBean().getName() : msg.getMessage("new"));
	}

	private IndividualTrustedSamlIdpConfiguration getTrustedFederation() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();

		return configBinder.getBean();
	}
}
