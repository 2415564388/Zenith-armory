package com.gundevourer.config;

import net.minecraftforge.common.ForgeConfigSpec;

/**
 * 模组数值配置。生成于 config/gundevourer-common.toml。
 */
public final class GunDevourerConfig {
    public static final ForgeConfigSpec SPEC;

    public static final ForgeConfigSpec.IntValue FLAT_DAMAGE_PER_GUN;
    public static final ForgeConfigSpec.IntValue GUNS_PER_PERCENT_STAGE;
    public static final ForgeConfigSpec.IntValue PERCENT_PER_STAGE;
    public static final ForgeConfigSpec.IntValue TRUE_DAMAGE_THRESHOLD;
    public static final ForgeConfigSpec.DoubleValue TRUE_DAMAGE_PERCENT;
    public static final ForgeConfigSpec.DoubleValue TRUE_DAMAGE_MIN_HEALTH;

    static {
        ForgeConfigSpec.Builder builder = new ForgeConfigSpec.Builder();

        builder.comment("Fixed and percentage gun damage bonuses.",
                        "固定与百分比枪械伤害加成。")
                .push("damage");
        FLAT_DAMAGE_PER_GUN = builder
                .comment("Flat gun damage added per devoured gun type.",
                         "每吞噬 1 种枪械增加的固定射击伤害。")
                .defineInRange("flatDamagePerGun", 1, 0, 100);
        GUNS_PER_PERCENT_STAGE = builder
                .comment("Devoured gun types required for one percentage stage.",
                         "每多少种枪械提升一次百分比伤害。")
                .defineInRange("gunsPerPercentStage", 9, 1, 4096);
        PERCENT_PER_STAGE = builder
                .comment("Percentage gun damage bonus granted per stage.",
                         "每阶段提升的枪械伤害百分比。")
                .defineInRange("percentPerStage", 10, 0, 10000);
        builder.pop();

        builder.comment("Absolute true damage unlocked at a devoured-count threshold.",
                        "达到吞噬数量后解锁的绝对真伤。")
                .push("trueDamage");
        TRUE_DAMAGE_THRESHOLD = builder
                .comment("Devoured gun types required to unlock true damage.",
                         "解锁真伤所需吞噬的枪械种数。")
                .defineInRange("threshold", 368, 1, 4096);
        TRUE_DAMAGE_PERCENT = builder
                .comment("True damage as a percentage of the hit's damage.",
                         "真伤占该次命中伤害的百分比。")
                .defineInRange("percent", 10.0, 0.0, 100.0);
        TRUE_DAMAGE_MIN_HEALTH = builder
                .comment("Minimum health true damage leaves the target at. Must be at least 1 so true damage never kills.",
                         "真伤最多把目标压到的最低血量，至少为 1，保证真伤不会直接击杀。")
                .defineInRange("minHealth", 1.0, 1.0, 100000.0);
        builder.pop();

        SPEC = builder.build();
    }

    private GunDevourerConfig() {
    }

    /**
     * 配置值可能是 10.0 这类整数小数，展示时去掉无意义的 ".0"。
     */
    public static String display(double value) {
        if (value == Math.rint(value)) {
            return Long.toString((long) value);
        }
        return Double.toString(value);
    }
}
