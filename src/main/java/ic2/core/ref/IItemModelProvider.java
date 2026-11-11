package ic2.core.ref;

import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;

public interface IItemModelProvider {
   @OnlyIn(Dist.CLIENT)
   void registerModels(ItemName var1);
}
