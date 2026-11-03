package dev.hollowdread;

import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.Difficulty;
import net.minecraft.world.World;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;

/**
 * "Режиссёр страха". Раз в секунду для каждого игрока в Верхнем мире:
 *  - копит «страх» (dread), пока игрок стоит в темноте (свет <= 5; под землёй быстрее);
 *  - на свету страх быстро спадает, поэтому факелы реально спасают;
 *  - при среднем страхе играет жуткие звуки за спиной;
 *  - при высоком страхе может призвать Наблюдателя (максимум один на игрока, с кулдауном).
 */
public final class HorrorDirector {
    private static final int DARK_LIGHT_LEVEL = 5;
    private static final int SOUND_THRESHOLD = 30;
    private static final int SPAWN_THRESHOLD = 90;
    private static final int COOLDOWN_SECONDS = 240;

    private static final Map<UUID, Integer> DREAD = new HashMap<>();
    private static final Map<UUID, Integer> COOLDOWN = new HashMap<>();
    private static final Map<UUID, WatcherEntity> ACTIVE = new HashMap<>();

    private static final SoundEvent[] SCARES = {
            SoundEvents.AMBIENT_CAVE,
            SoundEvents.AMBIENT_CAVE,
            SoundEvents.BLOCK_WOODEN_DOOR_OPEN,
            SoundEvents.ENTITY_ZOMBIE_ATTACK_WOODEN_DOOR,
            SoundEvents.ENTITY_ENDERMAN_STARE,
            SoundEvents.ENTITY_ENDERMAN_AMBIENT,
            SoundEvents.ENTITY_PHANTOM_AMBIENT,
            SoundEvents.BLOCK_CHEST_OPEN
    };

    private HorrorDirector() {
    }

    public static void tick(ServerWorld world) {
        if (world.getRegistryKey() != World.OVERWORLD || world.getTime() % 20L != 0L) {
            return;
        }
        for (ServerPlayerEntity player : world.getPlayers()) {
            if (player.isSpectator() || player.isCreative() || !player.isAlive()) {
                continue;
            }
            tickPlayer(world, player);
        }
    }

    private static void tickPlayer(ServerWorld world, ServerPlayerEntity player) {
        UUID id = player.getUuid();
        Random rand = world.random;

        int cooldown = COOLDOWN.getOrDefault(id, 0);
        if (cooldown > 0) {
            COOLDOWN.put(id, cooldown - 1);
        }

        WatcherEntity active = ACTIVE.get(id);
        if (active != null && !active.isAlive()) {
            ACTIVE.remove(id);
            active = null;
        }

        int light = world.getLightLevel(player.getBlockPos());
        int dread = DREAD.getOrDefault(id, 0);
        if (light <= DARK_LIGHT_LEVEL) {
            dread += player.getY() < 50 ? 2 : 1;
        } else {
            dread = Math.max(0, dread - 2);
        }

        if (dread >= SOUND_THRESHOLD && rand.nextInt(18) == 0) {
            playScare(world, player, rand);
        }

        if (dread >= SPAWN_THRESHOLD
                && active == null
                && cooldown == 0
                && world.getDifficulty() != Difficulty.PEACEFUL
                && rand.nextInt(8) == 0
                && trySpawn(world, player, rand)) {
            dread = 0;
            COOLDOWN.put(id, COOLDOWN_SECONDS);
        }

        DREAD.put(id, Math.min(dread, 400));
    }

    private static void playScare(ServerWorld world, PlayerEntity player, Random rand) {
        double ang = rand.nextDouble() * Math.PI * 2;
        double dist = 5 + rand.nextDouble() * 7;
        double x = player.getX() + Math.cos(ang) * dist;
        double z = player.getZ() + Math.sin(ang) * dist;
        SoundEvent sound = SCARES[rand.nextInt(SCARES.length)];
        world.playSound(null, x, player.getY(), z, sound, SoundCategory.AMBIENT,
                0.8f, 0.6f + rand.nextFloat() * 0.3f);
    }

    private static boolean trySpawn(ServerWorld world, ServerPlayerEntity player, Random rand) {
        Vec3d look = player.getRotationVec(1.0f);
        BlockPos pBlock = player.getBlockPos();

        for (int attempt = 0; attempt < 16; attempt++) {
            double ang = rand.nextDouble() * Math.PI * 2;
            double dx = Math.cos(ang);
            double dz = Math.sin(ang);
            // Только вне поля зрения игрока.
            if (look.x * dx + look.z * dz > 0.35) {
                continue;
            }
            double dist = 14 + rand.nextDouble() * 14;
            int bx = MathHelper.floor(player.getX() + dx * dist);
            int bz = MathHelper.floor(player.getZ() + dz * dist);
            if (!world.isChunkLoaded(new BlockPos(bx, pBlock.getY(), bz))) {
                continue;
            }

            for (int dy = 4; dy >= -6; dy--) {
                BlockPos feet = new BlockPos(bx, pBlock.getY() + dy, bz);
                BlockPos below = feet.down();
                if (!world.getBlockState(below).isSolidBlock(world, below)) {
                    continue;
                }
                if (world.getLightLevel(feet) > 7 || !world.getFluidState(feet).isEmpty()) {
                    continue;
                }

                WatcherEntity watcher = HollowDread.WATCHER.create(world);
                if (watcher == null) {
                    return false;
                }
                float yaw = (float) (MathHelper.atan2(player.getZ() - (bz + 0.5), player.getX() - (bx + 0.5))
                        * 180.0 / Math.PI) - 90.0f;
                watcher.refreshPositionAndAngles(bx + 0.5, feet.getY(), bz + 0.5, yaw, 0.0f);
                if (!world.isSpaceEmpty(watcher)) {
                    continue;
                }
                watcher.setTarget(player);
                world.spawnEntity(watcher);
                ACTIVE.put(player.getUuid(), watcher);
                return true;
            }
        }
        return false;
    }
}
