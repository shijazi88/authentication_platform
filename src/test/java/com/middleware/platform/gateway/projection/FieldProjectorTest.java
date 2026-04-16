package com.middleware.platform.gateway.projection;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class FieldProjectorTest {

    private final FieldProjector projector = new FieldProjector();

    @Test
    void hitOnly_returnsEmpty() {
        Map<String, Object> source = nestedSample();
        Map<String, Object> projected = projector.project(source, Set.of());
        assertThat(projected).isEmpty();
    }

    @Test
    void allowedLeafIsKept_othersStripped() {
        Map<String, Object> source = nestedSample();
        Map<String, Object> projected = projector.project(source, Set.of("verification.status"));
        assertThat(projected).containsOnlyKeys("verification");
        Map<String, Object> verification = asMap(projected.get("verification"));
        assertThat(verification).containsOnlyKeys("status");
    }

    @Test
    void allowedSubtreeKeepsEverythingBelow() {
        Map<String, Object> source = nestedSample();
        Map<String, Object> projected = projector.project(source, Set.of("person.demographics"));
        assertThat(projected).containsOnlyKeys("person");
        Map<String, Object> person = asMap(projected.get("person"));
        assertThat(person).containsOnlyKeys("demographics");
        Map<String, Object> demographics = asMap(person.get("demographics"));
        assertThat(demographics).containsKeys("names", "phone");
    }

    @Test
    void listsAreFilteredElementWise() {
        Map<String, Object> source = nestedSample();
        Map<String, Object> projected = projector.project(source, Set.of("person.cards"));
        Map<String, Object> person = asMap(projected.get("person"));
        assertThat(person.get("cards")).asInstanceOf(org.assertj.core.api.InstanceOfAssertFactories.LIST)
                .hasSize(1);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object value) {
        return (Map<String, Object>) value;
    }

    private Map<String, Object> nestedSample() {
        Map<String, Object> root = new LinkedHashMap<>();

        Map<String, Object> verification = new LinkedHashMap<>();
        verification.put("status", "MATCH");
        Map<String, Object> bio = new LinkedHashMap<>();
        bio.put("status", true);
        bio.put("score", 92);
        verification.put("biometric", bio);
        root.put("verification", verification);

        Map<String, Object> person = new LinkedHashMap<>();
        person.put("nationalNumber", "B123");
        Map<String, Object> demographics = new LinkedHashMap<>();
        Map<String, Object> names = new LinkedHashMap<>();
        names.put("first", "Mohammed");
        names.put("last", "Al-Husni");
        demographics.put("names", names);
        demographics.put("phone", "+967700000000");
        person.put("demographics", demographics);

        Map<String, Object> card = new LinkedHashMap<>();
        card.put("documentNumber", "ABC123");
        card.put("type", "ID_CARD");
        person.put("cards", List.of(card));

        root.put("person", person);
        return root;
    }
}
