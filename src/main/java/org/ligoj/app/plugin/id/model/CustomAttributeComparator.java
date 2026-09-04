/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.model;

import org.ligoj.app.iam.UserOrg;

/**
 * Order by a custom attribute value ({@link org.ligoj.app.iam.SimpleUser#getCustomAttributes()}), falling back to the
 * identifier for absent or equal values.
 */
public class CustomAttributeComparator extends AbstractNameComparator {

	private final String attribute;

	/**
	 * Comparator from the custom attribute name.
	 *
	 * @param attribute The custom attribute name, without the {@code customAttributes.} prefix.
	 */
	public CustomAttributeComparator(final String attribute) {
		this.attribute = attribute;
	}

	@Override
	public int compare(final UserOrg o1, final UserOrg o2) {
		return compare(o1, o2, u -> u.getCustomAttributes() == null ? null : u.getCustomAttributes().get(attribute));
	}
}
