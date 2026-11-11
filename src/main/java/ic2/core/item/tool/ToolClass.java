package ic2.core.item.tool;

import ic2.core.ref.BlockMaterial;
import ic2.core.ref.IC2Material;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import net.minecraft.world.level.block.Blocks;

public enum ToolClass implements IToolClass {
   Axe("axe", BlockMaterial.WOOD, BlockMaterial.PLANTS, BlockMaterial.VINE),
   Pickaxe("pickaxe", BlockMaterial.IRON, BlockMaterial.ANVIL, BlockMaterial.ROCK),
   Shears("shears", Blocks.COBWEB, Blocks.WOOL, Blocks.REDSTONE_WIRE, Blocks.TRIPWIRE, BlockMaterial.LEAVES),
   Shovel("shovel", Blocks.SNOW, Blocks.SNOW),
   Sword("sword", Blocks.COBWEB, BlockMaterial.PLANTS, BlockMaterial.VINE, BlockMaterial.CORAL, BlockMaterial.LEAVES, BlockMaterial.GOURD),
   Hoe(null, Blocks.DIRT, Blocks.GRASS_BLOCK, Blocks.MYCELIUM),
   Wrench("wrench", IC2Material.MACHINE, IC2Material.PIPE),
   WireCutter("wire_cutter", IC2Material.CABLE),
   Crowbar("crowbar", Blocks.RAIL, Blocks.ACTIVATOR_RAIL, Blocks.DETECTOR_RAIL, Blocks.POWERED_RAIL);

   public final String name;
   public final Set<Object> whitelist;
   public final Set<Object> blacklist;

   private ToolClass(String name, Object... whitelist) {
      this(name, whitelist, new Object[0]);
   }

   private ToolClass(String name, Object[] whitelist, Object[] blacklist) {
      this.name = name;
      this.whitelist = new HashSet<>(Arrays.asList(whitelist));
      this.blacklist = new HashSet<>(Arrays.asList(blacklist));
   }

   @Override
   public String getName() {
      return this.name;
   }

   @Override
   public Set<Object> getWhitelist() {
      return this.whitelist;
   }

   @Override
   public Set<Object> getBlacklist() {
      return this.blacklist;
   }
}
