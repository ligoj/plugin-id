package org.ligoj.app.plugin.id.resource;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotEmpty;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

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
	 * Group name, original CN.
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

}
