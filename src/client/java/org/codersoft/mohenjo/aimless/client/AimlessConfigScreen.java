package org.codersoft.mohenjo.aimless.client;

import me.shedaniel.clothconfig2.api.ConfigBuilder;
import me.shedaniel.clothconfig2.api.ConfigCategory;
import me.shedaniel.clothconfig2.api.ConfigEntryBuilder;
import net.minecraft.client.gui.screen.Screen;
import net.minecraft.text.Text;

public class AimlessConfigScreen {

    public static Screen build(Screen parent) {
        AimlessConfig config = AimlessConfig.CONFIG;

        ConfigBuilder builder = ConfigBuilder.create()
                .setParentScreen(parent)
                .setTitle(Text.literal("Aimless Config"));

        ConfigEntryBuilder entryBuilder = builder.entryBuilder();

        ConfigCategory aim = builder.getOrCreateCategory(Text.literal("Aim Point"));

        int bodyPct = (int) Math.round(config.getBodyHeight() * 100);
        aim.addEntry(entryBuilder.startIntSlider(
                Text.literal("Body Height"),
                bodyPct, 0, 100)
                .setDefaultValue(80)
                .setTextGetter(v -> {
                    double pct = v;
                    String label;
                    if (v >= 95) label = "Head top";
                    else if (v >= 80) label = "Eyes";
                    else if (v >= 60) label = "Chest";
                    else if (v >= 40) label = "Waist";
                    else if (v >= 20) label = "Knees";
                    else label = "Feet";
                    return Text.literal(String.format("%s (%.0f%%)", label, pct));
                })
                .setTooltip(Text.literal("Aim height on the target's body.\n0% = feet, 100% = top of head"))
                .setSaveConsumer(v -> config.setBodyHeight(v / 100.0))
                .build()
        );

        int offX = (int) Math.round(config.getOffsetX() * 100);
        aim.addEntry(entryBuilder.startIntSlider(
                Text.literal("Horizontal Offset (X)"),
                offX, -400, 400)
                .setDefaultValue(0)
                .setTextGetter(v -> Text.literal(String.format("%+.2f blocks", v / 100.0)))
                .setTooltip(Text.literal("Left/right offset from body center.\n-4.00 to +4.00 blocks"))
                .setSaveConsumer(v -> config.setOffsetX(v / 100.0))
                .build()
        );

        int offZ = (int) Math.round(config.getOffsetZ() * 100);
        aim.addEntry(entryBuilder.startIntSlider(
                Text.literal("Forward Offset (Z)"),
                offZ, -400, 400)
                .setDefaultValue(0)
                .setTextGetter(v -> Text.literal(String.format("%+.2f blocks", v / 100.0)))
                .setTooltip(Text.literal("Forward/backward offset from body center.\n-4.00 to +4.00 blocks"))
                .setSaveConsumer(v -> config.setOffsetZ(v / 100.0))
                .build()
        );

        aim.addEntry(entryBuilder.startIntSlider(
                Text.literal("Reaction Ticks"),
                config.getReactionTicks(), 1, 100)
                .setDefaultValue(6)
                .setTextGetter(v -> Text.literal(v + " ticks (" + (v * 50) + "ms)"))
                .setTooltip(Text.literal("Delay between aim corrections.\n1 tick = 50ms, 20 ticks = 1 second"))
                .setSaveConsumer(config::setReactionTicks)
                .build()
        );

        ConfigCategory targeting = builder.getOrCreateCategory(Text.literal("Targeting"));

        String exList = config.getExceptions().isEmpty()
                ? "None"
                : String.join(", ", config.getExceptions());

        targeting.addEntry(entryBuilder.startTextDescription(
                Text.literal("§7Exceptions: §f" + exList + "\n\nManage via §e/aimless exception§7 commands"))
                .build()
        );

        builder.setSavingRunnable(config::save);

        return builder.build();
    }
}
