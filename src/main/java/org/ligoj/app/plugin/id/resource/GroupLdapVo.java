package org.ligoj.app.plugin.id.resource;

import lombok.Getter;
import lombok.Setter;

/**
 * LDAP Group details.
 */
@Getter
@Setter
public class GroupLdapVo {

	/**
	 * Group name, original CN.
	 */
	private String name;

	/**
	 * Is this group can be written by the current principal user.
	 */
	private boolean canWrite;

}
