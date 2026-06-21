package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.ligoj.app.resource.node.NodeResource;
import org.ligoj.bootstrap.core.json.ObjectMapperTrim;
import org.ligoj.bootstrap.core.plugin.FeaturePlugin;
import org.ligoj.bootstrap.model.system.SystemConfiguration;
import org.ligoj.bootstrap.resource.system.configuration.ConfigurationResource;
import org.ligoj.bootstrap.resource.system.session.ISessionSettingsProvider;
import org.ligoj.bootstrap.resource.system.session.SessionSettings;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.core.type.TypeReference;

import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;

/**
 * Session resource.
 */
@Component
@Slf4j
@Transactional
public class ToolSessionSettingsProvider implements ISessionSettingsProvider, FeaturePlugin {

	@Autowired
	private ConfigurationResource configuration;

	@Autowired
	protected CompanyResource companyResource;

	@Autowired
	private ObjectMapperTrim objectMapper;

	@Autowired
	protected NodeResource nodeResource;

	private static final TypeReference<List<Map<String, Object>>> LIST_MAP_TYPE = new TypeReference<>() {
		// Nothing to do
	};

	@Override
	public void decorate(final SessionSettings settings) {
		final var userSetting = settings.getUserSettings();

		// Add the related one to the type of user
		final String source;
		if (companyResource.isUserInternalCompany()) {
			// Internal user
			userSetting.put("internal", Boolean.TRUE);
			source = configuration.get("global.tools.internal");
		} else {
			// External user
			userSetting.put("external", Boolean.TRUE);
			source = configuration.get("global.tools.external");
		}

		// Fetch the required node data
		try {
			final var rawGlobalTools = objectMapper.readValue(StringUtils.defaultIfEmpty(source, "[]"), LIST_MAP_TYPE);
			// Replace the node identifier by a Node instance
			userSetting.put("globalTools", rawGlobalTools.stream().filter(globalTool -> {
				// When the node does not exist anymore, the configuration is not returned
				globalTool.compute("node", (node, v) -> nodeResource.findAll().get(globalTool.get("id")));
				globalTool.remove("id");
				return globalTool.containsKey("node");
			}).toList());
		} catch (final IOException ioe) {
			log.error("Unable to write the global tools configuration for user {}", settings.getUserName(), ioe);
		}
	}

	@Override
	public String getKey() {
		return "feature:menu:node";
	}

	@Override
	public List<Class<?>> getInstalledEntities() {
		return Collections.singletonList(SystemConfiguration.class);
	}
}
