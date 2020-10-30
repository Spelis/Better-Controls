package chylex.bettercontrols.player;
import chylex.bettercontrols.BetterControlsMod;
import chylex.bettercontrols.config.BetterControlsConfig;
import chylex.bettercontrols.gui.BetterControlsScreen;
import chylex.bettercontrols.input.SprintMode;
import chylex.bettercontrols.input.ToggleTracker;
import chylex.bettercontrols.input.ToggleTrackerForStickyKey;
import chylex.bettercontrols.mixin.AccessCameraFields;
import chylex.bettercontrols.mixin.AccessClientPlayerFields;
import chylex.bettercontrols.mixin.AccessGameRendererFields;
import chylex.bettercontrols.mixin.AccessStickyKeyBindingStateGetter;
import chylex.bettercontrols.util.Key;
import net.minecraft.client.entity.player.ClientPlayerEntity;
import net.minecraft.util.MovementInput;
import net.minecraft.util.math.MathHelper;
import java.lang.ref.WeakReference;
import java.util.function.BooleanSupplier;
import static chylex.bettercontrols.util.Statics.KEY_FORWARD;
import static chylex.bettercontrols.util.Statics.KEY_JUMP;
import static chylex.bettercontrols.util.Statics.KEY_SNEAK;
import static chylex.bettercontrols.util.Statics.KEY_SPRINT;
import static chylex.bettercontrols.util.Statics.MINECRAFT;
import static chylex.bettercontrols.util.Statics.OPTIONS;

public final class PlayerTicker{
	private static PlayerTicker ticker = new PlayerTicker(null);
	
	public static PlayerTicker get(final ClientPlayerEntity player){
		if (ticker.ref.get() != player){
			ticker = new PlayerTicker(player);
		}
		
		return ticker;
	}
	
	private static BetterControlsConfig cfg(){
		return BetterControlsMod.config;
	}
	
	private final WeakReference<ClientPlayerEntity> ref;
	
	private PlayerTicker(final ClientPlayerEntity player){
		this.ref = new WeakReference<>(player);
		setup();
	}
	
	// Logic
	
	private final ToggleTracker toggleSprint = new ToggleTrackerForStickyKey(cfg().keyToggleSprint, KEY_SPRINT, toggled -> OPTIONS.toggleSprint = toggled);
	private final ToggleTracker toggleSneak = new ToggleTrackerForStickyKey(cfg().keyToggleSneak, KEY_SNEAK, toggled -> OPTIONS.toggleCrouch = toggled);
	private final ToggleTracker toggleWalkForward = new ToggleTracker(cfg().keyToggleWalkForward, KEY_FORWARD);
	private final ToggleTracker toggleJump = new ToggleTracker(cfg().keyToggleJump, KEY_JUMP);
	
	private boolean waitingForSprintKeyRelease = false;
	private boolean stopSprintingAfterReleasingSprintKey = false;
	
	private boolean wasHittingObstacle = false;
	private boolean wasSprintingBeforeHittingObstacle = false;
	private int temporarySprintTimer = 0;
	
	private boolean wasSneakingBeforeTouchingGround = false;
	private boolean holdingSneakWhileTouchingGround = false;
	private int temporaryFlyOnGroundTimer = 0;
	
	private void setup(){
		final AccessStickyKeyBindingStateGetter sprint = (AccessStickyKeyBindingStateGetter)KEY_SPRINT;
		BooleanSupplier getter = sprint.getToggleGetter();
		
		if (getter instanceof SprintPressGetter){
			getter = ((SprintPressGetter)getter).getWrapped();
		}
		
		sprint.setToggleGetter(new SprintPressGetter(getter, () -> temporarySprintTimer > 0));
	}
	
	public void atHead(final ClientPlayerEntity player){
		if (FlightHelper.shouldFlyOnGround(player)){
			player.setOnGround(false);
		}
		
		if (!cfg().doubleTapForwardToSprint){
			((AccessClientPlayerFields)player).setTicksLeftToDoubleTapSprint(0);
		}
		
		final SprintMode sprintMode;
		
		if (FlightHelper.isFlyingCreativeOrSpectator(player)){
			sprintMode = cfg().sprintModeWhileFlying;
		}
		else{
			sprintMode = cfg().sprintMode;
		}
		
		final boolean wasSprintToggled = OPTIONS.toggleSprint;
		final boolean isSprintToggled = toggleSprint.tick();
		
		if (temporarySprintTimer > 0){
			stopSprintingAfterReleasingSprintKey = false;
			waitingForSprintKeyRelease = false;
			
			final int nextTemporarySprintTimer = temporarySprintTimer - 1;
			temporarySprintTimer = 0;
			
			if (!Key.isPressed(KEY_SPRINT) && Key.isPressed(KEY_FORWARD)){
				temporarySprintTimer = nextTemporarySprintTimer;
			}
			else if (sprintMode == SprintMode.TAP_TO_TOGGLE){
				stopSprintingAfterReleasingSprintKey = true;
			}
		}
		
		if (isSprintToggled){
			stopSprintingAfterReleasingSprintKey = false;
			waitingForSprintKeyRelease = false;
		}
		else if (wasSprintToggled){
			stopSprintingAfterReleasingSprintKey = true;
			waitingForSprintKeyRelease = true;
		}
		else if (sprintMode == SprintMode.TAP_TO_TOGGLE){
			if (Key.isPressed(KEY_SPRINT)){
				if (!waitingForSprintKeyRelease){
					waitingForSprintKeyRelease = true;
					stopSprintingAfterReleasingSprintKey = player.isSprinting();
				}
			}
			else{
				if (player.isSprinting() && !waitingForSprintKeyRelease){
					stopSprintingAfterReleasingSprintKey = false;
				}
				
				waitingForSprintKeyRelease = false;
			}
		}
		else if (sprintMode == SprintMode.HOLD){
			if (Key.isPressed(KEY_SPRINT)){
				stopSprintingAfterReleasingSprintKey = true;
			}
		}
		
		if (stopSprintingAfterReleasingSprintKey && !Key.isPressed(KEY_SPRINT)){
			stopSprintingAfterReleasingSprintKey = false;
			waitingForSprintKeyRelease = false;
			player.setSprinting(false);
		}
		
		toggleSneak.tick();
	}
	
	public void afterInputAssignsPressingForward(final MovementInput input){
		if (MINECRAFT.currentScreen == null){
			input.forwardKeyDown |= toggleWalkForward.tick();
		}
	}
	
	public void afterInputTick(final ClientPlayerEntity player){
		if (MINECRAFT.currentScreen == null && !player.abilities.isFlying){
			player.movementInput.jump |= toggleJump.tick();
		}
		
		if (FlightHelper.isFlyingCreativeOrSpectator(player)){
			final float flightSpeed = FlightHelper.getFlightSpeed(player);
			
			if (flightSpeed > 0F){
				player.abilities.setFlySpeed(flightSpeed);
			}
			
			final float verticalVelocity = FlightHelper.getExtraVerticalVelocity(player);
			
			if (!MathHelper.epsilonEquals(verticalVelocity, 0F) && player == MINECRAFT.getRenderViewEntity()){
				int direction = 0;
				
				if (player.movementInput.sneaking){
					--direction;
				}
				
				if (player.movementInput.jump){
					++direction;
				}
				
				if (direction != 0){
					player.setMotion(player.getMotion().add(0D, flightSpeed * verticalVelocity * direction, 0D));
				}
			}
		}
		
		if (cfg().resumeSprintingAfterHittingObstacle){
			if (wasHittingObstacle != player.collidedHorizontally){
				if (!wasHittingObstacle){
					wasSprintingBeforeHittingObstacle = player.isSprinting() || Key.isPressed(KEY_SPRINT);
				}
				else if (wasSprintingBeforeHittingObstacle){
					wasSprintingBeforeHittingObstacle = false;
					temporarySprintTimer = 10;
				}
				
				// collision also stops when the player lets go of movement keys
				wasHittingObstacle = player.collidedHorizontally;
			}
		}
		else{
			wasHittingObstacle = player.collidedHorizontally;
			wasSprintingBeforeHittingObstacle = false;
		}
		
		if (cfg().disableChangingFovWhileFlying && FlightHelper.isFlyingCreativeOrSpectator(player)){
			((AccessGameRendererFields)MINECRAFT.gameRenderer).setMovementFovMultiplier(1F);
		}
	}
	
	public void afterSuperCall(final ClientPlayerEntity player){
		if (FlightHelper.shouldFlyOnGround(player)){
			final boolean isSneaking = player.isSneaking();
			final boolean isOnGround = player.isOnGround();
			
			if (!isSneaking){
				wasSneakingBeforeTouchingGround = false;
			}
			else if (!isOnGround){
				wasSneakingBeforeTouchingGround = true;
			}
			
			if (!isOnGround){
				holdingSneakWhileTouchingGround = false;
			}
			else{
				boolean cancelLanding = true;
				
				if (!wasSneakingBeforeTouchingGround){
					if (isSneaking){
						holdingSneakWhileTouchingGround = true;
					}
					else if (holdingSneakWhileTouchingGround){
						player.abilities.isFlying = false;
						player.sendPlayerAbilities();
						cancelLanding = false;
					}
				}
				
				if (cancelLanding){
					player.setOnGround(false);
				}
			}
		}
		else{
			wasSneakingBeforeTouchingGround = false;
			holdingSneakWhileTouchingGround = false;
		}
		
		if (player.isCreative()){
			if (Key.wasPressed(cfg().keyToggleFlight)){
				final boolean isFlying = !player.abilities.isFlying;
				
				player.abilities.isFlying = isFlying;
				player.sendPlayerAbilities();
				
				if (isFlying){
					temporaryFlyOnGroundTimer = 10;
				}
			}
			
			if (temporaryFlyOnGroundTimer > 0){
				if (player.isSneaking()){
					temporaryFlyOnGroundTimer = 0;
				}
				else{
					--temporaryFlyOnGroundTimer;
					player.setOnGround(false);
				}
			}
		}
		else{
			temporaryFlyOnGroundTimer = 0;
		}
		
		if (!cfg().sneakingMovesCameraSmoothly){
			final AccessCameraFields camera = (AccessCameraFields)MINECRAFT.gameRenderer.getActiveRenderInfo();
			
			if (camera.getFocusedEntity() == player){
				camera.setCameraY(player.getEyeHeight());
			}
		}
		
		if (Key.wasPressed(cfg().keyResetAllToggles)){
			toggleSprint.reset();
			toggleSneak.reset();
			toggleWalkForward.reset();
			toggleJump.reset();
		}
		
		if (Key.isPressed(cfg().keyOpenMenu)){
			MINECRAFT.displayGuiScreen(new BetterControlsScreen(null));
		}
	}
}
