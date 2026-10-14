package com.lulan.shincolle.client.particle;

import com.lulan.shincolle.Tags;
import net.minecraft.client.Minecraft;
import net.minecraft.client.particle.Particle;
import net.minecraft.client.renderer.BufferBuilder;
import net.minecraft.client.renderer.GlStateManager;
import net.minecraft.client.renderer.GlStateManager.DestFactor;
import net.minecraft.client.renderer.GlStateManager.SourceFactor;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.Tessellator;
import net.minecraft.client.renderer.vertex.DefaultVertexFormats;
import net.minecraft.entity.Entity;
import net.minecraft.util.ResourceLocation;
import net.minecraft.world.World;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;
import org.lwjgl.opengl.GL11;


/**
 * LINE PARTICLE
 */
@SideOnly(Side.CLIENT)
public class ParticleLine extends Particle {

    private static final ResourceLocation TEXTURE = new ResourceLocation(Tags.TEXTURES_PARTICLE + "ParticleGradientLine.png");
    private final int particleType;
    private final float[] parms;
    private final double[] vtx = new double[36];    // 3 planes x 4 vertices x xyz, reused every frame


    public ParticleLine(World world, int type, float[] parms) {
        super(world, 0D, 0D, 0D);
        this.setSize(0F, 0F);
        this.motionX = 0D;
        this.motionZ = 0D;
        this.motionY = 0D;
        this.parms = parms;
        this.particleType = type;
        this.canCollide = false;

        switch (type) {
            /**
             * parms: height, width forward, width backward, R, G, B, A, px, py, pz, mx, my, mz
             */
            case 0:        //high speed blur
                this.particleMaxAge = 50;
                this.particleRed = parms[3];
                this.particleGreen = parms[4];
                this.particleBlue = parms[5];
                this.particleAlpha = parms[6];
                this.posX = parms[7];
                this.posY = parms[8];
                this.posZ = parms[9];
                break;
        }

        //init pos
        this.prevPosX = this.posX;
        this.prevPosY = this.posY;
        this.prevPosZ = this.posZ;
    }

    @Override
    public void renderParticle(BufferBuilder render, Entity entity, float ptick, float cosYaw, float cosPitch, float sinYaw, float sinYawsinPitch, float cosYawsinPitch) {
        //particle是以玩家視野來render, 因此座標要扣掉interpPos轉換為玩家視野座標
        double px = this.posX - interpPosX;
        double py = this.posY - interpPosY;
        double pz = this.posZ - interpPosZ;
        // no per-frame arrays: offsets are scalars, plane vertices go into the reusable vtx buffer
        double h0 = parms[0] * parms[10], h1 = parms[0] * parms[11], h2 = parms[0] * parms[12];
        double f0 = parms[1] * parms[10], f1 = parms[1] * parms[11], f2 = parms[1] * parms[12];
        double b0 = -parms[2] * parms[10], b1 = -parms[2] * parms[11], b2 = -parms[2] * parms[12];
        double[] v = this.vtx;

        //三種正交方式: 若vec為(x, y, z), 則三種簡單正交為(-z, 0, x), (0, -z, y), (-y, x, 0)
        //三種正交都畫出來使各種方向看過去皆有機會看到整個面
        setVtx(v, 0, f0, f1 - h2, f2 + h1);
        setVtx(v, 3, f0, f1 + h2, f2 - h1);
        setVtx(v, 6, b0, b1 + h2, b2 - h1);
        setVtx(v, 9, b0, b1 - h2, b2 + h1);
        setVtx(v, 12, f0 - h2, f1, f2 + h0);
        setVtx(v, 15, f0 + h2, f1, f2 - h0);
        setVtx(v, 18, b0 - h2, b1, b2 + h0);
        setVtx(v, 21, b0 + h2, b1, b2 - h0);
        setVtx(v, 24, f0 - h1, f1 + h0, f2);
        setVtx(v, 27, f0 + h1, f1 - h0, f2);
        setVtx(v, 30, b0 - h1, b1 + h0, b2);
        setVtx(v, 33, b0 + h1, b1 - h0, b2);


        Minecraft.getMinecraft().renderEngine.bindTexture(TEXTURE);

        GlStateManager.pushMatrix();
        GlStateManager.enableBlend();
        GlStateManager.blendFunc(SourceFactor.SRC_ALPHA, DestFactor.ONE_MINUS_SRC_ALPHA);
        GlStateManager.depthMask(false);
        GlStateManager.disableLighting();

        //start tess
        render.begin(GL11.GL_QUADS, DefaultVertexFormats.POSITION_TEX_COLOR);
        OpenGlHelper.setLightmapTextureCoords(OpenGlHelper.lightmapTexUnit, 240F, 240F);

        for (int base = 0; base < 36; base += 12) {
            //front plane
            putVtx(render, px, py, pz, base, 0, 1D, 1D);
            putVtx(render, px, py, pz, base, 3, 1D, 0D);
            putVtx(render, px, py, pz, base, 6, 0D, 0D);
            putVtx(render, px, py, pz, base, 9, 0D, 1D);
            //back plane
            putVtx(render, px, py, pz, base, 9, 0D, 1D);
            putVtx(render, px, py, pz, base, 6, 0D, 0D);
            putVtx(render, px, py, pz, base, 3, 1D, 0D);
            putVtx(render, px, py, pz, base, 0, 1D, 1D);
        }

        Tessellator.getInstance().draw();

        GlStateManager.enableLighting();
        GlStateManager.depthMask(false);
        GlStateManager.disableBlend();
        GlStateManager.popMatrix();
    }

    private static void setVtx(double[] v, int i, double x, double y, double z) {
        v[i] = x;
        v[i + 1] = y;
        v[i + 2] = z;
    }

    private void putVtx(BufferBuilder render, double px, double py, double pz, int base, int off, double u, double w) {
        int i = base + off;
        render.pos(px + this.vtx[i], py + this.vtx[i + 1], pz + this.vtx[i + 2]).tex(u, w).color(this.particleRed, this.particleGreen, this.particleBlue, this.particleAlpha).endVertex();
    }

    //layer: 0:particle 1:terrain 2:items 3:custom?
    @Override
    public int getFXLayer() {
        return 3;
    }

    /**
     * Called to update the entity's position/logic.
     */
    @Override
    public void onUpdate() {
        if (this.particleAge++ > this.particleMaxAge) {
            this.setExpired();
        }

        switch (this.particleType) {
            case 0:
                this.parms[0] *= 0.88F;
                this.parms[2] *= 0.85F;
                this.particleAlpha *= 0.9F;
                break;
        }
    }


}
