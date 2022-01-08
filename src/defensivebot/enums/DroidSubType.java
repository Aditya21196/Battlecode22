package defensivebot.enums;

import battlecode.common.RobotType;

public enum DroidSubType {

    // TODO: think of better types
    MINER_ECO(0),
    MINER_EXPLORER(2),
    MINER_GOLD(3),
    MINER_SELF_DESTRUCT(1),
    BUILDER_FOR_LAB(1),
    BUILDER_FOR_WATCHTOWER(2),
    BUILDER_FOR_UPGRADE(3),
    SOLDIER_ATTACKER(1),
    SOLDIER_DEFENDER(2),
    SOLDIER_EXPLORER(3),
    SAGE_FLOCK_LEADER(1),
    SAGE_HOME(2);

    public final int code;

    DroidSubType(int code) {
        this.code = code;
    }


    public static DroidSubType getSubType(RobotType type,int code){
        switch (type){
            case MINER:
                switch (code){
                    case 0: return MINER_ECO;
                    case 2: return MINER_EXPLORER;
                    case 3: return MINER_GOLD;
                    case 1:return MINER_SELF_DESTRUCT;
                    default:return MINER_ECO;
                }
            case BUILDER:
                switch (code){
                    case 1:return BUILDER_FOR_LAB;
                    case 2:return BUILDER_FOR_WATCHTOWER;
                    case 3:return BUILDER_FOR_UPGRADE;
                    default:return BUILDER_FOR_LAB;
                }
            case SOLDIER:
                switch (code){
                    case 1: return SOLDIER_ATTACKER;
                    case 2: return SOLDIER_DEFENDER;
                    case 3: return SOLDIER_EXPLORER;
                    default:return SOLDIER_EXPLORER;
                }
            case SAGE:
                switch (code){
                    case 1: return SAGE_FLOCK_LEADER;
                    case 2: return SAGE_HOME;
                    default:return SAGE_HOME;
                }
        }
        // invalid droid type
        return null;
    }

}
