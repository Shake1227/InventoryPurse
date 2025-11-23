package shake1227.inventorypurse.core.config;

import net.minecraftforge.common.ForgeConfigSpec;
import java.util.List;

public class ServerConfig {
    public static final ForgeConfigSpec.Builder BUILDER = new ForgeConfigSpec.Builder();
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue BASE_SLOTS;

    public static final ForgeConfigSpec.BooleanValue SHOW_NOTIFICATIONS;

    public static final ForgeConfigSpec.IntValue PURSE_SLOTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> PURSE_ACTIVATION;

    public static final ForgeConfigSpec.IntValue RED_PURSE_SLOTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> RED_PURSE_ACTIVATION;

    public static final ForgeConfigSpec.IntValue BLACK_PURSE_SLOTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> BLACK_PURSE_ACTIVATION;

    public static final ForgeConfigSpec.IntValue WHITE_PURSE_SLOTS;
    public static final ForgeConfigSpec.ConfigValue<List<? extends String>> WHITE_PURSE_ACTIVATION;

    static {
        BUILDER.push("Inventory Lock Settings");

        BASE_SLOTS = BUILDER
                .comment("Number of inventory slots accessible by default (0-35). Slots are counted from the top-left of the main inventory (excluding hotbar). 0 means none, 9 is the first row, 36 includes hotbar and main inventory.")
                .defineInRange("baseAccessibleSlots", 9, 0, 36);
        SHOW_NOTIFICATIONS = BUILDER
                .comment("If true, sends a notification when inventory slots change.")
                .define("showNotifications", true);

        BUILDER.pop();

        BUILDER.push("Purse Settings");

        BUILDER.push("Normal Purse");
        PURSE_SLOTS = BUILDER.comment("Number of additional slots unlocked by the normal purse.").defineInRange("slots", 9, 0, 36);
        PURSE_ACTIVATION = BUILDER.comment("Which hand(s) activate the purse? Options: \"mainhand\", \"offhand\"").defineList("activation", List.of("offhand"), o -> o instanceof String s && (s.equals("mainhand") || s.equals("offhand")));
        BUILDER.pop();

        BUILDER.push("Red Purse");
        RED_PURSE_SLOTS = BUILDER.comment("Number of additional slots unlocked by the red purse.").defineInRange("slots", 18, 0, 36);
        RED_PURSE_ACTIVATION = BUILDER.comment("Which hand(s) activate the red purse?").defineList("activation", List.of("offhand"), o -> o instanceof String s && (s.equals("mainhand") || s.equals("offhand")));
        BUILDER.pop();

        BUILDER.push("Black Purse");
        BLACK_PURSE_SLOTS = BUILDER.comment("Number of additional slots unlocked by the black purse.").defineInRange("slots", 27, 0, 36);
        BLACK_PURSE_ACTIVATION = BUILDER.comment("Which hand(s) activate the black purse?").defineList("activation", List.of("offhand"), o -> o instanceof String s && (s.equals("mainhand") || s.equals("offhand")));
        BUILDER.pop();

        BUILDER.push("White Purse");
        WHITE_PURSE_SLOTS = BUILDER.comment("Number of additional slots unlocked by the white purse.").defineInRange("slots", 3, 0, 36);
        WHITE_PURSE_ACTIVATION = BUILDER.comment("Which hand(s) activate the white purse?").defineList("activation", List.of("mainhand", "offhand"), o -> o instanceof String s && (s.equals("mainhand") || s.equals("offhand")));
        BUILDER.pop();

        BUILDER.pop();
        SPEC = BUILDER.build();
    }
}