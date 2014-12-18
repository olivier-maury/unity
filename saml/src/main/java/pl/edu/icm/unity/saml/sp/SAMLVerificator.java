/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.sp;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URL;
import java.util.Collection;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.apache.xmlbeans.XmlException;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.samly2.SAMLBindings;
import eu.unicore.samly2.SAMLConstants;
import eu.unicore.samly2.trust.SamlTrustChecker;
import eu.unicore.samly2.validators.ReplayAttackChecker;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.saml.SAMLEndpointDefinition;
import pl.edu.icm.unity.saml.SAMLHelper;
import pl.edu.icm.unity.saml.SAMLResponseValidatorUtil;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.IdentityTypeMapper;
import pl.edu.icm.unity.saml.metadata.MetadataProvider;
import pl.edu.icm.unity.saml.metadata.MetadataProviderFactory;
import pl.edu.icm.unity.saml.metadata.MultiMetadataServlet;
import pl.edu.icm.unity.saml.metadata.cfg.MetaDownloadManager;
import pl.edu.icm.unity.saml.metadata.cfg.MetaToSPConfigConverter;
import pl.edu.icm.unity.saml.metadata.cfg.RemoteMetaManager;
import pl.edu.icm.unity.saml.slo.SAMLLogoutProcessor.SamlTrustProvider;
import pl.edu.icm.unity.saml.slo.SLOReplyInstaller;
import pl.edu.icm.unity.server.api.PKIManagement;
import pl.edu.icm.unity.server.api.TranslationProfileManagement;
import pl.edu.icm.unity.server.authn.AuthenticationException;
import pl.edu.icm.unity.server.authn.AuthenticationResult;
import pl.edu.icm.unity.server.authn.remote.AbstractRemoteVerificator;
import pl.edu.icm.unity.server.authn.remote.RemotelyAuthenticatedInput;
import pl.edu.icm.unity.server.authn.remote.InputTranslationEngine;
import pl.edu.icm.unity.server.utils.ExecutorsService;
import pl.edu.icm.unity.server.utils.Log;
import pl.edu.icm.unity.server.utils.UnityServerConfiguration;
import xmlbeans.org.oasis.saml2.assertion.NameIDType;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;
import xmlbeans.org.oasis.saml2.protocol.AuthnRequestDocument;
import xmlbeans.org.oasis.saml2.protocol.ResponseDocument;

/**
 * Binding irrelevant SAML logic: creation of a SAML authentication request and verification of the answer.
 * @author K. Benedyczak
 */
public class SAMLVerificator extends AbstractRemoteVerificator implements SAMLExchange
{
	private static final Logger log = Log.getLogger(Log.U_SERVER_SAML, SAMLVerificator.class);
	
	private UnityServerConfiguration mainConfig;
	private SAMLSPProperties samlProperties;
	private PKIManagement pkiMan;
	private MultiMetadataServlet metadataServlet;
	private ExecutorsService executorsService;
	private String responseConsumerAddress;
	private Map<String, RemoteMetaManager> remoteMetadataManagers;
	private MetaDownloadManager downloadManager;
	private RemoteMetaManager myMetadataManager;
	private ReplayAttackChecker replayAttackChecker;
	private SLOSPManager sloManager;
	private SLOReplyInstaller sloReplyInstaller;
	
	public SAMLVerificator(String name, String description,
			TranslationProfileManagement profileManagement,
			InputTranslationEngine trEngine, PKIManagement pkiMan,
			ReplayAttackChecker replayAttackChecker, ExecutorsService executorsService,
			MultiMetadataServlet metadataServlet, URL baseAddress, String baseContext,
			Map<String, RemoteMetaManager> remoteMetadataManagers,
			MetaDownloadManager downloadManager, UnityServerConfiguration mainConfig, 
			SLOSPManager sloManager, SLOReplyInstaller sloReplyInstaller)
	{
		super(name, description, SAMLExchange.ID, profileManagement, trEngine);
		this.remoteMetadataManagers = remoteMetadataManagers;
		this.downloadManager = downloadManager;
		this.pkiMan = pkiMan;
		this.mainConfig = mainConfig;
		this.metadataServlet = metadataServlet;
		this.executorsService = executorsService;
		this.responseConsumerAddress = baseAddress + baseContext + SAMLResponseConsumerServlet.PATH;
		this.replayAttackChecker = replayAttackChecker;
		this.sloManager = sloManager;
		this.sloReplyInstaller = sloReplyInstaller;
	}

	@Override
	public String getSerializedConfiguration() throws InternalException
	{
		StringWriter sbw = new StringWriter();
		try
		{
			samlProperties.getProperties().store(sbw, "");
		} catch (IOException e)
		{
			throw new InternalException("Can't serialize SAML verificator configuration", e);
		}
		return sbw.toString();	
	}

	/**
	 * Configuration in samlProperties is loaded, but it can be modified at runtime by the metadata manager.
	 * Therefore the source properties are used only to configure basic things (not related to trusted IDPs)
	 * while the virtual properties are used for authentication process setup.
	 */
	@Override
	public void setSerializedConfiguration(String source) throws InternalException
	{
		try
		{
			Properties properties = new Properties();
			properties.load(new StringReader(source));
			samlProperties = new SAMLSPProperties(properties, pkiMan);
		} catch(ConfigurationException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator", e);
		} catch (IOException e)
		{
			throw new InternalException("Invalid configuration of the SAML verificator(?)", e);
		}
		
		if (samlProperties.getBooleanValue(SamlProperties.PUBLISH_METADATA))
			exposeMetadata();
		if (!remoteMetadataManagers.containsKey(instanceName))
		{
			myMetadataManager = new RemoteMetaManager(samlProperties, 
					mainConfig, executorsService, pkiMan, 
					new MetaToSPConfigConverter(pkiMan), downloadManager, 
						SAMLSPProperties.IDPMETA_PREFIX);
			remoteMetadataManagers.put(instanceName, myMetadataManager);
			myMetadataManager.start();
		} else
		{
			myMetadataManager = remoteMetadataManagers.get(instanceName);
			myMetadataManager.setBaseConfiguration(samlProperties);
		}
		
		try
		{
			initSLO();
		} catch (EngineException e)
		{
			throw new InternalException("Can't initialize Single Logout subsystem for "
					+ "the authenticator " + getName(), e);
		}
	}

	private void initSLO() throws EngineException
	{
		SamlTrustProvider samlTrustProvider = new SamlTrustProvider()
		{
			@Override
			public SamlTrustChecker getTrustChecker()
			{
				SAMLSPProperties config = getSamlValidatorSettings();
				return config.getTrustChecker();
			}

			@Override
			public Collection<SAMLEndpointDefinition> getSLOEndpoints(NameIDType samlId)
			{
				SAMLSPProperties config = getSamlValidatorSettings();
				String configKey = config.getIdPConfigKey(samlId);
				if (configKey == null)
					return null;
				return config.getLogoutEndpointsFromStructuredList(configKey);
			}
		};
		
		String sloPath = samlProperties.getValue(SAMLSPProperties.SLO_PATH);
		String sloRealm = samlProperties.getValue(SAMLSPProperties.SLO_REALM);
		
		if (sloPath == null || sloRealm == null)
		{
			log.warn("Single Logout functionality will be disabled for SAML authenticator "
					+ getName() + " as its path and/or realm are/is undefined.");
			return;
		}
		
		String samlId = samlProperties.getValue(SAMLSPProperties.REQUESTER_ID);
		X509Credential credential = samlProperties.getRequesterCredential();
		IdentityTypeMapper idMapper = new IdentityTypeMapper(samlProperties);
		sloManager.deployAsyncServlet(sloPath,  
				idMapper, 
				600000, 
				samlId, 
				credential, 
				samlTrustProvider, 
				sloRealm);
		sloManager.deploySyncServlet(sloPath, 
				idMapper, 
				600000, 
				samlId, 
				credential, 
				samlTrustProvider, 
				sloRealm);
		
		sloReplyInstaller.enable();
	}
	
	private void exposeMetadata()
	{
		String metaPath = samlProperties.getValue(SAMLSPProperties.METADATA_PATH);
		IndexedEndpointType consumerEndpoint = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint.setIndex(1);
		consumerEndpoint.setBinding(SAMLConstants.BINDING_HTTP_POST);
		consumerEndpoint.setLocation(responseConsumerAddress);
		consumerEndpoint.setIsDefault(true);

		IndexedEndpointType consumerEndpoint2 = IndexedEndpointType.Factory.newInstance();
		consumerEndpoint2.setIndex(2);
		consumerEndpoint2.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);
		consumerEndpoint2.setLocation(responseConsumerAddress);
		consumerEndpoint2.setIsDefault(false);

		EndpointType[] sloEndpoints = null;
		String sloPath = samlProperties.getValue(SAMLSPProperties.SLO_PATH);
		String sloEndpointURL = sloPath != null ? sloManager.getAsyncServletURL(sloPath) : null;
		String sloSoapPath = sloPath != null ? sloManager.getSyncServletURL(sloPath) : null; 
		if (sloEndpointURL != null && sloSoapPath != null)
		{
			EndpointType sloPost = EndpointType.Factory.newInstance();
			sloPost.setLocation(sloEndpointURL);
			sloPost.setBinding(SAMLConstants.BINDING_HTTP_POST);
			sloPost.setResponseLocation(sloReplyInstaller.getServletURL());
			
			EndpointType sloRedirect = EndpointType.Factory.newInstance();
			sloRedirect.setLocation(sloEndpointURL);
			sloRedirect.setResponseLocation(sloReplyInstaller.getServletURL());
			sloRedirect.setBinding(SAMLConstants.BINDING_HTTP_REDIRECT);
			
			EndpointType sloSoap = EndpointType.Factory.newInstance();
			sloSoap.setLocation(sloSoapPath);
			sloSoap.setBinding(SAMLConstants.BINDING_SOAP);
			
			sloEndpoints = new EndpointType[] {sloPost, sloRedirect, sloSoap};
		}
		
		IndexedEndpointType[] assertionConsumerEndpoints = new IndexedEndpointType[] {consumerEndpoint,
				consumerEndpoint2};
		MetadataProvider provider = MetadataProviderFactory.newSPInstance(samlProperties, 
				executorsService, assertionConsumerEndpoints, sloEndpoints);
		metadataServlet.addProvider("/" + metaPath, provider);
	}
	
	@Override
	public RemoteAuthnContext createSAMLRequest(String idpKey, String servletPath) throws InternalException
	{
		RemoteAuthnContext context = new RemoteAuthnContext(getSamlValidatorSettings(), idpKey);
		
		SAMLSPProperties samlPropertiesCopy = context.getContextConfig();
		boolean sign = samlPropertiesCopy.isSignRequest(idpKey);
		String requesterId = samlPropertiesCopy.getValue(SAMLSPProperties.REQUESTER_ID);
		String identityProviderURL = samlPropertiesCopy.getValue(idpKey + SAMLSPProperties.IDP_ADDRESS);
		String requestedNameFormat = samlPropertiesCopy.getRequestedNameFormat(idpKey);
		X509Credential credential = sign ? samlPropertiesCopy.getRequesterCredential() : null;
		
		AuthnRequestDocument request = SAMLHelper.createSAMLRequest(responseConsumerAddress, sign, 
				requesterId, identityProviderURL,
				requestedNameFormat, true, credential);
		context.setRequest(request.xmlText(), request.getAuthnRequest().getID(), servletPath);
		return context;
	}

	@Override
	public AuthenticationResult verifySAMLResponse(RemoteAuthnContext context) throws AuthenticationException
	{
		RemoteAuthnState state = startAuthnResponseProcessing(context.getSandboxCallback(), 
				Log.U_SERVER_TRANSLATION, Log.U_SERVER_SAML);
		
		try
		{
			RemotelyAuthenticatedInput input = getRemotelyAuthenticatedInput(context);
			SAMLSPProperties config = context.getContextConfig();
			String idpKey = context.getContextIdpKey();
		
			return getResult(input, config.getValue(idpKey + SAMLSPProperties.IDP_TRANSLATION_PROFILE), 
					state);
		} catch (Exception e)
		{
			finishAuthnResponseProcessing(state, e);
			throw e;
		}
	}
	
	private RemotelyAuthenticatedInput getRemotelyAuthenticatedInput(RemoteAuthnContext context) 
			throws AuthenticationException 
	{
		ResponseDocument responseDocument;
		try
		{
			responseDocument = ResponseDocument.Factory.parse(context.getResponse());
		} catch (XmlException e)
		{
			throw new AuthenticationException("The SAML response can not be parsed - " +
					"XML data is corrupted", e);
		}
		
		SAMLResponseValidatorUtil responseValidatorUtil = new SAMLResponseValidatorUtil(
				getSamlValidatorSettings(), 
				replayAttackChecker, responseConsumerAddress);
		RemotelyAuthenticatedInput input = responseValidatorUtil.verifySAMLResponse(responseDocument, 
				context.getRequestId(), 
				SAMLBindings.valueOf(context.getResponseBinding().toString()), 
				context.getGroupAttribute(), context.getContextIdpKey());
		return input;
	}
	
	@Override
	public SAMLSPProperties getSamlValidatorSettings()
	{
		return (SAMLSPProperties) myMetadataManager.getVirtualConfiguration();
	}
}


