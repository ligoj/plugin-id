/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
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
 * Batch resource to import user.
 */
@Path(IdentityResource.SERVICE_URL + "/user/batch/full")
@Service
@Produces(MediaType.APPLICATION_JSON)
public class UserBatchImportResource extends AbstractBatchResource<UserImportEntry> {

	/**
	 * Default CSV headers for imports.
	 */
	private static final String[] DEFAULT_CSV_HEADERS = { "lastName", "firstName", "id", "mail", "company", "groups",
			"department", "localId" };

	/**
	 * Upload a file of entries to create or update users. The whole entry is replaced.
	 * 
	 * @param uploadedFile
	 *            Entries file to import. Currently support only CSV format.
	 * @param columns
	 *            the CSV header names.
	 * @param encoding
	 *            CSV encoding. Default is UTF-8.
	 * @param quiet
	 *            Optional flag to turn-off the possible notification such as mail. Default value is <code>false</code>.
	 * @return the import identifier.
	 * @throws IOException
	 *             When CSV read failed.
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	public long execute(@Multipart(value = "csv-file") final InputStream uploadedFile,
			@Multipart(value = "columns", required = false) final String[] columns,
			@Multipart(value = "encoding", required = false) final String encoding,
			@Multipart(value = "quiet", required = false) final Boolean quiet) throws IOException {
		return batch(uploadedFile, columns, encoding, DEFAULT_CSV_HEADERS, UserImportEntry.class, UserFullTask.class,
				quiet);
	}
}
