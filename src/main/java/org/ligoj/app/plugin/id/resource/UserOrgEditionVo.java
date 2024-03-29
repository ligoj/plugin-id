/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.Collection;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import org.ligoj.app.iam.SimpleUser;

import lombok.Getter;
import lombok.Setter;

/**
 * User definition for edition.
 */
@Getter
@Setter
@JsonIgnoreProperties(ignoreUnknown = true)
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

	/**
	 * When true, generated password is returned.
	 */
	private boolean returnGeneratePassword;

	@JsonIgnore
	private String generatedPassword;

}
