package ic2.core.gui.dynamic;

public interface IGuiValueProvider {
   double getGuiValue(String var1);

   public interface IActiveGuiValueProvider extends IGuiValueProvider {
      boolean isGuiValueActive(String var1);
   }
}
