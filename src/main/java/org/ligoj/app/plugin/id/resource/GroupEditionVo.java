/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
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
	 * Department number or name, multivalued. May be used to link user department attribute.
	 */
	private List<String> departments;

	/**
	 * Assistant of this group, multivalued. Must be UID of the related users. Must exist.
	 */
	private List<String> assistants;

	/**
	 * Owner of this group, multivalued. Must be UID of the related users. Must exist.
	 */
	private List<String> owners;

}
