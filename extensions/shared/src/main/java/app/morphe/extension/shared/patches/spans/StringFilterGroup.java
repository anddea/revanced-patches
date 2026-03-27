package app.morphe.extension.shared.patches.spans;

import app.morphe.extension.shared.settings.BooleanSetting;

public class StringFilterGroup extends FilterGroup<String> {

    public StringFilterGroup(final BooleanSetting setting, final String... filters) {
        super(setting, filters);
    }

    @Override
    public FilterGroupResult check(final String string) {
        int matchedIndex = -1;
        if (isEnabled()) {
            for (String pattern : filters) {
                if (!string.isEmpty()) {
                    final int indexOf = string.indexOf(pattern);
                    if (indexOf >= 0) {
                        matchedIndex = indexOf;
                        break;
                    }
                }
            }
        }
        return new FilterGroupResult(setting, matchedIndex);
    }
}
