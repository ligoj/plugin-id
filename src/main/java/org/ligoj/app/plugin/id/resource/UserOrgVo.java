/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
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
	 * <code>true</code> when this entry can be updated : delete and update all properties but groups.
	 * @see #canWriteGroups
	 */
	private boolean canWrite;

	/**
	 * <code>true</code> when the current principal user can write at least one group.
	 */
	private boolean canWriteGroups;

	/**
	 * Membership, CN of groups.
	 */
	private List<GroupVo> groups;

}
