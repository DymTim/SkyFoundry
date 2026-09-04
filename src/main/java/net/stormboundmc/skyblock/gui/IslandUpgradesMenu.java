package net.stormboundmc.skyblock.gui;

import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.stormboundmc.skyblock.StormboundSkyblock;
import net.stormboundmc.skyblock.gui.holder.IslandUpgradesMenuHolder;
import net.stormboundmc.skyblock.island.Island;
import net.stormboundmc.skyblock.island.IslandManager;
import net.stormboundmc.skyblock.island.IslandRole;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.entity.Player;
import org.bukkit.inventory.Inventory;
import org.bukkit.inventory.ItemStack;
import org.bukkit.inventory.meta.ItemMeta;
import java.util.ArrayList;
import java.util.List;

public final class IslandUpgradesMenu {
    private final StormboundSkyblock plugin;
    private final IslandManager islandManager;
    private final MiniMessage mini = MiniMessage.miniMessage();
    public IslandUpgradesMenu(StormboundSkyblock plugin, IslandManager islandManager) { this.plugin=plugin; this.islandManager=islandManager; }

    public void open(Player player) {
        Island island=islandManager.getIsland(player.getUniqueId()).orElse(null); if(island==null)return;
        IslandRole role=islandManager.getRole(player.getUniqueId()).orElse(IslandRole.MEMBER);
        boolean editable=role==IslandRole.OWNER||role==IslandRole.CO_OWNER;
        IslandUpgradesMenuHolder holder=new IslandUpgradesMenuHolder();
        Inventory inv=Bukkit.createInventory(holder,45,parse(plugin.getConfig().getString("gui.upgrades.title","<gold><bold>⚙ ISLAND UPGRADES</bold></gold>"))); holder.setInventory(inv);
        ItemStack filler=item(Material.GRAY_STAINED_GLASS_PANE," ",List.of()); for(int i=0;i<45;i++)inv.setItem(i,filler);
        Material accent=material("gui.upgrades.accents.material",Material.ORANGE_STAINED_GLASS_PANE); ItemStack a=item(accent," ",List.of()); for(int slot:plugin.getConfig().getIntegerList("gui.upgrades.accents.slots")) if(slot>=0&&slot<45)inv.setItem(slot,a);
        Integer nextSize=islandManager.getNextSize(island);
        inv.setItem(plugin.getConfig().getInt("gui.upgrades.buttons.size.slot",20), item(material("gui.upgrades.buttons.size.material",Material.GRASS_BLOCK),"<yellow><bold>Island Size</bold></yellow>",upgradeLore(island.getSize()+" x "+island.getSize(), nextSize==null?null:nextSize+" x "+nextSize,editable)));
        Integer nextMembers=islandManager.getNextMemberLimit(island);
        inv.setItem(plugin.getConfig().getInt("gui.upgrades.buttons.member-limit.slot",24), item(material("gui.upgrades.buttons.member-limit.material",Material.PLAYER_HEAD),"<yellow><bold>Member Limit</bold></yellow>",upgradeLore(island.getMemberLimit()+" members",nextMembers==null?null:nextMembers+" members",editable)));
        inv.setItem(plugin.getConfig().getInt("gui.upgrades.buttons.back.slot",40),item(Material.ARROW,"<yellow>Back</yellow>",List.of("<gray>Return to the island menu.</gray>")));
        inv.setItem(plugin.getConfig().getInt("gui.upgrades.buttons.close.slot",44),item(Material.BARRIER,"<red>Close</red>",List.of()));
        player.openInventory(inv);
    }
    private List<String> upgradeLore(String current,String next,boolean editable){ List<String> l=new ArrayList<>(); l.add("<gray>Current: <yellow>"+current+"</yellow></gray>"); if(next==null){l.add("");l.add("<green>Maximum upgrade reached.</green>");}else{l.add("<gray>Next: <green>"+next+"</green></gray>");l.add("");l.add(editable?"<gold>Click to upgrade.</gold>":"<dark_gray>Owner or Co-Owner required.</dark_gray>");} return l; }
    private Material material(String path,Material fallback){String n=plugin.getConfig().getString(path,fallback.name());try{return Material.valueOf(n.toUpperCase());}catch(Exception e){return fallback;}}
    private ItemStack item(Material m,String name,List<String> lore){ItemStack i=new ItemStack(m);ItemMeta meta=i.getItemMeta();meta.displayName(parse(name));meta.lore(lore.stream().map(this::parse).toList());i.setItemMeta(meta);return i;}
    private Component parse(String s){return mini.deserialize(s==null?"":s);}
}
