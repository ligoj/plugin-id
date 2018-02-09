package org.ligoj.app.plugin.id.resource.batch;

import java.util.List;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

/**
 * Task import bean.
 */
@Getter
@ToString(of = "id")
public class BatchTaskVo<B extends BatchElement> {

	/**
	 * Transaction identifier.
	 */
	@Setter
	private long id;

	/**
	 * Entries to persist.
	 */
	@Setter
	private List<B> entries;

	/**
	 * Import status.
	 */
	private ImportStatus status = new ImportStatus();

	/**
	 * User principal requesting the import.
	 */
	@Setter
	private String principal;

}
