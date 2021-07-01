package com.bilicraft.journeychest;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.Block;
import org.bukkit.block.Container;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.entity.HumanEntity;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.util.BlockIterator;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

public final class JourneyChest extends JavaPlugin implements Listener {
    private List<Location> locationList = new ArrayList<>();
    private File storageFolder = new File(getDataFolder(),"records");


    @Override
    public void onEnable() {
        // Plugin startup logic
        saveDefaultConfig();
        storageFolder = new File(getDataFolder(),"records");
        Bukkit.getPluginManager().registerEvents(this,this);
        loadLocations();
    }

    @Override
    public void onDisable() {
        // Plugin shutdown logic

    }

    private void loadLocations(){
        //noinspection unchecked
        locationList = (List<Location>) getConfig().getList("containers",new ArrayList<>());
    }

    private void saveLocations(){
        getConfig().set("containers",locationList);
        saveConfig();
    }
    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    public void onItemPost(InventoryCloseEvent event){
       Location location = event.getInventory().getLocation();
       if(location == null){
           return;
       }
       if(!locationList.contains(location)){
           return;
       }
       if(!(event.getInventory().getHolder() instanceof Container)){
           return;
       }
       if(Arrays.stream(event.getInventory().getStorageContents()).allMatch(itemStack -> itemStack == null || itemStack.getType() == Material.AIR)){
           return;
       }
       //Re-Tweaks
       List<ItemStack> contents = Arrays.stream(event.getInventory().getStorageContents())
                .filter(itemStack -> itemStack != null && itemStack.getType() != Material.AIR)
                .collect(Collectors.toList());
       Player player = (Player) event.getPlayer();
       File dbFile = new File(storageFolder, player.getUniqueId() +".yml");
       YamlConfiguration database = YamlConfiguration.loadConfiguration(dbFile);
       String sectionName = String.valueOf(System.currentTimeMillis());
       database.set(sectionName+".items", contents);
       database.set(sectionName+".username", player.getName());
       database.set(sectionName+".location", event.getPlayer().getLocation());
        try {
            database.save(dbFile);
            player.sendMessage(ChatColor.AQUA+":3 你的祝福我们收到了哦");
            event.getInventory().clear();
        } catch (IOException exception) {
            exception.printStackTrace();
            player.sendMessage(ChatColor.RED+":( 出了一点小差错，祝福接收失败了呢");
        }
    }


    @EventHandler(ignoreCancelled = true,priority = EventPriority.MONITOR)
    public void onContainerBreak(BlockBreakEvent event){
        if(!(event.getBlock().getState() instanceof Container)){
            return;
        }
        if(!locationList.contains(event.getBlock().getLocation())){
            return;
        }
        if(!event.getPlayer().hasPermission("journeychest.admin")){
            event.setCancelled(true);
            event.getPlayer().sendMessage("你不能破坏这个 JourneyChest，请联系服务器管理员");
            return;
        }
        locationList.remove(event.getBlock().getLocation());
        event.getPlayer().sendMessage("啊... 拆掉了呢....");
    }


    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if(sender.hasPermission("journeychest.admin") && sender instanceof Player){
            Player player = (Player)sender;
            BlockIterator bIt = new BlockIterator(player, 10);
            while (bIt.hasNext()){
                Block block = bIt.next();
                if(block.getState() instanceof Container){
                    if(!locationList.contains(block.getLocation())){
                        locationList.add(block.getLocation());
                        saveLocations();
                        player.sendMessage("已保存");
                        return true;
                    }
                }
            }
            player.sendMessage("你需要看向一个容器");
            return true;
        }

        return false;
    }
}
