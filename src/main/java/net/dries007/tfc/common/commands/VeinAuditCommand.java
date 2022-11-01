package net.dries007.tfc.common.commands;

import com.mojang.brigadier.Command;
import com.mojang.brigadier.arguments.BoolArgumentType;
import com.mojang.brigadier.builder.LiteralArgumentBuilder;
import net.minecraft.commands.CommandSourceStack;
import net.minecraft.commands.Commands;

import net.dries007.tfc.util.VeinAuditor;

public class VeinAuditCommand
{
    public static LiteralArgumentBuilder<CommandSourceStack> create()
    {
        return Commands.literal("audit_veins")
            .requires(source -> source.hasPermission(2))
            .then(Commands.literal("enable")
                .then(Commands.argument("enable", BoolArgumentType.bool())
                    .executes(context -> {
                        VeinAuditor.setEnabled(context.getArgument("enable", boolean.class));
                        return Command.SINGLE_SUCCESS;
                    })
                )
            )
            .then(Commands.literal("print")
                .executes(context -> {
                    VeinAuditor.print(context.getSource().getLevel());
                    return Command.SINGLE_SUCCESS;
                })
            );
    }
}
