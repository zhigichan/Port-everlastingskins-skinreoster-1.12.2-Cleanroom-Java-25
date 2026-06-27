package levosilimo.everlastingskins.skinchanger;

import com.mojang.authlib.properties.Property;
import levosilimo.everlastingskins.EverlastingSkins;
import net.minecraft.command.CommandBase;
import net.minecraft.command.CommandException;
import net.minecraft.command.ICommandSender;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.server.MinecraftServer;
import net.minecraft.util.text.TextComponentString;
import java.util.Collections;
import java.util.List;

public class SkinCommand extends CommandBase {

    @Override
    public String getName() {
        return "skin";
    }

    @Override
    public String getUsage(ICommandSender sender) {
        return "§6[EverlastingSkins]§c /skin set <ник> §7или §f/skin clear";
    }

    @Override
    public void execute(MinecraftServer server, ICommandSender sender, String[] args) throws CommandException {
        String usageMessage = "§6[EverlastingSkins]§c Использование:\n" +
                "§7» §f/skin set <лицензионный_ник>\n" +
                "§7» §f/skin clear §7(сбросить на дефолт)";

        if (args == null || args.length == 0) {
            sender.sendMessage(new TextComponentString(usageMessage));
            return;
        }

        EntityPlayerMP player = (EntityPlayerMP) sender.getCommandSenderEntity();
        if (player == null) return;

        String action = args[0].toLowerCase();

        if (action.equals("clear")) {
            sender.sendMessage(new TextComponentString("§6[EverlastingSkins]§f §aОчищаем ваш скин..."));
            EverlastingSkins.clearPlayerSkinInDatabase(player.getUniqueID());

            EverlastingSkins.skinCommandExecutor.submit(() -> {
                Property defaultSkin = MojangSkinProvider.getSkin(player.getName());
                if (SkinRestorer.server != null) {
                    SkinRestorer.server.addScheduledTask(() -> {
                        SkinRestorer.applySkin(player, defaultSkin);
                        new EmulateReconnectHandler(player).emulateReconnect();
                        sender.sendMessage(new TextComponentString("§6[EverlastingSkins]§f §aСкин успешно сброшен!"));
                    });
                }
            });
            return;
        }

        if (action.equals("set") && args.length >= 2) {
            String targetName = args[1].trim();
            sender.sendMessage(new TextComponentString("§6[EverlastingSkins]§f §aЗапрос обрабатывается..."));

            EverlastingSkins.skinCommandExecutor.submit(() -> {
                try {
                    Property newSkin = MojangSkinProvider.getSkin(targetName);

                    if (newSkin != null) {
                        EverlastingSkins.saveSkinToDatabase(player.getUniqueID(), targetName, newSkin);

                        if (SkinRestorer.server != null) {
                            SkinRestorer.server.addScheduledTask(() -> {
                                if (player.connection != null) {
                                    SkinRestorer.applySkin(player, newSkin);
                                    new EmulateReconnectHandler(player).emulateReconnect();
                                    sender.sendMessage(new TextComponentString("§6[EverlastingSkins]§f §aНовый скин успешно применён!"));
                                }
                            });
                        }
                    } else {
                        sender.sendMessage(new TextComponentString("§6[EverlastingSkins]§c Скин для ника " + targetName + " не найден в Mojang API."));
                    }
                } catch (Exception ignored) {}
            });
            return;
        }

        sender.sendMessage(new TextComponentString(usageMessage));
    }

    @Override
    public List<String> getTabCompletions(MinecraftServer server, ICommandSender sender, String[] args, net.minecraft.util.math.BlockPos targetPos) {
        if (args.length == 1) return getListOfStringsMatchingLastWord(args, "set", "clear");
        if (args.length == 2 && args[0].equalsIgnoreCase("set")) return getListOfStringsMatchingLastWord(args, server.getOnlinePlayerNames());
        return Collections.emptyList();
    }
}
