/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import java.util.Date;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.validation.constraints.NotBlank;

import org.ligoj.bootstrap.core.model.AbstractPersistable;
import org.springframework.data.annotation.CreatedBy;
import org.springframework.data.annotation.CreatedDate;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonProperty.Access;

import lombok.Getter;
import lombok.Setter;

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
	@Temporal(TemporalType.TIMESTAMP)
	@Column(updatable = false)
	@JsonProperty(access = Access.READ_ONLY)
	@CreatedDate
	private Date createdDate;

	/**
	 * Related user name/login/UID.
	 */
	@Setter
	@NotBlank
	@JsonProperty(access = Access.READ_ONLY)
	private String login;

}
