package gabot4.models;

import battlecode.common.MapLocation;
import gabot4.enums.TaskType;
import gabot4.strategies.Comms2;

public class Task {

    public TaskType type;
    public MapLocation target;

    public Task(TaskType type, MapLocation target){
        this.type=type;
        this.target=target;
    }

    public boolean equals(Task other){
        if(other == null)return false;
        if(type == other.type && Comms2.locToSectorInfo(other.target) == Comms2.locToSectorInfo(target))return true;
        return false;
    }

}
