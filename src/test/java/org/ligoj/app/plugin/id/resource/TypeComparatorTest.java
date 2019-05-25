/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.ligoj.app.iam.GroupOrg;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.dao.CacheContainerRepository;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.app.plugin.id.resource.AbstractContainerResource.TypeComparator;

/**
 * Test class of {@link TypeComparator}
 */
class TypeComparatorTest {

	@Test
	void compareNullType() {
		final Comparator<GroupOrg> comparator = newComparator(new ArrayList<>());
		final List<GroupOrg> groups = new ArrayList<>();
		groups.add(new GroupOrg("cn=NameA,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameA", null));
		groups.add(new GroupOrg("cn=NameC,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameC", null));
		groups.add(new GroupOrg("cn=NameB,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameB", null));
		groups.sort(comparator);
		Assertions.assertEquals("NameA", groups.get(0).getName());
		Assertions.assertEquals("NameB", groups.get(1).getName());
		Assertions.assertEquals("NameC", groups.get(2).getName());
	}

	@Test
	void compareNullLeft() {
		final ContainerScope containerType = new ContainerScope();
		containerType.setDn("ou=Internal,dc=sample,dc=com");
		containerType.setName("name1");
		containerType.setType(ContainerType.GROUP);

		final Comparator<GroupOrg> comparator = newComparator(Collections.singletonList(containerType));
		final List<GroupOrg> groups = new ArrayList<>();
		groups.add(new GroupOrg("cn=NameA,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameA", null));
		groups.add(new GroupOrg("cn=NameC,ou=Client1,ou=Project,ou=Internal,dc=sample,dc=com", "NameC", null));
		groups.add(new GroupOrg("cn=NameB,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameB", null));
		groups.sort(comparator);
		Assertions.assertEquals("NameC", groups.get(0).getName());
		Assertions.assertEquals("NameA", groups.get(1).getName());
		Assertions.assertEquals("NameB", groups.get(2).getName());
	}

	@Test
	void compareNullRight() {
		final ContainerScope containerType = new ContainerScope();
		containerType.setDn("ou=External,dc=sample,dc=com");
		containerType.setName("name1");
		containerType.setType(ContainerType.GROUP);

		final Comparator<GroupOrg> comparator = newComparator(Collections.singletonList(containerType));
		final List<GroupOrg> groups = new ArrayList<>();
		groups.add(new GroupOrg("cn=NameA,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameA", null));
		groups.add(new GroupOrg("cn=NameC,ou=Client1,ou=Project,ou=Internal,dc=sample,dc=com", "NameC", null));
		groups.add(new GroupOrg("cn=NameB,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameB", null));
		groups.sort(comparator);
		Assertions.assertEquals("NameA", groups.get(0).getName());
		Assertions.assertEquals("NameB", groups.get(1).getName());
		Assertions.assertEquals("NameC", groups.get(2).getName());
	}

	@Test
	void compare() {
		final ContainerScope containerType1 = new ContainerScope();
		containerType1.setDn("ou=External,dc=sample,dc=com");
		containerType1.setName("name2");
		containerType1.setType(ContainerType.GROUP);
		final ContainerScope containerType2 = new ContainerScope();
		containerType2.setDn("ou=Internal,dc=sample,dc=com");
		containerType2.setName("name1");
		containerType2.setType(ContainerType.GROUP);

		final Comparator<GroupOrg> comparator = newComparator(Arrays.asList(containerType1, containerType2));
		final List<GroupOrg> groups = new ArrayList<>();
		groups.add(new GroupOrg("cn=NameB,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameB", null));
		groups.add(new GroupOrg("cn=NameC,ou=Client1,ou=Project,ou=Internal,dc=sample,dc=com", "NameC", null));
		groups.add(new GroupOrg("cn=NameA,ou=Client1,ou=Project,ou=External,dc=sample,dc=com", "NameA", null));
		groups.sort(comparator);
		Assertions.assertEquals("NameC", groups.get(0).getName());
		Assertions.assertEquals("NameA", groups.get(1).getName());
		Assertions.assertEquals("NameB", groups.get(2).getName());
	}

	private Comparator<GroupOrg> newComparator(final List<ContainerScope> containerTypes) {
		return new AbstractContainerResource<GroupOrg, GroupEditionVo, CacheGroup>(ContainerType.GROUP) {

			@Override
			protected IGroupRepository getRepository() {
				return null;
			}

			@Override
			protected String toDn(final GroupEditionVo container, final ContainerScope scope) {
				return null;
			}

			@Override
			protected CacheContainerRepository<CacheGroup> getCacheRepository() {
				return null;
			}
		}.new TypeComparator(containerTypes);
	}
}
