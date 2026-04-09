package org.bsy841.flightduty;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * 规则引擎：封装了飞行机组执勤时间的查表逻辑与公共工具方法。
 * 所有规则均来源于原始 time.xlsx 中的最大飞行时间与最大执勤时间表格。
 */
public class RulesEngine {

    /** 单条规则的数据结构 */
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

    /** 判断 HHMM 字符串是否落在 [start, end] 闭区间内 */
    public static boolean timeInWindow(String hhmmStr, String startStr, String endStr) {
        int val = Integer.parseInt(hhmmStr);
        int start = Integer.parseInt(startStr);
        int end = Integer.parseInt(endStr);
        return start <= val && val <= end;
    }

    /**
     * 从左往右竖着查表，返回对应的小时数 tm。
     *
     * @param cat        类别："flt" 或 "duty"
     * @param peopleKey  机组类型："非扩编"/"加一人"/"加二人"
     * @param takeoffHhmm 起飞时间（如 "0900"）
     * @param leg        航段数（仅非扩编组使用）
     * @param rest       休息设施（仅扩编组使用）
     * @return 查到的最大小时数，未找到返回 null
     */
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

    /** 将分钟数转为 HH:MM 格式 */
    public static String minutesToHHmm(int totalMinutes) {
        int h = totalMinutes / 60;
        int m = totalMinutes % 60;
        return String.format(Locale.getDefault(), "%02d:%02d", h, m);
    }

    /**
     * 将分钟数转为 HH:MM 格式；若跨天（超过24小时），显示为 HH:MM (+N)。
     *
     * @param totalMinutes 总分钟数
     * @return 格式化后的时间字符串，如 "05:00 (+1)"
     */
    public static String minutesToHHmmWithDay(int totalMinutes) {
        int days = totalMinutes / 1440; // 1440 = 24*60
        int remainder = totalMinutes % 1440;
        int h = remainder / 60;
        int m = remainder % 60;
        String time = String.format(Locale.getDefault(), "%02d:%02d", h, m);
        if (days > 0) {
            return time + " (+" + days + ")";
        }
        return time;
    }
}
