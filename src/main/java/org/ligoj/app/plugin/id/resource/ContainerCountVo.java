/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import org.ligoj.app.model.ContainerType;

import lombok.Getter;
import lombok.Setter;

/**
 * Container where visible users are counted.<br>
 * DN is not exposed.
 */
@Getter
@Setter
public class ContainerCountVo extends ContainerWithScopeVo {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Unique visible members count.
	 */
	private int countVisible;

	/**
	 * Unique visible or not members count.
	 */
	private int count;

	/**
	 * Can manage the members of this group.
	 */
	private boolean canWrite;

	/**
	 * Can delete this group.
	 */
	private boolean canAdmin;

	/**
	 * Container type.
	 */
	private ContainerType containerType;
}
