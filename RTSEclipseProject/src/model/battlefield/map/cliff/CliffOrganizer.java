/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model.battlefield.map.cliff;

import math.Angle;
import model.battlefield.map.Tile;
import tools.LogUtil;

/**
 * Warning : this class is full of monstrous spiders and mysterious shadows in every corner. Proceed at your own risks.
 * @author Benoît
 */
public class CliffOrganizer {
    
    public static void organize(Cliff c){
        Tile t = c.getTile();
        Tile n = c.getTile().n;
        Tile s = c.getTile().s;
        Tile e = c.getTile().e;
        Tile w = c.getTile().w;

        if(n == null || s == null || e == null || w == null){
            c.type = Cliff.Type.Border;
            return;
        }
        
        if(c.getUpperGrounds().size()>5){
            c.type = Cliff.Type.Bugged;
            return;
        }
        
        switch(c.getConnexionConfiguration()){
            // orthogonal
            case "ns" :
                if(e.level>w.level){
                        c.angle = Angle.FLAT;
                        c.link(s, n);
                } else {
                        c.angle = 0;
                        c.link(n, s);
                }
                c.type = Cliff.Type.Orthogonal;
                break;
            case "ew" :
                if(n.level>s.level){
                        c.angle = -Angle.RIGHT;
                        c.link(e, w);
                } else {
                        c.angle = Angle.RIGHT;
                        c.link(w, e);
                }
                c.type = Cliff.Type.Orthogonal;
                break;

                
            // digonal
            case "sw" :
                c.angle = 0;
                if(w.getNeighborsMaxLevel()>t.getNeighborsMaxLevel()){
                        c.link(w, s);
                        c.type = Cliff.Type.Salient;
                } else {
                        c.link(s, w);
                        c.type = Cliff.Type.Corner;
                }
                break;
            case "se" :
                c.angle = Angle.RIGHT;
                if(s.getNeighborsMaxLevel()>t.getNeighborsMaxLevel()){
                        c.link(s, e);
                        c.type = Cliff.Type.Salient;
                } else {
                        c.link(e, s);
                        c.type = Cliff.Type.Corner;
                }
                break;
            case "ne" :
                c.angle = Angle.FLAT;
                if(e.getNeighborsMaxLevel()>t.getNeighborsMaxLevel()){
                        c.link(e, n);
                        c.type = Cliff.Type.Salient;
                } else {
                        c.link(n, e);
                        c.type = Cliff.Type.Corner;
                }
                break;
            case "nw" :
                c.angle = -Angle.RIGHT;
                if(n.getNeighborsMaxLevel()>t.getNeighborsMaxLevel()){
                        c.link(n, w);
                        c.type = Cliff.Type.Salient;
                } else {
                        c.link(w, n);
                        c.type = Cliff.Type.Corner;
                }
                break;
                
                
            // ending cliff (for ramp end)
            case "n" :
                if(e.level>w.level){
                        c.angle = Angle.FLAT;
                } else {
                        c.angle = 0;
                        c.link(n, null);
                }
                c.type = Cliff.Type.Orthogonal;
                break;
            case "s" :
                if(e.level>w.level){
                        c.angle = Angle.FLAT;
                        c.link(s, null);
                } else {
                        c.angle = 0;
                }
                c.type = Cliff.Type.Orthogonal;
                break;
            case "e" :
                if(n.level>s.level){
                        c.angle = -Angle.RIGHT;
                        c.link(e, null);
                } else {
                        c.angle = Angle.RIGHT;
                }
                c.type = Cliff.Type.Orthogonal;
                break;
            case "w" :
                if(n.level>s.level){
                        c.angle = -Angle.RIGHT;
                } else {
                        c.angle = Angle.RIGHT;
                        c.link(w, null);
                }
                c.type = Cliff.Type.Orthogonal;
                break;
            default : LogUtil.logger.info("Cliff neighboring is strange at "+c.getTile().getPos2D()+" : "+c.getConnexionConfiguration());
                c.type = Cliff.Type.Bugged;
        }
    }
}