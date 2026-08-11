/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import org.junit.jupiter.api.BeforeEach;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.iam.*;
import org.ligoj.app.iam.model.*;
import org.ligoj.app.plugin.id.dao.ContainerScopeRepository;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Common test class for containers.
 */
public abstract class AbstractContainerResourceTest extends AbstractAppTest {

	@Autowired
	protected ContainerScopeRepository containerScopeRepository;

	protected IUserRepository userRepository;
	protected IGroupRepository groupRepository;
	protected ICompanyRepository companyRepository;

	@BeforeEach
	void prepareData() throws IOException {
		persistEntities("csv",
				new Class<?>[] { DelegateOrg.class, ContainerScope.class, CacheCompany.class, CacheUser.class, CacheGroup.class, CacheMembership.class },
				StandardCharsets.UTF_8);
		cacheManager.getCache("container-scopes").clear();

		iamProvider = mock(IamProvider.class);
		final IamConfiguration configuration = mock(IamConfiguration.class);
		when(iamProvider.getConfiguration()).thenReturn(configuration);
		userRepository = mock(IUserRepository.class);
		groupRepository = mock(IGroupRepository.class);
		companyRepository = mock(ICompanyRepository.class);
		when(configuration.getUserRepository()).thenReturn(userRepository);
		when(configuration.getCompanyRepository()).thenReturn(companyRepository);
		when(configuration.getGroupRepository()).thenReturn(groupRepository);
	}
}
