package com.zerosio.utility;

import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class ChatUtils {

    public static void sendCopyableText(ProxiedPlayer player, String label, String value) {
        TextComponent component = new TextComponent(label + " §f" + value);
        component.setClickEvent(new ClickEvent(ClickEvent.Action.COPY_TO_CLIPBOARD, value));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder("§7Click to copy").create()));
        component.setUnderlined(true);
        player.sendMessage(component);
    }
    
    public static void sendClickableText(ProxiedPlayer player, String text, String hover, String value, ClickEvent.Action e) {
        TextComponent component = new TextComponent(text);
        component.setClickEvent(new ClickEvent(e, value));
        component.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
                new ComponentBuilder(hover).create()));
        //component.setUnderlined(true);
        player.sendMessage(component);
    }
}
