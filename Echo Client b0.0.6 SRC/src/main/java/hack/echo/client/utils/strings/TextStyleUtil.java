package hack.echo.client.utils.strings;

import hack.echo.client.utils.Imports;

import java.util.Map;


public class TextStyleUtil implements Imports {
    private static final Map<Character, Character> translate = Map.ofEntries(
            Map.entry('ᴀ', 'a'),
            Map.entry('ʙ', 'b'),
            Map.entry('ᴄ', 'c'),
            Map.entry('ᴅ', 'd'),
            Map.entry('ᴇ', 'e'),
            Map.entry('ꜰ', 'f'),
            Map.entry('ɢ', 'g'),
            Map.entry('ʜ', 'h'),
            Map.entry('ɪ', 'i'),
            Map.entry('ᴊ', 'j'),
            Map.entry('ᴋ', 'k'),
            Map.entry('ʟ', 'l'),
            Map.entry('ᴍ', 'm'),
            Map.entry('ɴ', 'n'),
            Map.entry('ᴏ', 'o'),
            Map.entry('ᴘ', 'p'),
            Map.entry('ǫ', 'q'),
            Map.entry('ʀ', 'r'),
            Map.entry('ꜱ', 's'),
            Map.entry('ᴛ', 't'),
            Map.entry('ᴜ', 'u'),
            Map.entry('ᴠ', 'v'),
            Map.entry('ᴡ', 'w'),
            Map.entry('x', 'x'),
            Map.entry('ʏ', 'y'),
            Map.entry('ᴢ', 'z'),
            Map.entry('ѕ', 's'),
            Map.entry('і', 'i'),
            Map.entry('ᴉ', 'i')
    );

    public static String translate(String input) {
        StringBuilder sb = new StringBuilder();

        for (char c : input.toCharArray()) {
            sb.append(translate.getOrDefault(c, c));
        }

        return sb.toString().replaceAll("[^a-zA-Z ]", "").toLowerCase();
    }
}
