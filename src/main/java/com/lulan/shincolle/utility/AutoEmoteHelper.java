package com.lulan.shincolle.utility;

import com.lulan.shincolle.capability.CapaTeitoku;
import com.lulan.shincolle.command.ShipCmdEmotes;
import com.lulan.shincolle.network.S2CSpawnParticle;
import com.lulan.shincolle.proxy.CommonProxy;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.text.TextComponentTranslation;
import net.minecraft.world.WorldServer;
import net.minecraftforge.fml.common.network.NetworkRegistry;

import java.util.*;

public class AutoEmoteHelper {
    private static final long COOLDOWN_MS = 3000; // 3초 쿨다운
    private static final long SAME_EMOTE_COOLDOWN_MS = 10000; // 같은 이모티콘 10초 쿨다운

    private final Map<UUID, Long> playerLastEmoteTime = new HashMap<>();
    private final Map<UUID, Map<Integer, Long>> playerEmoteHistory = new HashMap<>();

    public static final AutoEmoteHelper AUTO_EMOTE_MANAGER = new AutoEmoteHelper();


    /**
     * 플레이어의 채팅 메시지를 분석하여 자동 이모티콘 표시
     */
    public void tryShowAutoEmote(EntityPlayerMP player, WorldServer world, String message) {
        UUID playerId = player.getUniqueID();

        // 비활성화된 플레이어 체크 (issue #32: persisted AutoEmoteOff flag of the player capability)
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa != null && capa.isAutoEmoteOff()) {
            return;
        }

        // 쿨다운 체크
        long currentTime = System.currentTimeMillis();
        Long lastEmoteTime = playerLastEmoteTime.get(playerId);

        if (lastEmoteTime != null && currentTime - lastEmoteTime < COOLDOWN_MS) {
            return;
        }

        // 이모티콘 찾기
        int emoteId = findEmote(message);
        if (emoteId == -1) {
            return;
        }

        // 같은 이모티콘 쿨다운 체크
        Map<Integer, Long> emoteHistory = playerEmoteHistory.computeIfAbsent(playerId, k -> new HashMap<>());
        Long lastSameEmoteTime = emoteHistory.get(emoteId);

        if (lastSameEmoteTime != null && currentTime - lastSameEmoteTime < SAME_EMOTE_COOLDOWN_MS) {
            return;
        }

        // 이모티콘 표시
        showEmoteForPlayer(player, world, emoteId);

        // 기록 업데이트
        playerLastEmoteTime.put(playerId, currentTime);
        emoteHistory.put(emoteId, currentTime);

    }

    /** whole-word emote lookup for a chat message (issue #32); -1 = none */
    static int findEmote(String message) {
        return ShipCmdEmotes.EMOTE_DATA.findBestEmoteForMessageStrict(message);
    }

    public void toggleAutoEmote(EntityPlayer player) {
        CapaTeitoku capa = CapaTeitoku.getTeitokuCapability(player);
        if (capa == null) return;

        if (capa.isAutoEmoteOff()) {
            capa.setAutoEmoteOff(false);
            player.sendStatusMessage(new TextComponentTranslation("chat.shincolle:autoemote.enabled"), false);
        } else {
            capa.setAutoEmoteOff(true);
            player.sendStatusMessage(new TextComponentTranslation("chat.shincolle:autoemote.disabled"), false);
        }
    }

    private void showEmoteForPlayer(EntityPlayerMP player, WorldServer world, int id) {

        // 위치 및 엔티티 타입 결정
        float posX, posY, posZ, height;
        int entityType;  // 0: none, 1: EntityLivingBase, 2: block

        // 송신자가 생명체인 경우
        if (player == null) {
            return;
        }
        entityType = 1;

        posX = (float) player.posX;
        posY = (float) player.posY;
        posZ = (float) player.posZ;
        height = player.height * 0.65F;

        // 네트워크로 이모트 파티클 전송
        NetworkRegistry.TargetPoint targetPoint = new NetworkRegistry.TargetPoint(world.provider.getDimension(), posX, posY, posZ, 64D);
        CommonProxy.channelP.sendToAllAround(
                new S2CSpawnParticle(player, 36, height, entityType, id),
                targetPoint
        );
    }
}

