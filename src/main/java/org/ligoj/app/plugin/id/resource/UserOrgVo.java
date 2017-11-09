package org.ligoj.app.plugin.id.resource;

import java.util.List;

import org.ligoj.app.iam.SimpleUserOrg;

import lombok.Getter;
import lombok.Setter;

/**
 * User details with additional business details about rights.
 */
@Getter
@Setter
public class UserOrgVo extends SimpleUserOrg {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Is this entry can be managed by current user : delete and update all data but groups.
	 */
	private boolean managed;
	
	/**
	 * Is this entry can be administrated by current user : reset password.
	 */
	private boolean administrated;

	/**
	 * Membership, CN of groups.
	 */
	private List<GroupLdapVo> groups;

}
