/*
 * Copyright (c) 2014 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.saml.metadata;

import java.io.File;
import java.io.IOException;

import eu.emi.security.authn.x509.X509Credential;
import eu.unicore.util.configuration.ConfigurationException;
import pl.edu.icm.unity.engine.api.utils.ExecutorsService;
import pl.edu.icm.unity.saml.SamlProperties;
import pl.edu.icm.unity.saml.idp.SamlIdpProperties;
import pl.edu.icm.unity.saml.sp.SAMLSPProperties;
import xmlbeans.org.oasis.saml2.metadata.EndpointType;
import xmlbeans.org.oasis.saml2.metadata.IndexedEndpointType;

/**
 * Utility class simplifying creation of {@link MetadataProvider}s.
 * @author K. Benedyczak
 */
public class MetadataProviderFactory
{
	/**
	 * @param samlProperties
	 * @param executorsService
	 * @param endpoints
	 * @return metadata of an IDP
	 */
	public static MetadataProvider newIdpInstance(SamlIdpProperties samlProperties, 
			ExecutorsService executorsService, EndpointType[] ssoEndpoints, 
			EndpointType[] attributeQueryEndpoints, EndpointType[] sloEndpoints)
	{
		MetadataProvider metaProvider;
		File metadataFile = samlProperties.getFileValue(SamlProperties.METADATA_SOURCE, false);
		if (metadataFile == null)
		{
			metaProvider = new IdpMetadataGenerator(samlProperties, ssoEndpoints, 
					attributeQueryEndpoints, sloEndpoints);
		} else
		{
			try
			{
				metaProvider = new FileMetadataProvider(executorsService, metadataFile);
			} catch (IOException e)
			{
				throw new ConfigurationException("Can't initialize metadata provider, " +
						"problem loading metadata", e);
			}
		}
		return addSigner(metaProvider, samlProperties, samlProperties.getSamlIssuerCredential());
	}
	
	/**
	 * @param samlProperties
	 * @param executorsService
	 * @param endpoints
	 * @return metadata of a SP
	 */
	public static MetadataProvider newSPInstance(SAMLSPProperties samlProperties, 
			ExecutorsService executorsService, IndexedEndpointType[] assertionConsumerEndpoints, 
			EndpointType[] sloEndpoints)
	{
		MetadataProvider metaProvider;
		File metadataFile = samlProperties.getFileValue(SamlProperties.METADATA_SOURCE, false);
		if (metadataFile == null)
		{
			metaProvider = new SPMetadataGenerator(samlProperties, assertionConsumerEndpoints,
					sloEndpoints);
		} else
		{
			try
			{
				metaProvider = new FileMetadataProvider(executorsService, metadataFile);
			} catch (IOException e)
			{
				throw new ConfigurationException("Can't initialize metadata provider, " +
						"problem loading metadata", e);
			}
		}
		
		return addSigner(metaProvider, samlProperties, samlProperties.getRequesterCredential());
	}

	
	private static MetadataProvider addSigner(MetadataProvider metaProvider, SamlProperties samlProperties,
			X509Credential credential)
	{
		if (samlProperties.getBooleanValue(SamlProperties.SIGN_METADATA))
		{
			try
			{
				metaProvider = new MetadataSigner(metaProvider, credential);
			} catch (Exception e)
			{
				throw new ConfigurationException("Can't initialize metadata provider, " +
						"problem signing metadata", e);
			}
		}
		return metaProvider;
	}
}
