package ic2.core.ref;

import ic2.core.block.ITeBlock;
import ic2.core.block.TeBlockRegistry;
import ic2.core.util.Tuple;
import java.util.AbstractCollection;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.Map.Entry;
import net.minecraft.resources.Identifier;
import net.minecraft.world.level.block.state.properties.Property;

public class MetaTeBlockProperty extends Property<MetaTeBlock> {
   private final Collection<MetaTeBlock> allowedValues;
   private final String resourceLocationName;
   private static final Map<Identifier, Tuple.T2<Integer, List<MetaTeBlockProperty.MetaTePair>>> resourceToTeBlock = new HashMap<>();
   private static final Map<ITeBlock, MetaTeBlockProperty.MetaTePair> teResourceMapping = new IdentityHashMap<>();
   public static final MetaTeBlock invalid;

   public MetaTeBlockProperty(final Identifier identifier) {
      super("type", MetaTeBlock.class);
      this.resourceLocationName = identifier.toString();
      this.allowedValues = new AbstractCollection<MetaTeBlock>() {
         private final int trueSize;

         {
            Objects.requireNonNull(MetaTeBlockProperty.this);
            this.trueSize = (Integer)MetaTeBlockProperty.resourceToTeBlock.get(identifier).a;
         }

         @Override
         public Iterator<MetaTeBlock> iterator() {
            return new Iterator<MetaTeBlock>() {
               private int teBlockIdx;
               private boolean active;
               private final List<MetaTeBlockProperty.MetaTePair> teBlockMap;
               private final int allTeBlockSize;

               {
                  Objects.requireNonNull(<VAR_NAMELESS_ENCLOSURE>);
                  this.teBlockMap = (List<MetaTeBlockProperty.MetaTePair>)MetaTeBlockProperty.resourceToTeBlock.get(identifier).b;
                  this.allTeBlockSize = this.teBlockMap.size();
               }

               @Override
               public boolean hasNext() {
                  return this.teBlockIdx < this.allTeBlockSize;
               }

               public MetaTeBlock next() {
                  if (!this.hasNext()) {
                     throw new NoSuchElementException();
                  } else {
                     MetaTeBlockProperty.MetaTePair teBlockPair = this.teBlockMap.get(this.teBlockIdx);
                     MetaTeBlock ret = teBlockPair.getState(this.active);
                     if (!this.active && teBlockPair.hasActive()) {
                        this.active = true;
                     } else {
                        this.active = false;
                        this.teBlockIdx++;
                     }

                     return ret;
                  }
               }

               @Override
               public void remove() {
                  throw new UnsupportedOperationException("Cannot remove a MetaTeBlock state.");
               }
            };
         }

         @Override
         public int size() {
            return this.trueSize;
         }
      };
   }

   public String getName() {
      return "type";
   }

   public List<MetaTeBlock> getPossibleValues() {
      return List.copyOf(this.allowedValues);
   }

   public int getInternalIndex(MetaTeBlock value) {
      return this.getPossibleValues().indexOf(value);
   }

   public Class<MetaTeBlock> getValueClass() {
      return MetaTeBlock.class;
   }

   public Optional<MetaTeBlock> getValue(String value) {
      for (MetaTeBlock block : this.allowedValues) {
         if (this.getName(block).equals(value)) {
            return Optional.of(block);
         }
      }

      return Optional.empty();
   }

   public String getName(MetaTeBlock value) {
      return value.active ? value.teBlock.getName() + "_active" : value.teBlock.getName();
   }

   public String toString() {
      return "MetaTeBlockProperty{For " + this.resourceLocationName + "}";
   }

   public static List<MetaTeBlockProperty.MetaTePair> getAllStates(Identifier identifier) {
      return (List<MetaTeBlockProperty.MetaTePair>)resourceToTeBlock.get(identifier).b;
   }

   public static MetaTeBlock getState(ITeBlock teBlock) {
      return getState(teBlock, false);
   }

   public static MetaTeBlock getState(ITeBlock teBlock, boolean active) {
      MetaTeBlockProperty.MetaTePair state = teResourceMapping.get(teBlock);
      return state == null ? invalid : state.getState(active);
   }

   static {
      for (Entry<Identifier, Set<? extends ITeBlock>> blocks : TeBlockRegistry.getAll()) {
         List<MetaTeBlockProperty.MetaTePair> locationBlocks = new ArrayList<>(blocks.getValue().size());
         int states = 0;

         for (ITeBlock block : blocks.getValue()) {
            MetaTeBlockProperty.MetaTePair lastIn;
            if (block.hasActive()) {
               states += 2;
               locationBlocks.add(lastIn = new MetaTeBlockProperty.MetaTePair(block, true));
            } else {
               states++;
               locationBlocks.add(lastIn = new MetaTeBlockProperty.MetaTePair(block, false));
            }

            teResourceMapping.put(block, lastIn);
         }

         resourceToTeBlock.put(blocks.getKey(), new Tuple.T2<>(states, locationBlocks));
      }

      MetaTeBlockProperty.MetaTePair invalidStates = teResourceMapping.get(TeBlock.invalid);
      invalid = invalidStates.inactive;

      assert invalid != null : "Failed to properly map ITeBlocks to MetaTeBlocks!";

      for (Entry<Identifier, Tuple.T2<Integer, List<MetaTeBlockProperty.MetaTePair>>> type : resourceToTeBlock.entrySet()) {
         if (type.getKey() != invalid.teBlock.getIdentifier()) {
            type.getValue().a = (TA)(Integer)type.getValue().a + 1;
            ((List)type.getValue().b).add(invalidStates);
         }
      }
   }

   public static class MetaTePair {
      public final MetaTeBlock inactive;
      public final MetaTeBlock active;
      private final boolean hasActive;

      public MetaTePair(ITeBlock block, boolean active) {
         this.inactive = new MetaTeBlock(block, false);
         this.active = active ? new MetaTeBlock(block, true) : null;
         this.hasActive = active;
      }

      public ITeBlock getBlock() {
         return this.inactive.teBlock;
      }

      public MetaTeBlock getState(boolean active) {
         return active && this.hasActive ? this.active : this.inactive;
      }

      boolean hasActive() {
         return this.hasActive;
      }

      public boolean hasItem() {
         return this.getBlock().hasItem();
      }

      public Identifier getIdentifier() {
         return this.getBlock().getIdentifier();
      }

      public String getName() {
         return this.getBlock().getName();
      }
   }
}
