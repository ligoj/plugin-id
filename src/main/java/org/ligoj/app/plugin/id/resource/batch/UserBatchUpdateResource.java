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
 * Batch resource to update user.
 */
@Path(IdentityResource.SERVICE_URL + "/user/batch")
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
	 *            Entries file to import. Currently support only CSV format.
	 * @param columns
	 *            the CSV header names.
	 * @param encoding
	 *            CSV encoding. Default is UTF-8.
	 * @return the import identifier.
	 */
	@POST
	@Consumes(MediaType.MULTIPART_FORM_DATA)
	@Path("atomic")
	public long execute(@Multipart(value = "csv-file") final InputStream uploadedFile,
			@Multipart(value = "columns", required = false) final String[] columns,
			@Multipart(value = "encoding", required = false) final String encoding) throws IOException {
		return batch(uploadedFile, columns, encoding, DEFAULT_CSV_HEADERS, UserUpdateEntry.class, UserAtomicTask.class);
	}
}
