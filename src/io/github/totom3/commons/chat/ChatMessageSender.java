package io.github.totom3.commons.chat;

import com.google.common.base.Preconditions;
import net.minecraft.server.v1_8_R3.PacketPlayOutChat;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.entity.Player;

/**
 *
 * @author Totom3
 */
public final class ChatMessageSender {

    public static void send(ChatComponent comp, Player player) {
	send(comp, player, ChatMessageType.COMMAND);
    }

    public static void send(ChatComponent comp, Player player, ChatMessageType type) {

	Preconditions.checkNotNull(player);
	Preconditions.checkNotNull(type);
	if (comp == null) {
	    comp = new ChatComponent();
	}

	PacketPlayOutChat packet = new PacketPlayOutChat(comp.toNMS());
	((CraftPlayer) player).getHandle().playerConnection.sendPacket(packet);
    }

    public static void send(ChatComponent comp, CommandSender sender) {
	if (sender instanceof Player) {
	    send(comp, (Player) sender);
	} else {
	    sender.sendMessage(comp.toPlainText());
	}

    }

    private ChatMessageSender() {
    }

    public static enum ChatMessageType {

	CHAT,
	COMMAND,
	ACTION_BAR
    }
}
