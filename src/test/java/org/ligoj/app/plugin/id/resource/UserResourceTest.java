package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

import javax.transaction.Transactional;
import javax.ws.rs.core.UriInfo;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.MethodSorters;
import org.ligoj.app.AbstractAppTest;
import org.ligoj.app.MatcherUtil;
import org.ligoj.app.api.SimpleUserOrg;
import org.ligoj.app.api.UserOrg;
import org.ligoj.app.iam.IamProvider;
import org.ligoj.app.iam.model.DelegateOrg;
import org.ligoj.app.iam.model.DelegateType;
import org.ligoj.bootstrap.core.json.TableItem;
import org.ligoj.bootstrap.core.json.datatable.DataTableAttributes;
import org.ligoj.bootstrap.core.resource.BusinessException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import net.sf.ehcache.CacheManager;

/**
 * Test of {@link UserLdapResource}<br>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = "classpath:/META-INF/spring/application-context-test.xml")
@Rollback
@Transactional
@org.junit.FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class UserResourceTest extends AbstractAppTest {

	@Autowired
	private UserLdapResource resource;

	/**
	 * Check the result : expects one entry
	 */
	private void checkResult(final TableItem<UserOrgVo> tableItem) {
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("flasta", userLdap.getId());
		Assert.assertEquals("Firsta", userLdap.getFirstName());
		Assert.assertEquals("Lasta", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("flasta@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("DIG RHA", userLdap.getGroups().get(0).getName());
	}

	private UriInfo newUriInfoAsc(final String ascProperty) {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORTED_COLUMN, "2");
		uriInfo.getQueryParameters().add("columns[2][data]", ascProperty);
		uriInfo.getQueryParameters().add(DataTableAttributes.SORT_DIRECTION, "asc");
		return uriInfo;
	}

	private UriInfo newUriInfoDesc(final String property) {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "100");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORTED_COLUMN, "2");
		uriInfo.getQueryParameters().add("columns[2][data]", property);
		uriInfo.getQueryParameters().add(DataTableAttributes.SORT_DIRECTION, "desc");
		return uriInfo;
	}

	@Before
	public void prepareData() throws IOException {
		persistEntities("csv/app-test", new Class[] { DelegateOrg.class }, StandardCharsets.UTF_8.name());
		CacheManager.getInstance().getCache("ldap").removeAll();

		// Force the cache to be created
		getUser().findAll();
	}

	@Test
	public void findById() {
		final UserOrg userLdap = resource.findById("fdaugan");
		findById(userLdap);
	}

	@Test
	public void findByIdNoCache() {
		final UserOrg userLdap = resource.findByIdNoCache("fdaugan");
		Assert.assertNotNull(userLdap);
		Assert.assertEquals("fdaugan", userLdap.getId());
		Assert.assertEquals("Fabrice", userLdap.getFirstName());
		Assert.assertEquals("Daugan", userLdap.getLastName());
		Assert.assertEquals("gfi", userLdap.getCompany());
		Assert.assertEquals("fabrice.daugan@sample.com", userLdap.getMails().get(0));
	}

	@Test
	public void authenticate() {
		Assert.assertTrue(resource.authenticate("fdaugan", "Azerty01"));
		Assert.assertFalse(resource.authenticate("fdaugan", "-bad-"));
	}

	@Test
	public void findByIdCaseInsensitive() {
		final UserOrg userLdap = resource.findById("fdaugan");
		findById(userLdap);
	}

	@Test
	public void findBy() {
		final List<UserOrg> users = resource.findAllBy("mail", "marc.martin@sample.com");
		Assert.assertEquals(1, users.size());
		final UserOrg userLdap = users.get(0);
		Assert.assertEquals("mmartin", userLdap.getName());
		Assert.assertEquals("3890", userLdap.getDepartment());
		Assert.assertEquals("8234", userLdap.getLocalId());
	}

	private void findById(final UserOrg userLdap) {
		Assert.assertNotNull(userLdap);
		Assert.assertEquals("fdaugan", userLdap.getId());
		Assert.assertEquals("Fabrice", userLdap.getFirstName());
		Assert.assertEquals("Daugan", userLdap.getLastName());
		Assert.assertEquals("gfi", userLdap.getCompany());
		Assert.assertEquals("fabrice.daugan@sample.com", userLdap.getMails().get(0));
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("Hub Paris", userLdap.getGroups().iterator().next());
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdNotExists() {
		resource.findById("any");
	}

	@Test(expected = ValidationJsonException.class)
	public void findByIdNotManagedUser() {
		initSpringSecurityContext("any");
		resource.findById("fdaugan");
	}

	/**
	 * Show users inside the company "ing" (or sub company), and members of group "dig rha", and matching to criteria
	 * "iRsT"
	 */
	@Test
	public void findAllAllFiltersAllRights() {

		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", "dig rha", "iRsT", newUriInfoAsc("id"));
		Assert.assertEquals(2, tableItem.getRecordsTotal());
		Assert.assertEquals(2, tableItem.getRecordsFiltered());

		// Check the users
		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("fdoe2", userLdap.getId());
		Assert.assertEquals("jdoe5", tableItem.getData().get(1).getId());

		// Check the other attributes
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("First2", userLdap.getFirstName());
		Assert.assertEquals("Doe2", userLdap.getLastName());
		Assert.assertEquals("first2.doe2@ing.fr", userLdap.getMails().get(0));
		Assert.assertTrue(userLdap.isManaged());
		final List<GroupLdapVo> groups = new ArrayList<>(userLdap.getGroups());
		Assert.assertEquals(2, groups.size());
		Assert.assertEquals("Biz Agency", groups.get(0).getName());
		Assert.assertEquals("DIG RHA", groups.get(1).getName());
	}

	@Test
	public void findAllAllFiltersReducesGroupsAscLogin() {
		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", "dig rha", "iRsT", newUriInfoAsc("id"));
		Assert.assertEquals(2, tableItem.getRecordsTotal());
		Assert.assertEquals(2, tableItem.getRecordsFiltered());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.getData().get(0).getId());
		Assert.assertEquals("jdoe5", tableItem.getData().get(1).getId());

		// Check the other attributes
		Assert.assertEquals("ing", tableItem.getData().get(0).getCompany());
		Assert.assertEquals("First2", tableItem.getData().get(0).getFirstName());
		Assert.assertEquals("Doe2", tableItem.getData().get(0).getLastName());
		Assert.assertEquals("first2.doe2@ing.fr", tableItem.getData().get(0).getMails().get(0));
		final List<GroupLdapVo> groups = new ArrayList<>(tableItem.getData().get(0).getGroups());
		Assert.assertEquals(2, groups.size());
		Assert.assertEquals("Biz Agency", groups.get(0).getName());
		Assert.assertEquals("DIG RHA", groups.get(1).getName());
	}

	@Test
	public void findAllNotSecure() {
		initSpringSecurityContext("fdaugan");
		final List<UserOrg> tableItem = resource.findAllNotSecure("ing", "dig rha");
		Assert.assertEquals(4, tableItem.size());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.get(0).getId());
		Assert.assertEquals("jdoe4", tableItem.get(1).getId());
		Assert.assertEquals("jdoe5", tableItem.get(2).getId());

		// Check the other attributes
		Assert.assertEquals("ing", tableItem.get(0).getCompany());
		Assert.assertEquals("First2", tableItem.get(0).getFirstName());
		Assert.assertEquals("Doe2", tableItem.get(0).getLastName());
		Assert.assertEquals("first2.doe2@ing.fr", tableItem.get(0).getMails().get(0));
		Assert.assertEquals(2, tableItem.get(0).getGroups().size());
		Assert.assertTrue(tableItem.get(0).getGroups().contains("biz agency"));
		Assert.assertTrue(tableItem.get(0).getGroups().contains("dig rha"));
	}

	@Test
	public void findAllDefaultDescFirstName() {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "5");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORTED_COLUMN, "2");
		uriInfo.getQueryParameters().add(DataTableAttributes.START, "6");
		uriInfo.getQueryParameters().add("columns[2][data]", "firstName");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORT_DIRECTION, "desc");

		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "e", uriInfo);
		Assert.assertEquals(13, tableItem.getRecordsTotal());
		Assert.assertEquals(13, tableItem.getRecordsFiltered());
		Assert.assertEquals(5, tableItem.getData().size());

		// Check the users

		// My company
		// [SimpleUser(id=jdoe4), SimpleUser(id=hdurant), SimpleUser(id=fdoe2), SimpleUser(id=fdauganb)]
		Assert.assertEquals("jdoe4", tableItem.getData().get(0).getId());
		Assert.assertEquals("hdurant", tableItem.getData().get(1).getId());
		Assert.assertEquals("fdoe2", tableItem.getData().get(3).getId());

		// Not my company, brought by delegation
		Assert.assertEquals("jdoe5", tableItem.getData().get(2).getId()); //
	}

	@Test
	public void findAllDefaultDescMail() {
		final UriInfo uriInfo = newUriInfo();
		uriInfo.getQueryParameters().add(DataTableAttributes.PAGE_LENGTH, "5");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORTED_COLUMN, "2");
		uriInfo.getQueryParameters().add(DataTableAttributes.START, "2");
		uriInfo.getQueryParameters().add("columns[2][data]", "mail");
		uriInfo.getQueryParameters().add(DataTableAttributes.SORT_DIRECTION, "desc");

		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "@sample.com", uriInfo);
		Assert.assertEquals(6, tableItem.getRecordsTotal());
		Assert.assertEquals(6, tableItem.getRecordsFiltered());
		Assert.assertEquals(5, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("fdaugan", tableItem.getData().get(1).getId());
	}

	/**
	 * One delegation to members of group "gfi-gstack" to see the company "ing"
	 */
	@Test
	public void findAllUsingDelegateReceiverGroup() {
		initSpringSecurityContext("alongchu");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, null, newUriInfoAsc("id"));

		// Counts : 8 from ing, + 7 from the same company
		Assert.assertEquals(15, tableItem.getRecordsTotal());
		Assert.assertEquals(15, tableItem.getRecordsFiltered());
		Assert.assertEquals(15, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("alongchu", tableItem.getData().get(0).getId());
		Assert.assertFalse(tableItem.getData().get(0).isManaged());

		// Check the groups
		Assert.assertEquals(1, tableItem.getData().get(0).getGroups().size());
		Assert.assertEquals("gfi-gStack", tableItem.getData().get(0).getGroups().get(0).getName());
	}

	/**
	 * No delegation for any group, but only for a company. So see only users within these company : ing(5) + socygan(1)
	 */
	@Test
	public void findAllForMyCompany() {
		initSpringSecurityContext("assist");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, null, newUriInfoAsc("id"));
		Assert.assertEquals(9, tableItem.getRecordsTotal());
		Assert.assertEquals(9, tableItem.getRecordsFiltered());
		Assert.assertEquals(9, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.getData().get(0).getId());
		Assert.assertTrue(tableItem.getData().get(0).isManaged());

		// Check the groups
		Assert.assertEquals(0, tableItem.getData().get(0).getGroups().size());
	}

	/**
	 * No delegation for any group, but only for a company. So see only users within this company : ing(5)
	 */
	@Test
	public void findAllForMyCompanyFilter() {
		initSpringSecurityContext("assist");

		final TableItem<UserOrgVo> tableItem = resource.findAll("ing", null, null, newUriInfoAsc("id"));
		Assert.assertEquals(8, tableItem.getRecordsTotal());
		Assert.assertEquals(8, tableItem.getRecordsFiltered());
		Assert.assertEquals(8, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.getData().get(0).getId());
		Assert.assertTrue(tableItem.getData().get(0).isManaged());

		// Check the groups
		Assert.assertEquals(0, tableItem.getData().get(0).getGroups().size());
	}

	/**
	 * Whatever the managed company, if the current user sees a group, can search any user even in a different company
	 * this user can manage. <br>
	 */
	@Test
	public void findAllForMyGroup() {
		initSpringSecurityContext("mmartin");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, "dig as", null, newUriInfoAsc("id"));

		// 4 users from delegate and 1 from my company
		Assert.assertEquals(5, tableItem.getRecordsTotal());
		Assert.assertEquals(5, tableItem.getRecordsFiltered());
		Assert.assertEquals(5, tableItem.getData().size());

		// Check the users (from delegate)
		Assert.assertEquals("fdoe2", tableItem.getData().get(0).getId());
		Assert.assertTrue(tableItem.getData().get(0).isManaged());
	}

	/**
	 * Whatever the managed company, if the current user sees a group, then he can search any user even in a different
	 * company
	 * this user can manage. <br>
	 */
	@Test
	public void findAllForMySubGroup() {
		initSpringSecurityContext("mmartin");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, "biz agency", "fdoe2", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.getData().get(0).getId());
		Assert.assertTrue(tableItem.getData().get(0).isManaged());

		// Check the groups
		// "Biz Agency" is visible since "mmartin" is in the parent group "
		Assert.assertEquals(2, tableItem.getData().get(0).getGroups().size());
		Assert.assertEquals("Biz Agency", tableItem.getData().get(0).getGroups().get(0).getName());
		Assert.assertTrue(tableItem.getData().get(0).getGroups().get(0).isManaged());
		Assert.assertEquals("DIG RHA", tableItem.getData().get(0).getGroups().get(1).getName());
		Assert.assertFalse(tableItem.getData().get(0).getGroups().get(1).isManaged());
	}

	@Test
	public void findAllFullAscCompany() {
		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, null, newUriInfoAsc("company"));

		// 8 from delegate, 7 from my company
		Assert.assertEquals(15, tableItem.getRecordsTotal());
		Assert.assertEquals(15, tableItem.getRecordsFiltered());
		Assert.assertEquals(15, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.getData().get(7).getId());
	}

	@Test
	public void findAllFullDescCompany() {
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, null, newUriInfoDesc("company"));
		Assert.assertEquals(16, tableItem.getRecordsTotal());
		Assert.assertEquals(16, tableItem.getRecordsFiltered());
		Assert.assertEquals(16, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("flast0", tableItem.getData().get(0).getId());
		Assert.assertEquals("socygan", tableItem.getData().get(0).getCompany());
		Assert.assertEquals("fdaugan", tableItem.getData().get(14).getId());
		Assert.assertEquals("gfi", tableItem.getData().get(14).getCompany());
	}

	@Test
	public void findAllFullAscLastName() {
		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, null, newUriInfoAsc("lastName"));

		// 8 from delegate, 7 from my company
		Assert.assertEquals(15, tableItem.getRecordsTotal());
		Assert.assertEquals(15, tableItem.getRecordsFiltered());
		Assert.assertEquals(15, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.getData().get(3).getId());
	}

	@Test
	public void findAllMemberDifferentCase() {
		final TableItem<UserOrgVo> tableItem = resource.findAll("GfI", "ProductioN", "mmarTIN", newUriInfoAsc("lastName"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("mmartin", tableItem.getData().get(0).getId());
	}

	/**
	 * No available delegate for the current user -> 0
	 */
	@Test
	public void findAllNoRight() {
		initSpringSecurityContext("any");

		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, null, newUriInfoAsc("id"));
		Assert.assertEquals(0, tableItem.getRecordsTotal());
		Assert.assertEquals(0, tableItem.getRecordsFiltered());
		Assert.assertEquals(0, tableItem.getData().size());
	}

	/**
	 * Whatever the managed company, if the current user sees a group, can search any user even in a different company
	 * this user can manage. <br>
	 */
	@Test
	public void findAllNoWrite() {
		initSpringSecurityContext("mlavoine");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "fdoe2", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		// Check the users
		Assert.assertEquals("fdoe2", tableItem.getData().get(0).getId());
		Assert.assertFalse(tableItem.getData().get(0).isManaged());

		// Check the groups
		Assert.assertEquals(1, tableItem.getData().get(0).getGroups().size());
		Assert.assertEquals("Biz Agency", tableItem.getData().get(0).getGroups().get(0).getName());
		Assert.assertFalse(tableItem.getData().get(0).getGroups().get(0).isManaged());
	}

	@Test
	public void findAllNotExistingGroup() {
		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, "any", null, newUriInfoAsc("id"));
		Assert.assertEquals(0, tableItem.getRecordsTotal());
		Assert.assertEquals(0, tableItem.getRecordsFiltered());
		Assert.assertEquals(0, tableItem.getData().size());
	}

	@Test
	public void zcreateUser() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flasta");
		user.setFirstName("FirstA ");
		user.setLastName(" LASTA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rHA");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.create(user);

		// Check the result, using the cache
		checkResult(resource.findAll(null, null, "flasta", newUriInfoAsc("id")));

		// Check the result, using a fresh new cache
		CacheManager.getInstance().getCache("ldap").removeAll();
		checkResult(resource.findAll(null, null, "flasta", newUriInfoAsc("id")));

		// Restore the state, delete this new user
		resource.delete("flasta");
	}

	@Test
	public void createUserAlreadyExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", "already-exist"));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast1");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flast12@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.create(user);
	}

	@Test
	public void zcreateUserDelegateCompanyNotExist() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flastc");
		user.setFirstName("FirstC");
		user.setLastName("LastC");
		user.setCompany("any");
		user.setMail("flastc@ing.com");
		initSpringSecurityContext("fdaugan");
		resource.create(user);
	}

	@Test
	public void zcreateUserNoDelegate() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flastd");
		user.setFirstName("FirstD");
		user.setLastName("LastD");
		user.setCompany("ing");
		user.setMail("flastd@ing.com");
		initSpringSecurityContext("any");

		resource.create(user);
	}

	@Test
	public void zcreateUserNoDelegateCompany() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flastc");
		user.setFirstName("FirstC");
		user.setLastName("LastC");
		user.setCompany("socygan");
		user.setMail("flastc@ing.com");
		initSpringSecurityContext("fdaugan");
		resource.create(user);
	}

	@Test
	public void zcreateUserNoDelegateGroup() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("groups", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flastg");
		user.setFirstName("FirstG");
		user.setLastName("LastG");
		user.setCompany("ing");
		user.setMail("flastg@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig sud ouest");
		user.setGroups(groups);
		initSpringSecurityContext("someone");
		resource.create(user);
	}

	@Test
	public void deleteUserNoDelegateCompany() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", BusinessException.KEY_UNKNOW_ID));
		initSpringSecurityContext("mmartin");
		resource.delete("flast1");
	}

	@Test
	public void deleteLastMember() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", "last-member-of-group"));
		resource.delete("mmartin");
	}

	@Test
	public void deleteUserNoDelegateWriteCompany() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", BusinessException.KEY_UNKNOW_ID));
		initSpringSecurityContext("mtuyer");
		resource.delete("flast1");
	}

	@Test
	public void mergeUserNoChange() {
		final UserOrg userLdap2 = getUser().findById("flast1");
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());

		resource.mergeUser(userLdap2, new UserOrg());
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());
	}

	@Test
	public void mergeUser() {
		final UserOrg userLdap2 = getUser().findById("flast1");
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());

		final UserOrg newUser = new UserOrg();
		newUser.setDepartment("any");
		newUser.setLocalId("some");
		resource.mergeUser(userLdap2, newUser);
		Assert.assertEquals("any", userLdap2.getDepartment());
		Assert.assertEquals("some", userLdap2.getLocalId());

		// Revert to previous state (null)
		resource.mergeUser(userLdap2, new UserOrg());
		Assert.assertNull(userLdap2.getDepartment());
		Assert.assertNull(userLdap2.getLocalId());
	}

	/**
	 * Update everything : attributes and mails
	 */
	@Test
	public void update() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast1");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "flast1", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("flast1", userLdap.getId());
		Assert.assertEquals("Firsta", userLdap.getFirstName());
		Assert.assertEquals("Lasta", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("flasta@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("DIG RHA", userLdap.getGroups().get(0).getName());

		// Rollback attributes
		user.setId("flast1");
		user.setFirstName("First1");
		user.setLastName("Last1");
		user.setCompany("ing");
		user.setMail("first1.last1@ing.fr");
		user.setGroups(null);
		resource.update(user);
	}

	@Test
	public void updateFirstName() {
		// First name change only
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jlast3");
		user.setFirstName("John31");
		user.setLastName("Last3");
		user.setCompany("ing");
		user.setMail("john3.last3@ing.com");
		user.setGroups(null);
		initSpringSecurityContext("assist");
		resource.update(user);
		TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jlast3", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("jlast3", userLdap.getId());
		Assert.assertEquals("John31", userLdap.getFirstName());
		Assert.assertEquals("Last3", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("john3.last3@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(0, userLdap.getGroups().size());
		rollbackUser();
	}

	@Test
	public void updateLastName() {
		// Last name change only
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jlast3");
		user.setFirstName("John31");
		user.setLastName("Last31");
		user.setCompany("ing");
		user.setMail("john3.last3@ing.com");
		user.setGroups(null);
		user.setGroups(null);
		resource.update(user);
		TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jlast3", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("jlast3", userLdap.getId());
		Assert.assertEquals("John31", userLdap.getFirstName());
		Assert.assertEquals("Last31", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("john3.last3@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(0, userLdap.getGroups().size());
		rollbackUser();
	}

	@Test
	public void updateMail() {
		// Mail change only
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jlast3");
		user.setFirstName("John31");
		user.setLastName("Last31");
		user.setCompany("ing");
		user.setMail("john31.last31@ing.com");
		user.setGroups(null);
		resource.update(user);
		TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jlast3", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		UserOrgVo userLdap = tableItem.getData().get(0);
		user.setGroups(null);
		Assert.assertEquals("jlast3", userLdap.getId());
		Assert.assertEquals("John31", userLdap.getFirstName());
		Assert.assertEquals("Last31", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("john31.last31@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(0, userLdap.getGroups().size());
		rollbackUser();
	}

	private void rollbackUser() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jlast3");
		user.setFirstName("John3");
		user.setLastName("Last3");
		user.setCompany("ing");
		user.setMail("john3.last3@ing.com");
		user.setGroups(null);
		initSpringSecurityContext("assist");
		resource.update(user);
		TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jlast3", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("jlast3", userLdap.getId());
		Assert.assertEquals("John3", userLdap.getFirstName());
		Assert.assertEquals("Last3", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("john3.last3@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(0, userLdap.getGroups().size());
	}

	@Test
	public void updateUserChangeCompanyAndBackAgain() {

		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("ing"); // Previous is "socygan"
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test(expected = ValidationJsonException.class)
	public void updateUserChangeDepartmentNotVisible() {
		initSpringSecurityContext("assist");

		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("socygan"); // Unchanged
		user.setDepartment("456987"); // Previous is null -> "DIG AS" (not visible)
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		resource.update(user);
	}

	@Test
	public void updateUserChangeDepartmentAndBackAgain() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("socygan"); // Unchanged
		user.setDepartment("456987"); // Previous is null -> "DIG AS"
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		resource.update(user);
	}

	@Test
	public void updateUserChangeDepartmentNotExists() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("First0"); // Unchanged
		user.setLastName("Last0"); // Unchanged
		user.setCompany("socygan"); // Unchanged
		user.setDepartment("any"); // Previous is null -> No linked group exists
		user.setMail("first0.last0@socygan.fr"); // Unchanged
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test
	public void updateUserCompanyNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("any");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserGroupNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("groups", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast1");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("any");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoChange() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jlast3");
		user.setFirstName("John3");
		user.setLastName("Last3");
		user.setCompany("ing");
		user.setMail("jlast3@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jlast3", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("jlast3", userLdap.getId());
		Assert.assertEquals("John3", userLdap.getFirstName());
		Assert.assertEquals("Last3", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("jlast3@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("DIG RHA", userLdap.getGroups().get(0).getName());
	}

	@Test
	public void updateUserNoDelegate() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast1");
		user.setFirstName("FirstW");
		user.setLastName("LastW");
		user.setCompany("ing");
		user.setMail("flastw@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("any");

		resource.update(user);
	}

	@Test
	public void updateUserReadOnly() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("groups", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("First0");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.last0@socygan.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("Biz Agency");
		user.setGroups(groups);
		initSpringSecurityContext("mlavoine");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompany() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("socygan");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("any");

		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompanyChangeFirstName() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("FirstA");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.last0@socygan.fr");
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompanyChangeMail() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("company", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("First0");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.lastA@socygan.fr");
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateCompanyNoChange() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast0");
		user.setFirstName("First0");
		user.setLastName("Last0");
		user.setCompany("socygan");
		user.setMail("first0.last0@socygan.fr");
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test
	public void updateUserNoDelegateGroupForTarget() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("groups", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast1");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("dig sud ouest"); // no right on this group
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
	}

	@Test
	public void zupdateUserHadNoMail() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jdoe5");
		user.setFirstName("John5");
		user.setLastName("Doe5");
		user.setCompany("ing");
		user.setMail("first5.last5@ing.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jdoe5", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("jdoe5", userLdap.getId());
		Assert.assertEquals("John5", userLdap.getFirstName());
		Assert.assertEquals("Doe5", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("first5.last5@ing.fr", userLdap.getMails().get(0));
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("DIG RHA", userLdap.getGroups().get(0).getName());
	}

	@Test
	public void zupdateUserHasNoMail() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jdoe5");
		user.setFirstName("John5");
		user.setLastName("Doe5");
		user.setCompany("ing");
		user.setMail(null);
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jdoe5", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("jdoe5", userLdap.getId());
		Assert.assertEquals("John5", userLdap.getFirstName());
		Assert.assertEquals("Doe5", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertTrue(userLdap.getMails().isEmpty());
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("DIG RHA", userLdap.getGroups().get(0).getName());
	}

	@Test
	public void zupdateUserNoPassword() {
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("jdoe4");
		user.setFirstName("John4");
		user.setLastName("Doe4");
		user.setCompany("ing");
		user.setMail("fohn4.doe4@ing.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("dig rha");
		user.setGroups(groups);
		initSpringSecurityContext("fdaugan");
		resource.update(user);
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "jdoe4", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("jdoe4", userLdap.getId());
		Assert.assertEquals("John4", userLdap.getFirstName());
		Assert.assertEquals("Doe4", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("fohn4.doe4@ing.fr", userLdap.getMails().get(0));
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("DIG RHA", userLdap.getGroups().get(0).getName());
	}

	@Test
	public void updateUserNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", BusinessException.KEY_UNKNOW_ID));
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("flast11");
		user.setFirstName("FirstA");
		user.setLastName("LastA");
		user.setCompany("ing");
		user.setMail("flasta@ing.com");
		final List<String> groups = new ArrayList<>();
		user.setGroups(groups);
		initSpringSecurityContext("assist");
		resource.update(user);
	}

	@Test
	public void zupdateUserRemoveGroup() {
		// Pre-condition
		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> initialResult = resource.findAll(null, null, "fdoe2", newUriInfoAsc("id"));
		Assert.assertEquals(1, initialResult.getData().size());
		Assert.assertEquals(2, initialResult.getData().get(0).getGroups().size());
		Assert.assertEquals("Biz Agency", initialResult.getData().get(0).getGroups().get(0).getName());
		Assert.assertTrue(initialResult.getData().get(0).getGroups().get(0).isManaged());
		Assert.assertEquals("DIG RHA", initialResult.getData().get(0).getGroups().get(1).getName());
		Assert.assertTrue(initialResult.getData().get(0).getGroups().get(1).isManaged());

		// Remove group "Biz Agency"
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("fdoe2");
		user.setFirstName("First2");
		user.setLastName("Doe2");
		user.setCompany("ing");
		user.setMail("fdoe2@ing.com");
		final List<String> groups = new ArrayList<>();
		groups.add("DIG RHA");
		user.setGroups(groups);
		resource.update(user);
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "fdoe2", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());

		final UserOrgVo userLdap = tableItem.getData().get(0);
		Assert.assertEquals("fdoe2", userLdap.getId());
		Assert.assertEquals("First2", userLdap.getFirstName());
		Assert.assertEquals("Doe2", userLdap.getLastName());
		Assert.assertEquals("ing", userLdap.getCompany());
		Assert.assertEquals("fdoe2@ing.com", userLdap.getMails().get(0));
		Assert.assertEquals(1, userLdap.getGroups().size());
		Assert.assertEquals("DIG RHA", userLdap.getGroups().get(0).getName());

		// Remove all groups
		user.setGroups(null);
		resource.update(user);
		final TableItem<UserOrgVo> tableItemNoGroup = resource.findAll(null, null, "fdoe2", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItemNoGroup.getData().size());
		Assert.assertEquals(0, tableItemNoGroup.getData().get(0).getGroups().size());

	}

	/**
	 * Add a group to user having already some groups but not visible from the current user.
	 */
	@Test
	public void updateUserAddGroup() {
		// Pre condition, check the user "wuser", has not yet the group "DIG RHA" we want to be added by "fdaugan"
		initSpringSecurityContext("fdaugan");
		final TableItem<UserOrgVo> initialResultsFromUpdater = resource.findAll(null, null, "wuser", newUriInfoAsc("id"));
		Assert.assertEquals(1, initialResultsFromUpdater.getRecordsTotal());
		Assert.assertEquals(1, initialResultsFromUpdater.getData().get(0).getGroups().size());
		Assert.assertEquals("Biz Agency Manager", initialResultsFromUpdater.getData().get(0).getGroups().get(0).getName());

		// Pre condition, check the user "wuser", has no group visible by "assist"
		initSpringSecurityContext("assist");
		final TableItem<UserOrgVo> assisteResult = resource.findAll(null, null, "wuser", newUriInfoAsc("id"));
		Assert.assertEquals(1, assisteResult.getRecordsTotal());
		Assert.assertEquals(0, assisteResult.getData().get(0).getGroups().size());

		// Pre condition, check the user "wuser", "Biz Agency Manager" is not visible by "mtuyer"
		initSpringSecurityContext("mtuyer");
		final TableItem<UserOrgVo> usersFromOtherGroupManager = resource.findAll(null, null, "wuser", newUriInfoAsc("id"));
		Assert.assertEquals(1, usersFromOtherGroupManager.getRecordsTotal());
		Assert.assertEquals(0, usersFromOtherGroupManager.getData().get(0).getGroups().size());

		// Add a new valid group "DIG RHA" to "wuser" by "fdaugan"
		initSpringSecurityContext("fdaugan");
		final UserLdapEdition user = new UserLdapEdition();
		user.setId("wuser");
		user.setFirstName("William");
		user.setLastName("User");
		user.setCompany("ing");
		user.setMail("wuser.wuser@ing.fr");
		final List<String> groups = new ArrayList<>();
		groups.add("DIG RHA");
		groups.add("Biz Agency Manager");
		user.setGroups(groups);
		resource.update(user);

		// Check the group "DIG RHA" is added and
		final TableItem<UserOrgVo> tableItem = resource.findAll(null, null, "wuser", newUriInfoAsc("id"));
		Assert.assertEquals(1, tableItem.getRecordsTotal());
		Assert.assertEquals(1, tableItem.getRecordsFiltered());
		Assert.assertEquals(1, tableItem.getData().size());
		Assert.assertEquals(2, tableItem.getData().get(0).getGroups().size());
		Assert.assertEquals("Biz Agency Manager", tableItem.getData().get(0).getGroups().get(0).getName());
		Assert.assertEquals("DIG RHA", tableItem.getData().get(0).getGroups().get(1).getName());

		// Check the user "wuser", still has no group visible by "assist"
		initSpringSecurityContext("assist");
		final TableItem<UserOrgVo> assisteResult2 = resource.findAll(null, null, "wuser", newUriInfoAsc("id"));
		Assert.assertEquals(1, assisteResult2.getRecordsTotal());
		Assert.assertEquals(0, assisteResult2.getData().get(0).getGroups().size());

		// Check the user "wuser", still has the group "DIG RHA" visible by "mtuyer"
		initSpringSecurityContext("mtuyer");
		final TableItem<UserOrgVo> usersFromOtherGroupManager2 = resource.findAll(null, null, "wuser", newUriInfoAsc("id"));
		Assert.assertEquals(1, usersFromOtherGroupManager2.getRecordsTotal());
		Assert.assertEquals("DIG RHA", usersFromOtherGroupManager2.getData().get(0).getGroups().get(0).getName());

		// Restore the old state
		initSpringSecurityContext("fdaugan");
		final UserLdapEdition user2 = new UserLdapEdition();
		user2.setId("wuser");
		user2.setFirstName("William");
		user2.setLastName("User");
		user2.setCompany("ing");
		user2.setMail("wuser.wuser@ing.fr");
		final List<String> groups2 = new ArrayList<>();
		groups2.add("Biz Agency Manager");
		user.setGroups(groups2);
		resource.update(user);
		final TableItem<UserOrgVo> initialResultsFromUpdater2 = resource.findAll(null, null, "wuser", newUriInfoAsc("id"));
		Assert.assertEquals(1, initialResultsFromUpdater2.getRecordsTotal());
		Assert.assertEquals(1, initialResultsFromUpdater2.getData().get(0).getGroups().size());
		Assert.assertEquals("Biz Agency Manager", initialResultsFromUpdater2.getData().get(0).getGroups().get(0).getName());
	}

	@Test
	public void zlockUnlockUser() {
		resource.lock("aLongchu");
		resource.lock("aLongchu");
		resource.unlock("aLongchu");
		resource.unlock("aLongchu");
	}

	@Test
	public void zisolateRestoreUser() {
		resource.isolate("aLongchu");
		resource.isolate("aLongchu");
		resource.lock("aLongchu");
		resource.unlock("aLongchu");
	}

	@Test
	public void zzdeleteUser() {
		initSpringSecurityContext("assist");
		Assert.assertEquals(1, resource.findAll("ing", null, "jdoe5", newUriInfo()).getData().size());
		Assert.assertNotNull(getUser().findByIdNoCache("jdoE5"));
		Assert.assertTrue(getGroup().findAll().get("dig rha").getMembers().contains("jdoe5"));
		resource.delete("jDOE5");
	}

	/**
	 * Test user addition to a group this user is already member.
	 */
	@Test
	public void addUserToGroup() {
		// Pre condition
		Assert.assertTrue(resource.findById("wuser").getGroups().contains("Biz Agency Manager"));

		resource.addUserToGroup("wuser", "biz agency manager");

		// Post condition -> no change
		Assert.assertTrue(resource.findById("wuser").getGroups().contains("Biz Agency Manager"));
	}

	/**
	 * Test user addition to a group.
	 */
	@Test
	public void zaddRemoveUser() {
		// Pre condition
		Assert.assertFalse(resource.findById("wuser").getGroups().contains("DIG RHA"));
		Assert.assertFalse(getGroup().findById("dig rha").getMembers().contains("wuser"));

		resource.addUserToGroup("wuser", "dig rha");

		// Post condition
		Assert.assertTrue(resource.findById("wuser").getGroups().contains("DIG RHA"));
		Assert.assertTrue(getGroup().findById("dig rha").getMembers().contains("wuser"));

		resource.removeUser("wuser", "dig rha");

		// Post condition 2
		Assert.assertFalse(resource.findById("wuser").getGroups().contains("DIG RHA"));
		Assert.assertFalse(getGroup().findById("dig rha").getMembers().contains("wuser"));
	}

	@Test
	public void deleteUserLastInGroupCase() {
		thrown.expect(ValidationJsonException.class);
		initSpringSecurityContext("mmartin");
		Assert.assertEquals(1, resource.findAll(null, null, "wuser", newUriInfo()).getData().size());

		Assert.assertNotNull(getUser().findByIdNoCache("wuser"));
		Assert.assertTrue(getGroup().findAll().get("biz agency manager").getMembers().contains("wuser"));

		resource.delete("wuser");
	}

	@Test
	public void deleteUserNotExists() {
		thrown.expect(ValidationJsonException.class);
		thrown.expect(MatcherUtil.validationMatcher("id", BusinessException.KEY_UNKNOW_ID));
		initSpringSecurityContext("assist");
		resource.delete("any");
	}

	@Test
	public void isGrantedNotSameType() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assert.assertFalse(resource.isGrantedAccess(delegate, null, DelegateType.COMPANY, true));
	}

	@Test
	public void isGrantedSameTypeNoRight() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assert.assertFalse(resource.isGrantedAccess(delegate, null, DelegateType.GROUP, true));
	}

	@Test
	public void isGrantedSameTypeNotSameDn() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		Assert.assertFalse(resource.isGrantedAccess(delegate, null, DelegateType.GROUP, false));
	}

	@Test
	public void isGranted() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setDn("rightdn");
		Assert.assertTrue(resource.isGrantedAccess(delegate, "rightdn", DelegateType.GROUP, false));
	}

	@Test
	public void isGrantedAsAdmin() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setCanAdmin(true);
		delegate.setDn("rightdn");
		Assert.assertTrue(resource.isGrantedAccess(delegate, "rightdn", DelegateType.GROUP, true));
	}

	@Test
	public void isGrantedAsWriter() {
		final DelegateOrg delegate = new DelegateOrg();
		delegate.setType(DelegateType.GROUP);
		delegate.setCanWrite(true);
		delegate.setDn("rightdn");
		Assert.assertTrue(resource.isGrantedAccess(delegate, "rightdn", DelegateType.GROUP, true));
	}

	@Test
	public void convertUserRaw() {
		final UserOrg userLdap = getUser().toUser("jdoe5");
		checkRawUser(userLdap);
		Assert.assertNotNull(userLdap.getGroups());
		Assert.assertEquals(1, userLdap.getGroups().size());
	}

	@Test
	public void convertUserNotExist() {
		final UserOrg userLdap = getUser().toUser("any");
		Assert.assertNotNull(userLdap);
		Assert.assertEquals("any", userLdap.getId());
		Assert.assertNull(userLdap.getCompany());
		Assert.assertNull(userLdap.getGroups());
		Assert.assertNull(userLdap.getFirstName());
		Assert.assertNull(userLdap.getLastName());
		Assert.assertNull(userLdap.getMails());
	}

	private void checkRawUser(final SimpleUserOrg userLdap) {
		Assert.assertNotNull(userLdap);
		Assert.assertEquals("jdoe5", userLdap.getId());
		Assert.assertEquals("ing-internal", userLdap.getCompany());
		Assert.assertEquals("First5", userLdap.getFirstName());
		Assert.assertEquals("Last5", userLdap.getLastName());
		Assert.assertNotNull(userLdap.getMails());
	}

	/**
	 * Check a user can see all users from the same company
	 */
	@Test
	public void findAllMyCompany() {
		initSpringSecurityContext("mmartin");

		final TableItem<UserOrgVo> tableItem = resource.findAll("gfi", null, null, newUriInfoAsc("id"));

		// 7 users from company 'gfi', 0 from delegate
		Assert.assertEquals(7, tableItem.getRecordsTotal());
		Assert.assertEquals(7, tableItem.getRecordsFiltered());

		// Check the users
		Assert.assertEquals("alongchu", tableItem.getData().get(0).getId());
	}

	/**
	 * When the requested company does not exists, return an empty set.
	 */
	@Test
	public void findAllUnknowFilteredCompany() {
		final TableItem<UserOrgVo> tableItem = resource.findAll("any", null, null, newUriInfoAsc("id"));
		Assert.assertEquals(0, tableItem.getRecordsTotal());
		Assert.assertEquals(0, tableItem.getRecordsFiltered());
	}

	@Test
	public void setIamProviderForTest() {
		// There for test by other plugin/application
		new UserLdapResource().setIamProvider(Mockito.mock(IamProvider.class));
	}
}
