/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.engine.mock;

import pl.edu.icm.unity.engine.api.authn.AbstractCredentialRetrieval;
import pl.edu.icm.unity.engine.api.authn.CredentialExchange;
import pl.edu.icm.unity.exceptions.EngineException;
import pl.edu.icm.unity.exceptions.InternalException;

public class MockPasswordRetrieval extends AbstractCredentialRetrieval<MockExchange> implements MockBinding
{
	public MockPasswordRetrieval()
	{
		super("web");
	}

	@Override
	public void setCredentialExchange(CredentialExchange e, String id, long revision)
	{
		super.setCredentialExchange(e, id, revision);
		if (!(e instanceof MockExchange))
			throw new InternalException("Got unsupported exchange: " + 
					e.getClass() + " while only MockExchange is supported");
	}

	@Override
	public String getSerializedConfiguration()
	{
		return "";
	}

	@Override
	public void setSerializedConfiguration(String json)
	{
	}

	@Override
	public Long authenticate() throws EngineException
	{
		return credentialExchange.checkPassword("CN=foo", "PPPbar");
	}
}
