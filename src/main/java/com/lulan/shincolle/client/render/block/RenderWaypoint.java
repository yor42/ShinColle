package com.lulan.shincolle.client.render.block;

import com.lulan.shincolle.Tags;
import com.lulan.shincolle.client.model.ModelVortex;
import com.lulan.shincolle.client.model.ModelWayPoint;
import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.tileentity.TileEntityWaypoint;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.tileentity.TileEntitySpecialRenderer;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;

public class RenderWaypoint extends TileEntitySpecialRenderer<TileEntityWaypoint> {

    private final ModelWayPoint model_waypoint;

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.TEXTURES_BLOCKS + "blockwaypoint.png");

    public RenderWaypoint(){
        this.model_waypoint = new ModelWayPoint();
    }

    @Override
    public void render(TileEntityWaypoint te, double x, double y, double z, float partialTicks, int destroyStage, float alpha) {
        EntityPlayer player = Minecraft.getMinecraft().player;
        ItemStack stack = player.getHeldItemMainhand();
        Item item = stack.getItem();

        if (item != ModItems.TargetWrench && item != new ItemStack(ModBlocks.BlockWaypoint).getItem()) {
            return;
        }


        BlockPos pos = te.getPos();
        double distX = pos.getX() + 0.5D - player.posX;
        double distY = pos.getY() - 0.75D - player.posY;
        double distZ = pos.getZ() + 0.5D - player.posZ;
        float f1 = MathHelper.sqrt(distX * distX + distZ * distZ);
        float pitch = (float) (Math.atan2(f1, distY));
        float yaw = (float) (Math.atan2(distX, distZ));

        Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURE);
        GlStateManager.pushMatrix();
        GlStateManager.depthMask(ConfigHandler.vortexDepth);
        GlStateManager.translate((float) x + 0.5F, (float) y + 0.5F, (float) z + 0.5F);
        GlStateManager.rotate(yaw * 57.2957F, 0F, 1F, 0F);
        GlStateManager.rotate(pitch * 57.2957F, 1F, 0F, 0F);
        this.model_waypoint.render(0.03125F);
        GlStateManager.depthMask(true);
        GlStateManager.popMatrix();
    }
}
