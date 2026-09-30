package com.agent.codeagent.common;

import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Stream;

/** Discovers local SKILL.md files and exposes a compact index plus lazy full-content loading. */
@Service
public class SkillLoader {

    private final Path skillsDirectory;
    private final Map<String, Skill> skillsByAlias = new LinkedHashMap<>();
    private volatile Map<String, Skill> skills = Map.of();

    @Autowired
    public SkillLoader(@Value("${agent.skills.directory:.agents/skills}") String skillsDirectory) {
        this(Paths.get(skillsDirectory));
    }

    SkillLoader(Path skillsDirectory) {
        this.skillsDirectory = skillsDirectory;
    }

    @PostConstruct
    public void initialize() {
        Map<String, Skill> discovered = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        if (Files.isDirectory(skillsDirectory)) {
            try (Stream<Path> paths = Files.walk(skillsDirectory)) {
                paths.filter(path -> Files.isRegularFile(path)
                                && path.getFileName().toString().equalsIgnoreCase("SKILL.md"))
                        .forEach(path -> discover(path, discovered));
            } catch (IOException error) {
                throw new IllegalStateException("Unable to scan skills directory: "
                        + skillsDirectory, error);
            }
        }

        skillsByAlias.clear();
        discovered.forEach((key, skill) -> {
            skillsByAlias.put(alias(key), skill);
            skillsByAlias.put(alias(skill.name()), skill);
        });
        skills = Collections.unmodifiableMap(new LinkedHashMap<>(discovered));
    }

    /** Returns the discovered skills keyed by their directory-relative name. */
    public Map<String, Skill> skills() {
        return skills;
    }

    /** Returns the system prompt fragment listing available skills without loading their bodies. */
    public String systemPrompt() {
        if (skills.isEmpty()) {
            return "No local skills are available.";
        }
        StringBuilder prompt = new StringBuilder("Available local skills:\n");
        skills.values().forEach(skill -> prompt.append("- ")
                .append(skill.name())
                .append(": ")
                .append(skill.description())
                .append(" (load by name: ")
                .append(skill.lookupName())
                .append(")\n"));
        return prompt.toString().stripTrailing();
    }

    /** Loads the complete SKILL.md content by directory name or frontmatter name. */
    public String loadSkill(String name) {
        Skill skill = skillsByAlias.get(alias(name));
        if (skill == null) {
            throw new IllegalArgumentException("Unknown skill: " + name);
        }
        try {
            return Files.readString(skill.path(), StandardCharsets.UTF_8);
        } catch (IOException error) {
            throw new IllegalStateException("Unable to load skill: " + name, error);
        }
    }

    private void discover(Path path, Map<String, Skill> discovered) {
        try {
            String content = Files.readString(path, StandardCharsets.UTF_8);
            String directoryName = skillsDirectory.relativize(path.getParent()).toString();
            String name = frontmatter(content, "name", directoryName);
            String description = frontmatter(content, "description", "No description provided.");
            discovered.put(directoryName, new Skill(name, description, directoryName, path));
        } catch (IOException error) {
            throw new IllegalStateException("Unable to read skill: " + path, error);
        }
    }

    private static String frontmatter(String content, String key, String fallback) {
        if (!content.startsWith("---")) return fallback;
        int end = content.indexOf("\n---", 3);
        if (end < 0) return fallback;
        String prefix = content.substring(3, end);
        for (String line : prefix.split("\\R")) {
            String marker = key + ":";
            if (line.trim().startsWith(marker)) {
                String value = line.trim().substring(marker.length()).trim();
                return value.isEmpty() ? fallback : value;
            }
        }
        return fallback;
    }

    private static String alias(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Skill name must not be blank");
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    public record Skill(String name, String description, String lookupName, Path path) {
    }
}
