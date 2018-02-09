package org.ligoj.app.plugin.id.resource.batch;

/**
 * Batch action type.
 */
public enum UserBatchUpdateType {

	/**
	 * Isolate the user.
	 */
	ISOLATE,
	
	/**
	 * Restore the user.
	 */
	RESTORE,

	/**
	 * Lock the user.
	 */
	LOCK,

	/**
	 * Change an attribute from the user
	 */
	ATTRIBUTE,

	/**
	 * Delete a user.
	 */
	DELETE
}
