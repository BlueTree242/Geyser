/*
 * Copyright (c) 2019-2020 GeyserMC. http://geysermc.org
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 * @author GeyserMC
 * @link https://github.com/GeyserMC/Geyser
 */

package org.geysermc.connector.network.session.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import net.minidev.json.JSONObject;
import org.geysermc.floodgate.util.DeviceOs;
import org.geysermc.floodgate.util.InputMode;
import org.geysermc.floodgate.util.RawSkin;
import org.geysermc.floodgate.util.UiProfile;

import java.util.Base64;
import java.util.UUID;

@JsonIgnoreProperties(ignoreUnknown = true)
@Getter
public final class BedrockClientData {
    @JsonIgnore
    private JSONObject jsonData;

    @JsonProperty(value = "GameVersion")
    private String gameVersion;
    @JsonProperty(value = "ServerAddress")
    private String serverAddress;
    @JsonProperty(value = "ThirdPartyName")
    private String username;
    @JsonProperty(value = "LanguageCode")
    private String languageCode;

    @JsonProperty(value = "SkinId")
    private String skinId;
    @JsonProperty(value = "SkinData")
    private String skinData;
    @JsonProperty(value = "SkinImageHeight")
    private int skinImageHeight;
    @JsonProperty(value = "SkinImageWidth")
    private int skinImageWidth;
    @JsonProperty(value = "CapeId")
    private String capeId;
    @JsonProperty(value = "CapeData")
    private byte[] capeData;
    @JsonProperty(value = "CapeImageHeight")
    private int capeImageHeight;
    @JsonProperty(value = "CapeImageWidth")
    private int capeImageWidth;
    @JsonProperty(value = "CapeOnClassicSkin")
    private boolean capeOnClassicSkin;
    @JsonProperty(value = "SkinResourcePatch")
    private String geometryName;
    @JsonProperty(value = "SkinGeometryData")
    private String geometryData;
    @JsonProperty(value = "PersonaSkin")
    private boolean personaSkin;
    @JsonProperty(value = "PremiumSkin")
    private boolean premiumSkin;

    @JsonProperty(value = "DeviceId")
    private String deviceId;
    @JsonProperty(value = "DeviceModel")
    private String deviceModel;
    @JsonProperty(value = "DeviceOS")
    private DeviceOs deviceOS;
    @JsonProperty(value = "UIProfile")
    private UiProfile uiProfile;
    @JsonProperty(value = "GuiScale")
    private int guiScale;
    @JsonProperty(value = "CurrentInputMode")
    private InputMode currentInputMode;
    @JsonProperty(value = "DefaultInputMode")
    private InputMode defaultInputMode;
    @JsonProperty("PlatformOnlineId")
    private String platformOnlineId;
    @JsonProperty(value = "PlatformOfflineId")
    private String platformOfflineId;
    @JsonProperty(value = "SelfSignedId")
    private UUID selfSignedId;
    @JsonProperty(value = "ClientRandomId")
    private long clientRandomId;

    @JsonProperty(value = "ArmSize")
    private String armSize;
    @JsonProperty(value = "SkinAnimationData")
    private String skinAnimationData;
    @JsonProperty(value = "SkinColor")
    private String skinColor;
    @JsonProperty(value = "ThirdPartyNameOnly")
    private boolean thirdPartyNameOnly;

    public void setJsonData(JSONObject data) {
        if (this.jsonData != null && data != null) {
            this.jsonData = data;
        }
    }

    /**
     * Taken from https://github.com/NukkitX/Nukkit/blob/master/src/main/java/cn/nukkit/network/protocol/LoginPacket.java<br>
     * Internally only used for Skins, but can be used for Capes too
     */
    public RawSkin getImage(String name) {
        if (jsonData == null || !jsonData.containsKey(name + "Data")) return null;
        byte[] image = Base64.getDecoder().decode(jsonData.getAsString(name + "Data"));
        if (jsonData.containsKey(name + "ImageWidth") && jsonData.containsKey(name + "ImageHeight")) {
            return new RawSkin(
                    (int) jsonData.getAsNumber(name + "ImageWidth"),
                    (int) jsonData.get(name + "ImageHeight"),
                    image
            );
        }
        return getLegacyImage(image);
    }

    private static RawSkin getLegacyImage(byte[] imageData) {
        if (imageData == null) return null;
        // width * height * 4 (rgba)
        switch (imageData.length) {
            case 8192:
                return new RawSkin(64, 32, imageData);
            case 16384:
                return new RawSkin(64, 64, imageData);
            case 32768:
                return new RawSkin(64, 128, imageData);
            case 65536:
                return new RawSkin(128, 128, imageData);
            default:
                throw new IllegalArgumentException("Unknown legacy skin size");
        }
    }
}
