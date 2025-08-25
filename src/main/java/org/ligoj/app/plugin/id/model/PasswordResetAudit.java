/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import org.ligoj.bootstrap.core.model.AbstractPersistable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import java.time.Instant;

/**
 * Password reset audit entity.
 */
@Getter
@Entity
@Table(name = "LIGOJ_PASSWORD_RESET_AUDIT")
public class PasswordResetAudit extends AbstractPersistable<Integer> {

	/**
	 * Created author will never be updated.
	 */
	@Column(updatable = false)
	@JsonProperty(access = Access.READ_ONLY)
	@CreatedBy
	private String createdBy;

	/**
	 * Created date will never be updated.
	 */
	@Column(updatable = false)
	@JsonProperty(access = Access.READ_ONLY)
	@CreatedDate
	private Instant createdDate;

	/**
	 * Related username/login/UID.
	 */
	@Setter
	@NotBlank
	@JsonProperty(access = Access.READ_ONLY)
	private String login;

}
