/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.ligoj.app.iam.ContainerOrg;

import lombok.Getter;
import lombok.Setter;

/**
 * Group/company for edition.
 */
@Getter
@Setter
public class ContainerEditionVo {

	/**
	 * Container name, original CN.
	 */
	@NotNull
	@NotEmpty
	@NotBlank
	@Size(max = 255)
	@Pattern(regexp = ContainerOrg.NAME_PATTERN)
	private String name;

	/**
	 * The type of this container.
	 */
	@NotNull
	private Integer scope;

	/**
	 * Optional parent/company group name. Must exist.
	 */
	private String parent;

}
