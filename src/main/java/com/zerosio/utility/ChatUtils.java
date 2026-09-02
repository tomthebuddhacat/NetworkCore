package com.zerosio.utility;


import com.velocitypowered.api.command.CommandSource;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.event.HoverEvent;

public class ChatUtils {

    public static void sendCopyableText(CommandSource commandSource, String label, String value) {
        Component component = Component.text(label + " " + value)
                .clickEvent(ClickEvent.copyToClipboard(value))
                .hoverEvent(HoverEvent.showText(Component.text("Click to copy")));

        commandSource.sendMessage(component);
    }

    public static void sendClickableText(CommandSource commandSource, String text, String hover, String value, ClickEvent clickEvent) {
        Component component = Component.text(text)
                .clickEvent(clickEvent)
                .hoverEvent(HoverEvent.showText(Component.text(hover)));

        commandSource.sendMessage(component);
    }
}
