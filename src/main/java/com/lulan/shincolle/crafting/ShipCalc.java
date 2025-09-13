package com.lulan.shincolle.crafting;

import com.lulan.shincolle.handler.ConfigHandler;
import com.lulan.shincolle.init.ModBlocks;
import com.lulan.shincolle.init.ModItems;
import com.lulan.shincolle.reference.ID;
import com.lulan.shincolle.utility.CalcHelper;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.MathHelper;

import java.util.*;

/**
 * SHIP SPAWN CALC
 * <p>
 * Ship Build Rate: first roll -> second roll
 * 1. FIRST: roll ship type
 * <p>
 * 2. SECOND: roll ships of the type
 */
public class ShipCalc {

    private static final float[] baseRate = new float[]{64F, 16F, 4F, 1F};
    private static final Random rand = new Random();
    //roll table
    private static final List<int[]> EquipSmall = new ArrayList<int[]>();
    private static final List<int[]> EquipLarge = new ArrayList<int[]>();

    //init roll table
    static {
        /*roll table: 0:ship id, 1:material mean, 2:modified material type
          material mean: material amount correspond to normal dist, high = need more materials
          modified material type: mat can increase build rate: -1:none 0:grudge 1:metal 2:ammo 3:poly
         */
        //small build
        EquipSmall.add(new int[]{ID.ShipClass.DDI, 80, 0});
        EquipSmall.add(new int[]{ID.ShipClass.DDRO, 90, 0});
        EquipSmall.add(new int[]{ID.ShipClass.DDHA, 100, 0});
        EquipSmall.add(new int[]{ID.ShipClass.DDNI, 110, 0});
        EquipSmall.add(new int[]{ID.ShipClass.APWA, 120, 1});
        EquipSmall.add(new int[]{ID.ShipClass.SSKA, 140, 2});
        EquipSmall.add(new int[]{ID.ShipClass.SSYO, 160, 2});
        EquipSmall.add(new int[]{ID.ShipClass.SSSO, 180, 2});
        EquipSmall.add(new int[]{ID.ShipClass.CARI, 200, 2});
        EquipSmall.add(new int[]{ID.ShipClass.CANE, 256, 2});

        //large build
        EquipLarge.add(new int[]{ID.ShipClass.DDHime, 500, 0});
        EquipLarge.add(new int[]{ID.ShipClass.CVWO, 650, 3});
        EquipLarge.add(new int[]{ID.ShipClass.BBTA, 800, 2});
        EquipLarge.add(new int[]{ID.ShipClass.BBRU, 800, 2});
        EquipLarge.add(new int[]{ID.ShipClass.CAHime, 2000, 2});
        EquipLarge.add(new int[]{ID.ShipClass.NorthernHime, 2600, 1});
        EquipLarge.add(new int[]{ID.ShipClass.SSNH, 2600, 2});
        EquipLarge.add(new int[]{ID.ShipClass.IsolatedHime, 2700, 1});
        EquipLarge.add(new int[]{ID.ShipClass.HarbourHime, 2800, 1});
        EquipLarge.add(new int[]{ID.ShipClass.AirfieldHime, 3000, 1});
        EquipLarge.add(new int[]{ID.ShipClass.CVHime, 3000, 3});
        EquipLarge.add(new int[]{ID.ShipClass.SSHime, 3500, 2});
        EquipLarge.add(new int[]{ID.ShipClass.BBRE, 3800, 2});
        EquipLarge.add(new int[]{ID.ShipClass.BBHime, 4600, 2});
        EquipLarge.add(new int[]{ID.ShipClass.MidwayHime, 4800, 1});
        EquipLarge.add(new int[]{ID.ShipClass.CVWD, 5000, 3});
    }


    //get material amount from nbt data
    private static int[] getMatAmount(ItemStack itemstack) {
        int[] matAmount = {0, 0, 0, 0};    //grudge, abyssium, ammo, polymetal

        if (itemstack.hasTagCompound()) {
            assert itemstack.getTagCompound() != null;
            matAmount[0] = itemstack.getTagCompound().getInteger("Grudge");
            matAmount[1] = itemstack.getTagCompound().getInteger("Abyssium");
            matAmount[2] = itemstack.getTagCompound().getInteger("Ammo");
            matAmount[3] = itemstack.getTagCompound().getInteger("Polymetal");
        }

        LogHelper.debug("DEBUG: shipcalc get matAmount : " + matAmount[0] + " " + matAmount[1] + " " + matAmount[2] + " " + matAmount[3]);
        return matAmount;
    }

    //roll ship type by total number of materials
    public static int rollShipType(ItemStack item) {
        List<int[]> shipList = new ArrayList<int[]>();
        int[] material = new int[4];
        int totalMats = 0;

        if (item.getItemDamage() > 1) {    //is special egg
            return item.getItemDamage() - 2;
        }

        //get materials amount
        if (item.hasTagCompound()) {    //正常製造egg, 會有四個材料tag
            assert item.getTagCompound() != null;
            material[0] = item.getTagCompound().getInteger("Grudge");
            material[1] = item.getTagCompound().getInteger("Abyssium");
            material[2] = item.getTagCompound().getInteger("Ammo");
            material[3] = item.getTagCompound().getInteger("Polymetal");

            totalMats = material[0] + material[1] + material[2] + material[3];
        }

        List<int[]> shiplistOrg = null;  //raw ship list: 0:ship ID, 1:mean 2:specific material
        float te = 4000F;    //for gui info calc

        //get equip list by build type
        if (item.getItemDamage() == 0) {    //small egg
            shiplistOrg = EquipSmall;
            te = 256F;
        } else {
            shiplistOrg = EquipLarge;
        }

        /*roll ship type
          0. tweak roll list by mat.amount: specific material decrease the mean value
          1. get prob of ships in roll list
          2. roll 0~1 to get ship
          3. return ship id
         */
        //prob list: map<ship ID, prob parameter>
        Map<Integer, Float> probList = new HashMap<Integer, Float>();
        int meanNew = 0;
        int meanDist = 0;
        float prob = 0F;

        for (int[] i : shiplistOrg) {
            //get modified material type
            if (i[2] >= 0 && i[2] <= 3) {
                meanNew = i[1] - material[i[2]];
            } else {
                meanNew = i[1];
            }

            //get mean distance
            meanDist = MathHelper.abs(totalMats - meanNew);

            //mean value to prob value
            if (item.getItemDamage() == 0) {    //small egg
                //for small build, max material = 256
                //change scale to large build resolution
                meanDist = (int) (meanDist * 15.625F); // = mat / 256 * 4000
            }

            prob = CalcHelper.getNormDist(meanDist);
            //add to map
            probList.put(i[0], prob);
            LogHelper.debug("DEBUG: roll ship type: prob list: ID " + i[0] + " MEAN(ORG) " + i[1] + " MEAN(NEW) " + meanNew + " MEAN(P) " + (meanNew / te) + " MD " + meanDist + " PR " + prob);
        }

        float random = rand.nextFloat();
        float totalProb = 0F;
        float sumProb = 0.0125F;    //init value to prevent float comparison bug
        int rollresult = -1;

        //get total prob
        Iterator<Map.Entry<Integer, Float>> iter = probList.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, Float> entry = iter.next();
            totalProb += entry.getValue();
        }

        //scale random number to totalProb
        random *= totalProb;

        //roll ship
        iter = probList.entrySet().iterator();
        while (iter.hasNext()) {
            Map.Entry<Integer, Float> entry = iter.next();
            sumProb += entry.getValue();
            LogHelper.debug("DEBUG: roll ship type: random: " + random + " sum.pr " + sumProb + " total.pr " + totalProb);

            if (sumProb > random) {    //get item
                rollresult = entry.getKey();
                LogHelper.debug("DEBUG: roll ship type: get ship:" + rollresult);
                break;
            }
        }

        return rollresult;
    }

    /**
     * Get entity name from metadata
     * (entity name from ModEntity.class)
     * small egg: all non-hime ship
     * large egg: all ship
     * specific egg: specific ship
     */
    public static String getEntityToSpawnName(int type) {
        return switch (type) {
            case ID.ShipClass.DDI -> "EntityDestroyerI";
            case ID.ShipClass.DDRO -> "EntityDestroyerRo";
            case ID.ShipClass.DDHA -> "EntityDestroyerHa";
            case ID.ShipClass.DDNI -> "EntityDestroyerNi";
            case ID.ShipClass.CARI -> "EntityHeavyCruiserRi";
            case ID.ShipClass.CANE -> "EntityHeavyCruiserNe";
            case ID.ShipClass.CVWO -> "EntityCarrierWo";
            case ID.ShipClass.BBRU -> "EntityBattleshipRu";
            case ID.ShipClass.BBTA -> "EntityBattleshipTa";
            case ID.ShipClass.BBRE -> "EntityBattleshipRe";
            case ID.ShipClass.APWA -> "EntityTransportWa";
            case ID.ShipClass.SSKA -> "EntitySubmKa";
            case ID.ShipClass.SSYO -> "EntitySubmYo";
            case ID.ShipClass.SSSO -> "EntitySubmSo";
            case ID.ShipClass.AirfieldHime -> "EntityAirfieldHime";
            case ID.ShipClass.CVHime -> "EntityCarrierHime";
            case ID.ShipClass.BBHime -> "EntityBattleshipHime";
            case ID.ShipClass.DDHime -> "EntityDestroyerHime";
            case ID.ShipClass.HarbourHime -> "EntityHarbourHime";
            case ID.ShipClass.IsolatedHime -> "EntityIsolatedHime";
            case ID.ShipClass.MidwayHime -> "EntityMidwayHime";
            case ID.ShipClass.NorthernHime -> "EntityNorthernHime";
            case ID.ShipClass.SSHime -> "EntitySubmHime";
            case ID.ShipClass.SSNH -> "EntitySubmNewHime";
            case ID.ShipClass.CVWD -> "EntityCarrierWD";
            case ID.ShipClass.DDShimakaze -> "EntityDestroyerShimakaze";
            case ID.ShipClass.DDShimakaze + 2000 -> "EntityDestroyerShimakazeMob";
            case ID.ShipClass.BBKongou -> "EntityBattleshipKongou";
            case ID.ShipClass.BBKongou + 2000 -> "EntityBattleshipKongouMob";
            case ID.ShipClass.BBHiei -> "EntityBattleshipHiei";
            case ID.ShipClass.BBHiei + 2000 -> "EntityBattleshipHieiMob";
            case ID.ShipClass.BBHaruna -> "EntityBattleshipHaruna";
            case ID.ShipClass.BBHaruna + 2000 -> "EntityBattleshipHarunaMob";
            case ID.ShipClass.BBKirishima -> "EntityBattleshipKirishima";
            case ID.ShipClass.BBKirishima + 2000 -> "EntityBattleshipKirishimaMob";
            case ID.ShipClass.BBNagato -> "EntityBattleshipNGT";
            case ID.ShipClass.BBNagato + 2000 -> "EntityBattleshipNGTMob";
            case ID.ShipClass.BBYamato -> "EntityBattleshipYMT";
            case ID.ShipClass.BBYamato + 2000 -> "EntityBattleshipYMTMob";
            case ID.ShipClass.SSU511 -> "EntitySubmU511";
            case ID.ShipClass.SSU511 + 2000 -> "EntitySubmU511Mob";
            case ID.ShipClass.SSRo500 -> "EntitySubmRo500";
            case ID.ShipClass.SSRo500 + 2000 -> "EntitySubmRo500Mob";
            case ID.ShipClass.CVKaga -> "EntityCarrierKaga";
            case ID.ShipClass.CVKaga + 2000 -> "EntityCarrierKagaMob";
            case ID.ShipClass.CVAkagi -> "EntityCarrierAkagi";
            case ID.ShipClass.CVAkagi + 2000 -> "EntityCarrierAkagiMob";
            case ID.ShipClass.DDAkatsuki -> "EntityDestroyerAkatsuki";
            case ID.ShipClass.DDAkatsuki + 2000 -> "EntityDestroyerAkatsukiMob";
            case ID.ShipClass.DDHibiki -> "EntityDestroyerHibiki";
            case ID.ShipClass.DDHibiki + 2000 -> "EntityDestroyerHibikiMob";
            case ID.ShipClass.DDIkazuchi -> "EntityDestroyerIkazuchi";
            case ID.ShipClass.DDIkazuchi + 2000 -> "EntityDestroyerIkazuchiMob";
            case ID.ShipClass.DDInazuma -> "EntityDestroyerInazuma";
            case ID.ShipClass.DDInazuma + 2000 -> "EntityDestroyerInazumaMob";
            case ID.ShipClass.CLTenryuu -> "EntityCruiserTenryuu";
            case ID.ShipClass.CLTenryuu + 2000 -> "EntityCruiserTenryuuMob";
            case ID.ShipClass.CLTatsuta -> "EntityCruiserTatsuta";
            case ID.ShipClass.CLTatsuta + 2000 -> "EntityCruiserTatsutaMob";
            case ID.ShipClass.CAAtago -> "EntityCruiserAtago";
            case ID.ShipClass.CAAtago + 2000 -> "EntityCruiserAtagoMob";
            case ID.ShipClass.CATakao -> "EntityCruiserTakao";
            case ID.ShipClass.CATakao + 2000 -> "EntityCruiserTakaoMob";
            case ID.ShipClass.CAHime -> "EntityCAHime";
            default -> "EntityDestroyerI";
        };
    }

    /**
     * spawn ship mob name
     * rare level is calculated here
     * <p>
     * double roll:
     * 1. roll for ship rare level
     * 2. roll for ship name
     */
    public static String getRandomMobToSpawnName() {
        int ran1 = rand.nextInt(100);

        //25% for super rare level
        if (ran1 > 75) {
            return switch (rand.nextInt(3)) {
                case 1 -> getEntityToSpawnName(ID.ShipClass.BBYamato + 2000);
                case 2 -> switch (rand.nextInt(4)) {
                    case 1 -> getEntityToSpawnName(ID.ShipClass.BBHiei + 2000);
                    case 2 -> getEntityToSpawnName(ID.ShipClass.BBHaruna + 2000);
                    case 3 -> getEntityToSpawnName(ID.ShipClass.BBKirishima + 2000);
                    default -> getEntityToSpawnName(ID.ShipClass.BBKongou + 2000);
                };
                default -> getEntityToSpawnName(ID.ShipClass.BBNagato + 2000);
            };
        }
        //30% for rare level
        else if (ran1 > 45) {
            return switch (rand.nextInt(3)) {
                case 1, 2 ->  //66%
                        switch (rand.nextInt(4)) {
                            case 1 -> getEntityToSpawnName(ID.ShipClass.CLTenryuu + 2000);
                            case 2 -> getEntityToSpawnName(ID.ShipClass.CLTatsuta + 2000);
                            case 3 -> getEntityToSpawnName(ID.ShipClass.CAAtago + 2000);
                            default -> getEntityToSpawnName(ID.ShipClass.CATakao + 2000);
                        };
                default -> //33%
                        switch (rand.nextInt(2)) {
                            case 1 -> getEntityToSpawnName(ID.ShipClass.CVKaga + 2000);
                            default -> getEntityToSpawnName(ID.ShipClass.CVAkagi + 2000);
                        };
            };
        }
        //45% for common level
        else {
            return switch (rand.nextInt(7)) {
                case 1 -> getEntityToSpawnName(ID.ShipClass.DDHibiki + 2000);
                case 2 -> getEntityToSpawnName(ID.ShipClass.DDIkazuchi + 2000);
                case 3 -> getEntityToSpawnName(ID.ShipClass.DDInazuma + 2000);
                case 4 -> getEntityToSpawnName(ID.ShipClass.DDShimakaze + 2000);
                case 5 -> getEntityToSpawnName(ID.ShipClass.SSU511 + 2000);
                case 6 -> getEntityToSpawnName(ID.ShipClass.SSRo500 + 2000);
                default -> getEntityToSpawnName(ID.ShipClass.DDAkatsuki + 2000);
            };
        }
    }

    /**
     * get ship recycle items
     */
    public static ItemStack[] getKaitaiItems(int shipID) {
        ItemStack[] amount = new ItemStack[4];

        switch (shipID) {
            case -2:  //pri egg
                amount[0] = new ItemStack(ModItems.Grudge, 10 + rand.nextInt(8));
                amount[1] = new ItemStack(ModItems.AbyssMetal, 10 + rand.nextInt(8), 0);
                amount[2] = new ItemStack(ModItems.Ammo, 10 + rand.nextInt(8), 0);
                amount[3] = new ItemStack(ModItems.AbyssMetal, 10 + rand.nextInt(8), 1);
                break;
            case -1:  //adv egg
                amount[0] = new ItemStack(ModItems.Grudge, 90 + rand.nextInt(8));
                amount[1] = new ItemStack(ModItems.AbyssMetal, 90 + rand.nextInt(8), 0);
                amount[2] = new ItemStack(ModItems.Ammo, 90 + rand.nextInt(8), 0);
                amount[3] = new ItemStack(ModItems.AbyssMetal, 90 + rand.nextInt(8), 1);
                break;
            case ID.ShipClass.DDI:
            case ID.ShipClass.DDRO:
            case ID.ShipClass.DDHA:
            case ID.ShipClass.DDNI:
            case ID.ShipClass.CLHO:
            case ID.ShipClass.CLHE:
            case ID.ShipClass.CLTO:
            case ID.ShipClass.CLTSU:
            case ID.ShipClass.CLTCHI:
            case ID.ShipClass.CARI:
            case ID.ShipClass.CANE:
            case ID.ShipClass.CVLNU:
            case ID.ShipClass.APWA:
            case ID.ShipClass.SSKA:
            case ID.ShipClass.SSYO:
            case ID.ShipClass.SSSO:
                amount[0] = new ItemStack(ModItems.Grudge, 12 + rand.nextInt(8));
                amount[1] = new ItemStack(ModItems.AbyssMetal, 12 + rand.nextInt(8), 0);
                amount[2] = new ItemStack(ModItems.Ammo, 12 + rand.nextInt(8), 0);
                amount[3] = new ItemStack(ModItems.AbyssMetal, 12 + rand.nextInt(8), 1);
                break;
            case ID.ShipClass.CVWO:
            case ID.ShipClass.BBRU:
            case ID.ShipClass.BBTA:
            case ID.ShipClass.BBRE:
            case ID.ShipClass.CVHime:
            case ID.ShipClass.AirfieldHime:
            case ID.ShipClass.ArmoredCVHime:
            case ID.ShipClass.AnchorageHime:
            case ID.ShipClass.HarbourWD:
            case ID.ShipClass.AnchorageWD:
            case ID.ShipClass.BBHime:
            case ID.ShipClass.DDHime:
            case ID.ShipClass.HarbourHime:
            case ID.ShipClass.IsolatedHime:
            case ID.ShipClass.MidwayHime:
            case ID.ShipClass.NorthernHime:
            case ID.ShipClass.SouthernHime:
            case ID.ShipClass.CVWD:
            case ID.ShipClass.CLDemon:
            case ID.ShipClass.BBWD:
            case ID.ShipClass.STHime:
            case ID.ShipClass.AirdefenseHime:
            case ID.ShipClass.PTImp:
            case ID.ShipClass.CLHime:
            case ID.ShipClass.SSHime:
            case ID.ShipClass.DDWD:
            case ID.ShipClass.CAHime:
            case ID.ShipClass.SupplyDepotHime:
            case ID.ShipClass.SSNH:
                //easy mode: reduce amount to prevent resources dupe
                if (ConfigHandler.easyMode) {
                    amount[0] = new ItemStack(ModBlocks.BlockGrudge, 1);
                    amount[1] = new ItemStack(ModBlocks.BlockAbyssium, 1);
                    amount[2] = new ItemStack(ModItems.Ammo, 1, 1);
                    amount[3] = new ItemStack(ModBlocks.BlockPolymetal, 1);
                } else {
                    amount[0] = new ItemStack(ModBlocks.BlockGrudge, 10 + rand.nextInt(3));
                    amount[1] = new ItemStack(ModBlocks.BlockAbyssium, 10 + rand.nextInt(3));
                    amount[2] = new ItemStack(ModItems.Ammo, 10 + rand.nextInt(3), 1);
                    amount[3] = new ItemStack(ModBlocks.BlockPolymetal, 10 + rand.nextInt(3));
                }
                break;
            case ID.ShipClass.SSU511:
            case ID.ShipClass.SSRo500:
            case ID.ShipClass.DDAkatsuki:
            case ID.ShipClass.DDHibiki:
            case ID.ShipClass.DDIkazuchi:
            case ID.ShipClass.DDInazuma:
            case ID.ShipClass.DDShimakaze:
            case ID.ShipClass.Raiden:
                amount[0] = new ItemStack(ModItems.Grudge, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int) (ConfigHandler.kaitaiAmountSmall * 0.25F) + 1));
                amount[1] = new ItemStack(ModItems.AbyssMetal, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int) (ConfigHandler.kaitaiAmountSmall * 0.25F) + 1), 0);
                amount[2] = new ItemStack(ModItems.Ammo, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int) (ConfigHandler.kaitaiAmountSmall * 0.25F) + 1), 0);
                amount[3] = new ItemStack(ModItems.AbyssMetal, ConfigHandler.kaitaiAmountSmall + rand.nextInt((int) (ConfigHandler.kaitaiAmountSmall * 0.25F) + 1), 1);
                break;
            case ID.ShipClass.CAAtago:
            case ID.ShipClass.CATakao:
            case ID.ShipClass.CLTenryuu:
            case ID.ShipClass.CLTatsuta:
                amount[0] = new ItemStack(ModBlocks.BlockGrudge, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int) (ConfigHandler.kaitaiAmountLarge * 0.25F) + 1));
                amount[1] = new ItemStack(ModBlocks.BlockAbyssium, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int) (ConfigHandler.kaitaiAmountLarge * 0.25F) + 1));
                amount[2] = new ItemStack(ModItems.Ammo, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int) (ConfigHandler.kaitaiAmountLarge * 0.25F) + 1), 1);
                amount[3] = new ItemStack(ModBlocks.BlockPolymetal, ConfigHandler.kaitaiAmountLarge + rand.nextInt((int) (ConfigHandler.kaitaiAmountLarge * 0.25F) + 1));
                break;
            case ID.ShipClass.BBKongou:
            case ID.ShipClass.BBHiei:
            case ID.ShipClass.BBHaruna:
            case ID.ShipClass.BBKirishima:
            case ID.ShipClass.BBNagato:
            case ID.ShipClass.BBYamato:
            case ID.ShipClass.CVKaga:
            case ID.ShipClass.CVAkagi:
                amount[0] = new ItemStack(ModBlocks.BlockGrudge, ConfigHandler.kaitaiAmountLarge + rand.nextInt(ConfigHandler.kaitaiAmountLarge + 1));
                amount[1] = new ItemStack(ModBlocks.BlockAbyssium, ConfigHandler.kaitaiAmountLarge + rand.nextInt(ConfigHandler.kaitaiAmountLarge + 1));
                amount[2] = new ItemStack(ModItems.Ammo, ConfigHandler.kaitaiAmountLarge + rand.nextInt(ConfigHandler.kaitaiAmountLarge + 1), 1);
                amount[3] = new ItemStack(ModBlocks.BlockPolymetal, ConfigHandler.kaitaiAmountLarge + rand.nextInt(ConfigHandler.kaitaiAmountLarge + 1));
                break;
        }

        return amount;
    }


}