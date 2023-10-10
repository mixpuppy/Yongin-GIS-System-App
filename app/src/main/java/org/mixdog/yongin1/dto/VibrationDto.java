package org.mixdog.yongin1.dto;

import java.time.LocalDate;
import java.time.LocalTime;

public class VibrationDto {
    private String carNum;
    private LocalDate date;
    private LocalTime time;
    private int vibration;

    public VibrationDto(String carNum, LocalDate date, LocalTime time, int vibration) {
        this.carNum = carNum;
        this.date = date;
        this.time = time;
        this.vibration = vibration;
    }

    @Override
    public String toString() {
        return "VibrationDto{" +
                "carNum='" + carNum + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", vibration=" + vibration +
                '}';
    }

    public String getCarNum() {
        return carNum;
    }

    public void setCarNum(String carNum) {
        this.carNum = carNum;
    }

    public LocalDate getDate() {
        return date;
    }

    public void setDate(LocalDate date) {
        this.date = date;
    }

    public LocalTime getTime() {
        return time;
    }

    public void setTime(LocalTime time) {
        this.time = time;
    }

    public int getVibration() {
        return vibration;
    }

    public void setVibration(int vibration) {
        this.vibration = vibration;
    }
}
