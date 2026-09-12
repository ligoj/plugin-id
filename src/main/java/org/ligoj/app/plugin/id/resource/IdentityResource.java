/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.Arrays;
import java.util.List;

import javax.cache.annotation.CacheKey;
import javax.cache.annotation.CacheResult;

import org.ligoj.app.model.Node;
import org.ligoj.app.model.Parameter;
import org.ligoj.app.resource.plugin.AbstractServicePlugin;
import org.ligoj.bootstrap.core.security.SecurityHelper;
import org.ligoj.bootstrap.dao.system.SystemUserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The identity service.
 */
@Component
public class IdentityResource extends AbstractServicePlugin {

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_URL = BASE_URL + "/id";

	/**
	 * Plug-in key.
	 */
	public static final String SERVICE_KEY = SERVICE_URL.replace('/', ':').substring(1);

	/**
	 * Normalized Group name (CN).
	 */
	public static final String PARAMETER_GROUP = SERVICE_KEY + ":group";

	/**
	 * Normalized parent Group name (CN).
	 */
	public static final String PARAMETER_PARENT_GROUP = SERVICE_KEY + ":parent-group";

	/**
	 * Normalized Organizational Unit (OU).
	 */
	public static final String PARAMETER_OU = SERVICE_KEY + ":ou";

	/**
	 * Pattern determining the login is valid for an authentication.
	 */
	public static final String PARAMETER_UID_PATTERN = SERVICE_KEY + ":uid-pattern";

	/**
	 * Custom user attribute names, comma or space separated, the identity provider exposes as
	 * {@link org.ligoj.app.iam.SimpleUser#getCustomAttributes()} and lets the user dialog edit. A node parameter of
	 * the identity service: available on every identity tool node, the tool plugins read it to know which attributes
	 * to load. Supersedes the tool-level declarations such as <code>service:id:ldap:people-custom-attributes</code>.
	 */
	public static final String PARAMETER_PEOPLE_CUSTOM_ATTRIBUTES = SERVICE_KEY + ":people-custom-attributes";

	/**
	 * User attribute names, comma or space separated, that cannot be updated after the creation: <code>firstName</code>,
	 * <code>lastName</code>, <code>company</code>, <code>department</code>, <code>localId</code>, <code>mail</code>
	 * (the address list) or <code>customAttributes.&lt;name&gt;</code>. Unknown names are ignored. The API refuses an
	 * update changing one of them and the user dialog shows them read-only. A node parameter of the identity service.
	 */
	public static final String PARAMETER_READ_ONLY_ATTRIBUTES = SERVICE_KEY + ":read-only-attributes";

	@Override
	public String getKey() {
		return SERVICE_KEY;
	}

	@Override
	public List<Class<?>> getInstalledEntities() {
		return Arrays.asList(Node.class, Parameter.class);
	}

}
