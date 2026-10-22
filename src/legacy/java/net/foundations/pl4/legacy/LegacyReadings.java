package net.foundations.pl4.legacy;

import net.minecraft.nbt.NBTTagCompound;

/** Read-only, loaded-world reader selection. Never moves or simulates resources. */
public final class LegacyReadings {
    public static final int ITEMS=9, FLUIDS=10, ENERGY=11, NETWORK=12, DISPLAY=13;
    public static boolean reader(int role){return role>=ITEMS&&role<=NETWORK;}
    public static String[] lines(String a,String b,String c,String d){return new String[]{clean(a),clean(b),clean(c),clean(d)};}
    private static String clean(String value){
        if(value==null)return "";StringBuilder out=new StringBuilder();
        for(int i=0;i<value.length()&&out.length()<64;i++){
            char c=value.charAt(i);if(c=='\u00a7'){i++;continue;}
            if(c>=32&&c!=127&&!Character.isSurrogate(c))out.append(c);
        }return out.toString();
    }
    public static String[] privateLines(){return lines("PL4 Display","Private","Owner: click to inspect","/pl4legacy display ...");}
    public static String[] sample(LegacyTile tile){
        if(!tile.fluidServer()||tile.owner.isEmpty())return lines("PL4 Reader","Unavailable","No loaded owned host","");
        try{
            int role=tile.role();
            if(role==DISPLAY){
                BoundedNetwork.Plan plan=tile.network();
                if(plan.overflow)return lines("PL4 Display","Host limit exceeded","Reduce network size","");
                int count=0;for(BoundedNetwork.Point p:plan.nodes){LegacyTile t=tile.fluidNode(p);if(t!=null&&tile.owner.equals(t.owner)&&reader(t.role()))count++;}
                if(count==0)return lines("PL4 Display","No owned reader","Connect a reader","");
                int selected=tile.readerIndex%count,index=0;
                for(BoundedNetwork.Point p:plan.nodes){LegacyTile t=tile.fluidNode(p);if(t!=null&&tile.owner.equals(t.owner)&&reader(t.role())&&index++==selected)return sample(t);}
                return lines("PL4 Display","Reader disconnected","","");
            }
            if(role==NETWORK){
                BoundedNetwork.Plan plan=tile.network();
                if(plan.overflow)return lines("PL4 Network","Host limit exceeded","Reduce network size","");
                int exporters=0,importers=0,readers=0;
                for(BoundedNetwork.Point p:plan.nodes){LegacyTile t=tile.fluidNode(p);if(t==null||!tile.owner.equals(t.owner))continue;
                    int r=t.role();if(r==1||r==3||r==6)exporters++;if(r==2||r==4||r==7)importers++;if(reader(r))readers++;}
                return lines("PL4 Network","Hosts: "+plan.nodes.size(),"Export/import: "+exporters+"/"+importers,"Readers: "+readers);
            }
            if(reader(role))return LegacyReaderPlatform.sample(tile,role);
            return lines("PL4 Reader","Not a reader","","");
        }catch(RuntimeException error){return lines("PL4 Reader","Provider error","Reading unavailable","");}
    }
    /** Only the public four-line projection is sent to clients, never escrow or owner NBT. */
    public static NBTTagCompound clientTag(LegacyTile tile){
        NBTTagCompound tag=new NBTTagCompound();tag.setInteger("Facing",tile.displayFacing);
        String[] text=tile.publishReadings?tile.displayLines:privateLines();
        for(int i=0;i<4;i++)tag.setString("Line"+i,clean(text[i]));return tag;
    }
    public static void readClientTag(LegacyTile tile,NBTTagCompound tag){
        tile.displayFacing=horizontal(tag.getInteger("Facing"));
        tile.displayLines=lines(tag.getString("Line0"),tag.getString("Line1"),tag.getString("Line2"),tag.getString("Line3"));
    }
    public static int horizontal(int side){return side>=2&&side<=5?side:2;}
    public static void writeSettings(LegacyTile tile,NBTTagCompound tag){
        tag.setBoolean("PublishReadings",tile.publishReadings);tag.setInteger("ReaderIndex",tile.readerIndex);tag.setInteger("DisplayFacing",tile.displayFacing);
    }
    public static void readSettings(LegacyTile tile,NBTTagCompound tag){
        tile.publishReadings=tag.getBoolean("PublishReadings");tile.readerIndex=Math.max(0,Math.min(4095,tag.getInteger("ReaderIndex")));
        tile.displayFacing=horizontal(tag.getInteger("DisplayFacing"));tile.displayLines=privateLines();
    }
    private LegacyReadings(){}
}
