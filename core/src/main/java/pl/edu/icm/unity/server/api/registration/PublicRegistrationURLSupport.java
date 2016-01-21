/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.server.api.registration;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import pl.edu.icm.unity.server.api.internal.PublicWellKnownURLServlet;
import pl.edu.icm.unity.server.api.internal.SharedEndpointManagement;

/**
 * Defines constants and helper methods used to create public form access URI. 
 * Note that the public form filling code is in principle implemented in web endpoints,
 * however possibility to link to it is required in the core engine, for instance to fill 
 * invitation messages.
 * 
 * @author Krzysztof Benedyczak
 */
public class PublicRegistrationURLSupport
{
	public static final String FRAGMENT_PREFIX = "registration-";
	public static final String CODE_PARAM = "regcode";
	
	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a registration form
	 */
	public static String getPublicLink(String formName, SharedEndpointManagement sharedEndpointMan)
	{
		try
		{
			return sharedEndpointMan.getServletUrl(PublicWellKnownURLServlet.SERVLET_PATH) + 
				"#!" + FRAGMENT_PREFIX + 
				URLEncoder.encode(formName, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}

	/**
	 * @param formName
	 * @param sharedEndpointMan
	 * @return a link to a standalone UI of a registration form with included registration code
	 */
	public static String getPublicLink(String formName, String code, SharedEndpointManagement sharedEndpointMan)
	{
		try
		{
			return sharedEndpointMan.getServletUrl(PublicWellKnownURLServlet.SERVLET_PATH) +
				"?" + CODE_PARAM + "=" + code +
				"#!" + FRAGMENT_PREFIX + 
				URLEncoder.encode(formName, StandardCharsets.UTF_8.name());
		} catch (UnsupportedEncodingException e)
		{
			throw new IllegalStateException(e);
		}
	}
}