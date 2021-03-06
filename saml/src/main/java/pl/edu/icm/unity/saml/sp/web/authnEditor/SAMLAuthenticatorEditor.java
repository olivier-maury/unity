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
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Button;
import com.vaadin.ui.CheckBox;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.CustomField;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.RealmsManagement;
import pl.edu.icm.unity.engine.api.RegistrationsManagement;
import pl.edu.icm.unity.engine.api.identity.IdentityTypesRegistry;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.saml.sp.SAMLVerificator;
import pl.edu.icm.unity.saml.sp.web.authnEditor.SAMLConfiguration.SloMapping;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FieldSizeConstans;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.GridWithActionColumn;
import pl.edu.icm.unity.webui.common.GridWithEditor;
import pl.edu.icm.unity.webui.common.Images;
import pl.edu.icm.unity.webui.common.NotificationPopup;
import pl.edu.icm.unity.webui.common.SingleActionHandler;
import pl.edu.icm.unity.webui.common.chips.ChipsWithFreeText;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * SAML Authenticator editor
 * 
 * @author P.Piernik
 *
 */
class SAMLAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	public static final List<String> STANDART_NAME_FORMATS = Arrays.asList(
			"urn:oasis:names:tc:SAML:2.0:nameid-format:persistent",
			"urn:oasis:names:tc:SAML:1.1:nameid-format:emailAddress",
			"urn:oasis:names:tc:SAML:1.1:nameid-format:X509SubjectName",
			"urn:oasis:names:tc:SAML:2.0:nameid-format:transient");

	private PKIManagement pkiMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;

	private RegistrationsManagement registrationMan;

	private Binder<SAMLConfiguration> configBinder;
	private SubViewSwitcher subViewSwitcher;

	private Set<String> credentials;
	private List<String> registrationForms;
	private Set<String> realms;
	private List<String> idTypes;

	private CheckBox defSignRequest;
	private IndividualTrustedIdpComponent idps;

	SAMLAuthenticatorEditor(UnityMessageSource msg, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory,
			RegistrationsManagement registrationMan, RealmsManagement realmMan,
			IdentityTypesRegistry idTypesReg) throws EngineException
	{
		super(msg);
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		this.registrationMan = registrationMan;
		this.credentials = pkiMan.getCredentialNames();
		this.registrationForms = registrationMan.getForms().stream().map(f -> f.getName())
				.collect(Collectors.toList());
		this.realms = realmMan.getRealms().stream().map(r -> r.getName()).collect(Collectors.toSet());
		this.idTypes = idTypesReg.getAll().stream().map(i -> i.getId()).collect(Collectors.toList());

	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		this.subViewSwitcher = subViewSwitcher;

		boolean editMode = init(msg.getMessage("SAMLAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(SAMLConfiguration.class);

		FormLayoutWithFixedCaptionWidth header = buildHeaderSection();
		CollapsibleLayout trustedFederations = buildTrustedFederationsSection();
		trustedFederations.expand();
		CollapsibleLayout individualTrustedIdPs = buildIndividualTrustedIdPsSection();
		CollapsibleLayout metadataPublishing = buildSAMLMetadaPublishingSection();
		metadataPublishing.expand();
		CollapsibleLayout singleLogout = buildSingleLogoutSection();

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(trustedFederations);
		mainView.addComponent(individualTrustedIdPs);
		mainView.addComponent(metadataPublishing);
		mainView.addComponent(singleLogout);
		
		SAMLConfiguration config = new SAMLConfiguration();
		if (editMode)
		{
			config.fromProperties(pkiMan, msg, toEdit.configuration);
		}

		configBinder.setBean(config);
		
		return mainView;
	}

	private FormLayoutWithFixedCaptionWidth buildHeaderSection()
	{
		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);
		name.focus();

		TextField requesterId = new TextField(msg.getMessage("SAMLAuthenticatorEditor.requesterId"));
		requesterId.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(requesterId).asRequired(msg.getMessage("fieldRequired")).bind("requesterId");
		header.addComponent(requesterId);

		ComboBox<String> credential = new ComboBox<>();
		credential.setCaption(msg.getMessage("SAMLAuthenticatorEditor.credential"));
		credential.setItems(credentials);
		configBinder.forField(credential)
				.asRequired((v, c) -> ((v == null || v.isEmpty()) && isSignReq())
						? ValidationResult.error(msg.getMessage("fieldRequired"))
						: ValidationResult.ok())
				.bind("credential");
		header.addComponent(credential);

		ChipsWithFreeText acceptedNameFormats = new ChipsWithFreeText();
		acceptedNameFormats.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		acceptedNameFormats.setCaption(msg.getMessage("SAMLAuthenticatorEditor.acceptedNameFormats"));
		acceptedNameFormats.setItems(STANDART_NAME_FORMATS);
		header.addComponent(acceptedNameFormats);
		configBinder.forField(acceptedNameFormats).bind("acceptedNameFormats");

		CheckBox requireSignedAssertion = new CheckBox(
				msg.getMessage("SAMLAuthenticatorEditor.requireSignedAssertion"));
		configBinder.forField(requireSignedAssertion).bind("requireSignedAssertion");
		header.addComponent(requireSignedAssertion);

		defSignRequest = new CheckBox(msg.getMessage("SAMLAuthenticatorEditor.defSignRequest"));
		configBinder.forField(defSignRequest).bind("defSignRequest");
		header.addComponent(defSignRequest);

		ChipsWithFreeText defaultRequestedNameFormat = new ChipsWithFreeText();
		defaultRequestedNameFormat.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		defaultRequestedNameFormat
				.setCaption(msg.getMessage("SAMLAuthenticatorEditor.defaultRequestedNameFormat"));
		defaultRequestedNameFormat.setItems(STANDART_NAME_FORMATS);
		defaultRequestedNameFormat.setMaxSelection(1);
		header.addComponent(defaultRequestedNameFormat);
		configBinder.forField(defaultRequestedNameFormat).bind("defaultRequestedNameFormat");

		ComboBox<String> registrationForm = new ComboBox<>(
				msg.getMessage("SAMLAuthenticatorEditor.registrationForm"));
		registrationForm.setItems(registrationForms);
		header.addComponent(registrationForm);
		configBinder.forField(registrationForm).bind("registrationForm");

		CheckBox defAccountAssociation = new CheckBox(
				msg.getMessage("SAMLAuthenticatorEditor.defAccountAssociation"));
		configBinder.forField(defAccountAssociation).bind("defAccountAssociation");
		header.addComponent(defAccountAssociation);

		return header;
	}

	private CollapsibleLayout buildTrustedFederationsSection()
	{
		FormLayoutWithFixedCaptionWidth trustedFederations = new FormLayoutWithFixedCaptionWidth();
		trustedFederations.setMargin(false);
		TrustedFederationComponent federations = new TrustedFederationComponent();
		configBinder.forField(federations).bind("trustedFederations");
		trustedFederations.addComponent(federations);

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.trustedFederations"),
				trustedFederations);
	}

	private CollapsibleLayout buildIndividualTrustedIdPsSection()
	{
		FormLayoutWithFixedCaptionWidth individualTrustedIdPs = new FormLayoutWithFixedCaptionWidth();
		individualTrustedIdPs.setMargin(false);

		idps = new IndividualTrustedIdpComponent();
		configBinder.forField(idps).bind("individualTrustedIdps");
		individualTrustedIdPs.addComponent(idps);

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.individualTrustedIdPs"),
				individualTrustedIdPs);
	}

	private CollapsibleLayout buildSAMLMetadaPublishingSection()
	{
		FormLayoutWithFixedCaptionWidth metadataPublishing = new FormLayoutWithFixedCaptionWidth();
		metadataPublishing.setMargin(false);

		CheckBox publishMetadata = new CheckBox(msg.getMessage("SAMLAuthenticatorEditor.publishMetadata"));
		configBinder.forField(publishMetadata).bind("publishMetadata");
		metadataPublishing.addComponent(publishMetadata);

		TextField metadataPath = new TextField(msg.getMessage("SAMLAuthenticatorEditor.metadataPath"));
		metadataPath.setWidth(FieldSizeConstans.LINK_FIELD_WIDTH, FieldSizeConstans.LINK_FIELD_WIDTH_UNIT);
		configBinder.forField(metadataPath).bind("metadataPath");
		metadataPath.setEnabled(false);
		metadataPublishing.addComponent(metadataPath);

		CheckBox signMetadata = new CheckBox(msg.getMessage("SAMLAuthenticatorEditor.signMetadata"));
		configBinder.forField(signMetadata).bind("signMetadata");
		signMetadata.setEnabled(false);
		metadataPublishing.addComponent(signMetadata);

		CheckBox autoGenerateMetadata = new CheckBox(
				msg.getMessage("SAMLAuthenticatorEditor.autoGenerateMetadata"));
		configBinder.forField(autoGenerateMetadata).bind("autoGenerateMetadata");
		autoGenerateMetadata.setEnabled(false);
		metadataPublishing.addComponent(autoGenerateMetadata);

		publishMetadata.addValueChangeListener(e -> {
			boolean v = e.getValue();
			metadataPath.setEnabled(v);
			signMetadata.setEnabled(v);
			autoGenerateMetadata.setEnabled(v);
		});

		// TODO Add metadata file upload/add validator for
		// PUBLISH_METADATA && !isSet(METADATA_PATH

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.metadataPublishing"),
				metadataPublishing);
	}

	private CollapsibleLayout buildSingleLogoutSection()
	{
		FormLayoutWithFixedCaptionWidth singleLogout = new FormLayoutWithFixedCaptionWidth();
		singleLogout.setMargin(false);

		TextField sloPath = new TextField(msg.getMessage("SAMLAuthenticatorEditor.sloPath"));
		configBinder.forField(sloPath).bind("sloPath");
		singleLogout.addComponent(sloPath);

		ComboBox<String> sloRealm = new ComboBox<>(msg.getMessage("SAMLAuthenticatorEditor.sloRealm"));
		sloRealm.setItems(realms);
		singleLogout.addComponent(sloRealm);
		configBinder.forField(sloRealm).bind("sloRealm");

		GridWithEditor<SloMapping> sloMappings = new GridWithEditor<>(msg, SloMapping.class);
		sloMappings.setCaption(msg.getMessage("SAMLAuthenticatorEditor.sloMappings"));
		singleLogout.addComponent(sloMappings);
		sloMappings.addComboColumn(s -> s.getUnityId(), (t, v) -> t.setUnityId(v),
				msg.getMessage("SAMLAuthenticatorEditor.sloMappings.unityId"), idTypes, 30, false);
		sloMappings.addTextColumn(s -> s.getSamlId(), (t, v) -> t.setSamlId(v),
				msg.getMessage("SAMLAuthenticatorEditor.sloMappings.samlId"), 70, false);

		sloMappings.setWidth(FieldSizeConstans.MEDIUM_FIELD_WIDTH, FieldSizeConstans.MEDIUM_FIELD_WIDTH_UNIT);
		configBinder.forField(sloMappings).bind("sloMappings");

		return new CollapsibleLayout(msg.getMessage("SAMLAuthenticatorEditor.singleLogout"), singleLogout);
	}

	private boolean isSignReq()
	{
		boolean v = defSignRequest.getValue();

		if (idps != null && idps.getValue() != null)
		{
			for (IndividualTrustedSamlIdpConfiguration i : idps.getValue())
			{
				v |= i.isSignRequest();

			}
		}
		return v;
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), SAMLVerificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();
		try
		{
			return configBinder.getBean().toProperties(pkiMan);
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the SAML verificator", e);
		}
	}

	private Set<String> getRegistrationForms() throws EngineException
	{
		return registrationMan.getForms().stream().map(r -> r.getName()).collect(Collectors.toSet());
	}

	private class TrustedFederationComponent extends CustomField<List<TrustedFederationConfiguration>>
	{
		private GridWithActionColumn<TrustedFederationConfiguration> federationList;

		public TrustedFederationComponent()
		{
			federationList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			federationList.addColumn(p -> p.getName(), msg.getMessage("TrustedFederationComponent.name"),
					50);
		}

		private List<SingleActionHandler<TrustedFederationConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<TrustedFederationConfiguration> edit = SingleActionHandler
					.builder4Edit(msg, TrustedFederationConfiguration.class).withHandler(r -> {
						TrustedFederationConfiguration edited = r.iterator().next();
						gotoEditSubView(edited, federationList.getElements().stream()
								.filter(p -> p.getName() != edited.getName())
								.map(p -> p.getName()).collect(Collectors.toSet()),
								c -> {
									federationList.replaceElement(edited, c);
									fireChange();
									subViewSwitcher.exitSubView();
								});
					}

					).build();

			SingleActionHandler<TrustedFederationConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, TrustedFederationConfiguration.class).withHandler(r -> {
						federationList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoEditSubView(TrustedFederationConfiguration edited, Set<String> usedNames,
				Consumer<TrustedFederationConfiguration> onConfirm)
		{
			Set<String> forms;
			Set<String> validators;
			Set<String> certificates;

			try
			{
				validators = pkiMan.getValidatorNames();
				certificates = pkiMan.getAllCertificateNames();
				forms = getRegistrationForms();

			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, "Can not init trusted federation editor", e);
				return;
			}

			EditTrustedFederationSubView subView = new EditTrustedFederationSubView(msg,
					profileFieldFactory, edited, subViewSwitcher, usedNames, validators,
					certificates, forms, r -> {
						onConfirm.accept(r);
						federationList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						federationList.focus();
					});
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		public List<TrustedFederationConfiguration> getValue()
		{
			return federationList.getElements();
		}

		@Override
		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);

			Button add = new Button(msg.getMessage("TrustedFederationComponent.addFederation"));
			add.addClickListener(e -> {
				gotoEditSubView(null, federationList.getElements().stream().map(p -> p.getName())
						.collect(Collectors.toSet()), c -> {
							subViewSwitcher.exitSubView();
							federationList.addElement(c);
							federationList.focus();
							fireChange();
						});
			});
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
			main.addComponent(federationList);
			return main;
		}

		@Override
		protected void doSetValue(List<TrustedFederationConfiguration> value)
		{
			federationList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<TrustedFederationConfiguration>>(this,
					federationList.getElements(), true));
		}
	}

	private class IndividualTrustedIdpComponent extends CustomField<List<IndividualTrustedSamlIdpConfiguration>>
	{
		private GridWithActionColumn<IndividualTrustedSamlIdpConfiguration> idpList;

		public IndividualTrustedIdpComponent()
		{
			idpList = new GridWithActionColumn<>(msg, getActionsHandlers(), false);
			idpList.addColumn(p -> p.getName(), msg.getMessage("IndividualTrustedIdpComponent.name"), 50);
		}

		private List<SingleActionHandler<IndividualTrustedSamlIdpConfiguration>> getActionsHandlers()
		{
			SingleActionHandler<IndividualTrustedSamlIdpConfiguration> edit = SingleActionHandler
					.builder4Edit(msg, IndividualTrustedSamlIdpConfiguration.class)
					.withHandler(r -> {
						IndividualTrustedSamlIdpConfiguration edited = r.iterator().next();
						gotoEditSubView(edited, idpList.getElements().stream()
								.filter(p -> p.getName() != edited.getName())
								.map(p -> p.getName()).collect(Collectors.toSet()),
								c -> {
									idpList.replaceElement(edited, c);
									fireChange();
									subViewSwitcher.exitSubView();
								});
					}

					).build();

			SingleActionHandler<IndividualTrustedSamlIdpConfiguration> remove = SingleActionHandler
					.builder4Delete(msg, IndividualTrustedSamlIdpConfiguration.class)
					.withHandler(r -> {
						idpList.removeElement(r.iterator().next());
						fireChange();
					}).build();

			return Arrays.asList(edit, remove);
		}

		private void gotoEditSubView(IndividualTrustedSamlIdpConfiguration edited, Set<String> usedNames,
				Consumer<IndividualTrustedSamlIdpConfiguration> onConfirm)
		{
			Set<String> forms;
			Set<String> certificates;

			try
			{
				certificates = pkiMan.getAllCertificateNames();
				forms = getRegistrationForms();

			} catch (EngineException e)
			{
				NotificationPopup.showError(msg, "Can not init trusted IdP editor", e);
				return;
			}

			EditIndividualTrustedIdpSubView subView = new EditIndividualTrustedIdpSubView(msg,
					profileFieldFactory, edited, subViewSwitcher, usedNames, certificates, forms,
					r -> {
						onConfirm.accept(r);
						idpList.focus();
					}, () -> {
						subViewSwitcher.exitSubView();
						idpList.focus();
					});
			subViewSwitcher.goToSubView(subView);

		}

		@Override
		public List<IndividualTrustedSamlIdpConfiguration> getValue()
		{
			return idpList.getElements();
		}

		@Override
		protected Component initContent()
		{
			VerticalLayout main = new VerticalLayout();
			main.setMargin(false);

			Button add = new Button(msg.getMessage("IndividualTrustedIdpComponent.addIdp"));
			add.addClickListener(e -> {
				gotoEditSubView(null, idpList.getElements().stream().map(p -> p.getName())
						.collect(Collectors.toSet()), c -> {
							subViewSwitcher.exitSubView();
							idpList.addElement(c);
							idpList.focus();
							fireChange();
						});
			});
			add.setIcon(Images.add.getResource());
			main.addComponent(add);
			main.setComponentAlignment(add, Alignment.MIDDLE_RIGHT);
			main.addComponent(idpList);
			return main;
		}

		@Override
		protected void doSetValue(List<IndividualTrustedSamlIdpConfiguration> value)
		{
			idpList.setItems(value);
		}

		private void fireChange()
		{
			fireEvent(new ValueChangeEvent<List<IndividualTrustedSamlIdpConfiguration>>(this,
					idpList.getElements(), true));
		}

	}
}
