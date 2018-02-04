package org.ligoj.app.plugin.id.resource.batch;

import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.ligoj.app.plugin.id.resource.IdentityResource;
import org.springframework.stereotype.Service;

/**
 * LDAP batch resource for user.
 */
@Path(IdentityResource.SERVICE_URL + "/user/batch")
@Service
@Produces(MediaType.APPLICATION_JSON)
public class UserBatchImportLdapResource extends AbstractBatchResource<UserImportEntry> {

	/**
	 * Default CSV headers for imports.
	 */
	private static final String[] DEFAULT_CSV_HEADERS = { "lastName", "firstName", "id", "mail", "company", "groups", "department",
			"localId" };

	/**
	 * Upload a file of LDAP entries to create or update users. The whole entry is replaced.
	 * 
	 * @param uploadedFile
	 *            LDAP entries files to import. Currently support only CSV format.
	 * @param columns
	 *            the CSV header names.
	 * @param encoding
	 *            CSV encoding. Default is UTF-8.
	 * @return the import identifier.
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("full")
	public long execute(@Multipart(value = "csv-file") final InputStream uploadedFile,
			@Multipart(value = "columns", required = false) final String[] columns,
			@Multipart(value = "encoding", required = false) final String encoding) throws IOException {
		return batch(uploadedFile, columns, encoding, DEFAULT_CSV_HEADERS, UserImportEntry.class, UserFullLdapTask.class);
	}
}
