package org.ligoj.app.plugin.id.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import org.ligoj.bootstrap.core.model.AbstractAudited;
import org.ligoj.bootstrap.core.validation.LowerCase;

import lombok.Getter;
import lombok.Setter;

/**
 * administrator password reset request
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_ADMIN_PASSWORD_RESET")
public class AdminPasswordReset extends AbstractAudited<Integer> {

	/**
	 * serial UID
	 */
	private static final long serialVersionUID = -2317331866002580938L;

	/**
	 * User name/login/UID.
	 */
	@NotNull
	@NotBlank
	@LowerCase
	@Pattern(regexp = "^[a-z0-9]+$")
	private String login;

}
