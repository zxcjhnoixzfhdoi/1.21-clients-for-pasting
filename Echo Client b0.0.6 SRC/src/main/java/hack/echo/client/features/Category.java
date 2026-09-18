package hack.echo.client.features;

import hack.echo.client.utils.TextLuicideConstants;
import hack.echo.client.utils.strings.Concat;

public enum Category {
    // Category names are not left in memory but something else that Matches them is tho and I cannot figure out why.
    COMBAT(Concat.of("Combat"), TextLuicideConstants.swords),
    MOVEMENT(Concat.of("Movement"), TextLuicideConstants.gauge),
    RENDER(Concat.of("Visuals"), TextLuicideConstants.scanEye),
    MACROS(Concat.of("Macros"), TextLuicideConstants.wrench),
    UTILITY(Concat.of("Utility"), TextLuicideConstants.user),
    INTERNALS(Concat.of("Internals"), TextLuicideConstants.cog),
//    HUD(Concat.of("HUD"), TextLuicideConstants.textAlignStart)
    ;

    public final CharSequence name;
    public final CharSequence icon;

    Category(final CharSequence name, final CharSequence icon) {
        this.name = name;
        this.icon = icon;
    }
}
