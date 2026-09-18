package we.devs.opium.api.utilities;

import we.devs.opium.client.modules.client.ModuleCommands;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;

import java.util.ArrayList;
import java.util.List;

public class ChatUtils implements IMinecraft {
    public static void sendMessage(String message, Object... args) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            return;
        }
        message = fastFormat(message, args);
        Text component = Text.literal(getWatermark() + (!ModuleCommands.INSTANCE.watermarkMode.getValue().equals(ModuleCommands.WatermarkModes.None) ? " " : "") + ModuleCommands.getFirstColor() + message);
        mc.inGameHud.getChatHud().addMessage(component);
    }

    public static void sendMessage(String message, int id) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            return;
        }
        Text component = Text.literal(getWatermark() + (!ModuleCommands.INSTANCE.watermarkMode.getValue().equals(ModuleCommands.WatermarkModes.None) ? " " : "") + ModuleCommands.getFirstColor() + message);
        mc.inGameHud.getChatHud().addMessage(component);
        //((IChatHud) mc.inGameHud.getChatHud()).clientMessage(component, id);
    }

    public static void sendMessage(String message, String name, Object... args) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            return;
        }
        message = fastFormat(message, args);
        Text component = Text.literal(getWatermark() + (!ModuleCommands.INSTANCE.watermarkMode.getValue().equals(ModuleCommands.WatermarkModes.None) ? " " : "") + Formatting.BLUE + "[" + name + "]: " + ModuleCommands.getFirstColor() + message);
        mc.inGameHud.getChatHud().addMessage(component);
    }

    public static void sendMessage(String message, String name, int id) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            return;
        }
        Text component = Text.literal(getWatermark() + (!ModuleCommands.INSTANCE.watermarkMode.getValue().equals(ModuleCommands.WatermarkModes.None) ? " " : "") + Formatting.AQUA + "[" + name + "]: " + ModuleCommands.getFirstColor() + message);
        mc.inGameHud.getChatHud().addMessage(component);//((IChatHud) mc.inGameHud.getChatHud()).clientMessage(component, id);
    }

    public static void sendRawMessage(String message, Object... args) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            return;
        }
        message = fastFormat(message, args);
        Text component = Text.literal(message);
        mc.player.sendMessage(component);
    }

    public static void sendRawMessage(String message) {
        if (mc.player == null || mc.world == null || mc.inGameHud == null) {
            return;
        }
        Text component = Text.literal(message);
        mc.player.sendMessage(component);
    }

    public static String getWatermark() {
        if (!ModuleCommands.INSTANCE.watermarkMode.getValue().equals(ModuleCommands.WatermarkModes.None)) {
            return ModuleCommands.getSecondWatermarkColor() + ModuleCommands.INSTANCE.firstSymbol.getValue() + ModuleCommands.getFirstWatermarkColor() + (ModuleCommands.INSTANCE.watermarkMode.getValue().equals(ModuleCommands.WatermarkModes.Custom) ? ModuleCommands.INSTANCE.watermarkText.getValue() : (ModuleCommands.INSTANCE.watermarkMode.getValue().equals(ModuleCommands.WatermarkModes.Japanese) ? "アヘンハック" : "Opium")) + ModuleCommands.getSecondWatermarkColor() + ModuleCommands.INSTANCE.secondSymbol.getValue();
        }
        return "";
    }

    /**
     * Slightly faster than String#format
     * @param str target string
     * @param arg an array of args
     * @return the target string with all <code>{}</code> replaced with args
     */
    private static String fastFormat(String str, Object... arg) {
        StringBuilder finalStr = new StringBuilder();
        boolean fp = false;
        int i = 0;

        ArrayList<Object> args = new ArrayList<>(List.of(arg));

        for (char aChar : str.toCharArray()) {
            if (args.isEmpty()) {
                finalStr.append(str.substring(i));
                break;
            }

            if (aChar == '}' && fp) {
                finalStr.append(args.remove(0));
                fp = false;
            } else if (aChar == '{') {
                fp = true;
            } else {
                if (fp) {
                    finalStr.append('{');
                    fp = false;
                }
                finalStr.append(aChar);
            }
            i++;
        }

        if (fp) {
            finalStr.append('{');
        }

        return finalStr.toString();
    }
}
