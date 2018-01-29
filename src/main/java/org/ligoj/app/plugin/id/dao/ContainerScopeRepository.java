package org.ligoj.app.plugin.id.dao;

import java.util.List;

import org.ligoj.app.model.ContainerType;
import org.ligoj.app.plugin.id.model.ContainerScope;
import org.ligoj.bootstrap.core.dao.RestRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * {@link ContainerScope} repository.
 */
public interface ContainerScopeRepository extends RestRepository<ContainerScope, Integer> {

	/**
	 * Return all types, ordered by DN.
	 * 
	 * @param type
	 *            The {@link ContainerType} to filter. Required.
	 * @return types of group.
	 */
	@Query("FROM ContainerScope WHERE type = ?1 ORDER BY LENGTH(dn) DESC")
	List<ContainerScope> findAllOrderByDnDesc(ContainerType type);

	/**
	 * Delete an unlocked type.
	 * 
	 * @param id
	 *            identifier of entity to delete.
	 */
	@Modifying
	@Query("DELETE ContainerScope WHERE id = ?1 AND locked = false")
	void delete(int id);

	/**
	 * Return all types with a criteria by {@link ContainerType}.
	 * 
	 * @param type
	 *            The {@link ContainerType} to filter. Required.
	 * @param criteria
	 *            DN or Name to match.
	 * @param page
	 *            The {@link Pageable} context.
	 * @return types of group.
	 */
	@Query("SELECT g FROM ContainerScope g WHERE type=:type AND UPPER(g.name) LIKE UPPER(CONCAT(CONCAT('%',:criteria),'%'))")
	Page<ContainerScope> findAll(@Param("type") ContainerType type, @Param("criteria") String criteria, Pageable page);
}
