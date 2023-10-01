/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.BeforeEach;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.iam.ICompanyRepository;
import org.ligoj.app.iam.IGroupRepository;
import org.ligoj.app.iam.IUserRepository;
import org.ligoj.app.iam.IamConfiguration;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.model.CacheCompany;
import org.ligoj.app.iam.model.CacheGroup;
import org.ligoj.app.iam.model.CacheMembership;
import org.ligoj.app.iam.model.CacheUser;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.plugin.id.dao.ContainerScopeRepository;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;

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

		iamProvider = Mockito.mock(IamProvider.class);
		final IamConfiguration configuration = Mockito.mock(IamConfiguration.class);
		Mockito.when(iamProvider.getConfiguration()).thenReturn(configuration);
		userRepository = Mockito.mock(IUserRepository.class);
		groupRepository = Mockito.mock(IGroupRepository.class);
		companyRepository = Mockito.mock(ICompanyRepository.class);
		Mockito.when(configuration.getUserRepository()).thenReturn(userRepository);
		Mockito.when(configuration.getCompanyRepository()).thenReturn(companyRepository);
		Mockito.when(configuration.getGroupRepository()).thenReturn(groupRepository);
	}
}
