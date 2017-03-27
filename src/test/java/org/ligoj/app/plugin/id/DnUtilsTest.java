package org.ligoj.app.plugin.id;

import java.lang.reflect.Constructor;
import java.util.ArrayList;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;
import org.ligoj.app.plugin.id.DnUtils;

/**
 * Test class of {@link DnUtils}
 */
public class DnUtilsTest {
	@Test
	public void equalsOrParentOf() throws Exception {
		// For coverage only
		final Constructor<?> constructor = DnUtils.class.getDeclaredConstructor();
		constructor.setAccessible(true);
		constructor.newInstance();

		Assert.assertTrue(DnUtils.equalsOrParentOf("a", "a"));
	}

	@Test
	public void equalsOrParentOfParent() {
		Assert.assertTrue(DnUtils.equalsOrParentOf("a", "b,a"));
	}

	@Test
	public void equalsOrParentOfCollectionMultiple() {
		final List<String> strings = new ArrayList<>();
		strings.add("dummy");
		strings.add("ou=p2,ou=p1,ou=base");
		Assert.assertTrue(DnUtils.equalsOrParentOf(strings, "ou=p2,ou=p1,ou=base"));
		Assert.assertTrue(DnUtils.equalsOrParentOf(strings, "ou=p3,ou=p2,ou=p1,ou=base"));
		Assert.assertFalse(DnUtils.equalsOrParentOf(strings, "ou=px,ou=p1,ou=base"));
	}

	@Test
	public void equalsOrParentOfNullParent() {
		Assert.assertFalse(DnUtils.equalsOrParentOf("a", null));
	}

	@Test
	public void equalsOrParentOfNullChild() {
		Assert.assertFalse(DnUtils.equalsOrParentOf((String) null, "a"));
		Assert.assertFalse(DnUtils.equalsOrParentOf((String) null, null));
	}

	@Test
	public void toRdn() {
		Assert.assertEquals("b", DnUtils.toRdn("a=b"));
		Assert.assertEquals("b", DnUtils.toRdn("a=B"));
		Assert.assertEquals("b", DnUtils.toRdn("a=b,c=d"));
	}

	@Test
	public void toParentRdn() {
		Assert.assertEquals("d", DnUtils.toParentRdn("a=b,c=d"));
		Assert.assertEquals("d", DnUtils.toParentRdn(" a = b , c = D "));
		Assert.assertEquals("d", DnUtils.toParentRdn("a=b,c=d,e=f"));
	}

}
