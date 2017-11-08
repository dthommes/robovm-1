package org.robovm.utils.codesign.coderesources;

import com.dd.plist.NSDictionary;
import com.dd.plist.NSNumber;
import com.dd.plist.NSObject;

import java.util.Map;
import java.util.regex.Pattern;

/**
 * Simple reimplementation of ResourceBuilder, in the Apple Open Source
 * file bundlediskrep.cpp
 */
class RuleVo {
    public final static RuleVo NULL_PATH_RULE = new RuleVo();

    // flags
    public static final class Flags {
        static final int OPTIONAL = 0x01;       // may be absent at runtime
        static final int OMITTED = 0x02;        // do not seal even if present
        static final int NESTED = 0x04;         // nested code (recursively signed)
        static final int EXCLUSION = 0x10;      // overriding exclusion (stop looking)
//        static final int SOFT_TARGET = 0x20;    // valid symlink target even though omitted/excluded
    }


    private final Pattern pattern;
    int flags;
    float weight;


    public RuleVo() {
        this("", null);
    }

    public RuleVo(String pattern, int flags) {
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.flags = flags;
        this.weight = 0;
    }

    @SuppressWarnings({"unchecked", "ConstantConditions"})
    public RuleVo(String pattern, Object properties) {
        // on Mac OS the FS is case-insensitive; simulate that here
        this.pattern = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
        this.flags = 0;
        this.weight = 0;
        if (properties != null) {
            if (properties instanceof NSNumber) {
                if (!((NSNumber) properties).boolValue())
                    this.flags |= RuleVo.Flags.OMITTED;
                // if it was true, this file is required;
                // do nothing
            } else if (properties instanceof Map) {
                for (Map.Entry<String, Object> e : ((Map<String, Object>) properties).entrySet()) {
                    String key = e.getKey();
                    Object value = e.getValue();
                    if ("optional".equals(key) && ((NSNumber) value).boolValue())
                        this.flags |= RuleVo.Flags.OPTIONAL;
                    else if ("omit".equals(key) && ((NSNumber) value).boolValue())
                        this.flags |= RuleVo.Flags.OMITTED;
                    else if ("nested".equals(key) && ((NSNumber) value).boolValue())
                        this.flags |= RuleVo.Flags.NESTED;
                    else if ("weight".equals(key))
                        this.weight = ((NSNumber) value).floatValue();
                }
            }
        }
    }

    public boolean isOptional() {
        return (this.flags & RuleVo.Flags.OPTIONAL) != 0;
    }

    public boolean is_omitted() {
        return (this.flags & RuleVo.Flags.OMITTED) != 0;
    }

    public boolean is_nested() {
        return (this.flags & RuleVo.Flags.NESTED) != 0;
    }

    public boolean is_exclusion() {
        return (this.flags & RuleVo.Flags.EXCLUSION) != 0;
    }

    public boolean matches(String path) {
        return this.pattern.matcher(path).find();
    }

    public static RuleVo findRule(RuleVo[] rules, String path) {
        RuleVo best_rule = NULL_PATH_RULE;
        for (RuleVo rule : rules) {
            // log.debug("trying rule " + str(rule) + " against " + path)
            if (rule.matches(path)) {
                if (rule.is_exclusion() || rule.is_omitted()) {
                    best_rule = rule;
                    break;
                } else if (rule.weight > best_rule.weight)
                    best_rule = rule;
            }
        }
        return best_rule;
    }

    public static class List {
        private final NSDictionary plistRules;
        private final RuleVo[] items;

        public List(NSDictionary plistRules) {
            this.plistRules = plistRules;
            items = new RuleVo[plistRules.size()];
            int idx = 0;
            for (Map.Entry<String, NSObject> e : plistRules.entrySet()) {
                items[idx++] = new RuleVo(e.getKey(), e.getValue());
            }
        }

        public NSDictionary plistRules() {
            return plistRules;
        }

        public RuleVo[] getItems() {
            return items;
        }
    }


    @Override
    public String toString() {
        return "RuleVo{" +
                "pattern=" + pattern +
                ", flags=" + flags +
                ", weight=" + weight +
                '}';
    }
}

