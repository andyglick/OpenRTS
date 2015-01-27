/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package model.battlefield.army.components;

import model.battlefield.abstractComps.Hiker;
import model.battlefield.army.motion.pathfinding.FlowField;
import geometry.BoundingCircle;
import geometry.Point2D;
import geometry3D.Point3D;
import java.util.ArrayList;
import java.util.List;
import math.Angle;
import model.battlefield.map.Map;
import model.battlefield.army.motion.CollisionManager;
import model.battlefield.army.motion.SteeringMachine;

/**
 *
 * @author Benoît
 */
public class Mover {
    public enum Heightmap {SKY, AIR, GROUND};
    public enum PathfindingMode {FLY, WALK};

    // final 
    public final Heightmap heightmap;
    public final PathfindingMode pathfindingMode;

    public final Hiker hiker;
    final Map map;
    SteeringMachine sm;
    CollisionManager cm;

    // variables
    public Point3D velocity = Point3D.ORIGIN;
    
    public double desiredYaw = 0;
    
    public boolean hasMoved = false;
    
    public ArrayList<Mover> toAvoid = new ArrayList<>();
    public ArrayList<Mover> toFlockWith = new ArrayList<>();
    public ArrayList<Mover> toLetPass = new ArrayList<>();
    
    
    public FlowField flowfield;
    private boolean hasDestination;
    public boolean hasFoundPost;
    public boolean holdPosition = false;
    public boolean tryHold = false;

    public Mover(Heightmap heightmap, PathfindingMode pathfindingMode, Hiker movable, Map map) {
        this.heightmap = heightmap;
        this.pathfindingMode = pathfindingMode;
        this.hiker = movable;
        this.map = map;
        cm = new CollisionManager(this, map);
        sm = new SteeringMachine(this);
        updateElevation();
    }
    public Mover(Mover o, Hiker movable) {
        this.heightmap = o.heightmap;
        this.pathfindingMode = o.pathfindingMode;
        this.hiker = movable;
        this.map = o.map;
        cm = new CollisionManager(this, map);
        sm = new SteeringMachine(this);
        updateElevation();
    }
    
    public void updatePosition(double elapsedTime) {
        double lastYaw = hiker.yaw;
        Point3D lastPos = new Point3D(hiker.pos);
        
        if(!holdPosition){
            Point3D steering = sm.getSteeringAndReset(elapsedTime);
            cm.applySteering(steering, elapsedTime, toAvoid);
        }
        head(elapsedTime);
        
        hasMoved = hiker.hasMoved(lastPos, lastYaw);
        if(hasMoved)
            updateElevation();
        
        if(hasDestination)
            hasFoundPost = false;
        else {
            hasFoundPost = true;
            for(Mover m : toFlockWith)
                if(m.hasDestination){
                    hasFoundPost = false;
                }
        }
        if(!tryHold)
            holdPosition = false;
    }
    
    public void tryToHoldPositionSoftly(){
        tryHold = true;
        if(fly())
            holdPosition = true;
        else {
            List<Mover> all = new ArrayList<>();
            all.addAll(toAvoid);
            all.addAll(toFlockWith);
            all.addAll(toLetPass);
            for(Mover m : all)
                if(hiker.collide(m.hiker))
                    return;
            for(Mover m : toFlockWith)
                if(m.tryHold && !m.holdPosition)
                    return;
            holdPosition = true;
        }
    }
    public void tryToHoldPositionHardly(){
        tryHold = true;
        if(fly())
            holdPosition = true;
        else {
            ArrayList<Mover> all = new ArrayList<>();
            all.addAll(toAvoid);
            all.addAll(toFlockWith);
            all.addAll(toLetPass);
            for(Mover m : all)
                if(m.holdPosition && hiker.collide(m.hiker))
                    return;
            holdPosition = true;
        }
    }
    
    public void setDestination(FlowField ff){
        flowfield = ff;
        hasDestination = true;
        hasFoundPost = false;
    }
    
    public void setDestinationReached(){
        hasDestination = false;
        for(Mover m : toFlockWith)
            if(hiker.getDistance(m.hiker) < hiker.getSpacing(m.hiker)+3)
                m.hasDestination = false;
    }
    
    public boolean hasDestination(){
        return hasDestination;
    }
    
    public Point2D getDestination(){
        if(flowfield != null)
            return flowfield.destination;
        return null;
    }
    
    public void head(double elapsedTime) {
        if(!velocity.isOrigin())
            desiredYaw = velocity.get2D().getAngle();

        if(!Angle.areSimilar(desiredYaw, hiker.yaw)){
            double diff = Angle.getOrientedDifference(hiker.yaw, desiredYaw);
            if(diff > 0)
                hiker.yaw += Math.min(diff, hiker.getRotSpeed()*elapsedTime);
            else
                hiker.yaw -= Math.min(-diff, hiker.getRotSpeed()*elapsedTime);
        } else
            hiker.yaw = desiredYaw;
    }

    // TODO ici le toFlockWith perd son sens quand il ne s'agit que de separation.
    public void separate(){
        sm.applySeparation(toLetPass);
    }
    
    public void flock(){
        sm.applySeparation(toFlockWith);
//        sm.applyCohesion(neighbors);
//        sm.applyAlignment(neighbors);
    }
    
    public void seek(Mover target){
        flock();
        separate();
        sm.seek(target);

        ArrayList<Mover> toAvoidExceptTarget = new ArrayList<>(toAvoid);
        toAvoidExceptTarget.remove(target);
        sm.avoidHoldingUnits(toAvoidExceptTarget);
    }

    public void seek(Point3D position){
        flock();
        separate();
        sm.seek(position);
        sm.avoidHoldingUnits(toAvoid);
    }
    
    public void followPath() {
        flock();
        separate();
        sm.proceedToDestination();
        sm.avoidHoldingUnits(toAvoid);
    }
    

    public void followPath(Mover target) {
        flock();
        separate();
        sm.proceedToDestination();

        ArrayList<Mover> toAvoidExceptTarget = new ArrayList<>(toAvoid);
        toAvoidExceptTarget.remove(target);
        sm.avoidHoldingUnits(toAvoidExceptTarget);
    }
    
    private void updateElevation(){
        if(heightmap == Heightmap.GROUND)
            hiker.pos = hiker.getCoord().get3D(0).getAddition(0, 0, map.getGroundAltitude(hiker.getCoord())+0.25);
        else if(heightmap == Heightmap.SKY)
            hiker.pos = hiker.getCoord().get3D(0).getAddition(0, 0, map.getTile(hiker.getCoord()).level+3);
            
    }
    
    public boolean fly(){
        return pathfindingMode == PathfindingMode.FLY;
    }
    
    public double getSpeed(){
        return hiker.getSpeed();
    }
    
    public void changeCoord(Point2D p){
        velocity = Point3D.ORIGIN;
        hiker.pos = p.get3D(0);
        updateElevation();
    }
}