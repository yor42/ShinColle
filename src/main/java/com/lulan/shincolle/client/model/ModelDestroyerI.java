package com.lulan.shincolle.client.model;


import java.util.Random;

import org.lwjgl.opengl.GL11;

import com.lulan.shincolle.reference.AttrID;
import com.lulan.shincolle.reference.AttrValues;
import com.lulan.shincolle.utility.LogHelper;

import net.minecraft.client.Minecraft;
import net.minecraft.client.model.ModelBase;
import net.minecraft.client.model.ModelRenderer;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.util.MathHelper;

import com.lulan.shincolle.entity.EntityDestroyerI;

/**Created in 2015/1/9
 * �`�N: model class���Ҧ���k���Ofps 60�Ҧ�
 *      �]�N�O������k�@�����|call 60��, ��entity�����@���O20�����ɶ��ܤƳt�פ��P
 */
public class ModelDestroyerI extends ModelBase {
    //fields
	public ModelRenderer PBack;
	public ModelRenderer PNeck;
	public ModelRenderer PHead;
	public ModelRenderer[] PEyeLightL = new ModelRenderer[3];
	public ModelRenderer[] PEyeLightR = new ModelRenderer[3];
	public ModelRenderer PJawBottom;
	public ModelRenderer PBody;
	public ModelRenderer PLegLeft;
	public ModelRenderer PLegLeftEnd;
	public ModelRenderer PLegRight;
	public ModelRenderer PLegRightEnd;
	public ModelRenderer PTail;
	public ModelRenderer PTailLeft;
	public ModelRenderer PTailLeftEnd;
	public ModelRenderer PTailRight;
	public ModelRenderer PTailRightEnd;
	public ModelRenderer PTailEnd;
	//add Kisaragi
	public ModelRenderer PKisaragi00;
	public ModelRenderer PKisaragi01;
	public ModelRenderer PKisaragi02;
	public ModelRenderer PKisaragi03;
	
	private int EntityEmotion;	//�ثe����
	private int EntityFace;		//�ثe�ϥΪ��Y��model
	private int EmotionTime;	//�ثe����
	public float Scale;			//�w�]�j�p��1.0��
	
  public ModelDestroyerI() {

    EntityEmotion = 0;
    EntityFace = 0;
    Scale = 1F;
    EmotionTime = 0;

    textureWidth = 256;
    textureHeight = 128;

    setTextureOffset("PBack.Back", 128, 8);
    setTextureOffset("PNeck.NeckBack", 128, 0);
    setTextureOffset("PNeck.NeckNeck", 128, 28);
    setTextureOffset("PNeck.NeckBody", 0, 70);
    setTextureOffset("PHead.Head", 0, 0);
    setTextureOffset("PEyeLightL0.LEye", 138, 64);
    setTextureOffset("PEyeLightR0.REye", 138, 64);
    setTextureOffset("PEyeLightL1.LEye", 138, 85);
    setTextureOffset("PEyeLightR1.REye", 138, 85);
    setTextureOffset("PEyeLightL2.LEye", 138, 106);
    setTextureOffset("PEyeLightR2.REye", 138, 106);
    setTextureOffset("PHead.ToothTopMid", 96, 0);
    setTextureOffset("PHead.ToothTopRight", 128, 54);
    setTextureOffset("PHead.ToothTopLeft", 128, 54);
    setTextureOffset("PHead.JawTop", 0, 102);
    setTextureOffset("PJawBottom.JawBottom", 92, 64);
    setTextureOffset("PJawBottom.ToothBottomLeft", 96, 19);
    setTextureOffset("PJawBottom.ToothBottomRight", 96, 19);
    setTextureOffset("PJawBottom.ToothBottomMid", 0, 0);
    setTextureOffset("PBody.Body", 0, 64);
    setTextureOffset("PLegLeft.LegLeftFront", 0, 80);
    setTextureOffset("PLegLeftEnd.LegLeftEnd", 0, 90);
    setTextureOffset("PLegRight.LegRightFront", 0, 80);
    setTextureOffset("PLegRightEnd.LegRightEnd", 0, 90);
    setTextureOffset("PTail.TailBack", 128, 16);
    setTextureOffset("PTail.TailBody", 0, 68);
    setTextureOffset("PTailLeft.TailLeftFront", 128, 28);
    setTextureOffset("PTailLeftEnd.TailLeftEnd", 128, 36);
    setTextureOffset("PTailRight.TailRightFront", 128, 28);
    setTextureOffset("PTailRightEnd.TailRightEnd", 128, 36);
    setTextureOffset("PTailEnd.TailEnd", 128, 26);
    //add Kisaragi
    setTextureOffset("PKisaragi00.k00", 66, 102);
    setTextureOffset("PKisaragi01.k01", 114, 102);
    setTextureOffset("PKisaragi02.k02", 92, 102);
    setTextureOffset("PKisaragi03.k03", 92, 102);
    
    PBack = new ModelRenderer(this, "PBack");
    PBack.setRotationPoint(-8F, -16F, 0F);
    setRotation(PBack, 0F, 0F, -0.31F);
      PBack.addBox("Back", -12F, -10F, -12F, 28, 20, 24);
    PNeck = new ModelRenderer(this, "PNeck");
    PNeck.setRotationPoint(15F, 0F, 0F);
    setRotation(PNeck, 0F, 0F, 0.2F);
      PNeck.addBox("NeckBack", -3F, -11F, -13F, 30, 26, 26);
      PNeck.addBox("NeckNeck", 6F, 15F, -10F, 21, 4, 20);
      PNeck.addBox("NeckBody", -8F, 7F, -9F, 18, 14, 18);
    PHead = new ModelRenderer(this, "PHead");
    PHead.setRotationPoint(26F, 0F, 0F);
    setRotation(PHead, 0F, 0F, 0.3F);
      PHead.addBox("Head", -3F, -12F, -16F, 32, 32, 32);
      PHead.addBox("ToothTopMid", 14.5F, 20F, -6F, 4, 6, 12);
      PHead.addBox("ToothTopRight", 0F, 20F, -10F, 18, 6, 4);
      PHead.addBox("ToothTopLeft", 0F, 20F, 6F, 18, 6, 4);
      PHead.addBox("JawTop", -3F, 20F, -11F, 22, 2, 22);
      
    //3 emotion eye
    PEyeLightL[0] = new ModelRenderer(this, "PEyeLightL0");
    PEyeLightR[0] = new ModelRenderer(this, "PEyeLightR0");
    PEyeLightL[0].addBox("LEye", -3F, 0F, 15.1F, 24, 20, 1);
    PEyeLightR[0].addBox("REye", -3F, 0F, -16.1F, 24, 20, 1);
    PEyeLightL[1] = new ModelRenderer(this, "PEyeLightL1");
    PEyeLightR[1] = new ModelRenderer(this, "PEyeLightR1");
    PEyeLightL[1].addBox("LEye", -3F, 0F, 15.1F, 24, 20, 1).isHidden = true;
    PEyeLightR[1].addBox("REye", -3F, 0F, -16.1F, 24, 20, 1).isHidden = true;
    PEyeLightL[2] = new ModelRenderer(this, "PEyeLightL2");
    PEyeLightR[2] = new ModelRenderer(this, "PEyeLightR2");
    PEyeLightL[2].addBox("LEye", -3F, 0F, 15.1F, 24, 20, 1).isHidden = true;
    PEyeLightR[2].addBox("REye", -3F, 0F, -16.1F, 24, 20, 1).isHidden = true;
    
    //add Kisaragi
    PKisaragi00 = new ModelRenderer(this, "PKisaragi00");
    PKisaragi01 = new ModelRenderer(this, "PKisaragi01");
    PKisaragi02 = new ModelRenderer(this, "PKisaragi02");
    PKisaragi03 = new ModelRenderer(this, "PKisaragi03");
    PKisaragi00.setRotationPoint(-7F, -9F, 14F);
    PKisaragi01.setRotationPoint(-7F, -9F, 14F);
    PKisaragi02.setRotationPoint(-7F, -9F, 14F);
    PKisaragi03.setRotationPoint(-7F, -9F, 14F);
    PKisaragi00.addBox("k00", 0F, 0F, 0F, 8, 8, 5);
    PKisaragi01.addBox("k01", -2F, -16F, 1F, 8, 20, 3);
    PKisaragi02.addBox("k02", -7F, -17F, 0.8F, 8, 18, 3);
    PKisaragi03.addBox("k03", -9F, -18F, 0.6F, 8, 18, 3);
    setRotation(PKisaragi01, 0F, 0F, -0.524F);
    setRotation(PKisaragi02, 0F, 0F, -1.396F);
    setRotation(PKisaragi03, 0F, 0F, -2.094F);
    
    PJawBottom = new ModelRenderer(this, "PJawBottom");
    PJawBottom.setRotationPoint(-6F, 18F, 0F);
    setRotation(PJawBottom, 0F, 0F, -0.2F);
      PJawBottom.addBox("JawBottom", -3F, 0F, -10F, 3, 18, 20);
      PJawBottom.addBox("ToothBottomLeft", -1F, 7.5F, 6F, 4, 10, 3);
      PJawBottom.addBox("ToothBottomRight", -1F, 7.5F, -9F, 4, 10, 3);
      PJawBottom.addBox("ToothBottomMid", -1F, 14.5F, -6F, 4, 3, 12);
      PHead.addChild(PJawBottom);
      PHead.addChild(PEyeLightL[0]);
      PHead.addChild(PEyeLightR[0]);
      PHead.addChild(PEyeLightL[1]);
      PHead.addChild(PEyeLightR[1]);
      PHead.addChild(PEyeLightL[2]);
      PHead.addChild(PEyeLightR[2]);
      PHead.addChild(PKisaragi00);
      PHead.addChild(PKisaragi01);
      PHead.addChild(PKisaragi02);
      PHead.addChild(PKisaragi03);
      PNeck.addChild(PHead);
      PBack.addChild(PNeck);
    PBody = new ModelRenderer(this, "PBody");
    PBody.setRotationPoint(0F, 0F, 0F);
      PBody.addBox("Body", -10F, 10F, -11F, 24, 16, 22);
    PLegLeft = new ModelRenderer(this, "PLegLeft");
    PLegLeft.setRotationPoint(-3F, 24F, 6F);
      PLegLeft.addBox("LegLeftFront", -3F, -4F, -1F, 8, 14, 8);
    PLegLeftEnd = new ModelRenderer(this, "PLegLeftEnd");
    PLegLeftEnd.setRotationPoint(1F, 8F, 4F);
      PLegLeftEnd.addBox("LegLeftEnd", -12F, -3F, -4F, 12, 6, 6);
      PLegLeft.addChild(PLegLeftEnd);
      PBody.addChild(PLegLeft);
    PLegRight = new ModelRenderer(this, "PLegRight");
    PLegRight.setRotationPoint(-3F, 24F, -8F);
      PLegRight.addBox("LegRightFront", -3F, -4F, -5F, 8, 14, 8);
    PLegRightEnd = new ModelRenderer(this, "PLegRightEnd");
    PLegRightEnd.setRotationPoint(1F, 8F, -1F);
      PLegRightEnd.addBox("LegRightEnd", -12F, -3F, -3F, 12, 6, 6);
      PLegRight.addChild(PLegRightEnd);
      PBody.addChild(PLegRight);
      PBack.addChild(PBody);
    PTail = new ModelRenderer(this, "PTail");
    PTail.setRotationPoint(-12F, -2F, 0F);
    setRotation(PTail, 0F, 0F, 0.25F);
      PTail.addBox("TailBack", -22F, -6F, -10F, 26, 16, 20);
      PTail.addBox("TailBody", -8F, 2F, -8F, 18, 18, 14);
    PTailLeft = new ModelRenderer(this, "PTailLeft");
    PTailLeft.setRotationPoint(-12F, 4F, 8F);
    setRotation(PTailLeft, 0.5F, 0F, 0.25F);
      PTailLeft.addBox("TailLeftFront", -8F, -4F, 0F, 12, 18, 6);
    PTailLeftEnd = new ModelRenderer(this, "PTailLeftEnd");
    PTailLeftEnd.setRotationPoint(0F, 9F, 5F);
   setRotation(PTailLeftEnd, 0F, 0F, -0.4F);
      PTailLeftEnd.addBox("TailLeftEnd", -24F, -4F, -2F, 24, 12, 4);
      PTailLeft.addChild(PTailLeftEnd);
      PTail.addChild(PTailLeft);
    PTailRight = new ModelRenderer(this, "PTailRight");
    PTailRight.setRotationPoint(-12F, 4F, -8F);
    setRotation(PTailRight, -0.5F, 0F, 0.25F);
      PTailRight.addBox("TailRightFront", -8F, -4F, -6F, 12, 18, 6);
    PTailRightEnd = new ModelRenderer(this, "PTailRightEnd");
    PTailRightEnd.setRotationPoint(0F, 9F, -5F);
    setRotation(PTailRightEnd, 0F, 0F, -0.4F);
      PTailRightEnd.addBox("TailRightEnd", -24F, -4F, -2F, 24, 12, 4);
      PTailRight.addChild(PTailRightEnd);
      PTail.addChild(PTailRight);
    PTailEnd = new ModelRenderer(this, "PTailEnd");
    PTailEnd.setRotationPoint(-22F, 2F, 0F);
    setRotation(PTailEnd, 0F, 0F, 0.3F);
      PTailEnd.addBox("TailEnd", -20F, -6F, -8F, 24, 10, 16);
      PTail.addChild(PTailEnd);
      PBack.addChild(PTail);
  }
  
  public void render(Entity entity, float f, float f1, float f2, float f3, float f4, float f5) {
	setRotationAngles(f, f1, f2, f3, f4, f5, entity);
	
	// Scale, Translate, Rotate
	// GL11.glScalef(this.scale, this.scale, this.scale);
//	GL11.glEnable(GL11.GL_BLEND);			//�}�ҳz���׼Ҧ�
//	GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
//	GL11.glPushMatrix();
	GL11.glScalef(0.5F, 0.45F, 0.45F);	//debug��
	GL11.glRotatef(90F, 0F, 1F, 0F);	//���ҫ��Y����V���~ �]��render�ɽվ�^��
	
	PBack.render(f5);
	
//	GL11.glDisable(GL11.GL_BLEND);		//�����z���׼Ҧ�
//	GL11.glPopMatrix();
  }
  
  private void setRotation(ModelRenderer model, float x, float y, float z) {
    model.rotateAngleX = x;
    model.rotateAngleY = y;
    model.rotateAngleZ = z;
  }
  
  //for idle/run animation
  public void setRotationAngles(float f, float f1, float f2, float f3, float f4, float f5, Entity entity) {
    super.setRotationAngles(f, f1, f2, f3, f4, f5, entity);

    float angleZ = MathHelper.cos(f2*0.125F);
       
    EntityDestroyerI ent = (EntityDestroyerI)entity;
    
    isKisaragi(ent);   
    rollEmotion(ent);    
    motionWatch(f3,f4,angleZ);	//include watch head & normal head
    
    if(ent.isSitting()) {
    	motionSit(ent, angleZ);
    }
    else {
    	motionLeg(f,f1);
    	motionTail(angleZ);
    	
    	//reset sit pose
    	PBack.rotateAngleZ = -0.31F;
    	GL11.glTranslatef(0F, 0.8F, 0F);
    }   

  }


	private void isKisaragi(EntityDestroyerI ent) {
		if(ent.getEntityState(AttrID.State) >= AttrValues.State.EQUIP) {
			PKisaragi00.isHidden = false;
			PKisaragi01.isHidden = false;
			PKisaragi02.isHidden = false;
			PKisaragi03.isHidden = false;
		}
		else {
			PKisaragi00.isHidden = true;
			PKisaragi01.isHidden = true;
			PKisaragi02.isHidden = true;
			PKisaragi03.isHidden = true;
		}
	
  	}
	
	//���U�ʧ@
  	private void motionSit(EntityDestroyerI ent, float angleZ) {		
  		if(ent.getEntityState(AttrID.Emotion) == AttrValues.Emotion.BORED) {
  			GL11.glTranslatef(0F, 0.9F, 0F);	//1.4
  			PBack.rotateAngleZ = 0.6F;
  	  		PNeck.rotateAngleZ = -0.25F;
  	  	    PHead.rotateAngleZ = -0.3F;
  	    	PLegRight.rotateAngleZ = -1F;
  		    PLegLeft.rotateAngleZ = -1F;
  		    PLegRightEnd.rotateAngleZ = -1.1F;
  		    PLegLeftEnd.rotateAngleZ = -1.1F;
  		    PTail.rotateAngleZ = -0.6F;
  	  	    PTailEnd.rotateAngleZ = -0.6F;
  	  	    PJawBottom.rotateAngleZ = -0.7F; 	  	    
  		}
  		else {
  			GL11.glTranslatef(0F, 1.4F, 0F);	//1.4
  			PBack.rotateAngleZ = -0.8F;
  	  		PNeck.rotateAngleZ = -0.3F;
  	    	PLegRight.rotateAngleZ = -0.8F;
  		    PLegLeft.rotateAngleZ = -0.8F;
  		    PLegRightEnd.rotateAngleZ = -1.4F;
  		    PLegLeftEnd.rotateAngleZ = -1.4F;
  		    PTail.rotateAngleZ = 0.4F;
  	  	    PTailEnd.rotateAngleZ = angleZ * 0.2F + 0.4F;
  	  	    PJawBottom.rotateAngleZ = angleZ * 0.05F -0.3F;
  	  	    PHead.rotateAngleZ = angleZ * 0.02F + 0.4F;
  		}
  	}

	//�`���\�ʧ��ڸ�U��
  	private void motionTail(float angleZ) { 	
  	    PTail.rotateAngleZ = angleZ * 0.2F;
  	    PTailEnd.rotateAngleZ = angleZ * 0.3F;
  	    PJawBottom.rotateAngleZ = angleZ * 0.2F -0.3F;
  	}

	//���}���ʭp��
  	private void motionLeg(float f, float f1) {
		//�������} ���ҫ���V�]�� �]���令��Z
	    PLegRight.rotateAngleZ = MathHelper.cos(f * 0.6662F) * 1.4F * f1 - 0.6F;
	    PLegLeft.rotateAngleZ = MathHelper.cos(f * 0.6662F + 3.1415927F) * 1.4F * f1 - 0.6F;
	    PLegRightEnd.rotateAngleZ = MathHelper.sin(f * 0.6662F) * f1 - 0.4F;
	    PLegLeftEnd.rotateAngleZ = MathHelper.sin(f * 0.6662F + 3.1415927F) * f1 - 0.4F;	
	}

  	//�Y���ݤH��ʭp��
	private void motionWatch(float f3, float f4, float angleZ) {
		//�����Y�� �Ϩ�ݤH, ���ݤH�ɫ����\���Y��
	    if(f4 != 0) {
		    PNeck.rotateAngleY = f3 / 160F;		//���k���� �����নrad �Y���H57.29578
		    PNeck.rotateAngleZ = f4 / 130F; 	//�W�U����
		    PHead.rotateAngleY = f3 / 160F;
		    PHead.rotateAngleZ = f4 / 130F;
		    PTail.rotateAngleY = f3 / -130F;	//���ڥH�Ϥ�V�\��
	    }
	    else {
	    	PNeck.rotateAngleY = 0;			//���k���� �����নrad �Y���H57.29578
		    PNeck.rotateAngleZ = 0.2F; 		//�W�U����
		    PHead.rotateAngleY = 0;
		    PHead.rotateAngleZ = angleZ * 0.15F + 0.2F;
		    PTail.rotateAngleY = 0;  	
	    }	
	}

	//�H�������ܪ����� 
    private void rollEmotion(EntityDestroyerI ent) {   	
    	switch(ent.getEntityState(AttrID.Emotion)) {
    	case AttrValues.Emotion.BLINK:	//blink
    		EmotionBlink(ent);
    		break;
    	case AttrValues.Emotion.T_T:	//cry
    	case AttrValues.Emotion.O_O:
    	case AttrValues.Emotion.HUNGRY:
    		if(ent.getStartEmotion() <= 0) setFace(2);
    		break;
    	case AttrValues.Emotion.BORED:	//cry
    		if(ent.getStartEmotion() <= 0) setFace(1);
    		break;
    	default:						//normal face
    		if(ent.getStartEmotion() <= 0) setFace(0); 			    //reset face to 0
    		//roll emotion (3 times) every 6 sec
    		//1 tick in entity = 3 tick in model class (20 vs 60 fps)
    		if(ent.ticksExisted % 120 == 0) {  			
        		int emotionRand = ent.getRNG().nextInt(10);   		
        		if(emotionRand > 7) {
        			EmotionBlink(ent);
        		} 		
        	}
    		break;
    	}	
    }

	//�w���ʧ@, this emotion is CLIENT ONLY, no sync packet required
	private void EmotionBlink(EntityDestroyerI ent) {
		if(ent.getEntityState(AttrID.Emotion) == AttrValues.Emotion.NORMAL) {	//�n�b�S�������A�~������		
			ent.setStartEmotion(ent.ticksExisted);		//�����}�l�ɶ�
			ent.setEntityEmotion(AttrValues.Emotion.BLINK, false);	//�аO������blink
		}
		
		int EmoTime = ent.ticksExisted - ent.getStartEmotion();
 		
    	if(EmoTime > 61) {	//reset face
    		setFace(0);
			ent.setEntityEmotion(AttrValues.Emotion.NORMAL, false);
			ent.setStartEmotion(-1);
    	}
    	else if(EmoTime > 45) {
    		setFace(1);
    	}
    	else if(EmoTime > 35) {
    		setFace(0);
    	}
    	else if(EmoTime > 1) {
    		setFace(1);
    	}
	}
	
	//�]�w��ܪ��y��
	private void setFace(int emo) {
		switch(emo) {
		case 0:
			PEyeLightL[0].isHidden = false;
			PEyeLightR[0].isHidden = false;
			PEyeLightL[1].isHidden = true;
			PEyeLightR[1].isHidden = true;
			PEyeLightL[2].isHidden = true;
			PEyeLightR[2].isHidden = true;
			break;
		case 1:
			PEyeLightL[0].isHidden = true;
			PEyeLightR[0].isHidden = true;
			PEyeLightL[1].isHidden = false;
			PEyeLightR[1].isHidden = false;
			PEyeLightL[2].isHidden = true;
			PEyeLightR[2].isHidden = true;
			break;
		case 2:
			PEyeLightL[0].isHidden = true;
			PEyeLightR[0].isHidden = true;
			PEyeLightL[1].isHidden = true;
			PEyeLightR[1].isHidden = true;
			PEyeLightL[2].isHidden = false;
			PEyeLightR[2].isHidden = false;
			break;
		default:
			break;
		}
	}

}