package org.ligoj.app.plugin.id.resource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

/**
 * Contract of the identity service parameters shipped by this plugin.
 */
class IdentityParametersTest {

	/**
	 * The custom attribute names are a concept of the identity service, not of one tool: declared once on the
	 * service, offered on every identity node, never on subscriptions.
	 */
	@Test
	void peopleCustomAttributesIsAServiceParameter() throws IOException {
		Assertions.assertEquals("service:id:people-custom-attributes", IdentityResource.PARAMETER_PEOPLE_CUSTOM_ATTRIBUTES);
		final var row = csvRow("csv/parameter.csv", IdentityResource.PARAMETER_PEOPLE_CUSTOM_ATTRIBUTES);
		Assertions.assertEquals("service:id", row.get("owner.id"));
		Assertions.assertEquals("TEXT", row.get("type"));
		Assertions.assertEquals("FALSE", row.get("mandatory"));
		Assertions.assertEquals("FALSE", row.get("secured"));
		Assertions.assertEquals("TRUE", row.get("availableForNode"));
		Assertions.assertEquals("FALSE", row.get("availableForSubscription"));
	}

	private Map<String, String> csvRow(final String resource, final String id) throws IOException {
		final var lines = IOUtils.readLines(getClass().getClassLoader().getResourceAsStream(resource), StandardCharsets.UTF_8);
		final var headers = Arrays.asList(lines.getFirst().split(";", -1));
		final var values = lines.stream().filter(l -> l.startsWith(id + ";")).findFirst().map(l -> Arrays.asList(l.split(";", -1)))
				.orElseThrow(() -> new AssertionError("No row for " + id));
		return headers.stream().filter(h -> headers.indexOf(h) < values.size()).collect(Collectors.toMap(h -> h, h -> values.get(headers.indexOf(h)), (a, b) -> a));
	}

	@Test
	void csvRowHelperReadsExistingRow() throws IOException {
		Assertions.assertEquals(List.of("service:id").getFirst(), csvRow("csv/parameter.csv", "service:id:uid-pattern").get("owner.id"));
	}
}
