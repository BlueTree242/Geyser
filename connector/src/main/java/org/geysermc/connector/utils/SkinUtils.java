package org.geysermc.connector.utils;

import com.github.steveice10.mc.auth.data.GameProfile;
import com.google.gson.JsonObject;
import com.nukkitx.protocol.bedrock.data.ImageData;
import com.nukkitx.protocol.bedrock.data.SerializedSkin;
import com.nukkitx.protocol.bedrock.packet.PlayerListPacket;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.apache.commons.codec.Charsets;
import org.geysermc.api.Geyser;
import org.geysermc.connector.GeyserConnector;
import org.geysermc.connector.entity.PlayerEntity;
import org.geysermc.connector.network.session.GeyserSession;

import java.util.Base64;
import java.util.Collections;
import java.util.UUID;
import java.util.function.Consumer;

public class SkinUtils {
    public static PlayerListPacket.Entry buildCachedEntry(GameProfile profile, long geyserId) {
        GameProfileData data = GameProfileData.from(profile);
        SkinProvider.Cape cape = SkinProvider.getCachedCape(data.getCapeUrl());

        return buildEntryManually(
                profile.getId(),
                profile.getName(),
                geyserId,
                profile.getIdAsString(),
                SkinProvider.getCachedSkin(profile.getId()).getSkinData(),
                cape.getCapeId(),
                cape.getCapeData(),
                getLegacySkinGeometry("geometry.humanoid.custom" + (data.isAlex() ? "Slim" : "")),
                ""
        );
    }

    public static PlayerListPacket.Entry buildDefaultEntry(GameProfile profile, long geyserId) {
        return buildEntryManually(
                profile.getId(),
                profile.getName(),
                geyserId,
                profile.getIdAsString(),
                SkinProvider.STEVE_SKIN,
                SkinProvider.EMPTY_CAPE.getCapeId(),
                SkinProvider.EMPTY_CAPE.getCapeData(),
                getLegacySkinGeometry("geometry.humanoid"),
                ""
        );
    }

    public static PlayerListPacket.Entry buildEntryManually(UUID uuid, String username, long geyserId,
                                                            String skinId, byte[] skinData,
                                                            String capeId, byte[] capeData,
                                                            String geometryName, String geometryData) {
        SerializedSkin serializedSkin = SerializedSkin.of(
                skinId, geometryName, ImageData.of(skinData), Collections.emptyList(),
                ImageData.of(capeData), geometryData, "", true, false, false, capeId, uuid.toString()
        );

        PlayerListPacket.Entry entry = new PlayerListPacket.Entry(uuid);
        entry.setName(username);
        entry.setEntityId(geyserId);
        entry.setSkin(serializedSkin);
        entry.setXuid("");
        entry.setPlatformChatId("");
        entry.setTeacher(false);
        return entry;
    }

    @AllArgsConstructor
    @Getter
    public static class GameProfileData {
        private String skinUrl;
        private String capeUrl;
        private boolean alex;

        public static GameProfileData from(GameProfile profile) {
            try {
                GameProfile.Property skinProperty = profile.getProperty("textures");

                JsonObject skinObject = SkinProvider.GSON.fromJson(new String(Base64.getDecoder().decode(skinProperty.getValue()), Charsets.UTF_8), JsonObject.class);
                JsonObject textures = skinObject.getAsJsonObject("textures");

                JsonObject skinTexture = textures.getAsJsonObject("SKIN");
                String skinUrl = skinTexture.get("url").getAsString();

                boolean isAlex = skinTexture.has("metadata");

                String capeUrl = null;
                if (textures.has("CAPE")) {
                    JsonObject capeTexture = textures.getAsJsonObject("CAPE");
                    capeUrl = capeTexture.get("url").getAsString();
                }

                return new GameProfileData(skinUrl, capeUrl, isAlex);
            } catch (Exception exception) {
                if (!((GeyserConnector) Geyser.getConnector()).getConfig().getRemote().getAuthType().equals("offline")) {
                    Geyser.getLogger().debug("Got invalid texture data for " + profile.getName() + " " + exception.getMessage());
                }
                // return default skin with default cape when texture data is invalid
                return new GameProfileData("", "", false);
            }
        }
    }

    public static void requestAndHandleSkinAndCape(PlayerEntity entity, GeyserSession session,
                                                   Consumer<SkinProvider.SkinAndCape> skinAndCapeConsumer) {
        Geyser.getGeneralThreadPool().execute(() -> {
            GameProfileData data = GameProfileData.from(entity.getProfile());

            SkinProvider.requestSkinAndCape(entity.getUuid(), data.getSkinUrl(), data.getCapeUrl())
                    .whenCompleteAsync((skinAndCape, throwable) -> {
                        try {
                            SkinProvider.Skin skin = skinAndCape.getSkin();
                            SkinProvider.Cape cape = skinAndCape.getCape();

                            if (cape.isFailed() && SkinProvider.ALLOW_THIRD_PARTY_CAPES) {
                                cape = SkinProvider.getOrDefault(SkinProvider.requestUnofficialCape(
                                        cape, entity.getUuid(),
                                        entity.getUsername(), false
                                ), SkinProvider.EMPTY_CAPE, SkinProvider.UnofficalCape.VALUES.length * 3);
                            }

                            if (entity.getLastSkinUpdate() < skin.getRequestedOn()) {
                                entity.setLastSkinUpdate(skin.getRequestedOn());

                                if (session.getUpstream().isInitialized()) {
                                    PlayerListPacket.Entry updatedEntry = buildEntryManually(
                                            entity.getUuid(),
                                            entity.getUsername(),
                                            entity.getGeyserId(),
                                            entity.getUuid().toString(),
                                            skin.getSkinData(),
                                            cape.getCapeId(),
                                            cape.getCapeData(),
                                            getLegacySkinGeometry("geometry.humanoid.custom" + (data.isAlex() ? "Slim" : "")),
                                            ""
                                    );

                                    PlayerListPacket playerRemovePacket = new PlayerListPacket();
                                    playerRemovePacket.setType(PlayerListPacket.Type.REMOVE);
                                    playerRemovePacket.getEntries().add(updatedEntry);
                                    session.getUpstream().sendPacket(playerRemovePacket);

                                    PlayerListPacket playerAddPacket = new PlayerListPacket();
                                    playerAddPacket.setType(PlayerListPacket.Type.ADD);
                                    playerAddPacket.getEntries().add(updatedEntry);
                                    session.getUpstream().sendPacket(playerAddPacket);
                                }
                            }
                        } catch (Exception e) {
                            Geyser.getLogger().error("Failed getting skin for " + entity.getUuid(), e);
                        }

                        if (skinAndCapeConsumer != null) skinAndCapeConsumer.accept(skinAndCape);
                    });
        });
    }

    private static String getLegacySkinGeometry(String geometryName) {
        return "{\"geometry\" :{\"default\" :\"" + geometryName + "\"}}";
    }
}
