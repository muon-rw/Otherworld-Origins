package dev.muon.otherworldorigins.network;

import com.seniors.justlevelingfork.common.capability.AptitudeCapability;
import com.seniors.justlevelingfork.network.packet.client.SyncAptitudeCapabilityCP;
import com.seniors.justlevelingfork.network.packet.common.AptitudeLevelUpSP;
import com.seniors.justlevelingfork.registry.RegistryAptitudes;
import com.seniors.justlevelingfork.registry.RegistrySkills;
import com.seniors.justlevelingfork.registry.aptitude.Aptitude;
import com.seniors.justlevelingfork.registry.skills.Skill;
import dev.muon.otherworldorigins.OtherworldOrigins;
import dev.muon.otherworldorigins.power.InnateAptitudeBonusPower;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.server.level.ServerPlayer;
import net.minecraftforge.network.NetworkEvent;

import java.util.function.Supplier;

public class RespecAptitudesMessage {
    private static final int RESPEC_COST = 10;
    private static final float REFUND_PERCENT = 0.25F;

    public RespecAptitudesMessage() {}

    public static RespecAptitudesMessage decode(FriendlyByteBuf buf) {
        return new RespecAptitudesMessage();
    }

    public void encode(FriendlyByteBuf buf) {}

    public static void handle(RespecAptitudesMessage message, Supplier<NetworkEvent.Context> ctx) {
        ctx.get().enqueueWork(() -> {
            ServerPlayer player = ctx.get().getSender();
            if (player != null) {
                AptitudeCapability capability = AptitudeCapability.get(player);
                if (capability == null) {
                    OtherworldOrigins.LOGGER.error("AptitudeCapability is null");
                    return;
                }

                int totalLevels = capability.getGlobalLevel();
                int xpCost = calculateXpCost(capability, player);
                int refundXp = (int) (xpCost * REFUND_PERCENT);

                OtherworldOrigins.LOGGER.debug("Player level: {}, Total aptitude levels: {}, XP cost: {}, Refund XP: {}",
                        player.experienceLevel, totalLevels, xpCost, refundXp);

                if (player.experienceLevel >= RESPEC_COST) {
                    player.giveExperienceLevels(-RESPEC_COST);
                    player.giveExperiencePoints(refundXp);

                    resetAptitudes(capability, player);
                    resetSkills(capability);
                    ResetFeatsMessage.handle(new ResetFeatsMessage(), ctx); // Reset feats

                    SyncAptitudeCapabilityCP.send(player);
                    OtherworldOrigins.LOGGER.debug("Respec completed successfully");
                } else {
                    OtherworldOrigins.LOGGER.warn("Player doesn't have enough levels");
                }
            }
        });
        ctx.get().setPacketHandled(true);
    }

    private static int calculateXpCost(AptitudeCapability capability, ServerPlayer player) {
        int xpCost = 0;
        for (Aptitude aptitude : RegistryAptitudes.APTITUDES_REGISTRY.get().getValues()) {
            String aptitudeName = aptitude.getName();
            int currentLevel = capability.getAptitudeLevel(aptitude);
            int innateBonus = InnateAptitudeBonusPower.getBonus(player, aptitudeName);
            int investedLevels = Math.max(currentLevel - innateBonus, 0);

            for (int i = 1; i <= investedLevels; i++) {
                xpCost += AptitudeLevelUpSP.requiredPoints(i);
            }
        }
        return xpCost;
    }

    private static void resetAptitudes(AptitudeCapability capability, ServerPlayer player) {
        for (Aptitude aptitude : RegistryAptitudes.APTITUDES_REGISTRY.get().getValues()) {
            String aptitudeName = aptitude.getName();
            int innateBonus = InnateAptitudeBonusPower.getBonus(player, aptitudeName);
            int newLevel = 1 + innateBonus;
            capability.setAptitudeLevel(aptitude, newLevel);
            OtherworldOrigins.LOGGER.debug("Reset aptitude {} to level {} (including innate bonus of {})", aptitudeName, newLevel, innateBonus);
        }
    }


    private static void resetAptitudes(AptitudeCapability capability) {
        for (Aptitude aptitude : RegistryAptitudes.APTITUDES_REGISTRY.get().getValues()) {
            capability.setAptitudeLevel(aptitude, 1);
        }
    }

    private static void resetSkills(AptitudeCapability capability) {
        for (Skill skill : RegistrySkills.SKILLS_REGISTRY.get().getValues()) {
            capability.setToggleSkill(skill, false);
        }
    }

    public static void send() {
        OtherworldOrigins.CHANNEL.sendToServer(new RespecAptitudesMessage());
    }
}