package thermalfoundation.entity.monster;

import cofh.core.entity.EntitySelectorInRangeByType;
import cofh.core.util.CoreUtils;
import cofh.lib.util.helpers.ItemHelper;
import cofh.lib.util.helpers.ServerHelper;
import com.google.common.collect.Lists;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

import java.util.Arrays;
import java.util.List;

import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EnumCreatureType;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.monster.EntityBlaze;
import net.minecraft.entity.monster.EntityMob;
import net.minecraft.entity.passive.EntityAnimal;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.init.Blocks;
import net.minecraft.init.Items;
import net.minecraft.item.Item;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.MathHelper;
import net.minecraft.world.World;
import net.minecraft.world.biome.BiomeGenBase;
import net.minecraftforge.common.BiomeDictionary;
import net.minecraftforge.common.BiomeDictionary.Type;

import thermalfoundation.ThermalFoundation;
import thermalfoundation.entity.projectile.EntityBlizzBall;
import thermalfoundation.entity.projectile.EntityBlizzSlowball;
import thermalfoundation.item.TFItems;

public class EntityBlizz extends EntityMob {

	public static int entityId = -1;

	public static void initialize() {

		try {
			entityId = EntityRegistry.findGlobalUniqueEntityId();
		} catch (Exception e) {
			ThermalFoundation.log.error("Error - No Global Entity IDs remaining. This is REALLY bad. Using a mod-specific ID instead.", e);
			entityId = CoreUtils.getEntityId();
		}
		try {
			EntityRegistry.registerGlobalEntityID(EntityBlizz.class, "Blizz", entityId, 0xE0FBFF, 0x6BE6FF);
		} catch (Exception e) {
			ThermalFoundation.log.error("Another mod is improperly using the Entity Registry. This is REALLY bad. Using a mod-specific ID instead.", e);
			entityId = CoreUtils.getEntityId();
			EntityRegistry.registerGlobalEntityID(EntityBlizz.class, "Blizz", entityId, 0xE0FBFF, 0x6BE6FF);
		}
		// Add Blizz spawn to snow/frozen biomes only if non-ocean/river
		List<BiomeGenBase> validBiomes = Arrays.asList(BiomeDictionary.getBiomesForType(Type.SNOWY));
		List<Type> types = Lists.newArrayList();
		List<Type> fine = Lists.newArrayList(Type.END, Type.SNOWY);
		for (BiomeGenBase biome : BiomeDictionary.getBiomesForType(Type.MAGICAL)) {
			if (biome.isHighHumidity()) {
				continue; // No high humidity biomes.
			}
			if (validBiomes.contains(biome)) {
				continue; // Already have it.
			}
			types.clear();
			types.addAll(Arrays.asList(BiomeDictionary.getTypesForBiome(biome))); // we don't want to modify the underlying array
			types.removeAll(fine);
			if (types.size() == 0) {
				validBiomes.add(biome);
			}
		}
		types.clear();

		EntityRegistry.addSpawn(EntityBlizz.class, 4, 1, 3, EnumCreatureType.monster, validBiomes.toArray(new BiomeGenBase[0]));
	}

	/** Random offset used in floating behaviour */
	protected float heightOffset = 0.5F;

	/** ticks until heightOffset is randomized */
	protected int heightOffsetUpdateTime;
	protected int firingState;

	public static final String SOUND_AMBIENT = CoreUtils.getSoundName(ThermalFoundation.modId, "mobBlizzAmbient");
	public static final String SOUND_ATTACK = CoreUtils.getSoundName(ThermalFoundation.modId, "mobBlizzAttack");
	public static final String SOUND_LIVING[] = { CoreUtils.getSoundName(ThermalFoundation.modId, "mobBlizzBreathe0"),
			CoreUtils.getSoundName(ThermalFoundation.modId, "mobBlizzBreathe1"), CoreUtils.getSoundName(ThermalFoundation.modId, "mobBlizzBreathe2") };

	protected static final int SOUND_AMBIENT_FREQUENCY = 200; // How often it does ambient sound loop

	public EntityBlizz(World world) {

		super(world);
		this.experienceValue = 10;
	}

	@Override
	protected void applyEntityAttributes() {

		super.applyEntityAttributes();
		this.getEntityAttribute(SharedMonsterAttributes.attackDamage).setBaseValue(6.0D);
	}

	@Override
	protected void entityInit() {

		super.entityInit();
		this.dataWatcher.addObject(16, new Byte((byte) 0));
	}

	@Override
	protected String getLivingSound() {

		return SOUND_LIVING[this.rand.nextInt(3)];
	}

	@Override
	protected String getHurtSound() {

		return "mob.blaze.hit";
	}

	@Override
	protected String getDeathSound() {

		return "mob.blaze.death";
	}

	@Override
	@SideOnly(Side.CLIENT)
	public int getBrightnessForRender(float par1) {

		return 0xF000F0;
	}

	@Override
	public float getBrightness(float par1) {

		return 2.0F;
	}

	@Override
	public void onLivingUpdate() {

		if (ServerHelper.isServerWorld(worldObj)) {
			--this.heightOffsetUpdateTime;

			if (this.heightOffsetUpdateTime <= 0) {
				this.heightOffsetUpdateTime = 100;
				this.heightOffset = 0.5F + (float) this.rand.nextGaussian() * 3.0F;
			}
			Entity target = this.getEntityToAttack();
			if (target != null) {
				if ((target.posY + target.getEyeHeight()) > (this.posY + this.getEyeHeight() + this.heightOffset)) {
					this.motionY += (0.30000001192092896D - this.motionY) * 0.30000001192092896D;
				}
			}
		}
		if (this.rand.nextInt(SOUND_AMBIENT_FREQUENCY) == 0) {
			this.worldObj.playSoundEffect(this.posX + 0.5D, this.posY + 0.5D, this.posZ + 0.5D, SOUND_AMBIENT, this.rand.nextFloat() * 0.3F + 0.4F,
					this.rand.nextFloat() * 0.3F + 0.4F);
		}
		if (!this.onGround && this.motionY < 0.0D) {
			this.motionY *= 0.6D;
		}
		for (int i = 0; i < 2; i++) {
			this.worldObj.spawnParticle("snowballpoof", this.posX + (this.rand.nextDouble() - 0.5D) * this.width, this.posY + this.rand.nextDouble()
					* (this.height * 0.2D), this.posZ + (this.rand.nextDouble() - 0.5D) * this.width, 0.0D, 0.0D, 0.0D);
		}
		super.onLivingUpdate();
	}

	/**
	 * Finds the closest player within 16 blocks to attack, or null if this Entity isn't interested in attacking (Animals, Spiders at day, peaceful PigZombies).
	 */
	@Override
	protected Entity findPlayerToAttack() {

		EntityPlayer player = this.worldObj.getClosestVulnerablePlayerToEntity(this, 16.0D);
		if (player != null && this.canEntityBeSeen(player)) {
			return player;
		}
		return getClosestVictim(16.0D);
	}

	/**
	 * Gets the closest victim to the point within the specified distance (distance can be set to less than 0 to not limit the distance). Args: x, y, z, dist
	 */
	@SuppressWarnings("unchecked")
	public Entity getClosestVictim(double dist) {

		AxisAlignedBB aabb = AxisAlignedBB.getBoundingBox(this.posX - dist, this.posY - dist, this.posZ - dist, this.posX + dist, this.posY + dist, this.posZ
				+ dist);
		EntitySelectorInRangeByType entsel = new EntitySelectorInRangeByType(this, dist, EntityBlaze.class, EntityAnimal.class);

		List<Entity> entities = this.worldObj.getEntitiesWithinAABBExcludingEntity(this, aabb, entsel);
		if (entities.isEmpty()) {
			return null;
		}
		Entity victim = null;
		boolean hasBlaze = false;
		double closest = Double.MAX_VALUE;

		for (Entity entity : entities) {
			boolean isBlaze = entity instanceof EntityBlaze;

			// If we already have a Blaze in our sights, ignore passives
			if (hasBlaze && !isBlaze) {
				continue;
			}
			double distVsq = this.getDistanceSqToEntity(entity);

			// Blaze distance overrides existing Animal distances
			// Otherwise, closer is better.
			if ((isBlaze && !hasBlaze) || (distVsq < closest)) {
				closest = distVsq;
				victim = entity;
				hasBlaze |= isBlaze;
			}
		}
		return victim;
	}

	@Override
	protected void attackEntity(Entity target, float distance) {

		// Melee distance
		if (this.attackTime <= 0 && distance < 2.0F && target.boundingBox.maxY > this.boundingBox.minY && target.boundingBox.minY < this.boundingBox.maxY) {
			this.attackTime = 20;
			this.attackEntityAsMob(target);
		}
		// Within range (30)
		else if (distance < 30.0F) {
			double dX = target.posX - this.posX;
			double dZ = target.posZ - this.posZ;

			if (this.attackTime == 0) {
				// BLIZZBALL FIGHT!!!! (or an animal)
				if (target instanceof EntityBlizz || target.isCreatureType(EnumCreatureType.creature, false)) {
					EntityBlizzBall blizzBall = new EntityBlizzBall(this.worldObj, this);
					double dSY = target.posY + target.getEyeHeight() - 1.100000023841858D - blizzBall.posY;
					float f1 = MathHelper.sqrt_double(dX * dX + dZ * dZ) * 0.2F;
					blizzBall.setThrowableHeading(dX, dSY + f1, dZ, 1.6F, 12.0F);
					this.playSound("random.bow", 1.0F, 1.0F / (this.rand.nextFloat() * 0.4F + 0.8F));
					this.worldObj.spawnEntityInWorld(blizzBall);

					// Negate target so it re-selects nearest mob
					this.setTarget(null);

					this.attackTime = 80;
					this.firingState = 0;
					this.setInAttackMode(false);
				} else {
					++this.firingState;

					if (this.firingState == 1) {
						this.attackTime = 60;
						this.setInAttackMode(true); // Flary goodness :D
					} else if (this.firingState <= 5) {
						this.attackTime = 6;
					} else {
						this.attackTime = 80; // 100
						this.firingState = 0;
						this.setInAttackMode(false); // Unflary sadness :(
					}
					if (this.firingState > 1) {
						//EntityLivingBase tgt = (EntityLivingBase) target;
						//double dY = tgt.boundingBox.minY + tgt.height / 2.0F - (this.posY + this.height / 2.0F);
						EntityBlizzSlowball ball = new EntityBlizzSlowball(this.worldObj, this);
						ball.posY = this.posY + this.height / 2.0F + 0.5D;

						this.playSound(SOUND_ATTACK, 2.0F, (this.rand.nextFloat() - this.rand.nextFloat()) * 0.2F + 1.0F);
						this.worldObj.spawnEntityInWorld(ball);
					}
				}
			}
			this.rotationYaw = (float) (Math.atan2(dZ, dX) * 180.0D / Math.PI) - 90.0F;
			this.hasAttacked = true;
		}
	}

	@Override
	protected void fall(float distance) {

	}

	@Override
	protected Item getDropItem() {

		return Items.snowball;
	}

	@Override
	protected void dropFewItems(boolean wasHitByPlayer, int looting) {

		if (wasHitByPlayer) {
			// Drop snowballs :D
			int items = this.rand.nextInt(4 + looting);
			for (int i = 0; i < items; i++) {
				this.dropItem(getDropItem(), 1);
			}
			// Drop Blizz Rods
			items = this.rand.nextInt(2 + looting);
			for (int i = 0; i < items; i++) {
				this.entityDropItem(ItemHelper.cloneStack(TFItems.rodBlizz, 1), 0);
			}
		}
	}

	public boolean isInAttackMode() {

		return (this.dataWatcher.getWatchableObjectByte(16) & 1) != 0;
	}

	public void setInAttackMode(boolean mode) {

		byte b0 = this.dataWatcher.getWatchableObjectByte(16);

		if (mode) {
			b0 = (byte) (b0 | 1);
		} else {
			b0 &= -2;
		}
		this.dataWatcher.updateObject(16, Byte.valueOf(b0));
	}

	@Override
	public boolean getCanSpawnHere() {

		int x = MathHelper.floor_double(this.posX);
		int y = MathHelper.floor_double(this.boundingBox.minY);
		int z = MathHelper.floor_double(this.posZ);

		Block block = this.worldObj.getBlock(x, y - 1, z);

		if (block == Blocks.snow || block == Blocks.ice || block == Blocks.snow_layer || block == Blocks.stone && super.isValidLightLevel()) {
			return super.getCanSpawnHere();
		}
		return false;
	}

	@Override
	protected boolean isValidLightLevel() {

		return true;
	}

}
