package ic2.core.ref;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IBlockModelProvider {
   @OnlyIn(Dist.CLIENT)
   void registerModels(BlockName var1);
}
