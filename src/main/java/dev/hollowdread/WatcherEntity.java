package dev.hollowdread;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityDimensions;
import net.minecraft.entity.EntityPose;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.ai.goal.LookAtEntityGoal;
import net.minecraft.entity.ai.goal.MeleeAttackGoal;
import net.minecraft.entity.ai.goal.SwimGoal;
import net.minecraft.entity.attribute.DefaultAttributeContainer;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.effect.StatusEffectInstance;
import net.minecraft.entity.effect.StatusEffects;
import net.minecraft.entity.mob.HostileEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.particle.ParticleTypes;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;

/**
 * Наблюдатель. Идёт к игроку, пока тот на него не смотрит.
 * Стоит как вкопанный под взглядом и растворяется, если смотреть на него дольше ~2.5 сек.
 * Подпустишь вплотную — ослепит и ударит.
 *
 * Оптимизация: нет поиска цели, нет лишних целей ИИ, жизнь ограничена 4 минутами,
 * один на игрока (контролирует HorrorDirector).
 */
public class WatcherEntity extends HostileEntity {
    private static final int VANISH_AFTER_STARE_TICKS = 50;
    private static final int MAX_AGE_TICKS = 20 * 60 * 4;

    private int stareTicks = 0;
    private boolean frozen = false;

    public WatcherEntity(EntityType<? extends WatcherEntity> type, World world) {
        super(type, world);
        this.experiencePoints = 5;
    }

    public static DefaultAttributeContainer.Builder createAttributes() {
        return HostileEntity.createHostileAttributes()
                .add(EntityAttributes.GENERIC_MAX_HEALTH, 40.0)
                .add(EntityAttributes.GENERIC_MOVEMENT_SPEED, 0.30)
                .add(EntityAttributes.GENERIC_ATTACK_DAMAGE, 6.0)
                .add(EntityAttributes.GENERIC_FOLLOW_RANGE, 64.0);
    }

    @Override
    protected void initGoals() {
        this.goalSelector.add(0, new SwimGoal(this));
        this.goalSelector.add(1, new MeleeAttackGoal(this, 1.0, false));
        this.goalSelector.add(2, new LookAtEntityGoal(this, PlayerEntity.class, 48.0f));
        // Цель назначает HorrorDirector, поэтому FollowTargetGoal не нужен.
    }

    @Override
    protected void mobTick() {
        super.mobTick();

        LivingEntity target = this.getTarget();
        if (this.age > MAX_AGE_TICKS || !(target instanceof PlayerEntity) || !target.isAlive()) {
            this.remove();
            return;
        }

        PlayerEntity player = (PlayerEntity) target;

        // Вплотную ему всё равно, смотришь ты или нет.
        boolean stared = this.squaredDistanceTo(player) > 9.0 && isStaredAt(player);

        if (stared) {
            this.frozen = true;
            if (++this.stareTicks >= VANISH_AFTER_STARE_TICKS) {
                vanish();
            }
        } else {
            this.frozen = false;
            this.stareTicks = 0;
        }
    }

    /** Та же идея, что у эндермена: угол между взглядом и направлением на существо. */
    private boolean isStaredAt(PlayerEntity player) {
        Vec3d look = player.getRotationVec(1.0f).normalize();
        Vec3d to = new Vec3d(
                this.getX() - player.getX(),
                this.getEyeY() - player.getEyeY(),
                this.getZ() - player.getZ());
        double dist = to.length();
        if (dist < 1.0E-4) {
            return true;
        }
        to = to.normalize();
        return look.dotProduct(to) > 1.0 - 0.06 / dist && player.canSee(this);
    }

    private void vanish() {
        if (this.world instanceof ServerWorld) {
            ServerWorld sw = (ServerWorld) this.world;
            sw.spawnParticles(ParticleTypes.LARGE_SMOKE, this.getX(), this.getBodyY(0.5), this.getZ(),
                    14, 0.3, 0.9, 0.3, 0.01);
            sw.playSound(null, this.getX(), this.getY(), this.getZ(),
                    SoundEvents.ENTITY_ENDERMAN_DEATH, SoundCategory.HOSTILE, 0.8f, 0.5f);
        }
        this.remove();
    }

    @Override
    public void travel(Vec3d movementInput) {
        if (this.frozen) {
            this.setVelocity(0.0, this.getVelocity().y, 0.0);
            super.travel(Vec3d.ZERO);
        } else {
            super.travel(movementInput);
        }
    }

    @Override
    public boolean tryAttack(Entity target) {
        boolean hit = super.tryAttack(target);
        if (hit && target instanceof LivingEntity) {
            LivingEntity living = (LivingEntity) target;
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.BLINDNESS, 100, 0));
            living.addStatusEffect(new StatusEffectInstance(StatusEffects.SLOWNESS, 60, 1));
        }
        return hit;
    }

    @Override
    protected float getActiveEyeHeight(EntityPose pose, EntityDimensions dimensions) {
        return 2.35f;
    }

    @Override
    protected SoundEvent getAmbientSound() {
        return null; // тишина страшнее
    }

    @Override
    protected SoundEvent getHurtSound(DamageSource source) {
        return SoundEvents.ENTITY_ENDERMAN_HURT;
    }

    @Override
    protected SoundEvent getDeathSound() {
        return SoundEvents.ENTITY_ENDERMAN_DEATH;
    }
}
