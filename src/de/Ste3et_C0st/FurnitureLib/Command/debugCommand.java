package de.Ste3et_C0st.FurnitureLib.Command;

import java.util.Random;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.World;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

import de.Ste3et_C0st.FurnitureLib.Crafting.Project;
import de.Ste3et_C0st.FurnitureLib.NBT.MathHelper;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureLib;
import de.Ste3et_C0st.FurnitureLib.main.FurnitureManager;
import de.Ste3et_C0st.FurnitureLib.main.ObjectID;
import de.Ste3et_C0st.FurnitureLib.main.Type.SQLAction;
import net.md_5.bungee.api.ChatMessageType;
import net.md_5.bungee.api.chat.ComponentBuilder;

public class debugCommand extends iCommand{
	
	public debugCommand(String subCommand, String ...args) {
		super(subCommand);
	}

	@Override
	public void execute(CommandSender sender, String[] args) {
		if(args.length == 2) {
			if(args[1].equalsIgnoreCase("database")) {
				AtomicInteger aInt = new AtomicInteger(100000);
				AtomicInteger aInt2 = new AtomicInteger(300);
				World w = Bukkit.getWorlds().get(0);
				int i = FurnitureManager.getInstance().getProjects().size() - 1;
				sender.sendMessage("§7Database Manipulation start");
				Player player = (Player) sender;
				UUID uuid = player.getUniqueId();
				new BukkitRunnable() {
					@Override
					public void run() {
						if(aInt2.get() > 0) {
							while (aInt2.getAndDecrement() > 0) {
								double x = MathHelper.a(new Random(), -1000d, 1000d);
								double y = MathHelper.a(new Random(), -1000d, 1000d);
								double z = MathHelper.a(new Random(), -1000d, 1000d);
								Project pro = FurnitureManager.getInstance().getProjects().stream().filter(proj -> !proj.getName().equalsIgnoreCase("billboard")).collect(Collectors.toList()).get((int) MathHelper.a(new Random(), 0, i));
								ObjectID obj = new ObjectID(pro.getName(), pro.getPlugin().getName(), new Location(w, x, y, z));
								FurnitureLib.getInstance().spawn(obj.getProjectOBJ(), obj);
								obj.setSQLAction(SQLAction.SAVE);
								obj.setUUID(uuid);
								player.spigot().sendMessage(ChatMessageType.ACTION_BAR, new ComponentBuilder("§2" +aInt.get() + "§7/§e" + 100000).create());
							}
						}else {
							if(aInt.get() > 0) {
								aInt2.set(300);
								aInt.set(aInt.get() - 300);
							}else {
								cancel();
								sender.sendMessage("§7Database Manipulation §2finish");
							}
						}
					}
				}.runTaskTimer(FurnitureLib.getInstance(), 0, 20*1);
			}
		}else {
			if(args.length!=1){sender.sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.WrongArgument"));return;}
			if(sender instanceof Player){
				if(hasCommandPermission(sender)){
					command.playerList.add((Player) sender);
					sender.sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.DebugModeEntered"));
				}else{
					sender.sendMessage(FurnitureLib.getInstance().getLangManager().getString("message.NoPermissions"));
				}
			}
		}
	}
}
