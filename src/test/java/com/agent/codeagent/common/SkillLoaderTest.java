package com.agent.codeagent.common;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SkillLoaderTest {

    @Test
    void scansSkillNamesAndDescriptionsAtInitialization() throws Exception {
        Path skillsDirectory = Files.createTempDirectory("skills");
        writeSkill(skillsDirectory, "alpha", "Alpha Skill", "Use alpha for planning.");
        writeSkill(skillsDirectory, "beta", "Beta Skill", "Use beta for testing.");

        SkillLoader loader = new SkillLoader(skillsDirectory);
        loader.initialize();

        assertEquals(2, loader.skills().size());
        assertTrue(loader.systemPrompt().contains("Alpha Skill"));
        assertTrue(loader.systemPrompt().contains("Use alpha for planning."));
        assertTrue(loader.systemPrompt().contains("beta"));
    }

    @Test
    void loadsCompleteSkillContentByName() throws Exception {
        Path skillsDirectory = Files.createTempDirectory("skills");
        writeSkill(skillsDirectory, "alpha", "Alpha Skill", "Use alpha for planning.");
        String expected = "---\nname: Alpha Skill\ndescription: Use alpha for planning.\n---\n\nFull instructions.";
        Files.writeString(skillsDirectory.resolve("alpha").resolve("SKILL.md"), expected);

        SkillLoader loader = new SkillLoader(skillsDirectory);
        loader.initialize();

        assertEquals(expected, loader.loadSkill("alpha"));
        assertEquals(expected, loader.loadSkill("Alpha Skill"));
        assertThrows(IllegalArgumentException.class, () -> loader.loadSkill("missing"));
    }

    private static void writeSkill(Path root, String directoryName, String name, String description)
            throws Exception {
        Path directory = Files.createDirectories(root.resolve(directoryName));
        Files.writeString(directory.resolve("SKILL.md"), "---\nname: " + name
                + "\ndescription: " + description + "\n---\n\nFull instructions.");
    }
}
