package org.mixdog.yongin1.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

public class NoiseDto {
    private String carNum;
    private LocalDate date;
    private LocalTime time;
    private String noise;  // 원래 더블

    public NoiseDto(String carNum, LocalDate date, LocalTime time, String noise) {
        this.carNum = carNum;
        this.date = date;
        this.time = time;
        this.noise = noise;
    }

    @Override
    public String toString() {
        return "NoiseDto{" +
                "carNum='" + carNum + '\'' +
                ", date=" + date +
                ", time=" + time +
                ", noise=" + noise +
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

    public String getNoise() {
        return noise;
    }

    public void setNoise(String noise) {
        this.noise = noise;
    }
}
