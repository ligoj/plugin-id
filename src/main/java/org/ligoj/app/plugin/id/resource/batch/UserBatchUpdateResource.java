/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import java.io.IOException;
import java.io.InputStream;

import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

import org.apache.cxf.jaxrs.ext.multipart.Multipart;
import org.ligoj.app.plugin.id.resource.IdentityResource;
import org.springframework.stereotype.Service;

/**
 * Batch resource to update user.
 */
@Path(IdentityResource.SERVICE_URL + "/user/batch/atomic")
@Service
@Produces(MediaType.APPLICATION_JSON)
public class UserBatchUpdateResource extends AbstractBatchResource<UserUpdateEntry> {

	/**
	 * Default CSV headers for actions.
	 */
	private static final String[] DEFAULT_CSV_HEADERS = { "user", "operation", "value" };

	/**
	 * Upload a file of entries to execute atomic operations on existing users.
	 * 
	 * @param uploadedFile
	 *            Entries file to import. Currently, support only CSV format.
	 * @param columns
	 *            the CSV header names.
	 * @param encoding
	 *            CSV encoding. Default is UTF-8.
	 * @param quiet
	 *            Optional flag to turn off the possible notification such as mail. Default value is <code>false</code>.
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
		return batch(uploadedFile, columns, encoding, DEFAULT_CSV_HEADERS, UserUpdateEntry.class, UserAtomicTask.class,
				quiet);
	}
}
