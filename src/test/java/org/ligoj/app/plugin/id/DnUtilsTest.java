/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Test class of {@link DnUtils}
 */
class DnUtilsTest {
	@Test
	void equalsOrParentOf() throws Exception {
		// For coverage only
		final Constructor<?> constructor = DnUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();

		Assertions.assertTrue(DnUtils.equalsOrParentOf("a", "a"));
	}

	@Test
	void equalsOrParentOfParent() {
		Assertions.assertTrue(DnUtils.equalsOrParentOf("a", "b,a"));
	}

	@Test
	void equalsOrParentOfCollectionMultiple() {
		final List<String> strings = new ArrayList<>();
		strings.add("dummy");
		strings.add("ou=p2,ou=p1,ou=base");
		Assertions.assertTrue(DnUtils.equalsOrParentOf(strings, "ou=p2,ou=p1,ou=base"));
		Assertions.assertTrue(DnUtils.equalsOrParentOf(strings, "ou=p3,ou=p2,ou=p1,ou=base"));
		Assertions.assertFalse(DnUtils.equalsOrParentOf(strings, "ou=px,ou=p1,ou=base"));
	}

	@Test
	void equalsOrParentOfNullParent() {
		Assertions.assertFalse(DnUtils.equalsOrParentOf("a", null));
	}

	@Test
	void equalsOrParentOfNullChild() {
		Assertions.assertFalse(DnUtils.equalsOrParentOf((String) null, "a"));
		Assertions.assertFalse(DnUtils.equalsOrParentOf((String) null, null));
	}

	@Test
	void toRdn() {
		Assertions.assertEquals("b", DnUtils.toRdn("a=b"));
		Assertions.assertEquals("b", DnUtils.toRdn("a=B"));
		Assertions.assertEquals("b", DnUtils.toRdn("a=b,c=d"));
	}

	@Test
	void toParentRdn() {
		Assertions.assertEquals("d", DnUtils.toParentRdn("a=b,c=d"));
		Assertions.assertEquals("d", DnUtils.toParentRdn(" a = b , c = D "));
		Assertions.assertEquals("d", DnUtils.toParentRdn("a=b,c=d,e=f"));
	}

}
