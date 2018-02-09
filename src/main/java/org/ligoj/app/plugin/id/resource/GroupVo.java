package org.ligoj.app.plugin.id.resource;

import lombok.Getter;
import lombok.Setter;

/**
 * Group details.
 */
@Getter
@Setter
public class GroupVo {

	/**
	 * Group name, original CN.
	 */
	private String name;

	/**
	 * Is this group can be written by the current principal user.
	 */
	private boolean canWrite;

}
