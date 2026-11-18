package navigator.trajectory.matching;

import navigator.trajectory.api.MatchResult;
import navigator.trajectory.api.RoleProfile;
import navigator.trajectory.api.RoleProfileSkill;
import navigator.trajectory.api.SkillMatch;
import navigator.trajectory.external.RoleCatalog;
import navigator.trajectory.external.RoleInfo;
import navigator.trajectory.external.SkillDictionary;

import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MatchConsoleView {
    private static final int TOTAL_SEGMENTS = 20;
    private static final int SKILL_SEGMENTS = 6;
    private static final String GREEN = "\u001b[32m";
    private static final String RED = "\u001b[31m";
    private static final String RESET = "\u001b[0m";
    private static final DateTimeFormatter DATE = DateTimeFormatter.ofPattern("dd.MM");

    private final RoleCatalog roles;
    private final SkillDictionary dictionary;
    private final boolean styled;

    public MatchConsoleView(RoleCatalog roles, SkillDictionary dictionary) {
        this(roles, dictionary,
                System.console() != null
                        && System.getenv("NO_COLOR") == null
                        && !"dumb".equalsIgnoreCase(System.getenv("TERM")));
    }

    public MatchConsoleView(RoleCatalog roles, SkillDictionary dictionary, boolean styled) {
        this.roles = roles;
        this.dictionary = dictionary;
        this.styled = styled;
    }

    public void print(RoleProfile profile, MatchResult result) {
        String roleName = roles.all().stream()
                .filter(role -> role.id().equals(profile.roleId()))
                .map(RoleInfo::name)
                .findFirst()
                .orElse(profile.roleId());

        System.out.printf("%s (%s) — соответствие %.0f%%%n",
                roleName, profile.grade(), result.matchPercent());
        System.out.printf("По %d вакансиям, данные от %s%n",
                profile.sampleSize(), profile.computedAt().format(DATE));
        int filled = (int) Math.round(result.matchPercent() * TOTAL_SEGMENTS / 100);
        System.out.println("[" + bar(filled, TOTAL_SEGMENTS) + "]");

        if (profile.skills().isEmpty()) {
            System.out.println("В профиле роли нет навыков.");
            return;
        }

        String heading = "Навык";
        int nameWidth = heading.length();
        Map<Long, String> names = new HashMap<>();
        for (RoleProfileSkill skill : profile.skills()) {
            String name = dictionary.nameOf(skill.skillId());
            names.put(skill.skillId(), name);
            nameWidth = Math.max(nameWidth, name.length());
        }

        if (!result.blockingGaps().isEmpty()) {
            List<String> blocking = result.blockingGaps().stream()
                    .map(skill -> names.get(skill.skillId()))
                    .toList();
            System.out.println(color("Блокирующие пробелы: " + String.join(", ", blocking), RED));
        }

        List<Long> unfilled = result.unfilledSkillIds();
        if (!unfilled.isEmpty()) {
            List<String> missing = unfilled.stream().map(names::get).toList();
            System.out.printf("Не заполнено: %d (%s) — fill%n",
                    missing.size(), String.join(", ", missing));
        }

        String nameColumn = "%-" + nameWidth + "s";
        System.out.printf("%n" + nameColumn + "  %-6s  %s%n", heading, "", "Есть / Нужно");

        List<SkillMatch> rows = new ArrayList<>(result.strengths());
        rows.addAll(result.gaps());
        int strengths = result.strengths().size();
        for (int i = 0; i < rows.size(); i++) {
            SkillMatch skill = rows.get(i);
            boolean strong = i < strengths;
            String actual = unfilled.contains(skill.skillId())
                    ? "-"
                    : Integer.toString(skill.actualLevel());
            String status = strong ? "сильная сторона" : "приоритет " + (i - strengths + 1);
            String row = (nameColumn + "  %s  %s/%d  %s").formatted(
                    names.get(skill.skillId()),
                    bar(skill.actualLevel(), SKILL_SEGMENTS),
                    actual,
                    skill.requiredLevel(),
                    status
            );
            System.out.println(color(row, strong ? GREEN : RED));
        }
    }

    private String bar(int filled, int total) {
        String full = styled ? "█" : "#";
        String empty = styled ? "░" : ".";
        return full.repeat(filled) + empty.repeat(total - filled);
    }

    private String color(String text, String ansi) {
        return styled ? ansi + text + RESET : text;
    }
}