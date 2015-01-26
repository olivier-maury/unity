/*
 * Copyright (c) 2015 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.confirmations.states;

import pl.edu.icm.unity.exceptions.InternalException;

import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Base class for states of confirmation process which are bound to a user existing in the database (as opposite 
 * to confirmations associated with a registration request).
 * @author K. Benedyczak
 */
public class UserConfirmationState extends BaseConfirmationState
{
	protected long ownerEntityId;
	
	
	public UserConfirmationState(String facilityId, String type, String value, String locale,
			long ownerEntityId)
	{
		super(facilityId, type, value, locale);
		this.ownerEntityId = ownerEntityId;
	}


	public UserConfirmationState(String facilityId, String type, String value, String locale,
			String successUrl, String errorUrl, long ownerEntityId)
	{
		super(facilityId, type, value, locale, successUrl, errorUrl);
		this.ownerEntityId = ownerEntityId;
	}


	public UserConfirmationState(String serializedState)
	{
		super(serializedState);
		setSerializedConfiguration(serializedState);
	}

	protected UserConfirmationState()
	{
		super();
	}


	public long getOwnerEntityId()
	{
		return ownerEntityId;
	}

	public void setOwnerEntityId(long ownerEntityId)
	{
		this.ownerEntityId = ownerEntityId;
	}
	
	@Override
	protected ObjectNode createState()
	{
		ObjectNode state = super.createState();
		state.put("ownerEntityId", getOwnerEntityId());
		return state;
	}
	
	protected void setSerializedConfiguration(ObjectNode main) throws InternalException
	{
		try
		{
			super.setSerializedConfiguration(main);
			ownerEntityId = main.get("ownerEntityId").asLong();	
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

	}

	
	@Override
	protected void setSerializedConfiguration(String json) throws InternalException
	{
		try
		{
			ObjectNode main = mapper.readValue(json, ObjectNode.class);
			setSerializedConfiguration(main);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}
	}
}
