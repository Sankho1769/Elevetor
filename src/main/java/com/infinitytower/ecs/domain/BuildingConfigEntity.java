package com.infinitytower.ecs.domain;

import jakarta.persistence.*;

@Entity
@Table(name = "building_config")
public class BuildingConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name = "Infinity Tower";
    private int floors = 12;
    private int basements = 2;
    private int lobby = 1;

    @Column(name = "idle_floor")
    private int idleFloor = 1;

    @Column(name = "parking_floors")
    private String parkingFloors = "-1,-2";

    @Column(name = "vip_floors")
    private String vipFloors = "10,11,12";

    @Column(name = "service_floors")
    private String serviceFloors = "-2";

    @Column(name = "mechanical_floors")
    private String mechanicalFloors = "";

    @Column(name = "elevator_count")
    private int elevatorCount = 4;

    private int capacity = 8;

    @Column(name = "weight_limit")
    private int weightLimit = 900;

    @Column(name = "max_speed")
    private double maxSpeed = 2.2;

    private double acceleration = 1.1;
    private double deceleration = 1.1;

    @Column(name = "door_open_time")
    private double doorOpenTime = 1.6;

    @Column(name = "door_close_time")
    private double doorCloseTime = 1.6;

    @Column(name = "load_time")
    private double loadTime = 1.2;

    @Column(name = "unload_time")
    private double unloadTime = 1.0;

    @Column(name = "energy_saving")
    private boolean energySaving = false;

    private String algorithm = "look";

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public int getFloors() { return floors; }
    public void setFloors(int floors) { this.floors = floors; }
    public int getBasements() { return basements; }
    public void setBasements(int basements) { this.basements = basements; }
    public int getLobby() { return lobby; }
    public void setLobby(int lobby) { this.lobby = lobby; }
    public int getIdleFloor() { return idleFloor; }
    public void setIdleFloor(int idleFloor) { this.idleFloor = idleFloor; }
    public String getParkingFloors() { return parkingFloors; }
    public void setParkingFloors(String parkingFloors) { this.parkingFloors = parkingFloors; }
    public String getVipFloors() { return vipFloors; }
    public void setVipFloors(String vipFloors) { this.vipFloors = vipFloors; }
    public String getServiceFloors() { return serviceFloors; }
    public void setServiceFloors(String serviceFloors) { this.serviceFloors = serviceFloors; }
    public String getMechanicalFloors() { return mechanicalFloors; }
    public void setMechanicalFloors(String mechanicalFloors) { this.mechanicalFloors = mechanicalFloors; }
    public int getElevatorCount() { return elevatorCount; }
    public void setElevatorCount(int elevatorCount) { this.elevatorCount = elevatorCount; }
    public int getCapacity() { return capacity; }
    public void setCapacity(int capacity) { this.capacity = capacity; }
    public int getWeightLimit() { return weightLimit; }
    public void setWeightLimit(int weightLimit) { this.weightLimit = weightLimit; }
    public double getMaxSpeed() { return maxSpeed; }
    public void setMaxSpeed(double maxSpeed) { this.maxSpeed = maxSpeed; }
    public double getAcceleration() { return acceleration; }
    public void setAcceleration(double acceleration) { this.acceleration = acceleration; }
    public double getDeceleration() { return deceleration; }
    public void setDeceleration(double deceleration) { this.deceleration = deceleration; }
    public double getDoorOpenTime() { return doorOpenTime; }
    public void setDoorOpenTime(double doorOpenTime) { this.doorOpenTime = doorOpenTime; }
    public double getDoorCloseTime() { return doorCloseTime; }
    public void setDoorCloseTime(double doorCloseTime) { this.doorCloseTime = doorCloseTime; }
    public double getLoadTime() { return loadTime; }
    public void setLoadTime(double loadTime) { this.loadTime = loadTime; }
    public double getUnloadTime() { return unloadTime; }
    public void setUnloadTime(double unloadTime) { this.unloadTime = unloadTime; }
    public boolean isEnergySaving() { return energySaving; }
    public void setEnergySaving(boolean energySaving) { this.energySaving = energySaving; }
    public String getAlgorithm() { return algorithm; }
    public void setAlgorithm(String algorithm) { this.algorithm = algorithm; }
}
