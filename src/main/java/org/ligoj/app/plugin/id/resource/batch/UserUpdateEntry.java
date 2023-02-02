/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import jakarta.persistence.Transient;
import jakarta.validation.constraints.NotNull;

import org.ligoj.app.plugin.id.resource.UserOrgEditionVo;

import com.fasterxml.jackson.annotation.JsonIgnore;

import lombok.Getter;
import lombok.Setter;

/**
 * Entry update for batch.
 */
@Getter
@Setter
public class UserUpdateEntry implements BatchElement {

	/**
	 * The related user
	 */
	@NotNull
	private String user;

	/**
	 * The operation to execute for this user.
	 */
	@NotNull
	private String operation;

	/**
	 * The optional parameter for the operation. Some operation such as deletion does not require additional
	 * parameters.
	 */
	private String value;

	/**
	 * Import status. <code>null</code> when not proceeded.
	 */
	private Boolean status;

	/**
	 * Import status text. <code>null</code> when not proceeded.
	 */
	private String statusText;

	/**
	 * The related resolved user
	 */
	@Transient
	@JsonIgnore
	private UserOrgEditionVo userEdit;

}
