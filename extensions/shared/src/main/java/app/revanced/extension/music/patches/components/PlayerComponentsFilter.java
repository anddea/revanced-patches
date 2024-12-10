package app.revanced.extension.music.patches.components;

import app.revanced.extension.music.settings.Settings;
import app.revanced.extension.shared.patches.components.Filter;
import app.revanced.extension.shared.patches.components.StringFilterGroup;

@SuppressWarnings("unused")
public final class PlayerComponentsFilter extends Filter {

    public PlayerComponentsFilter() {
        addIdentifierCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_COMMENT_CHANNEL_GUIDELINES,
                        "channel_guidelines_entry_banner.eml",
                        "community_guidelines.eml"
                )
        );
        addPathCallbacks(
                new StringFilterGroup(
                        Settings.HIDE_COMMENT_TIMESTAMP_AND_EMOJI_BUTTONS,
                        "|CellType|ContainerType|ContainerType|ContainerType|ContainerType|ContainerType|"
                )
        );
    }
}
