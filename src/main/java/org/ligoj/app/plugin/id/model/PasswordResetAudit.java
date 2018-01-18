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
 * Password reset audit entity.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_PASSWORD_RESET_AUDIT")
public class PasswordResetAudit extends AbstractAudited<Integer> {

	/**
	 * User name/login/UID.
	 */
	@NotNull
	@NotBlank
	@LowerCase
	@Pattern(regexp = "^[a-z0-9]+$")
	private String login;

}
