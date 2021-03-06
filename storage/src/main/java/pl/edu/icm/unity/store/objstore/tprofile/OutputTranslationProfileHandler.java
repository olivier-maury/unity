/*
 * Copyright (c) 2013 ICM Uniwersytet Warszawski All rights reserved.
 * See LICENCE.txt file for licensing information.
 */
package pl.edu.icm.unity.store.objstore.tprofile;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;

import pl.edu.icm.unity.JsonUtil;
import pl.edu.icm.unity.store.impl.objstore.GenericObjectBean;
import pl.edu.icm.unity.store.objstore.DefaultEntityHandler;
import pl.edu.icm.unity.types.translation.ProfileType;
import pl.edu.icm.unity.types.translation.TranslationProfile;

/**
 * Handler for {@link TranslationProfile}.
 * 
 * @author K. Benedyczak
 */
@Component
public class OutputTranslationProfileHandler extends DefaultEntityHandler<TranslationProfile>
{
	public static final String TRANSLATION_PROFILE_OBJECT_TYPE = "outputTranslationProfile";
	
	@Autowired
	public OutputTranslationProfileHandler(ObjectMapper jsonMapper)
	{
		super(jsonMapper, TRANSLATION_PROFILE_OBJECT_TYPE, TranslationProfile.class);
	}

	@Override
	public GenericObjectBean toBlob(TranslationProfile value)
	{
		if (value.getProfileType() != ProfileType.OUTPUT)
			throw new IllegalArgumentException("Trying to save profile of " + 
					value.getProfileType() + " as output profile");
		byte[] contents = JsonUtil.serialize2Bytes(value.toJsonObject());
		return new GenericObjectBean(value.getName(), contents, supportedType);
	}

	@Override
	public TranslationProfile fromBlob(GenericObjectBean blob)
	{
		return new TranslationProfile(JsonUtil.parse(blob.getContents()));
	}
}
