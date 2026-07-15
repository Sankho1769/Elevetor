package com.infinitytower.ecs.simulation;

class PassengerModel {
    long id;
    int origin;
    int dest;
    String priority;
    double weight;
    double spawnTime;
    Double boardTime;
    Double exitTime;
    int group;
    String state = "waiting";
    Integer assignedElevatorId;

    static double priorityWeight(String priority) {
        if (priority == null) return 1;
        return switch (priority) {
            case "staff" -> 1.1;
            case "elderly" -> 1.4;
            case "disabled" -> 1.5;
            case "vip" -> 1.8;
            case "emergency", "firefighter" -> 3.0;
            case "medical" -> 2.4;
            default -> 1.0;
        };
    }
}
