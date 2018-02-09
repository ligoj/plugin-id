package org.ligoj.app.plugin.id.resource;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

/**
 * Group bean for edition.
 */
@Getter
@Setter
public class GroupEditionVo extends ContainerEditionVo {

	/**
	 * Optional parent group name. Must exists.
	 */
	private String parent;

	/**
	 * Department number or name, multi-valued. May be used to link user department attribute.
	 */
	private List<String> departments;

	/**
	 * Assistant of this group, multi-valued. Must be UID of the related users. Must exists.
	 */
	private List<String> assistants;

	/**
	 * Owner of this group, multi-valued. Must be UID of the related users. Must exists.
	 */
	private List<String> owners;

}
