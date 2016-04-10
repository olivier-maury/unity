/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.identity;

import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.base.registries.IdentityTypesRegistry;
import pl.edu.icm.unity.exceptions.IllegalTypeException;
import pl.edu.icm.unity.exceptions.InternalException;
import pl.edu.icm.unity.store.rdbms.model.BaseBean;
import pl.edu.icm.unity.types.basic.IdentityType;

/**
 * Handles serialization of {@link IdentityType} metadata. The metadata
 * is common for all identity types.
 * @author K. Benedyczak
 */
@Component
public class IdentityTypeSerializer
{
	@Autowired
	private ObjectMapper mapper;
	
	@Autowired
	private IdentityTypesRegistry idTypesRegistry;
	
	public IdentityType resolveIdentityType(BaseBean raw) throws IllegalTypeException
	{
		IdentityType it = new IdentityType(idTypesRegistry.getByName(raw.getName()));
		fromJson(raw.getContents(), it);
		return it;
	}
	
	public BaseBean serialize(IdentityType idType)
	{
		BaseBean toAdd = new BaseBean();

		if (idType.getDescription() == null)
			idType.setDescription(idType.getIdentityTypeProvider().getDefaultDescription());
		toAdd.setName(idType.getIdentityTypeProvider().getId());
		toAdd.setContents(toJson(idType));
		return toAdd;
	}
	
	/**
	 * @param src
	 * @return Json as byte[] with the src contents.
	 */
	private byte[] toJson(IdentityType src)
	{
		ObjectNode main = mapper.createObjectNode();
		main.put("description", src.getDescription());
		main.put("selfModificable", src.isSelfModificable());
		main.put("minInstances", src.getMinInstances());
		main.put("maxInstances", src.getMaxInstances());
		main.put("minVerifiedInstances", src.getMinVerifiedInstances());
		ArrayNode extractedA = main.putArray("extractedAttributes");
		for (Map.Entry<String, String> a: src.getExtractedAttributes().entrySet())
		{
			ObjectNode entry = mapper.createObjectNode();
			entry.put("key", a.getKey());
			entry.put("value", a.getValue());
			extractedA.add(entry);
		}
		try
		{
			return mapper.writeValueAsBytes(main);
		} catch (JsonProcessingException e)
		{
			throw new InternalException("Can't perform JSON serialization", e);
		}
	}
	
	/**
	 * Fills target with JSON contents, checking it for correctness
	 * @param json
	 * @param target
	 */
	private void fromJson(byte[] json, IdentityType target)
	{
		if (json == null)
			return;
		ObjectNode main;
		try
		{
			main = mapper.readValue(json, ObjectNode.class);
		} catch (Exception e)
		{
			throw new InternalException("Can't perform JSON deserialization", e);
		}

		target.setDescription(main.get("description").asText());
		ArrayNode attrs = main.withArray("extractedAttributes");
		Map<String, String> attrs2 = new HashMap<String, String>();
		for (JsonNode a: attrs)
		{
			attrs2.put(a.get("key").asText(), a.get("value").asText());
		}
		target.setExtractedAttributes(attrs2);
		
		if (main.has("selfModificable"))
			target.setSelfModificable(main.get("selfModificable").asBoolean());
		else
			target.setSelfModificable(false);
		
		if (main.has("minInstances"))
		{
			target.setMinInstances(main.get("minInstances").asInt());
			target.setMinVerifiedInstances(main.get("minVerifiedInstances").asInt());
			target.setMaxInstances(main.get("maxInstances").asInt());
		} else
		{
			target.setMinInstances(0);
			target.setMinVerifiedInstances(0);
			target.setMaxInstances(Integer.MAX_VALUE);
		}
	}
}



