package com.infinitytower.ecs.simulation;

class SafetyStateModel {
    boolean fire = false;
    boolean smoke = false;
    boolean estop = false;
    boolean power = false;
    boolean earthquake = false;
    boolean flood = false;

    void clearAll() {
        fire = false; smoke = false; estop = false;
        power = false; earthquake = false; flood = false;
    }
}
