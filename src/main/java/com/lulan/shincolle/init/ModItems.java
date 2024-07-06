package com.lulan.shincolle.init;

import com.lulan.shincolle.item.*;
import com.lulan.shincolle.reference.Reference;
import com.lulan.shincolle.utility.LogHelper;
import net.minecraft.item.Item;
import net.minecraftforge.client.event.ModelRegistryEvent;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.registry.GameRegistry;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.util.ArrayList;
import java.util.List;

@Mod.EventBusSubscriber
@GameRegistry.ObjectHolder(Reference.MOD_ID)
public class ModItems {


    //list for item
    private static final List<BasicItem> ListItems=new ArrayList<>();

    private static final ArrayList<Item> ITEMS = new ArrayList<>();

    //spawn egg
    public static final Item ShipSpawnEgg = initItems(new ShipSpawnEgg());
    //materials
    public static final Item AbyssMetal = initItems(new AbyssMetal());
    public static final Item AbyssNugget = initItems(new AbyssNugget());
    public static final Item Ammo = initItems(new Ammo());
    public static final Item Grudge = initItems(new Grudge());
    //equip
    public static final Item EquipAirplane = initItems(new EquipAirplane());
    public static final Item EquipAmmo = initItems(new EquipAmmo());
    public static final Item EquipArmor = initItems(new EquipArmor());
    public static final Item EquipCannon = initItems(new EquipCannon());
    public static final Item EquipCatapult = initItems(new EquipCatapult());
    public static final Item EquipCompass = initItems(new EquipCompass());
    public static final Item EquipDrum = initItems(new EquipDrum());
    public static final Item EquipFlare = initItems(new EquipFlare());
    public static final Item EquipMachinegun = initItems(new EquipMachinegun());
    public static final Item EquipRadar = initItems(new EquipRadar());
    public static final Item EquipSearchlight = initItems(new EquipSearchlight());
    public static final Item EquipTorpedo = initItems(new EquipTorpedo());
    public static final Item EquipTurbine = initItems(new EquipTurbine());
    //misc
    public static final Item BucketRepair = initItems(new BucketRepair());
    public static final Item CombatRation = initItems(new CombatRation());
    public static final Item DeskItemBook = initItems(new DeskItemBook());
    public static final Item DeskItemRadar = initItems(new DeskItemRadar());
    public static final Item InstantConMat = initItems(new InstantConMat());
    public static final Item KaitaiHammer = initItems(new KaitaiHammer());
    public static final Item MarriageRing = initItems(new MarriageRing());
    public static final Item ModernKit = initItems(new ModernKit());
    public static final Item OwnerPaper = initItems(new OwnerPaper());
    public static final Item OPTool = initItems(new OPTool());
    public static final Item PointerItem = initItems(new PointerItem());
    public static final Item RecipePaper = initItems(new RecipePaper());
    public static final Item RepairGoddess = initItems(new RepairGoddess());
    public static final Item ShipTank = initItems(new ShipTank());
    public static final Item TargetWrench = initItems(new TargetWrench());
    public static final Item TrainingBook = initItems(new TrainingBook());
    //toy
    public static final Item ToyAirplane = initItems(new ToyAirplane());


    @SubscribeEvent
    public static void registerItems(RegistryEvent.Register<Item> event) {
        for (Item item : ITEMS) {
            event.getRegistry().register(item);
        }
    }

    private static Item initItems(Item item) {
        if(item instanceof BasicItem) {
            ListItems.add((BasicItem) item);
        }
        ITEMS.add(item);
        return item;
    }

    @SideOnly(Side.CLIENT)
    @SubscribeEvent
    public static void loadItemModels(ModelRegistryEvent event) {
        for (BasicItem i : ListItems) {
            i.initModel();
        }
    }
}