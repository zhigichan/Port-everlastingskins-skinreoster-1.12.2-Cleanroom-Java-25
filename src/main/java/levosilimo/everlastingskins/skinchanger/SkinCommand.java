package levosilimo.everlastingskins.skinchanger;

import com.mojang.authlib.properties.Property;
import levosilimo.everlastingskins.EternalSkins;
import net.minecraft.command.CommandBase;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.text.TextComponentString;

import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public class SkinCommand extends CommandBase {

    @Override
    public String getName() {
        return "skin";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "§cИспользование: /skin <никнейм_скина> ИЛИ /skin <игрок> <никнейм_скина>";
    }

    @Override
    public int getRequiredPermissionLevel() {
        return 0;
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) {
        if (args.length < 1) {
            sender.sendMessage(new TextComponentString(getUsage(sender)));
            return;
        }

        EntityPlayerMP targetPlayer;
        String targetSkinName;

        if (args.length >= 2) {
            if (!sender.canUseCommand(2, this.getName())) {
                sender.sendMessage(new TextComponentString("§c[EternalSkins] У вас нет прав для изменения скина другим игрокам!"));
                return;
            }

            targetPlayer = server.getPlayerList().getPlayerByUsername(args[0]);
            if (targetPlayer == null) {
                sender.sendMessage(new TextComponentString("§c[EternalSkins] Игрок " + args[0] + " не найден в сети!"));
                return;
            }
            targetSkinName = args[1].trim();
        } else {
            if (!(sender instanceof EntityPlayerMP playerMP)) {
                sender.sendMessage(new TextComponentString("§cЭту команду для себя можно использовать только из игры!"));
                return;
            }
            targetPlayer = playerMP;
            targetSkinName = args[0].trim();
        }

        final EntityPlayerMP finalTarget = targetPlayer;
        final String finalSkinName = targetSkinName;

        sender.sendMessage(new TextComponentString("§6[EternalSkins]§f Запрос на скин " + finalSkinName + " отправлен. Пожалуйста, подождите..."));

        EternalSkins.skinCommandExecutor.submit(() -> {
            try {
                Property newSkin = MojangSkinProvider.getSkin(finalSkinName);
                String skinHash = "";
                boolean hashError = false;

                if (newSkin != null) {
                    try {
                        String json = new String(java.util.Base64.getDecoder().decode(newSkin.getValue()));
                        int index = json.indexOf("/texture/");
                        if (index != -1) {
                            int start = index + 9;
                            int end = json.indexOf("\"", start);
                            if (end != -1) {
                                skinHash = json.substring(start, end);
                            }
                        } else {
                            skinHash = newSkin.getValue().substring(0, Math.min(newSkin.getValue().length(), 32));
                        }
                    } catch (Exception ignored) {
                        hashError = true;
                    }
                }

                if (skinHash.isEmpty() || "error_hash".equals(skinHash)) {
                    hashError = true;
                }

                final String finalSkinHash = skinHash;
                final boolean finalHashError = hashError;

                if (SkinRestorer.server != null) {
                    SkinRestorer.server.addScheduledTask(() -> {
                        if (finalTarget.connection != null) {

                            if (finalTarget.getGameProfile() != null) {
                                finalTarget.getGameProfile().getProperties().removeAll("textures");
                            }

                            if (newSkin != null && !finalHashError) {
                                EternalSkins.saveSkinData(finalTarget.getUniqueID(), finalSkinName, finalSkinHash, newSkin);
                                SkinRestorer.applySkin(finalTarget, newSkin);
                                sender.sendMessage(new TextComponentString("§6[EternalSkins]§f §aСкин " + finalSkinName + " успешно применён к игроку " + finalTarget.getName() + "!"));
                                if (finalTarget != sender) {
                                    finalTarget.sendMessage(new TextComponentString("§6[EternalSkins]§f §aАдминистратор изменил ваш скин на " + finalSkinName));
                                }
                            } else {
                                sender.sendMessage(new TextComponentString("§6[EternalSkins]§c Ошибка: у никнейма " + finalSkinName + " нет скина или сервера Mojang временно ограничили запросы (Rate Limit)."));
                            }

                            new EmulateReconnectHandler(finalTarget).emulateReconnect();
                        }
                    });
                }
            } catch (Exception e) {
                EternalSkins.logger.error(e);
            }
        });
    }


    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, @Nullable BlockPos targetPos) {
        if (args.length == 1) {
            return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        }
        return Collections.emptyList();
    }
}
