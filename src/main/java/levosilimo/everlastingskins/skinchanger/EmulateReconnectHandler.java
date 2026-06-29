package levosilimo.everlastingskins.skinchanger;

import levosilimo.everlastingskins.EternalSkins;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.network.play.server.*;
import net.minecraft.world.WorldServer;
import java.util.Collections;

public record EmulateReconnectHandler(EntityPlayerMP player) {

    public void emulateReconnect() {
        if (player == null || player.connection == null || SkinRestorer.server == null) return;

        try {
            SPacketPlayerListItem removeListItem = new SPacketPlayerListItem(
                    SPacketPlayerListItem.Action.REMOVE_PLAYER, Collections.singletonList(player)
            );
            SPacketPlayerListItem addListItem = new SPacketPlayerListItem(
                    SPacketPlayerListItem.Action.ADD_PLAYER, Collections.singletonList(player)
            );

            player.connection.sendPacket(removeListItem);
            player.connection.sendPacket(addListItem);

            if (player.world instanceof WorldServer worldServer) {
                SPacketRespawn respawnPacket = new SPacketRespawn(
                        player.dimension,
                        worldServer.getDifficulty(),
                        worldServer.getWorldInfo().getTerrainType(),
                        player.interactionManager.getGameType()
                );

                player.connection.sendPacket(respawnPacket);
                player.connection.setPlayerLocation(player.posX, player.posY, player.posZ, player.rotationYaw, player.rotationPitch);
                player.sendContainerToPlayer(player.inventoryContainer);
                player.setPlayerHealthUpdated();
            }

            if (player.world instanceof WorldServer worldServer) {
                SPacketDestroyEntities destroyPacket = new SPacketDestroyEntities(player.getEntityId());
                SPacketSpawnPlayer spawnPacket = new SPacketSpawnPlayer(player);

                for (EntityPlayerMP otherPlayer : SkinRestorer.server.getPlayerList().getPlayers()) {
                    if (otherPlayer != player && otherPlayer.connection != null && otherPlayer.dimension == player.dimension) {
                        if (worldServer.getPlayerChunkMap().isPlayerWatchingChunk(otherPlayer, player.chunkCoordX, player.chunkCoordZ)) {
                            otherPlayer.connection.sendPacket(removeListItem);
                            otherPlayer.connection.sendPacket(addListItem);
                            otherPlayer.connection.sendPacket(destroyPacket);
                            otherPlayer.connection.sendPacket(spawnPacket);
                        }
                    }
                }
            }
        } catch (Exception e) {
            EternalSkins.logger.error("[EverlastingSkins] Ошибка респавна при смене скина", e);
        }
    }
}
