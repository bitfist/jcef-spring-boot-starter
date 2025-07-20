package io.github.bitfist.jcef.spring;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledIfEnvironmentVariable;
import org.springframework.modulith.Modulith;
import org.springframework.modulith.core.ApplicationModules;
import org.springframework.modulith.docs.Documenter;

@Modulith
public class Architecture {

    private final ApplicationModules applicationModules = ApplicationModules.of(Architecture.class);

    @Test
    @DisplayName("🔍 Verify module architecture")
    void verifyArchitecture() {
        applicationModules.verify();
    }

    @Test
    @DisplayName("📝 Write documentation")
    @DisabledIfEnvironmentVariable(named = "CI", matches = ".*", disabledReason = "Unnecessary in CI")
    void writeDocumentation() {
        new Documenter(applicationModules)
                .writeModulesAsPlantUml()
                .writeIndividualModulesAsPlantUml()
                .writeDocumentation()
                .writeAggregatingDocument();
    }
}
