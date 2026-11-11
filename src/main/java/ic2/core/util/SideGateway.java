package ic2.core.util;

import net.neoforged.fml.loading.FMLEnvironment;

public final class SideGateway<T> {
   private final T clientInstance;
   private final T serverInstance;

   public SideGateway(String serverClass, String clientClass) {
      try {
         if (FMLEnvironment.getDist().isClient()) {
            this.clientInstance = (T)Class.forName(clientClass).newInstance();
         } else {
            this.clientInstance = null;
         }

         this.serverInstance = (T)Class.forName(serverClass).newInstance();
      } catch (Exception var4) {
         throw new RuntimeException(var4);
      }
   }

   public T get(boolean simulating) {
      return simulating ? this.serverInstance : this.clientInstance;
   }
}
