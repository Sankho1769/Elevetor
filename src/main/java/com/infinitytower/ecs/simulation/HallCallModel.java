package com.infinitytower.ecs.simulation;

import java.util.ArrayList;
import java.util.List;

class HallCallModel {
    long id;
    int floor;
    String dir;
    String priority;
    double time;
    List<Long> passengerIds = new ArrayList<>();
    boolean assigned = false;
    Integer elevatorId;
    boolean everRejected = false;
}
