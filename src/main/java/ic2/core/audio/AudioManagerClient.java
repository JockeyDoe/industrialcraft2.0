package ic2.core.audio;

import ic2.core.IC2;
import ic2.core.IHitSoundOverride;
import ic2.core.init.MainConfig;
import ic2.core.util.ConfigUtil;
import ic2.core.util.LogCategory;
import ic2.core.util.WorldQueries;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Queue;
import java.util.Map.Entry;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.HitResult.Type;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.api.distmarker.OnlyIn;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.sound.PlaySoundEvent;
import net.neoforged.neoforge.client.event.sound.SoundEngineLoadEvent;
import net.neoforged.neoforge.common.NeoForge;

@OnlyIn(Dist.CLIENT)
public final class AudioManagerClient extends AudioManager {
   public float fadingDistance = 16.0F;
   private boolean enabled = true;
   private boolean wasPaused = false;
   private int maxSourceCount = 32;
   private final int streamingSourceCount = 4;
   private SoundManager soundManager;
   private Field soundManagerLoaded;
   private volatile Thread initThread;
   private NativeAudioSystem soundSystem = null;
   float masterVolume = 0.5F;
   private int nextId = 0;
   private final Map<AudioManagerClient.WeakObject, List<AudioSourceClient>> objectToAudioSourceMap = new HashMap<>();
   private final Queue<AudioSource> validAudioSources = new PriorityQueue<>();
   private final Map<String, FutureSound> singleSoundQueue = new HashMap<>();

   @Override
   public void initialize() {
      this.enabled = ConfigUtil.getBool(MainConfig.get(), "audio/enabled");
      this.masterVolume = ConfigUtil.getFloat(MainConfig.get(), "audio/volume");
      this.fadingDistance = ConfigUtil.getFloat(MainConfig.get(), "audio/fadeDistance");
      this.maxSourceCount = ConfigUtil.getInt(MainConfig.get(), "audio/maxSourceCount");
      if (this.maxSourceCount <= 6) {
         IC2.log.info(LogCategory.Audio, "The audio source limit is too low to enable IC2 sounds.");
         this.enabled = false;
      }

      if (!this.enabled) {
         IC2.log.debug(LogCategory.Audio, "Sounds disabled.");
      } else if (this.maxSourceCount < 6) {
         this.enabled = false;
      } else {
         IC2.log.debug(LogCategory.Audio, "Using %d audio sources.", this.maxSourceCount);
         this.soundManager = Minecraft.getInstance().getSoundManager();
         this.soundSystem = new NativeAudioSystem(this.soundManager);
         NeoForge.EVENT_BUS.register(this);
         NeoForge.EVENT_BUS.register(AudioConfigHandler.class);
      }
   }

   public void onSoundSetup(SoundEngineLoadEvent event) {
      for (List<AudioSourceClient> sources : this.objectToAudioSourceMap.values()) {
         for (AudioSourceClient source : sources) {
            source.setInvalid();
         }
      }

      this.objectToAudioSourceMap.clear();
      this.singleSoundQueue.clear();
      this.soundManager = Minecraft.getInstance().getSoundManager();
      this.soundSystem = new NativeAudioSystem(this.soundManager);
   }

   @Override
   public void onTick() {
      if (this.enabled && this.valid()) {
         assert IC2.platform.isRendering();

         IC2.platform.profilerStartSection("UpdateSourceVolume");
         Player player = IC2.platform.getPlayerInstance();
         if (player == null) {
            for (List<AudioSourceClient> sources : this.objectToAudioSourceMap.values()) {
               removeSources(sources);
            }

            this.objectToAudioSourceMap.clear();
            this.singleSoundQueue.clear();
         } else {
            boolean isPaused = Minecraft.getInstance().isPaused();
            if (!isPaused && !this.singleSoundQueue.isEmpty()) {
               IC2.platform.profilerStartSection("SoundQueuing");
               Iterator<Entry<String, FutureSound>> it = this.singleSoundQueue.entrySet().iterator();

               while (it.hasNext()) {
                  Entry<String, FutureSound> entry = it.next();
                  if (!this.soundSystem.playing(entry.getKey())) {
                     entry.getValue().onFinish();
                     it.remove();
                  } else if (entry.getValue().isCancelled()) {
                     this.removeSource(entry.getKey());
                     it.remove();
                  }
               }

               synchronized (this.soundSystem) {
                  IC2.platform.profilerEndSection();
               }
            }

            Iterator<Entry<AudioManagerClient.WeakObject, List<AudioSourceClient>>> it = this.objectToAudioSourceMap.entrySet().iterator();

            while (it.hasNext()) {
               Entry<AudioManagerClient.WeakObject, List<AudioSourceClient>> entry = it.next();
               if (entry.getKey().get() == null) {
                  it.remove();
                  removeSources(entry.getValue());
               } else {
                  for (AudioSource audioSource : entry.getValue()) {
                     if (!this.wasPaused) {
                        audioSource.updateVolume(player);
                     }

                     if (audioSource.getRealVolume() > 0.0F) {
                        this.validAudioSources.add(audioSource);
                     }
                  }
               }
            }

            IC2.platform.profilerEndStartSection("Culling");
            if (isPaused) {
               if (isPaused != this.wasPaused) {
                  this.wasPaused = true;

                  while (!this.validAudioSources.isEmpty()) {
                     this.validAudioSources.poll().pause();
                  }
               } else {
                  assert isPaused;

                  assert this.wasPaused;

                  this.validAudioSources.clear();
               }
            } else {
               if (this.wasPaused) {
                  for (AudioSource source : this.validAudioSources) {
                     source.play();
                  }

                  this.wasPaused = false;
               }

               for (int i = 0; !this.validAudioSources.isEmpty(); i++) {
                  AudioSource source = this.validAudioSources.poll();
                  if (i < this.maxSourceCount) {
                     source.activate();
                  } else {
                     source.cull();
                  }
               }
            }
         }

         IC2.platform.profilerEndSection();
      }
   }

   @Override
   public AudioSource createSource(Object obj, String initialSoundFile) {
      return this.createSource(obj, PositionSpec.Center, initialSoundFile, false, false, this.getDefaultVolume());
   }

   @Override
   public AudioSource createSource(Object obj, PositionSpec positionSpec, String initialSoundFile, boolean loop, boolean priorized, float volume) {
      if (!this.enabled) {
         return null;
      } else if (!this.valid()) {
         return null;
      } else {
         assert IC2.platform.isRendering();

         String sourceName = getSourceName(this.nextId);
         this.nextId++;
         AudioSourceClient audioSource = new AudioSourceClient(this.soundSystem, sourceName, obj, positionSpec, initialSoundFile, loop, priorized, volume);
         audioSource.setup();
         AudioManagerClient.WeakObject key = new AudioManagerClient.WeakObject(obj);
         List<AudioSourceClient> sources = this.objectToAudioSourceMap.get(key);
         if (sources == null) {
            sources = new ArrayList<>();
            this.objectToAudioSourceMap.put(key, sources);
         }

         sources.add(audioSource);
         return audioSource;
      }
   }

   static URL getSourceURL(String soundFile) {
      int colonIndex = soundFile.indexOf(58);
      return colonIndex > -1
         ? AudioSource.class.getClassLoader().getResource("assets/" + soundFile.substring(0, colonIndex) + "/sounds/" + soundFile.substring(++colonIndex))
         : AudioSource.class.getClassLoader().getResource("ic2/sounds/" + soundFile);
   }

   @Override
   public void removeSources(Object obj) {
      if (this.valid()) {
         assert IC2.platform.isRendering();

         if (!(obj instanceof AudioManagerClient.WeakObject key)) {
            key = new AudioManagerClient.WeakObject(obj);
         }

         List<AudioSourceClient> sources = this.objectToAudioSourceMap.remove(key);
         if (sources != null) {
            removeSources(sources);
         }
      }
   }

   private static void removeSources(List<AudioSourceClient> sources) {
      for (AudioSourceClient audioSource : sources) {
         audioSource.remove();
      }
   }

   @Override
   public void playOnce(Object obj, String soundFile) {
      this.playOnce(obj, PositionSpec.Center, soundFile, true, this.getDefaultVolume());
   }

   @Override
   public String playOnce(Object obj, PositionSpec positionSpec, String soundFile, boolean priorized, float volume) {
      if (!this.enabled) {
         return null;
      } else if (!this.valid()) {
         return null;
      } else {
         assert IC2.platform.isRendering();

         AudioPosition position = AudioPosition.getFrom(obj, positionSpec);
         if (position == null) {
            return null;
         } else {
            URL url = getSourceURL(soundFile);
            if (url == null) {
               IC2.log.warn(LogCategory.Audio, "Invalid sound file: %s.", soundFile);
               return null;
            } else {
               String sourceName = this.soundSystem
                  .quickPlay(priorized, url, soundFile, false, position.x, position.y, position.z, 2, this.fadingDistance * Math.max(volume, 1.0F));
               this.soundSystem.setVolume(sourceName, this.masterVolume * Math.min(volume, 1.0F));
               return sourceName;
            }
         }
      }
   }

   @Override
   public void chainSource(String source, FutureSound onFinish) {
      this.singleSoundQueue.put(source, onFinish);
   }

   @Override
   public void removeSource(String source) {
      if (source != null) {
         this.soundSystem.stop(source);
         this.soundSystem.removeSource(source);
      }
   }

   @Override
   public float getDefaultVolume() {
      return 1.2F;
   }

   @Override
   public float getMasterVolume() {
      return this.masterVolume;
   }

   @Override
   protected boolean valid() {
      return this.soundSystem != null && this.soundManager != null;
   }

   @SubscribeEvent
   public void onSoundPlayed(PlaySoundEvent event) {
      if (event.getSound() != null && Minecraft.getInstance().player != null) {
         SoundSource category = event.getSound().getSource();
         if (category == SoundSource.NEUTRAL && event.getSound().getIdentifier().getPath().endsWith(".hit")
            || category == SoundSource.BLOCKS && event.getSound().getIdentifier().getPath().endsWith(".break")) {
            LocalPlayer player = Minecraft.getInstance().player;
            ItemStack stack = player.getInventory().getSelectedItem();
            if (stack != null && stack.getItem() instanceof IHitSoundOverride) {
               Level world = player.level();
               HitResult mop = getMovingObjectPositionFromPlayer(world, player, false);
               BlockPos pos = BlockPos.containing(event.getSound().getX(), event.getSound().getY(), event.getSound().getZ());
               if (mop != null && mop.getType() == Type.BLOCK && pos.equals(((BlockHitResult)mop).getBlockPos())) {
                  String replace;
                  if (event.getSound().getSource() == SoundSource.NEUTRAL) {
                     replace = ((IHitSoundOverride)stack.getItem()).getHitSoundForBlock(player, world, pos, stack);
                  } else {
                     replace = ((IHitSoundOverride)stack.getItem()).getBreakSoundForBlock(player, world, pos, stack);
                  }

                  if (replace != null) {
                     event.setSound(null);
                     if (!replace.isEmpty()) {
                        IC2.platform.playSoundSp(replace, 1.0F, 1.0F);
                     }
                  }
               }
            }
         }
      }
   }

   private static HitResult getMovingObjectPositionFromPlayer(Level worldIn, Player playerIn, boolean useLiquids) {
      float f = playerIn.getXRot();
      float f1 = playerIn.getYRot();
      double d0 = playerIn.getX();
      double d1 = playerIn.getY() + playerIn.getEyeHeight();
      double d2 = playerIn.getZ();
      Vec3 vec3 = new Vec3(d0, d1, d2);
      float f2 = Mth.cos(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f3 = Mth.sin(-f1 * (float) (Math.PI / 180.0) - (float) Math.PI);
      float f4 = -Mth.cos(-f * (float) (Math.PI / 180.0));
      float f5 = Mth.sin(-f * (float) (Math.PI / 180.0));
      float f6 = f3 * f4;
      float f7 = f2 * f4;
      double d3 = 5.0;
      Vec3 vec31 = vec3.add(f6 * d3, f5 * d3, f7 * d3);
      return WorldQueries.clip(worldIn, vec3, vec31, useLiquids);
   }

   private static String getSourceName(int id) {
      return "asm_snd" + id;
   }

   @EventBusSubscriber(modid = "ic2", value = Dist.CLIENT)
   public static final class SoundSetup {
      @SubscribeEvent
      public static void loaded(SoundEngineLoadEvent event) {
         if (IC2.audioManager instanceof AudioManagerClient audio) {
            audio.onSoundSetup(event);
         }
      }
   }

   public static class WeakObject extends WeakReference<Object> {
      public WeakObject(Object referent) {
         super(referent);
      }

      @Override
      public boolean equals(Object object) {
         return object instanceof AudioManagerClient.WeakObject ? ((AudioManagerClient.WeakObject)object).get() == this.get() : this.get() == object;
      }

      @Override
      public int hashCode() {
         Object object = this.get();
         return object == null ? 0 : object.hashCode();
      }
   }
}
