/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp.web;

import java.net.URI;
import java.util.Collection;

import org.apache.log4j.Logger;

import pl.edu.icm.unity.saml.sp.RemoteAuthnContext;
import pl.edu.icm.unity.saml.sp.SAMLExchange;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import pl.edu.icm.unity.saml.sp.SamlContextManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.AuthenticationResult.Status;
import pl.edu.icm.unity.server.authn.remote.SandboxAuthnResultCallback;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityMessageSource;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.AuthenticationResultCallback;
import pl.edu.icm.unity.webui.authn.VaadinAuthentication.VaadinAuthenticationUI;
import pl.edu.icm.unity.webui.common.ErrorPopup;
import pl.edu.icm.unity.webui.common.Styles;
import pl.edu.icm.unity.webui.common.idpselector.IdPComponent;
import pl.edu.icm.unity.webui.common.idpselector.IdpSelectorComponent.ScaleMode;
import pl.edu.icm.unity.webui.common.safehtml.HtmlSimplifiedLabel;

import com.vaadin.server.Page;
import com.vaadin.server.RequestHandler;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinSession;
import com.vaadin.server.WrappedSession;
import com.vaadin.ui.Component;
import com.vaadin.ui.Label;
import com.vaadin.ui.VerticalLayout;

/**
 * The UI part of the remote SAML authn. Shows widget with a single, chosen IdP, implements 
 * authN start and awaits for answer in the context. When it is there, the validator is contacted for verification.
 * It is also possible to cancel the authentication which is in progress.
 * @author K. Benedyczak
 */
public class SAMLRetrievalUI implements VaadinAuthenticationUI
{	
	private Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLRetrievalUI.class);

	private UnityMessageSource msg;
	private SAMLExchange credentialExchange;
	private AuthenticationResultCallback callback;
	private SandboxAuthnResultCallback sandboxCallback;
	private String redirectParam;
	
	private String idpKey;
	private Label messageLabel;
	private HtmlSimplifiedLabel errorDetailLabel;
	private SamlContextManagement samlContextManagement;
	
	
	public SAMLRetrievalUI(UnityMessageSource msg, SAMLExchange credentialExchange, 
			SamlContextManagement samlContextManagement, String idpKey)
	{
		this.msg = msg;
		this.credentialExchange = credentialExchange;
		this.samlContextManagement = samlContextManagement;
		this.idpKey = idpKey;
	}

	@Override
	public Component getComponent()
	{
		redirectParam = installRequestHandler();
		
		final SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		VerticalLayout ret = new VerticalLayout();

		ScaleMode scaleMode = samlProperties.getEnumValue(SAMLSPProperties.ICON_SCALE, ScaleMode.class); 
		String name = getName(samlProperties);
		String logoUrl = samlProperties.getLocalizedValue(idpKey + SAMLSPProperties.IDP_LOGO, msg.getLocale());
		IdPComponent idpComponent = new IdPComponent(idpKey, logoUrl, name, scaleMode);

		messageLabel = new Label();
		messageLabel.addStyleName(Styles.error.toString());
		errorDetailLabel = new HtmlSimplifiedLabel();
		errorDetailLabel.addStyleName(Styles.italic.toString());
		errorDetailLabel.setVisible(false);
		ret.addComponents(idpComponent, messageLabel, errorDetailLabel);

		return ret;
	}

	private String getName(SAMLSPProperties samlProperties)
	{
		String name = samlProperties.getLocalizedName(idpKey + SAMLSPProperties.IDP_NAME, msg.getLocale());
		if (name == null)
			return samlProperties.getValue(idpKey + SAMLSPProperties.IDP_ID);
		return name;
	}
	
	private String installRequestHandler()
	{
		VaadinSession session = VaadinSession.getCurrent();
		Collection<RequestHandler> requestHandlers = session.getRequestHandlers();
		for (RequestHandler rh: requestHandlers)
		{
			if (rh instanceof RedirectRequestHandler)
			{
				return ((RedirectRequestHandler)rh).getTriggeringParam();
			}
		}
	
		RedirectRequestHandler rh = new RedirectRequestHandler(); 
		session.addRequestHandler(rh);
		return rh.getTriggeringParam();
	}
	
	private void breakLogin(boolean invokeCancel)
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			session.removeAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
			samlContextManagement.removeAuthnContext(context.getRelayState());
		}
		if (invokeCancel)
			this.callback.cancelAuthentication();
	}
	
	private void showError(String message)
	{
		if (message == null)
		{
			messageLabel.setValue("");
			showErrorDetail(null);
			return;
		}
		messageLabel.setValue(message);
	}

	private void showErrorDetail(String message, Object... args)
	{
		if (message == null)
		{
			errorDetailLabel.setVisible(false);
			errorDetailLabel.setValue("");
			return;
		}
		errorDetailLabel.setVisible(true);
		errorDetailLabel.setValue(msg.getMessage(message, args));
	}
	
	private void startLogin()
	{
		WrappedSession session = VaadinSession.getCurrent().getSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context != null)
		{
			ErrorPopup.showError(msg, msg.getMessage("error"), 
					msg.getMessage("WebSAMLRetrieval.loginInProgressError"));
			return;
		}
		URI requestURI = Page.getCurrent().getLocation();
		String servletPath = requestURI.getPath();
		String query = requestURI.getQuery() == null ? "" : "?" + requestURI.getQuery();
		String currentRelativeURI = servletPath + query;
		try
		{
			context = credentialExchange.createSAMLRequest(idpKey, currentRelativeURI);
			context.setSandboxCallback(sandboxCallback);
		} catch (Exception e)
		{
			ErrorPopup.showError(msg, msg.getMessage("WebSAMLRetrieval.configurationError"), e);
			log.error("Can not create SAML request", e);
			breakLogin(true);
			return;
		}		
		session.setAttribute(SAMLRetrieval.REMOTE_AUTHN_CONTEXT, context);
		samlContextManagement.addAuthnContext(context);
		
		Page.getCurrent().open(servletPath + "?" + redirectParam, null);
	}

	/**
	 * Called when a SAML response is received.
	 * @param authnContext
	 */
	private void onSamlAnswer(RemoteAuthnContext authnContext)
	{
		AuthenticationResult authnResult;
		showError(null);
		String reason = null;
		Exception savedException = null;
		
		try
		{
			authnResult = credentialExchange.verifySAMLResponse(authnContext);
		} catch (AuthenticationException e)
		{
			savedException = e;
			reason = ErrorPopup.getHumanMessage(e, "<br>");
			authnResult = e.getResult();
		} catch (Exception e)
		{
			log.error("Runtime error during SAML response processing or principal mapping", e);
			authnResult = new AuthenticationResult(Status.deny, null);
		}

		if (authnResult.getStatus() == Status.success)
		{
			showError(null);
			breakLogin(false);
		} else if (authnResult.getStatus() == Status.unknownRemotePrincipal && 
				authnContext.getRegistrationFormForUnknown() != null) 
		{
			log.debug("There is a registration form to show for the unknown user: " + 
					authnContext.getRegistrationFormForUnknown());
			authnResult.setFormForUnknownPrincipal(authnContext.getRegistrationFormForUnknown());
			showError(null);
			breakLogin(false);
		} else
		{
			if (savedException != null)
				log.warn("SAML response verification or processing failed", savedException);
			else
				log.warn("SAML response verification or processing failed");
			if (reason != null)
				showErrorDetail("WebSAMLRetrieval.authnFailedDetailInfo", reason);
			showError(msg.getMessage("WebSAMLRetrieval.authnFailedError"));
			breakLogin(false);
		}

		callback.setAuthenticationResult(authnResult);
	}
	
	@Override
	public void setAuthenticationResultCallback(AuthenticationResultCallback callback)
	{
		this.callback = callback;
	}

	@Override
	public void triggerAuthentication()
	{
		startLogin();
	}

	@Override
	public void cancelAuthentication()
	{
		breakLogin(false);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void refresh(VaadinRequest request) 
	{
		WrappedSession session = request.getWrappedSession();
		RemoteAuthnContext context = (RemoteAuthnContext) session.getAttribute(
				SAMLRetrieval.REMOTE_AUTHN_CONTEXT);
		if (context == null)
		{
			log.trace("Either user refreshes page, or different authN arrived");
		} else if (context.getResponse() == null)
		{
			log.debug("Authentication started but SAML response not arrived (user back button)");
		} else 
		{
			onSamlAnswer(context);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getLabel()
	{	
		return getName(credentialExchange.getSamlValidatorSettings());
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public String getImageURL()
	{
		final SAMLSPProperties samlProperties = credentialExchange.getSamlValidatorSettings();
		return samlProperties.getLocalizedValue(idpKey + SAMLSPProperties.IDP_LOGO, msg.getLocale());
	}

	@Override
	public void clear()
	{
		//nop
	}

	@Override
	public void setSandboxAuthnResultCallback(SandboxAuthnResultCallback callback) 
	{
		sandboxCallback = callback;
	}

	@Override
	public String getId()
	{
		return idpKey;
	}
}