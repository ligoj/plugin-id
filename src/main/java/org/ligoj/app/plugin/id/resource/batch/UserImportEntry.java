/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.ligoj.app.iam.SimpleUser;

import lombok.Getter;
import lombok.Setter;

/**
 * Import entry
 */
@Getter
@Setter
public class UserImportEntry extends SimpleUser implements BatchElement {

	/**
	 * SID
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * User mail address.
	 */
	@NotNull
	@NotBlank
	@Email
	private String mail;

	/**
	 * Import status. <code>null</code> when not proceeded.
	 */
	private Boolean status;

	/**
	 * Import status text. <code>null</code> when not proceeded.
	 */
	private String statusText;

	/**
	 * Groups aliases.
	 */
	private String groups;

}
