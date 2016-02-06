/**********************************************************************
 *                     Copyright (c) 2015, Jirav
 *                        All Rights Reserved
 *
 *         This is unpublished proprietary source code of Jirav.
 *    Reproduction or distribution, in whole or in part, is forbidden
 *          except by express written permission of Jirav, Inc.
 **********************************************************************/
package pl.edu.icm.unity.restadm;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.io.UnsupportedEncodingException;
import java.util.List;

import javax.ws.rs.core.Response.Status;

import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.StringEntity;
import org.apache.http.util.EntityUtils;
import org.junit.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Lists;

import pl.edu.icm.unity.types.I18nString;
import pl.edu.icm.unity.types.authn.AuthenticationOptionDescription;
import pl.edu.icm.unity.types.endpoint.EndpointConfiguration;
import pl.edu.icm.unity.types.endpoint.EndpointDescription;

/**
 * Endpoints management test
 * @author Krzysztof Benedyczak
 */
public class TestEndpointsManagement extends RESTAdminTestBase
{
	@Test
	public void deployedEndpointsAreReturned() throws Exception
	{
		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		HttpResponse response = client.execute(host, get, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		System.out.println(contents);
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		List<EndpointDescription> returnedL = m.readValue(contents,
				new TypeReference<List<EndpointDescription>>() {});

		assertThat(returnedL.size(), is(1));
		EndpointDescription returned = returnedL.get(0);
		assertThat(returned.getAuthenticatorSets(),
			is(Lists.newArrayList(new AuthenticationOptionDescription("ApassREST"))));
		assertThat(returned.getContextAddress(), is("/restadm"));
		assertThat(returned.getDescription(), is("desc"));
		assertThat(returned.getDisplayedName(), is(new I18nString("restAdmin")));
		assertThat(returned.getId(), is("restAdmin"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpointFactory.NAME));
	}

	@Test
	public void informationOnDeployedEndpointIsReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();

		HttpResponse response = client.execute(host, deploy, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		EndpointDescription returned = m.readValue(contents, EndpointDescription.class);

		assertThat(returned.getAuthenticatorSets(), is(
				Lists.newArrayList(new AuthenticationOptionDescription("ApassREST"))));
		assertThat(returned.getContextAddress(), is("contextA"));
		assertThat(returned.getDescription(), is("desc"));
		assertThat(returned.getDisplayedName(), is(new I18nString("endpoint")));
		assertThat(returned.getId(), is("newEndpoint"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpointFactory.NAME));
	}

	@Test
	public void undeployedEndpointIsNotReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, localcontext);

		HttpDelete delete = new HttpDelete("/restadm/v1/endpoint/newEndpoint");
		client.execute(host, delete, localcontext);

		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		HttpResponse response = client.execute(host, get, localcontext);
		String contents = EntityUtils.toString(response.getEntity());
		assertEquals(contents, Status.OK.getStatusCode(), response.getStatusLine().getStatusCode());
		List<EndpointDescription> returnedL = m.readValue(contents,
				new TypeReference<List<EndpointDescription>>() {});

		assertThat(returnedL.size(), is(1));
	}

	@Test
	public void updatedEndpointIsReturned() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, localcontext);

		HttpPut update = getUpdateRequest();
		HttpResponse response2 = client.execute(host, update, localcontext);

		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getStatusLine().getStatusCode());

		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		HttpResponse response3 = client.execute(host, get, localcontext);
		String contents3 = EntityUtils.toString(response3.getEntity());
		assertEquals(contents3, Status.OK.getStatusCode(), response3.getStatusLine().getStatusCode());
		List<EndpointDescription> returnedL = m.readValue(contents3,
				new TypeReference<List<EndpointDescription>>() {});

		assertThat(returnedL.size(), is(2));

		EndpointDescription returned = returnedL.get(1);
		assertThat(returned.getAuthenticatorSets(), is(
				Lists.newArrayList(new AuthenticationOptionDescription("ApassREST"))));
		assertThat(returned.getContextAddress(), is("contextA"));
		assertThat(returned.getDescription(), is("desc2"));
		assertThat(returned.getDisplayedName(), is(new I18nString("endpoint2")));
		assertThat(returned.getId(), is("newEndpoint"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpointFactory.NAME));
	}


	@Test
	public void onlyTheSetEntriesAreUpdated() throws Exception
	{
		HttpPost deploy = getDeployRequest();
		client.execute(host, deploy, localcontext);

		HttpPut update = getEmptyUpdateRequest();
		HttpResponse response2 = client.execute(host, update, localcontext);

		assertEquals(Status.NO_CONTENT.getStatusCode(), response2.getStatusLine().getStatusCode());

		HttpGet get = new HttpGet("/restadm/v1/endpoints");
		HttpResponse response3 = client.execute(host, get, localcontext);
		String contents3 = EntityUtils.toString(response3.getEntity());
		assertEquals(contents3, Status.OK.getStatusCode(), response3.getStatusLine().getStatusCode());
		List<EndpointDescription> returnedL = m.readValue(contents3,
				new TypeReference<List<EndpointDescription>>() {});

		assertThat(returnedL.size(), is(2));

		EndpointDescription returned = returnedL.get(1);
		assertThat(returned.getAuthenticatorSets(), is(
				Lists.newArrayList(new AuthenticationOptionDescription("ApassREST"))));
		assertThat(returned.getContextAddress(), is("contextA"));
		assertThat(returned.getDescription(), is("desc"));
		assertThat(returned.getDisplayedName(), is(new I18nString("endpoint")));
		assertThat(returned.getId(), is("newEndpoint"));
		assertThat(returned.getRealm().getName(), is("testr"));
		assertThat(returned.getType().getName(), is(RESTAdminEndpointFactory.NAME));

	}

	@Test
	public void deployWithInvalidConfigurationResultsInBadRequest() throws Exception
	{
		HttpPost deploy = getDeployRequestWithInvalidAuthn();
		HttpResponse response = client.execute(host, deploy, localcontext);
		assertEquals(Status.BAD_REQUEST.getStatusCode(), response.getStatusLine().getStatusCode());
	}

	private HttpPost getDeployRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		return getDeployRequest("ApassREST");
	}

	private HttpPost getDeployRequestWithInvalidAuthn() throws UnsupportedEncodingException, JsonProcessingException
	{
		return getDeployRequest("Invalid authn");
	}

	private HttpPost getDeployRequest(String authnDescription) throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPost deploy = new HttpPost("/restadm/v1/endpoint/newEndpoint?typeId=" + RESTAdminEndpointFactory.NAME
				+ "&address=contextA");
		List<AuthenticationOptionDescription> authn = Lists.newArrayList(new AuthenticationOptionDescription(authnDescription));
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("endpoint"),
				"desc",
				authn,
				"",
				"testr");
		String jsonconfig = m.writeValueAsString(config);
		System.out.println(jsonconfig);
		deploy.setEntity(new StringEntity(jsonconfig, ContentType.APPLICATION_JSON));
		return deploy;
	}

	private HttpPut getUpdateRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPut update = new HttpPut("/restadm/v1/endpoint/newEndpoint");
		List<AuthenticationOptionDescription> authn = Lists.newArrayList(new AuthenticationOptionDescription("ApassREST"));
		EndpointConfiguration config = new EndpointConfiguration(new I18nString("endpoint2"),
				"desc2",
				authn,
				"",
				"testr");
		update.setEntity(new StringEntity(m.writeValueAsString(config), ContentType.APPLICATION_JSON));
		return update;
	}

	private HttpPut getEmptyUpdateRequest() throws UnsupportedEncodingException, JsonProcessingException
	{
		HttpPut update = new HttpPut("/restadm/v1/endpoint/newEndpoint");
		update.setEntity(new StringEntity("{}", ContentType.APPLICATION_JSON));
		return update;
	}
}
