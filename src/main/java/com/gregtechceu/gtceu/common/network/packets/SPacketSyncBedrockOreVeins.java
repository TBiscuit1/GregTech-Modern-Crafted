package com.gregtechceu.gtceu.common.network.packets;

import com.gregtechceu.gtceu.GTCEu;
import com.gregtechceu.gtceu.api.data.worldgen.bedrockore.BedrockOreDefinition;
import com.gregtechceu.gtceu.api.registry.GTRegistries;

import com.lowdragmc.lowdraglib.Platform;
import com.lowdragmc.lowdraglib.networking.IHandlerContext;
import com.lowdragmc.lowdraglib.networking.IPacket;

import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.RegistryOps;
import net.minecraft.resources.ResourceLocation;

import lombok.RequiredArgsConstructor;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Stream;

@RequiredArgsConstructor
public class SPacketSyncBedrockOreVeins implements IPacket {

    private final Map<ResourceLocation, BedrockOreDefinition> veins;

    public SPacketSyncBedrockOreVeins() {
        this.veins = new HashMap<>();
    }

    @Override
    public void encode(FriendlyByteBuf buf) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, Platform.getFrozenRegistry());
        int size = veins.size();
        buf.writeVarInt(size);
        for (var entry : veins.entrySet()) {
            buf.writeResourceLocation(entry.getKey());
            CompoundTag tag = (CompoundTag) BedrockOreDefinition.FULL_CODEC.encodeStart(ops, entry.getValue())
                    .getOrThrow(false, GTCEu.LOGGER::error);
            buf.writeNbt(tag);
        }
    }

    @Override
    public void decode(FriendlyByteBuf buf) {
        RegistryOps<Tag> ops = RegistryOps.create(NbtOps.INSTANCE, Platform.getFrozenRegistry());
        Stream.generate(() -> {
            ResourceLocation id = buf.readResourceLocation();
            CompoundTag tag = buf.readAnySizeNbt();
            BedrockOreDefinition def = BedrockOreDefinition.FULL_CODEC.parse(ops, tag).getOrThrow(false,
                    GTCEu.LOGGER::error);
            return Map.entry(id, def);
        }).limit(buf.readVarInt()).forEach(entry -> veins.put(entry.getKey(), entry.getValue()));
    }

    @Override
    public void execute(IHandlerContext handler) {
        if (GTRegistries.BEDROCK_ORE_DEFINITIONS.isFrozen()) {
            GTRegistries.BEDROCK_ORE_DEFINITIONS.unfreeze();
        }
        GTRegistries.BEDROCK_ORE_DEFINITIONS.registry().clear();
        for (var entry : veins.entrySet()) {
            GTRegistries.BEDROCK_ORE_DEFINITIONS.registerOrOverride(entry.getKey(), entry.getValue());
        }
        if (!GTRegistries.BEDROCK_ORE_DEFINITIONS.isFrozen()) {
            GTRegistries.BEDROCK_ORE_DEFINITIONS.freeze();
        }
    }
}
