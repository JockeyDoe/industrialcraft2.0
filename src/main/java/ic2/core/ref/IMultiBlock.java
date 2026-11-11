package ic2.core.ref;

import ic2.core.block.state.IIdProvider;
import net.minecraft.world.level.block.state.BlockState;

public interface IMultiBlock<T extends IIdProvider> extends IMultiItem<T> {
   BlockState getState(T var1);

   BlockState getState(String var1);
}
