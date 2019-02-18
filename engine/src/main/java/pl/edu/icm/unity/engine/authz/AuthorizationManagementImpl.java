/*
 * Copyright (c) 2019 Bixbit - Krzysztof Benedyczak. All rights reserved.
 * See LICENCE.txt file for licensing information.
 */

package pl.edu.icm.unity.engine.authz;

import java.util.Set;

import org.springframework.stereotype.Component;

import com.google.common.collect.Sets;

import pl.edu.icm.unity.engine.api.AuthorizationManagement;
import pl.edu.icm.unity.exceptions.AuthorizationException;

/**
 * Implementation of the {@link AuthorizationManagement}
 * 
 * @author P.Piernik
 *
 */
@Component
public class AuthorizationManagementImpl implements AuthorizationManagement
{
	private InternalAuthorizationManager authz;

	public AuthorizationManagementImpl(InternalAuthorizationManager authz)
	{
		this.authz = authz;
	}

	@Override
	public boolean hasAdminAccess() throws AuthorizationException
	{

		Set<AuthzRole> roles = authz.getRoles();
		Set<String> adminRoles = Sets.newHashSet(InternalAuthorizationManagerImpl.SYSTEM_MANAGER_ROLE,
				InternalAuthorizationManagerImpl.CONTENTS_MANAGER_ROLE,
				InternalAuthorizationManagerImpl.PRIVILEGED_INSPECTOR_ROLE,
				InternalAuthorizationManagerImpl.INSPECTOR_ROLE);

		if (roles.stream().map(r -> r.getName()).anyMatch(adminRoles::contains))
		{
			return true;
		}
		return false;
	}
}
