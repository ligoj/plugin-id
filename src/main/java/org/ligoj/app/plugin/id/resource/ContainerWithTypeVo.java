package org.ligoj.app.plugin.id.resource;

import org.ligoj.bootstrap.core.NamedBean;

import lombok.Getter;
import lombok.Setter;

/**
 * Container with type.<br>
 * DN is not exposed, Original CN (not normalized) is exposed as {@link #name}
 */
@Getter
@Setter
public class ContainerWithTypeVo extends NamedBean<String> {

	/**
	 * Type name.
	 */
	private String type;

	/**
	 * Is this container is locked? A locked container cannot be created or deleted from the UI. It's likely a vital
	 * organizational container.
	 */
	private boolean locked;

}
