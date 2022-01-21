package defensivebot2.models;

import battlecode.common.MapLocation;
import defensivebot2.enums.TaskType;

public class Task {

    TaskType type;
    MapLocation target;

    Task(TaskType type,MapLocation target){
        this.type=type;
        this.target=target;
    }

}
