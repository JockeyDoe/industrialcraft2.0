package ic2.core.audio;

import com.mojang.blaze3d.audio.Channel;
import ic2.core.util.ReflectionUtil;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.Sound;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.client.resources.sounds.Sound.Type;
import net.minecraft.client.resources.sounds.SoundInstance.Attenuation;
import net.minecraft.client.sounds.SoundEngine;
import net.minecraft.client.sounds.SoundManager;
import net.minecraft.client.sounds.WeighedSoundEvents;
import net.minecraft.client.sounds.ChannelAccess.ChannelHandle;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.valueproviders.ConstantFloat;

final class NativeAudioSystem {
   private final SoundManager manager;
   private final Map<String, NativeAudioSystem.Source> sources = new HashMap<>();
   private int nextId;

   NativeAudioSystem(SoundManager manager) {
      this.manager = manager;
   }

   static Identifier location(String file) {
      return AudioPaths.location(file);
   }

   void newSource(boolean priority, String name, URL url, String file, boolean loop, float x, float y, float z, int attenuation, float distance) {
      this.sources.put(name, new NativeAudioSystem.Source(file, loop, x, y, z, attenuation != 0, distance));
   }

   String quickPlay(boolean priority, URL url, String file, boolean loop, float x, float y, float z, int attenuation, float distance) {
      String name = "ic2-once-" + this.nextId++;
      this.newSource(priority, name, url, file, loop, x, y, z, attenuation, distance);
      this.play(name);
      return name;
   }

   void play(String name) {
      NativeAudioSystem.Source s = this.sources.get(name);
      if (s != null) {
         if (this.manager.isActive(s)) {
            this.channel(s, Channel::unpause);
            s.paused = false;
         } else {
            NativeAudioSystem.Source copy = s.copy();
            this.sources.put(name, copy);
            this.manager.play(copy);
         }
      }
   }

   void pause(String name) {
      NativeAudioSystem.Source s = this.sources.get(name);
      if (s != null) {
         s.paused = true;
         this.channel(s, Channel::pause);
      }
   }

   void stop(String name) {
      NativeAudioSystem.Source s = this.sources.get(name);
      if (s != null) {
         s.end();
         this.manager.stop(s);
      }
   }

   void removeSource(String name) {
      this.stop(name);
      this.sources.remove(name);
   }

   void flush(String name) {
      this.stop(name);
   }

   void cull(String name) {
      this.pause(name);
   }

   void activate(String name) {
      NativeAudioSystem.Source s = this.sources.get(name);
      if (s != null) {
         s.paused = false;
         this.channel(s, Channel::unpause);
      }
   }

   boolean playing(String name) {
      NativeAudioSystem.Source s = this.sources.get(name);
      return s != null && !s.paused && this.manager.isActive(s);
   }

   float getVolume(String name) {
      NativeAudioSystem.Source s = this.sources.get(name);
      return s == null ? 0.0F : s.getVolume();
   }

   void setVolume(String name, float volume) {
      NativeAudioSystem.Source s = this.sources.get(name);
      if (s != null) {
         s.volumeValue(volume);
      }
   }

   void setPitch(String name, float pitch) {
      NativeAudioSystem.Source s = this.sources.get(name);
      if (s != null) {
         s.pitchValue(pitch);
      }
   }

   void setPosition(String name, float x, float y, float z) {
      NativeAudioSystem.Source s = this.sources.get(name);
      if (s != null) {
         s.position(x, y, z);
      }
   }

   private void channel(NativeAudioSystem.Source source, Consumer<Channel> action) {
      Object engine = ReflectionUtil.getValue(this.manager, SoundEngine.class);

      try {
         Map<SoundInstance, ChannelHandle> handles = ReflectionUtil.getValueRecursive(engine, "instanceToChannel");
         ChannelHandle handle = handles.get(source);
         if (handle != null) {
            handle.execute(action);
         }
      } catch (NoSuchFieldException var6) {
         throw new IllegalStateException("Native per-source audio channels unavailable", var6);
      }
   }

   private static final class Source extends AbstractTickableSoundInstance {
      private final String file;
      private final boolean attenuate;
      private final float distance;
      private boolean paused;

      Source(String file, boolean loop, float x, float y, float z, boolean attenuate, float distance) {
         super(SoundEvent.createVariableRangeEvent(NativeAudioSystem.location(file)), SoundSource.BLOCKS, RandomSource.create());
         this.file = file;
         this.attenuate = attenuate;
         this.distance = distance;
         this.looping = loop;
         this.attenuation = attenuate ? Attenuation.LINEAR : Attenuation.NONE;
         this.position(x, y, z);
      }

      NativeAudioSystem.Source copy() {
         NativeAudioSystem.Source result = new NativeAudioSystem.Source(
            this.file, this.looping, (float)this.x, (float)this.y, (float)this.z, this.attenuate, this.distance
         );
         result.volume = this.volume;
         result.pitch = this.pitch;
         return result;
      }

      public WeighedSoundEvents getOrResolve(SoundManager manager) {
         if (this.soundEvent == null) {
            this.sound = new Sound(
               NativeAudioSystem.location(this.file),
               ConstantFloat.of(1.0F),
               ConstantFloat.of(1.0F),
               1,
               Type.FILE,
               false,
               false,
               Math.max(1, (int)this.distance)
            );
            this.soundEvent = new WeighedSoundEvents(NativeAudioSystem.location(this.file), null);
            this.soundEvent.addSound(this.sound);
         }

         return this.soundEvent;
      }

      public boolean canStartSilent() {
         return true;
      }

      public void tick() {
      }

      void end() {
         this.stop();
      }

      void volumeValue(float value) {
         this.volume = value;
      }

      void pitchValue(float value) {
         this.pitch = value;
      }

      void position(float px, float py, float pz) {
         this.x = px;
         this.y = py;
         this.z = pz;
      }
   }
}
