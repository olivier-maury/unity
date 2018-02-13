/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.store.export.update;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashSet;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import pl.edu.icm.unity.store.export.Update;
import pl.edu.icm.unity.store.objstore.msgtemplate.MessageTemplateHandler;
import pl.edu.icm.unity.store.objstore.reg.eform.EnquiryFormHandler;
import pl.edu.icm.unity.store.objstore.reg.form.RegistrationFormHandler;
import pl.edu.icm.unity.store.objstore.reg.invite.InvitationHandler;

/**
 * Update db from 2.4.1 to 2.5.0
 * @author P.Piernik
 *
 */
@Component
public class UpdateFrom2_4_x implements Update

{
	@Autowired
	private ObjectMapper objectMapper;

	@Override
	public InputStream update(InputStream is) throws IOException
	{
		ObjectNode root = (ObjectNode) objectMapper.readTree(is);
		ObjectNode contents = (ObjectNode) root.get("contents");
		updateGenericForm(contents, EnquiryFormHandler.ENQUIRY_FORM_OBJECT_TYPE);
		updateGenericForm(contents, RegistrationFormHandler.REGISTRATION_FORM_OBJECT_TYPE);
		updateInvitationWithCode(contents);
		updateConfirmationTemplateName(contents);

		return new ByteArrayInputStream(objectMapper.writeValueAsBytes(root));
	}

	private Set<ObjectNode> getGenericContent(ObjectNode contents, String type)
	{
		Set<ObjectNode> ret = new HashSet<>();
		ArrayNode generics = (ArrayNode) contents.get(type);
		if (generics != null)
		{
			for (JsonNode obj : generics)
			{
				ret.add((ObjectNode) obj.get("obj"));
			}
		}
		return ret;
	}

	private void updateInvitationWithCode(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				InvitationHandler.INVITATION_OBJECT_TYPE))
		{
			if (objContent.has("channelId"))
				objContent.remove("channelId");
		}
	}

	private void updateConfirmationTemplateName(ObjectNode contents)
	{
		for (ObjectNode objContent : getGenericContent(contents,
				MessageTemplateHandler.MESSAGE_TEMPLATE_OBJECT_TYPE))
		{
			if (objContent.get("consumer").asText().equals("Confirmation"))
			{
				objContent.put("consumer", "EmailConfirmation");
			}
		}
	}

	private void updateGenericForm(ObjectNode contents, String fromType) throws IOException
	{
		for (ObjectNode objContent : getGenericContent(contents, fromType))
		{
			ObjectNode notCfg = (ObjectNode) objContent
					.get("NotificationsConfiguration");
			if (notCfg.has("channel"))
				notCfg.remove("channel");
		}
	}

}