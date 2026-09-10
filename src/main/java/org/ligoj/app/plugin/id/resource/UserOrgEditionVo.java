/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.Collection;
import java.util.Objects;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Email;
import org.apache.commons.lang3.StringUtils;
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
	/**
	 * Single mail address, the legacy contract (CLI, batch imports). Ignored when {@link #mails} carries at least
	 * one address. A user may have no mail: no password notification is sent then.
	 */
	@Email
	private String mail;

	/**
	 * Mail addresses, the contract of the user dialog. Blank entries are ignored.
	 */
	private List<@Email String> mails;

	/**
	 * The mail addresses to store: the non-blank entries of {@link #mails} when there is at least one, else the
	 * single {@link #mail}, else an empty list.
	 *
	 * @return The mail addresses to store, never <code>null</code>.
	 */
	@JsonIgnore
	public List<String> getEffectiveMails() {
		final var list = mails == null ? List.<String>of()
				: mails.stream().map(StringUtils::trimToNull).filter(Objects::nonNull).toList();
		if (!list.isEmpty()) {
			return list;
		}
		return StringUtils.isBlank(mail) ? List.of() : List.of(mail.trim());
	}

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
