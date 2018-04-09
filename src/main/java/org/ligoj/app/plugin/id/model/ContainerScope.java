/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.Length;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.validation.DistinguishName;
import org.ligoj.bootstrap.core.model.AbstractNamedEntity;

import lombok.Getter;
import lombok.Setter;

/**
 * Logical scope of container defined by the parent DN. Name attribute is the name of the type.
 */
@Getter
@Setter
@Entity
@Table(name = "LIGOJ_CONTAINER_SCOPE", uniqueConstraints = { @UniqueConstraint(columnNames = { "name", "type" }),
		@UniqueConstraint(columnNames = "dn") })
public class ContainerScope extends AbstractNamedEntity<Integer> {

	/**
	 * SID, for Hazelcast
	 */
	private static final long serialVersionUID = 1L;

	/**
	 * Special name for project for {@link ContainerType#GROUP} type.
	 */
	public static final String TYPE_PROJECT = "Project";

	/**
	 * The "Distinguished Name" parent of of container's DN. The base DN is not included into this String.
	 */
	@NotNull
	@NotBlank
	@Length(max = 255)
	@DistinguishName
	private String dn;

	@NotNull
	private ContainerType type;

	/**
	 * When a type is locked, there is no way to create, update or delete a group of this type.
	 */
	private boolean locked;
}
