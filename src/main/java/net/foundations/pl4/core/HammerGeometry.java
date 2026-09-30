package net.foundations.pl4.core;

import java.util.List;

/** PL2 ModelHammer, commit 4772196: original box UVs/pivots/leg angles, not a cube-texture approximation.
 * Derived from Ollie Lansdell's MIT-licensed model; see LICENSE and docs/R5_UPSTREAM_CONTRACT.md.
 * Model coordinates are 1/16 block units, positive Y down, with render origin (0.5, 1.5, 0.5).
 */
public final class HammerGeometry {
    public static final int TEXTURE_WIDTH = 128, TEXTURE_HEIGHT = 64;
    public static final float TRAVEL = 26;
    public record Piece(String name, int u, int v, float x, float y, float z,
                        float width, float height, float depth, float px, float py, float pz,
                        float rx, float ry, float rz, boolean moving, boolean upper) {}
    private static Piece piece(String name,int u,int v,float x,float y,float z,float w,float h,float d,
                               float px,float py,float pz,float rx,float rz,boolean moving,boolean upper) {
        return new Piece(name,u,v,x,y,z,w,h,d,px,py,pz,rx,0,rz,moving,upper);
    }
    public static final List<Piece> PIECES = List.of(
        piece("surface",0,0,0,0,0,16,4,16,-8,12,-8,0,0,false,false),
        piece("base1",76,14,0,0,0,16,2,2,-8,22,6,0,0,false,false),
        piece("base2",76,14,0,0,0,16,2,2,-8,22,-8,0,0,false,false),
        piece("base3",76,0,0,0,0,2,2,12,-8,22,-6,0,0,false,false),
        piece("base4",76,0,0,0,0,2,2,12,6,22,-6,0,0,false,false),
        piece("leg1",76,18,-2,-8,0,2,8,2,-6,23,6,.2094395F,.2094395F,false,false),
        piece("leg2",76,18,-2,-8,-2,2,8,2,-6,23,-6,-.2094395F,.2094395F,false,false),
        piece("leg3",76,18,0,-8,-2,2,8,2,6,23,-6,-.2094395F,-.2094395F,false,false),
        piece("leg4",76,18,0,-8,0,2,8,2,6,23,6,.2094395F,-.2094395F,false,false),
        piece("pedestal",32,40,0,0,0,8,2,8,-4,10,-4,0,0,false,false),
        piece("beam1",64,0,0,0,0,3,32,3,4,-20,4,0,0,false,true),
        piece("beam2",64,0,0,0,0,3,32,3,4,-20,-7,0,0,false,true),
        piece("beam3",64,0,0,0,0,3,32,3,-7,-20,4,0,0,false,true),
        piece("beam4",64,0,0,0,0,3,32,3,-7,-20,-7,0,0,false,true),
        piece("top",0,20,0,0,0,16,4,16,-8,-24,-8,0,0,false,true),
        piece("weight",0,40,0,0,0,8,4,8,-4,-20,-4,0,0,true,true)
    );
    private HammerGeometry() {}
}
