package org.mixdog.yongin1.dto;

import java.time.LocalDate;
import java.time.LocalTime;


public class LocationDto {
    private String carNum;
    private LocalDate date;
    private LocalTime time;
    private double latitude;
    private double longitude;

    public LocationDto(String carNum, LocalDate date, LocalTime time, double latitude, double longitude) {
        this.carNum = carNum;
        this.date = date;
        this.time = time;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    @Override
    public String toString() {
        return "LocationDto{" +
                "carNum='" + carNum + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", latitude=" + latitude +
                ", longitude=" + longitude +
                '}';
    }

    public void setCarNum(String carNum) {
        this.carNum = carNum;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public String getCarNum() {
        return carNum;
    }

    public LocalDate getDate() {
        return date;
    }

    public LocalTime getTime() {
        return time;
    }

    public double getLatitude() {
        return latitude;
    }

    public double getLongitude() {
        return longitude;
    }




}
