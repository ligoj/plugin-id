/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import java.time.Instant;

import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import org.hibernate.validator.constraints.Length;
import org.ligoj.bootstrap.core.model.AbstractBusinessEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * The state of the database copy of the identity data, keyed by the node that wrote it. A reset of the shared cache
 * tables deletes it; the identity plugin rebuilding its memory from these tables writes it back after a complete
 * full read, so a copy without it is never trusted.
 */
@Entity
@Table(name = "LIGOJ_ID_CACHE_COPY")
@Getter
@Setter
public class CacheCopyState extends AbstractBusinessEntity<String> {

	/**
	 * The time of the full read the copy comes from.
	 */
	private Instant lastFullRead;

	/**
	 * The fingerprint of the configuration the copy was read with.
	 */
	@Length(max = 64)
	private String fingerprint;
}
