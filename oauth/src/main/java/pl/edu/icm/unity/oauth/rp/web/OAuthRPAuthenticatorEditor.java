/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.web;

import java.util.Set;

import com.vaadin.data.Binder;
import com.vaadin.data.ValidationResult;
import com.vaadin.data.converter.StringToIntegerConverter;
import com.vaadin.server.Sizeable.Unit;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Component;
import com.vaadin.ui.FormLayout;
import com.vaadin.ui.TextField;
import com.vaadin.ui.VerticalLayout;

import eu.unicore.util.configuration.ConfigurationException;
import eu.unicore.util.httpclient.ServerHostnameCheckingMode;
import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.client.config.CustomProviderProperties.ClientHttpMethod;
import pl.edu.icm.unity.oauth.rp.OAuthRPProperties.VerificationProtocol;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;
import pl.edu.icm.unity.types.authn.AuthenticatorDefinition;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.BaseAuthenticatorEditor;
import pl.edu.icm.unity.webui.common.CollapsibleLayout;
import pl.edu.icm.unity.webui.common.FormLayoutWithFixedCaptionWidth;
import pl.edu.icm.unity.webui.common.FormValidationException;
import pl.edu.icm.unity.webui.common.webElements.SubViewSwitcher;

/**
 * OAuthRP authenticator editor
 * 
 * @author P.Piernik
 *
 */
class OAuthRPAuthenticatorEditor extends BaseAuthenticatorEditor implements AuthenticatorEditor
{
	private static final int LINK_FIELD_WIDTH = 50;

	private TokensManagement tokenMan;
	private PKIManagement pkiMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;

	private Set<String> validators;
	private Binder<OAuthRPConfiguration> configBinder;

	OAuthRPAuthenticatorEditor(UnityMessageSource msg, TokensManagement tokenMan, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory) throws EngineException
	{
		super(msg);
		this.tokenMan = tokenMan;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
		validators = pkiMan.getValidatorNames();
	}

	@Override
	public Component getEditor(AuthenticatorDefinition toEdit, SubViewSwitcher subViewSwitcher,
			boolean forceNameEditable)
	{
		boolean editMode = init(msg.getMessage("OAuthRPAuthenticatorEditor.defaultName"), toEdit,
				forceNameEditable);

		configBinder = new Binder<>(OAuthRPConfiguration.class);

		FormLayout header = buildHeaderSection();

		CollapsibleLayout remoteDataMapping = profileFieldFactory.getWrappedFieldInstance(subViewSwitcher,
				configBinder, "translationProfile");
		CollapsibleLayout advanced = buildAdvancedSection();

		OAuthRPConfiguration config = new OAuthRPConfiguration();
		if (editMode)
		{
			config.fromProperties(toEdit.configuration, pkiMan, tokenMan);
		}

		configBinder.setBean(config);

		VerticalLayout mainView = new VerticalLayout();
		mainView.setMargin(false);
		mainView.addComponent(header);
		mainView.addComponent(remoteDataMapping);
		mainView.addComponent(advanced);

		return mainView;
	}

	private FormLayout buildHeaderSection()
	{

		FormLayoutWithFixedCaptionWidth header = new FormLayoutWithFixedCaptionWidth();
		header.setMargin(true);
		header.addComponent(name);

		TextField clientId = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.clientId"));
		clientId.setWidth(30, Unit.EM);
		configBinder.forField(clientId).asRequired(msg.getMessage("fieldRequired")).bind("clientId");
		header.addComponent(clientId);

		TextField clientSecret = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.clientSecret"));
		clientSecret.setWidth(30, Unit.EM);
		configBinder.forField(clientSecret).asRequired(msg.getMessage("fieldRequired")).bind("clientSecret");
		header.addComponent(clientSecret);

		TextField requiredScopes = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.requiredScopes"));
		requiredScopes.setWidth(30, Unit.EM);
		configBinder.forField(requiredScopes).bind("requiredScopes");
		header.addComponent(requiredScopes);

		ComboBox<VerificationProtocol> verificationProtocol = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.verificationProtocol"));
		verificationProtocol.setItems(VerificationProtocol.values());
		verificationProtocol.setValue(VerificationProtocol.unity);
		configBinder.forField(verificationProtocol).bind("verificationProtocol");
		header.addComponent(verificationProtocol);

		TextField verificationEndpoint = new TextField(
				msg.getMessage("OAuthRPAuthenticatorEditor.verificationEndpoint"));
		verificationEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		verificationEndpoint.setRequiredIndicatorVisible(false);
		configBinder.forField(verificationEndpoint).asRequired((v, c) -> {

			if (verificationProtocol.getValue() != VerificationProtocol.internal && v.isEmpty())
			{
				return ValidationResult.error(msg.getMessage("fieldRequired"));
			} else
			{
				return ValidationResult.ok();
			}
		}).bind("verificationEndpoint");
		header.addComponent(verificationEndpoint);

		TextField profileEndpoint = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.profileEndpoint"));
		profileEndpoint.setWidth(LINK_FIELD_WIDTH, Unit.EM);
		configBinder.forField(profileEndpoint).bind("profileEndpoint");
		header.addComponent(profileEndpoint);

		return header;
	}

	private CollapsibleLayout buildAdvancedSection()
	{
		FormLayoutWithFixedCaptionWidth advanced = new FormLayoutWithFixedCaptionWidth();
		advanced.setMargin(false);

		TextField cacheTime = new TextField(msg.getMessage("OAuthRPAuthenticatorEditor.cacheTime"));
		configBinder.forField(cacheTime)
				.withConverter(new StringToIntegerConverter(
						msg.getMessage("OAuthRPAuthenticatorEditor.cacheTime.notANumber")))
				.bind("cacheTime");

		advanced.addComponent(cacheTime);

		ComboBox<ServerHostnameCheckingMode> clientHostnameChecking = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientHostnameChecking"));
		clientHostnameChecking.setItems(ServerHostnameCheckingMode.values());
		clientHostnameChecking.setValue(ServerHostnameCheckingMode.FAIL);
		configBinder.forField(clientHostnameChecking).bind("clientHostnameChecking");
		advanced.addComponent(clientHostnameChecking);

		ComboBox<String> clientTrustStore = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientTrustStore"));
		clientTrustStore.setItems(validators);

		configBinder.forField(clientTrustStore).bind("clientTrustStore");
		advanced.addComponent(clientTrustStore);

		ComboBox<ClientHttpMethod> clientHttpMethodForProfileAccess = new ComboBox<>(
				msg.getMessage("OAuthRPAuthenticatorEditor.clientHttpMethodForProfileAccess"));
		clientHttpMethodForProfileAccess.setItems(ClientHttpMethod.values());
		clientHttpMethodForProfileAccess.setValue(ClientHttpMethod.get);
		configBinder.forField(clientHttpMethodForProfileAccess).bind("clientHttpMethodForProfileAccess");
		advanced.addComponent(clientHttpMethodForProfileAccess);

		return new CollapsibleLayout(msg.getMessage("OAuthRPAuthenticatorEditor.advanced"), advanced);
	}

	@Override
	public AuthenticatorDefinition getAuthenticatorDefiniton() throws FormValidationException
	{
		return new AuthenticatorDefinition(getName(), BearerTokenVerificator.NAME, getConfiguration(), null);
	}

	private String getConfiguration() throws FormValidationException
	{
		if (configBinder.validate().hasErrors())
			throw new FormValidationException();
		try
		{
			return configBinder.getBean().toProperties(pkiMan, tokenMan);
		} catch (ConfigurationException e)
		{
			throw new FormValidationException("Invalid configuration of the oauth-rp verificator", e);
		}
	}

	
}
