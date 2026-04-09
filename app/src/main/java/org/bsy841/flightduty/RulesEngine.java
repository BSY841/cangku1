package org.bsy841.flightduty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class RulesEngine {

    public static class Rule {
        public final String cat;
        public final String people;
        public final String leg;
        public final String rest;
        public final String start;
        public final String end;
        public final int tm;

        public Rule(String cat, String people, String leg, String rest, String start, String end, int tm) {
            this.cat = cat;
            this.people = people;
            this.leg = leg;
            this.rest = rest;
            this.start = start;
            this.end = end;
            this.tm = tm;
        }
    }

    public static final List<Rule> RULES = new ArrayList<>();
    public static final Map<String, String> PEOPLE_MAP = new HashMap<>();
    public static final Map<String, String> LEG_MAP = new HashMap<>();

    static {
        PEOPLE_MAP.put("非扩编组", "非扩编");
        PEOPLE_MAP.put("扩编组（3人）", "加一人");
        PEOPLE_MAP.put("扩编组（4人）", "加二人");

        LEG_MAP.put("1-4段", "4");
        LEG_MAP.put("5段", "5");
        LEG_MAP.put("6段", "6");
        LEG_MAP.put("7段", "7");

        // 最大飞行时间
        RULES.add(new Rule("flt", "非扩编", null, null, "0000", "0459", 8));
        RULES.add(new Rule("flt", "非扩编", null, null, "0500", "1959", 9));
        RULES.add(new Rule("flt", "非扩编", null, null, "2000", "2359", 8));
        RULES.add(new Rule("flt", "加一人", null, null, "0000", "2359", 13));
        RULES.add(new Rule("flt", "加二人", null, null, "0000", "2359", 17));

        // 最大执勤时间 — 非扩编
        RULES.add(new Rule("duty", "非扩编", "4", null, "0000", "0459", 12));
        RULES.add(new Rule("duty", "非扩编", "5", null, "0000", "0459", 11));
        RULES.add(new Rule("duty", "非扩编", "6", null, "0000", "0459", 10));
        RULES.add(new Rule("duty", "非扩编", "7", null, "0000", "0459", 9));
        RULES.add(new Rule("duty", "非扩编", "4", null, "0500", "1159", 14));
        RULES.add(new Rule("duty", "非扩编", "5", null, "0500", "1159", 13));
        RULES.add(new Rule("duty", "非扩编", "6", null, "0500", "1159", 12));
        RULES.add(new Rule("duty", "非扩编", "7", null, "0500", "1159", 11));
        RULES.add(new Rule("duty", "非扩编", "4", null, "1200", "2359", 13));
        RULES.add(new Rule("duty", "非扩编", "5", null, "1200", "2359", 12));
        RULES.add(new Rule("duty", "非扩编", "6", null, "1200", "2359", 11));
        RULES.add(new Rule("duty", "非扩编", "7", null, "1200", "2359", 10));

        // 最大执勤时间 — 扩编组
        RULES.add(new Rule("duty", "加一人", null, "1级休息设施", "0000", "2359", 18));
        RULES.add(new Rule("duty", "加二人", null, "1级休息设施", "0000", "2359", 20));
        RULES.add(new Rule("duty", "加一人", null, "2级休息设施", "0000", "2359", 17));
        RULES.add(new Rule("duty", "加二人", null, "2级休息设施", "0000", "2359", 19));
        RULES.add(new Rule("duty", "加一人", null, "3级休息设施", "0000", "2359", 16));
        RULES.add(new Rule("duty", "加二人", null, "3级休息设施", "0000", "2359", 18));
    }

    public static boolean timeInWindow(String hhmmStr, String startStr, String endStr) {
        int val = Integer.parseInt(hhmmStr);
        int start = Integer.parseInt(startStr);
        int end = Integer.parseInt(endStr);
        return start <= val && val <= end;
    }

    public static Integer lookupValue(String cat, String peopleKey, String takeoffHhmm, String leg, String rest) {
        for (Rule rule : RULES) {
            if (!rule.cat.equals(cat)) continue;
            if (!rule.people.equals(peopleKey)) continue;
            if (leg != null && !leg.equals(rule.leg)) continue;
            if (rest != null && !rest.equals(rule.rest)) continue;
            if (timeInWindow(takeoffHhmm, rule.start, rule.end)) {
                return rule.tm;
            }
        }
        return null;
    }

    public static String minutesToHHmm(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }
}
