/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.oauth.rp.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import io.imunity.webconsole.utils.tprofile.InputTranslationProfileFieldFactory;
import pl.edu.icm.unity.engine.api.PKIManagement;
import pl.edu.icm.unity.engine.api.msg.UnityMessageSource;
import pl.edu.icm.unity.engine.api.token.TokensManagement;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.oauth.rp.verificator.BearerTokenVerificator;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditor;
import pl.edu.icm.unity.webui.authn.authenticators.AuthenticatorEditorFactory;

/**
 * Factory for {@link OAuthRPAuthenticatorEditor}
 * 
 * @author P.Piernik
 *
 */
@Component
class OAuthRPAuthenticatorEditorFactory implements AuthenticatorEditorFactory
{
	private UnityMessageSource msg;
	private TokensManagement tokenMan;
	private PKIManagement pkiMan;
	private InputTranslationProfileFieldFactory profileFieldFactory;;

	@Autowired
	OAuthRPAuthenticatorEditorFactory(UnityMessageSource msg, TokensManagement tokenMan, PKIManagement pkiMan,
			InputTranslationProfileFieldFactory profileFieldFactory)
	{
		this.msg = msg;
		this.tokenMan = tokenMan;
		this.pkiMan = pkiMan;
		this.profileFieldFactory = profileFieldFactory;
	}

	@Override
	public String getSupportedAuthenticatorType()
	{
		return BearerTokenVerificator.NAME;
	}

	@Override
	public AuthenticatorEditor createInstance() throws EngineException
	{
		return new OAuthRPAuthenticatorEditor(msg, tokenMan, pkiMan, profileFieldFactory);
	}

}
