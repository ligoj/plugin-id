/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.Collection;

import javax.validation.constraints.Email;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.ligoj.app.iam.SimpleUser;

import lombok.Getter;
import lombok.Setter;

/**
 * User definition for edition.
 */
@Getter
@Setter
public class UserOrgEditionVo extends SimpleUser {

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
	 * Normalized visible groups aliases the principal. Must include writable and
	 * read-only groups.
	 */
	private Collection<String> groups;

}
