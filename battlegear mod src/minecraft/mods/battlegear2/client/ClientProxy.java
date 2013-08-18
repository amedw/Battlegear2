package mods.battlegear2.client;


import cpw.mods.fml.client.FMLClientHandler;
import cpw.mods.fml.client.registry.KeyBindingRegistry;
import cpw.mods.fml.common.registry.TickRegistry;
import cpw.mods.fml.relauncher.Side;
import mods.battlegear2.BattlegearTickHandeler;
import mods.battlegear2.CommonProxy;
import mods.battlegear2.api.IShield;
import mods.battlegear2.client.renderer.ShieldRenderer;
import mods.battlegear2.client.renderer.SpearRenderer;
import mods.battlegear2.inventory.InventoryPlayerBattle;
import mods.battlegear2.items.ItemShield;
import mods.battlegear2.packet.BattlegearAnimationPacket;
import mods.battlegear2.utils.BattlegearConfig;
import mods.battlegear2.utils.EnumBGAnimations;
import net.minecraft.client.Minecraft;
import net.minecraft.client.entity.EntityClientPlayerMP;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.network.packet.Packet15Place;
import net.minecraft.util.AxisAlignedBB;
import net.minecraft.util.Icon;
import net.minecraft.util.MovingObjectPosition;
import net.minecraft.util.Vec3;
import net.minecraftforge.client.MinecraftForgeClient;
import net.minecraftforge.common.MinecraftForge;

import java.util.List;

public class ClientProxy extends CommonProxy {

    public static Icon[] backgroundIcon;

    @Override
    public void registerKeyHandelers() {
        KeyBindingRegistry.registerKeyBinding(new BattlegearKeyHandeler());
    }

    @Override
    public void registerTickHandelers() {
        super.registerTickHandelers();
        MinecraftForge.EVENT_BUS.register(new BattlegearClientEvents());
        TickRegistry.registerTickHandler(new BattlegearTickHandeler(), Side.CLIENT);
        TickRegistry.registerTickHandler(new BattlegearClientTickHandeler(), Side.CLIENT);
    }

    @Override
    public void sendAnimationPacket(EnumBGAnimations animation, EntityPlayer entityPlayer) {

        if (entityPlayer instanceof EntityClientPlayerMP) {
            ((EntityClientPlayerMP) entityPlayer).sendQueue.addToSendQueue(
                    BattlegearAnimationPacket.generatePacket(animation, entityPlayer.username));
        }
    }

    @Override
    public void sendPlaceBlockPacket(EntityPlayer entityPlayer, int x, int y, int z, int face, Vec3 par8Vec3) {

        System.out.println(par8Vec3);
        System.out.println(((InventoryPlayerBattle)entityPlayer.inventory).getCurrentOffhandWeapon());

        if (entityPlayer instanceof EntityClientPlayerMP) {
            ((EntityClientPlayerMP) entityPlayer).sendQueue.addToSendQueue(
                    new Packet15Place(x, y, z, face,
                            ((InventoryPlayerBattle)entityPlayer.inventory).getCurrentOffhandWeapon(),
                            (float)par8Vec3.xCoord - (float)x,
                            (float)par8Vec3.yCoord - (float)y,
                            (float)par8Vec3.zCoord - (float)z)
            );
        }
    }

    @Override
    public void startFlash(EntityPlayer player, float damage) {

        Minecraft.getMinecraft().sndManager.playSound("battlegear2:shield", (float)player.posX, (float)player.posY, (float)player.posZ, 1, 1);

        if(player.username.equals(Minecraft.getMinecraft().thePlayer.username)){
            BattlegearClientTickHandeler.flashTimer = 30;
            ItemStack offhand = ((InventoryPlayerBattle)player.inventory).getCurrentOffhandWeapon();

            if(offhand != null && offhand.getItem() instanceof IShield)
                BattlegearClientTickHandeler.blockBar -= ((IShield) offhand.getItem()).getDamageDecayRate(offhand, damage);
        }
    }

    @Override
    public void registerItemRenderers() {

        SpearRenderer spearRenderer =  new SpearRenderer();
        for(Item spear: BattlegearConfig.spear){
            MinecraftForgeClient.registerItemRenderer(spear.itemID, spearRenderer);
        }

        ShieldRenderer shieldRenderer = new ShieldRenderer();
        for(Item shield : BattlegearConfig.shield){
            MinecraftForgeClient.registerItemRenderer(shield.itemID, shieldRenderer);
        }

    }

    @Override
    public Icon getSlotIcon(int index){
        if(backgroundIcon != null){
            return backgroundIcon[index];
        }else{
            return null;
        }
    }


    /**
     * Finds what block or object the mouse is over at the specified partial tick time. Args: partialTickTime
     */
    public MovingObjectPosition getMouseOver(float tickPart, float maxDist)
    {
        Minecraft mc = FMLClientHandler.instance().getClient();
        if (mc.renderViewEntity != null)
        {
            if (mc.theWorld != null)
            {
                mc.pointedEntityLiving = null;
                double d0 = (double)maxDist;
                MovingObjectPosition objectMouseOver = mc.renderViewEntity.rayTrace(d0, tickPart);
                double d1 = d0;
                Vec3 vec3 = mc.renderViewEntity.getPosition(tickPart);

                if (objectMouseOver != null)
                {
                    d1 = objectMouseOver.hitVec.distanceTo(vec3);
                }

                Vec3 vec31 = mc.renderViewEntity.getLook(tickPart);
                Vec3 vec32 = vec3.addVector(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0);
                Entity pointedEntity = null;
                float f1 = 1.0F;
                List list = mc.theWorld.getEntitiesWithinAABBExcludingEntity(mc.renderViewEntity, mc.renderViewEntity.boundingBox.addCoord(vec31.xCoord * d0, vec31.yCoord * d0, vec31.zCoord * d0).expand((double)f1, (double)f1, (double)f1));
                double d2 = d1;

                for (int i = 0; i < list.size(); ++i)
                {
                    Entity entity = (Entity)list.get(i);

                    if (entity.canBeCollidedWith())
                    {
                        float f2 = entity.getCollisionBorderSize();
                        AxisAlignedBB axisalignedbb = entity.boundingBox.expand((double)f2, (double)f2, (double)f2);
                        MovingObjectPosition movingobjectposition = axisalignedbb.calculateIntercept(vec3, vec32);

                        if (axisalignedbb.isVecInside(vec3))
                        {
                            if (0.0D < d2 || d2 == 0.0D)
                            {
                                pointedEntity = entity;
                                d2 = 0.0D;
                            }
                        }
                        else if (movingobjectposition != null)
                        {
                            double d3 = vec3.distanceTo(movingobjectposition.hitVec);

                            if (d3 < d2 || d2 == 0.0D)
                            {
                                pointedEntity = entity;
                                d2 = d3;
                            }
                        }
                    }
                }

                if (pointedEntity != null && (d2 < d1 || objectMouseOver == null))
                {
                    objectMouseOver = new MovingObjectPosition(pointedEntity);
                }

                return objectMouseOver;
            }
        }
        return null;
    }
}
