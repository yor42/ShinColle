package com.lulan.shincolle.client.gui;

import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL12;

import com.lulan.shincolle.entity.BasicEntityShip;
import com.lulan.shincolle.inventory.ContainerShipInventory;
import com.lulan.shincolle.inventory.ShipInventory;
import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.tileentity.TileEntitySmallShipyard;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.inventory.GuiContainer;
import net.minecraft.client.renderer.OpenGlHelper;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.entity.RenderManager;
import net.minecraft.client.resources.I18n;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.SharedMonsterAttributes;
import net.minecraft.entity.player.InventoryPlayer;
import net.minecraft.util.MathHelper;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.StatCollector;

/**ShipTypeIcon(157,18) 
 * NameIcon: LargeShip(0,0)(40x42) SmallShip(0,43)(30x30) 
 *           驅逐(41,0)(28x28) 輕巡(41,29) 重巡(41,58) 雷巡(41,87) 補給(12,74)
 *           戰艦(70,0) 航母(70,29) 輕母(70,58) 姬(70,87) 潛水(99,0) 浮游(99,29)
 *           
 * Color note:gold:16766720 gray:4210752 dark-gray:3158064 white:16777215 green:65280
 *            yellow:16776960 orange:16753920 red:16711680 cyan:65535
 *            magenta:16711935
 */
public class GuiShipInventory extends GuiContainer {

	private BasicEntityShip entity;
	private InventoryPlayer player;
	private float xMouse,yMouse;
	private static final ResourceLocation guiBackground = new ResourceLocation(Reference.TEXTURES_GUI+"GuiShipInventory.png");
	private static final ResourceLocation guiNameicon = new ResourceLocation(Reference.TEXTURES_GUI+"GuiNameIcon.png");
	//ship type icon array
	private static final short[][] shipTypeIcon = {
		{41,0}, {41,0}, {41,0}, {41,0}, {41,29}, {41,29}, {41,29}, {41,29},
		{41,87}, {41,58}, {41,58}, {70,58}, {70,29}, {70,0}, {70,0}, {70,0},
		{12,74}, {99,0}, {99,0}, {99,0}, {70,87}, {70,87}, {70,87}, {70,87},
		{99,29}, {99,29}, {70,87}, {70,87}, {70,87}, {70,87}, {70,87}, {70,87}, {70,87}};
	//ship name icon array
	private static final short[][] shipNameIcon = {
		{128,0}, {139,0}, {150,0}, {161,0}, {172,0}, {183,0}, {194,0}, {205,0},
		{216,0}, {227,0}, {238,0}, {128,60}, {139,60}, {150,60}, {161,60}, {172,60},
		{183,60}, {194,60}, {205,60}, {216,60}, {227,60}, {238,60}, {128,120}, {139,120},
		{150,120}, {161,120}, {172,120}, {183,120}, {194,120}, {205,120}, {216,120}, 
		{227,120}, {238,120}};
	
	public GuiShipInventory(InventoryPlayer invPlayer, BasicEntityShip entity1) {
		super(new ContainerShipInventory(invPlayer, entity1));
		this.entity = entity1;
		this.player = invPlayer;
		this.xSize = 250;
		this.ySize = 214;
	}
	
	//GUI前景: 文字 
	@Override
	protected void drawGuiContainerForegroundLayer(int i, int j) {
		//取得gui顯示名稱
		String titlename = entity.getCustomNameTag();	//get type name from nbt
		
		//畫出字串 parm: string, x, y, color, (是否dropShadow)
		//draw entity name (title) 
		this.fontRendererObj.drawString(titlename, 8, 6, 0);
			
		drawAttributes();	
		
	}

	//GUI背景: 背景圖片
	@Override
	protected void drawGuiContainerBackgroundLayer(float par1,int par2, int par3) {
		GL11.glColor4f(1.0F, 1.0F, 1.0F, 1.0F);	//RGBA
		
		//draw background
        Minecraft.getMinecraft().getTextureManager().bindTexture(guiBackground);
        drawTexturedModalRect(guiLeft, guiTop, 0, 0, xSize, ySize);
        
        //draw level, ship type icon
        Minecraft.getMinecraft().getTextureManager().bindTexture(guiNameicon);
        if(entity.ShipLevel > 75) {
        	drawTexturedModalRect(guiLeft+157, guiTop+18, 0, 0, 40, 42);
        	drawTexturedModalRect(guiLeft+159, guiTop+22, shipTypeIcon[entity.ShipID][0], shipTypeIcon[entity.ShipID][1], 28, 28);
        }
        else {
        	drawTexturedModalRect(guiLeft+157, guiTop+18, 0, 43, 30, 30);
        	drawTexturedModalRect(guiLeft+157, guiTop+18, shipTypeIcon[entity.ShipID][0], shipTypeIcon[entity.ShipID][1], 28, 28);
        }
        
        //draw left bottom name
        drawTexturedModalRect(guiLeft+166, guiTop+63, shipNameIcon[entity.ShipID][0], shipNameIcon[entity.ShipID][1], 11, 59);
        
        //draw entity model
        drawEntityModel(guiLeft+210, guiTop+100, 25, (float)(guiLeft + 200 - xMouse), (float)(guiTop + 50 - yMouse), this.entity);
          
	}
	
	//get new mouseX,Y and redraw gui
	@Override
	public void drawScreen(int mouseX, int mouseY, float f) {
		super.drawScreen(mouseX, mouseY, f);
		xMouse = mouseX;
		yMouse = mouseY;
	}
	
	//draw entity model, copy from player inventory class
	public static void drawEntityModel(int x, int y, int scale, float yaw, float pitch, BasicEntityShip entity) {		
		GL11.glEnable(GL11.GL_COLOR_MATERIAL);
		GL11.glPushMatrix();
		GL11.glTranslatef(x, y, 50.0F);
		GL11.glScalef(-scale, scale, scale);
		GL11.glRotatef(180.0F, 0.0F, 0.0F, 1.0F);
		float f2 = entity.renderYawOffset;
		float f3 = entity.rotationYaw;
		float f4 = entity.rotationPitch;
		float f5 = entity.prevRotationYawHead;
		float f6 = entity.rotationYawHead;
		GL11.glRotatef(135.0F, 0.0F, 1.0F, 0.0F);
		RenderHelper.enableStandardItemLighting();
		GL11.glRotatef(-135.0F, 0.0F, 1.0F, 0.0F);
		GL11.glRotatef(-((float) Math.atan(pitch / 40.0F)) * 20.0F, 1.0F, 0.0F, 0.0F);
		entity.renderYawOffset = (float) Math.atan(yaw / 40.0F) * 20.0F;
		entity.rotationYaw = (float) Math.atan(yaw / 40.0F) * 40.0F;
		entity.rotationPitch = -((float) Math.atan(pitch / 40.0F)) * 20.0F;
		entity.rotationYawHead = entity.rotationYaw;
		entity.prevRotationYawHead = entity.rotationYaw;		
		GL11.glTranslatef(0.0F, entity.yOffset, 0.0F);
		RenderManager.instance.playerViewY = 180.0F;
		RenderManager.instance.renderEntityWithPosYaw(entity, 0.0D, 0.0D, 0.0D, 0.0F, 1.0F);
		entity.renderYawOffset = f2;
		entity.rotationYaw = f3;
		entity.rotationPitch = f4;
		entity.prevRotationYawHead = f5;
		entity.rotationYawHead = f6;
		GL11.glPopMatrix();
		RenderHelper.disableStandardItemLighting();
		GL11.glDisable(GL12.GL_RESCALE_NORMAL);
		OpenGlHelper.setActiveTexture(OpenGlHelper.lightmapTexUnit);
		GL11.glDisable(GL11.GL_TEXTURE_2D);
		OpenGlHelper.setActiveTexture(OpenGlHelper.defaultTexUnit);
	}

	//draw level,hp,atk,def...
	private void drawAttributes() {
		String shiplevel = String.valueOf(entity.ShipLevel);
		int hpCurrent = MathHelper.ceiling_float_int(entity.getHealth());
		int hpMax = (int)entity.getMaxHealth();
		short atkEqip = entity.AttrEquipShort[AttrID.ATK];
		short atkBase = (short) (entity.AttrFinalShort[AttrID.ATK] - atkEqip);
		short defEqip = (short) entity.AttrEquipShort[AttrID.DEF];
		short defBase = (short) (entity.AttrFinalShort[AttrID.DEF] - defEqip);
		float spdEqip = entity.AttrEquipFloat[AttrID.SPD];
		float spdBase = entity.AttrFinalFloat[AttrID.SPD] - spdEqip;
		float movEqip = entity.AttrEquipFloat[AttrID.MOV];
		float movBase = entity.AttrFinalFloat[AttrID.MOV] - movEqip;
		float hitEqip = entity.AttrEquipFloat[AttrID.HIT];
		float hitBase = entity.AttrFinalFloat[AttrID.HIT] - hitEqip;
		int color = 0;
		
		//draw attribute name 
		this.fontRendererObj.drawStringWithShadow("Level", 196, 6, 65535);
		this.fontRendererObj.drawStringWithShadow("HP", 132, 6, 65535);
		this.fontRendererObj.drawString(I18n.format("gui.shincolle:firepower"), 87, 21, 3158064);
		this.fontRendererObj.drawString(I18n.format("gui.shincolle:armor"), 87, 41, 3158064);
		this.fontRendererObj.drawString(I18n.format("gui.shincolle:movespeed"), 87, 61, 3158064);
		this.fontRendererObj.drawString(I18n.format("gui.shincolle:attackspeed"), 87, 81, 3158064);
		this.fontRendererObj.drawString(I18n.format("gui.shincolle:range"), 87, 101, 3158064);
		
		//draw attribute value 
		//draw level: 150->gold other->white
		if(entity.ShipLevel < 150) {
			color = 16777215;  //white
		}
		else {
			color = 16766720;  //gold	
		}
		this.fontRendererObj.drawStringWithShadow(shiplevel, xSize-7-this.fontRendererObj.getStringWidth(shiplevel), 6, color);
		
		//draw hp / maxhp, if currHP < maxHP, use darker color
		color = pickBonusColor(entity.BonusPoint[AttrID.HP]);
		this.fontRendererObj.drawStringWithShadow("/"+String.valueOf(hpMax), 148 + this.fontRendererObj.getStringWidth(String.valueOf(hpCurrent)), 6, color);
		if(hpCurrent < hpMax) {
			switch(entity.BonusPoint[AttrID.HP]) {
			case 0:
				color = 16119285;	//gray
				break;
			case 1:
				color = 13421568;	//dark yellow
				break;
			case 2:
				color = 16747520;	//dark orange
				break;
			default:
				color = 13107200;	//dark red
				break;
			}
		}
		this.fontRendererObj.drawStringWithShadow(String.valueOf(hpCurrent), 147, 6, color);	
				
		//draw firepower
		color = pickBonusColor(entity.BonusPoint[AttrID.ATK]);
		this.fontRendererObj.drawStringWithShadow(String.valueOf(atkBase), 87, 31, color);
		this.fontRendererObj.drawStringWithShadow("+"+String.valueOf(atkEqip), 120, 31, 65280);
		
		//draw armor
		color = pickBonusColor(entity.BonusPoint[AttrID.DEF]);
		this.fontRendererObj.drawStringWithShadow(String.valueOf(defBase), 87, 51, color);
		this.fontRendererObj.drawStringWithShadow("+"+String.valueOf(defEqip), 120, 51, 65280);
		
		//draw attack speed
		color = pickBonusColor(entity.BonusPoint[AttrID.SPD+3]);
		this.fontRendererObj.drawStringWithShadow(String.format("%.2f", spdBase), 87, 71, color);
		this.fontRendererObj.drawStringWithShadow("+"+String.format("%.2f", spdEqip), 120, 71, 65280);
		
		//draw movement speed
		color = pickBonusColor(entity.BonusPoint[AttrID.MOV+3]);
		this.fontRendererObj.drawStringWithShadow(String.format("%.2f", movBase), 87, 91, color);
		this.fontRendererObj.drawStringWithShadow("+"+String.format("%.2f", movEqip), 120, 91, 65280);
				
		//draw range
		color = pickBonusColor(entity.BonusPoint[AttrID.HIT+3]);
		this.fontRendererObj.drawStringWithShadow(String.format("%.2f", hitBase), 87, 111, color);
		this.fontRendererObj.drawStringWithShadow("+"+String.format("%.2f", hitEqip), 120, 111, 65280);
	}

	//0:white 1:yellow 2:orange 3:red
	private int pickBonusColor(byte b) {
		switch(b) {
		case 0:
			return 16777215;	//white
		case 1:
			return 16776960;	//yellow
		case 2:
			return 16753920;	//orange
		default:
			return 16711680;	//red
		}
	}
	

}