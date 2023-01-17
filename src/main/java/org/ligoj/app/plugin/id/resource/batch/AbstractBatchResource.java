/*
 * Licensed under MIT (https://github.com/ligoj/ligoj/blob/master/LICENSE)
 */
package org.ligoj.app.plugin.id.resource.batch;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.SequenceInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.BooleanUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.ligoj.bootstrap.core.SpringUtils;
import org.ligoj.bootstrap.core.csv.CsvForBean;
import org.ligoj.bootstrap.core.resource.OnNullReturn404;
import org.ligoj.bootstrap.core.resource.TechnicalException;
import org.ligoj.bootstrap.core.validation.ValidationJsonException;
import org.ligoj.bootstrap.core.validation.ValidatorBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.task.TaskExecutor;
import org.springframework.security.core.context.SecurityContextHolder;

/**
 * Base batch resource class.
 * @param <B> A batch 
 */
public abstract class AbstractBatchResource<B extends BatchElement> {

	@Autowired
	protected TaskExecutor executor;

	@Autowired
	private CsvForBean csvForBean;

	/**
	 * Hold pending and previous imports. Key is an identifier built from the username requesting the import, and a
	 * random String. This table is clean before each import.
	 */
	private final Map<String, BatchTaskVo<B>> imports = new ConcurrentHashMap<>();

	@Autowired
	private ValidatorBean validator;

	/**
	 * Return the import task from its identifier. The internal identifier is build from the current user and the formal
	 * identifier parameter.
	 * 
	 * @param id
	 *            task's identifier.
	 * @return <code>null</code> or corresponding task.
	 */
	@GET
	@Path("{id:\\d+}")
	@OnNullReturn404
	public BatchTaskVo<B> getImportTask(@PathParam("id") final long id) {
		return imports.get(SecurityContextHolder.getContext().getAuthentication().getName() + "-" + id);
	}

	/**
	 * Return the status of given task
	 * 
	 * @param id
	 *            Identifier of the task.
	 * @return status or <code>null</code> when no task matches.
	 */
	@GET
	@Path("{id:\\d+}/status")
	@OnNullReturn404
	public ImportStatus getImportStatus(@PathParam("id") final long id) {
		return Optional.ofNullable(getImportTask(id)).map(BatchTaskVo::getStatus).orElse(null);
	}

	/**
	 * Cleanup the previous tasks.
	 */
	private void cleanup() {
		for (final var entry : imports.entrySet()) {
			if (isFinished(entry.getValue())) {
				// This task is finished since yesterday
				imports.remove(entry.getKey());
			}
		}
	}

	/**
	 * Is the current task is finished.
	 */
	private boolean isFinished(final BatchTaskVo<?> task) {
		return task.getStatus().getEnd() != null
				&& task.getStatus().getEnd().getTime() + DateUtils.MILLIS_PER_DAY < System.currentTimeMillis();
	}

	/**
	 * Execute a batch operation from the given input.
	 * 
	 * @param uploadedFile
	 *            The CSV input without header
	 * @param columns
	 *            The ordered columns associated to the given input.
	 * @param encoding
	 *            CSV encoding. Default is UTF-8.
	 * @param defaultColumns
	 *            The handled/accepted column for the target entity.
	 * @param batchType
	 *            The target batch entity type.
	 * @param taskType
	 *            The task class running this batch.
	 * @param <T>
	 *            The task type running this batch.
	 * @param quiet
	 *            Optional flag to turn off the possible notification such as mail. Default value is <code>false</code>.
	 * @return the import identifier.
	 * @throws IOException
	 *             When CSV read failed.
	 */
	protected <T extends AbstractBatchTask<B>> long batch(final InputStream uploadedFile, final String[] columns,
			final String encoding, final String[] defaultColumns, final Class<B> batchType, final Class<T> taskType,
			final Boolean quiet) throws IOException {
		try {
			return batchInternal(uploadedFile, columns, encoding, defaultColumns, batchType, taskType, quiet);
		} catch (final TechnicalException io) {
			// Handle technical exception there to associate to csv-file parameter.
			throw new ValidationJsonException("csv-file", io.getMessage());
		}
	}

	protected <T extends AbstractBatchTask<B>> long batchInternal(final InputStream uploadedFile,
			final String[] columns, final String encoding, final String[] defaultColumns, final Class<B> batchType,
			final Class<T> taskType, final Boolean quiet) throws IOException {

		// Public identifier is based on system date
		final var id = System.currentTimeMillis();

		// Check column's name validity
		final var sanitizeColumns = ArrayUtils.isEmpty(columns) ? defaultColumns : columns;
		checkHeaders(defaultColumns, sanitizeColumns);

		// Build CSV header from array
		final var csvHeaders = StringUtils.chop(ArrayUtils.toString(sanitizeColumns)).substring(1).replace(',', ';')
				+ "\n";

		// Build entries with prepended CSV header
		final var encSafe = ObjectUtils.defaultIfNull(encoding, StandardCharsets.UTF_8.name());
		final var input = new ByteArrayInputStream(csvHeaders.getBytes(encSafe));
		final var entries = csvForBean.toBean(batchType,
				new InputStreamReader(new SequenceInputStream(input, uploadedFile), encSafe));
		entries.removeIf(Objects::isNull);

		// Validate them
		validator.validateCheck(entries);

		// Clone the context for the asynchronous import
		final var importTask = new BatchTaskVo<B>();
		importTask.setEntries(entries);
		importTask.setPrincipal(SecurityContextHolder.getContext().getAuthentication().getName());
		importTask.setId(id);
		importTask.setQuiet(BooleanUtils.isTrue(quiet));

		// Schedule the import
		final var task = SpringUtils.getBean(taskType);
		task.configure(importTask);
		executor.execute(task);

		// Also cleanup the previous tasks
		cleanup();

		// Expose the task with internal identifier, based on current user PLUS the public identifier
		imports.put(importTask.getPrincipal() + "-" + importTask.getId(), importTask);

		// Return private task identifier
		return id;
	}

	/**
	 * Check column's name validity
	 */
	private void checkHeaders(final String[] requested, final String... columns) {
		for (final var column : columns) {
			if (!ArrayUtils.contains(requested, column.trim())) {
				throw new ValidationJsonException("csv-file", "Invalid header " + column);
			}
		}
	}
}
