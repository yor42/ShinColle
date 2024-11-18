package com.lulan.shincolle.client.render;

import com.lulan.shincolle.Tags;
import com.lulan.shincolle.client.model.*;
import com.lulan.shincolle.reference.ID;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.renderer.entity.Render;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.entity.EntityLiving;
import net.minecraft.util.ResourceLocation;
import net.minecraftforge.fml.client.registry.IRenderFactory;

import javax.annotation.Nonnull;

public class RenderSummonEntity extends RenderBasic {

    //textures
    public static final ResourceLocation TEX_Airplane = new ResourceLocation(Tags.TEXTURES_ENTITY + "EntityAircraft.png");
    public static final ResourceLocation TEX_AirplaneT = new ResourceLocation(Tags.TEXTURES_ENTITY + "EntityAirplaneT.png");
    public static final ResourceLocation TEX_AirplaneTako = new ResourceLocation(Tags.TEXTURES_ENTITY + "EntityAircraftTakoyaki.png");
    public static final ResourceLocation TEX_AirplaneZero = new ResourceLocation(Tags.TEXTURES_ENTITY + "EntityAirplaneZero.png");
    public static final ResourceLocation TEX_FloatingFort = new ResourceLocation(Tags.TEXTURES_ENTITY + "EntityFloatingFort.png");
    public static final ResourceLocation TEX_Rensouhou = new ResourceLocation(Tags.TEXTURES_ENTITY + "EntityRensouhou.png");
    public static final ResourceLocation TEX_RensouhouS = new ResourceLocation(Tags.TEXTURES_ENTITY + "EntityRensouhouS.png");
    //factory
    public static final FactoryDefault FACTORY_SUMMON = new FactoryDefault();
    public static ModelBase MD_Airplane = new ModelAirplane();
    public static ModelBase MD_AirplaneT = new ModelAirplaneT();
    public static ModelBase MD_AirplaneTako = new ModelTakoyaki();
    public static ModelBase MD_AirplaneZero = new ModelAirplaneZero();
    public static ModelBase MD_FloatingFort = new ModelFloatingFort();
    public static ModelBase MD_Rensouhou = new ModelRensouhou();
    public static ModelBase MD_RensouhouS = new ModelRensouhouS();


    public RenderSummonEntity(RenderManager rm) {
        super(rm);
    }

    @Override
    @Nonnull
    protected ResourceLocation getEntityTexture(@Nonnull EntityLiving entity) {
        switch (this.shipClass) {
            case ID.ShipMisc.Airplane:
                return TEX_Airplane;
            case ID.ShipMisc.AirplaneT:
                return TEX_AirplaneT;
            case ID.ShipMisc.AirplaneTako:
                return TEX_AirplaneTako;
            case ID.ShipMisc.AirplaneZero:
                return TEX_AirplaneZero;
            case ID.ShipMisc.FloatingFort:
                return TEX_FloatingFort;
            case ID.ShipMisc.Rensouhou:
                return TEX_Rensouhou;
            case ID.ShipMisc.RensouhouS:
                return TEX_RensouhouS;
            default:    //default texture
                return TEX_Rensouhou;
        }//end switch
    }

    /**
     * set mainModel, shadowSize, scale
     */
    @Override
    protected void setModel() {
        switch (this.shipClass) {
            case ID.ShipMisc.Airplane:
                this.mainModel = MD_Airplane;
                break;
            case ID.ShipMisc.AirplaneT:
                this.mainModel = MD_AirplaneT;
                break;
            case ID.ShipMisc.AirplaneTako:
                this.mainModel = MD_AirplaneTako;
                break;
            case ID.ShipMisc.AirplaneZero:
                this.mainModel = MD_AirplaneZero;
                break;
            case ID.ShipMisc.FloatingFort:
                this.mainModel = MD_FloatingFort;
                break;
            case ID.ShipMisc.Rensouhou:
                this.mainModel = MD_Rensouhou;
                break;
            case ID.ShipMisc.RensouhouS:
                this.mainModel = MD_RensouhouS;
                break;
            default:    //default model
                this.mainModel = MD_Rensouhou;
                break;
        }//end switch
    }

    @Override
    protected void setMiscModel() {
    }

    /**
     * set shadow size
     */
    @Override
    protected void setShadowSize() {
        switch (this.shipClass) {
            case ID.ShipMisc.Airplane:
            case ID.ShipMisc.AirplaneZero:
            case ID.ShipMisc.FloatingFort:
                this.shadowSize = 0.5F;
                break;
            case ID.ShipMisc.AirplaneT:
            case ID.ShipMisc.AirplaneTako:
            case ID.ShipMisc.Rensouhou:
            case ID.ShipMisc.RensouhouS:
                this.shadowSize = 0.7F;
                break;
            default:    //default model
                this.shadowSize = 1F;
                break;
        }//end switch
    }

    public static class FactoryDefault implements IRenderFactory<EntityLiving> {
        @Override
        public Render<? super EntityLiving> createRenderFor(RenderManager rm) {
            return new RenderSummonEntity(rm);
        }
    }


}

