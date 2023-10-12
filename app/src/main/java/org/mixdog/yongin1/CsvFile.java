package org.mixdog.yongin1;

import android.util.Log;

import com.android.volley.BuildConfig;
import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;
import com.opencsv.exceptions.CsvValidationException;

import org.mixdog.yongin1.dto.LocationDto;
import org.mixdog.yongin1.dto.NoiseDto;
import org.mixdog.yongin1.dto.VibrationDto;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class CsvFile {
    private final String filePath;

    // 파일경로 생성자
    public CsvFile(String filePath) {
        this.filePath = filePath;
    }

    // 리스트 데이터 쓰기 메소드
    public void writeAllData(String fileName, List<String[]> dataList) {
        try {
            FileWriter fileWriter = new FileWriter(new File(filePath + "/" + fileName));
            CSVWriter csvWriter = new CSVWriter(fileWriter);
            csvWriter.writeAll(dataList);
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            if(BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    // 각 문자열 배열 개별행으로 쓰기 메소드
    public void writeData(String fileName, List<String[]> dataList) {
        try {
            FileWriter fileWriter = new FileWriter(new File(filePath + "/" + fileName));
            CSVWriter csvWriter = new CSVWriter(fileWriter);

            for(String[] data : dataList) {
                csvWriter.writeNext(data);
                Log.d("mixpuppy", "filePath : " + filePath + ", fileName : " + fileName);
                Log.d("mixpuppy", "입력데이터 : " + data);
            }
            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }

    //csv 파일에서 모든 데이터 읽어오는 메소드
    public List<String[]> readAllCsvDate(String fileName) {
        try{
            FileReader fileReader = new FileReader(new File(filePath + "/" + fileName));
            CSVReader csvReader = new CSVReader(fileReader);
            List<String[]> data = csvReader.readAll();
            csvReader.close();
            return data;
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        } catch (CsvException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }
    }

    // 파일 한행씩 읽어서 리스트에 추가하는 방식
    public List<String[]> readCsvData(String fileName) {
        try {
            FileReader fileReader = new FileReader(new File(filePath + "/" + fileName));
            CSVReader csvReader = new CSVReader(fileReader);
            List<String[]> dataList = new ArrayList<>();
            String[] data;
            while ((data = csvReader.readNext()) != null) {
                dataList.add(data);
            }
            csvReader.close();
            return dataList;

        } catch (IOException | CsvValidationException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
            return new ArrayList<>();
        }
    }

    // dto 를 이용한 csv 파일 저장 메소드
    public void locationDTOListToCsv(String fileName, List<LocationDto> dtoList) {
        try {
            FileWriter fw = new FileWriter(new File(filePath + "/" + fileName));
            CSVWriter csvWriter = new CSVWriter(fw);

            // CSV 헤더 작성
            String[] header = {"date", "time", "car_num", "lon", "lat"};
            csvWriter.writeNext(header);

            // DTO 리스트를 CSV 파일로 저장
            for (LocationDto dto : dtoList) {
                String[] data = {String.valueOf(dto.getDate()), String.valueOf(dto.getTime()),
                        dto.getCarNum(), String.valueOf(dto.getLongitude()),
                        String.valueOf(dto.getLatitude())
                        };
                csvWriter.writeNext(data);
            }

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }
    public void noiseDTOListToCsv(String fileName, List<NoiseDto> dtoList) {
        try {
            FileWriter fw = new FileWriter(new File(filePath + "/" + fileName));
            CSVWriter csvWriter = new CSVWriter(fw);

            // CSV 헤더 작성
            String[] header = {"date", "time", "car_num", "noise"};
            csvWriter.writeNext(header);

            // DTO 리스트를 CSV 파일로 저장
            for (NoiseDto dto : dtoList) {
                String[] data = {String.valueOf(dto.getDate()), String.valueOf(dto.getTime()),
                        dto.getCarNum(), String.valueOf(dto.getNoise())};
                csvWriter.writeNext(data);
            }

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }
    public void vibrationDTOListToCsv(String fileName, List<VibrationDto> dtoList) {
        try {
            FileWriter fw = new FileWriter(new File(filePath + "/" + fileName));
            CSVWriter csvWriter = new CSVWriter(fw);

            // CSV 헤더 작성
            String[] header = {"date", "time", "car_num", "rpm"};
            csvWriter.writeNext(header);

            // DTO 리스트를 CSV 파일로 저장
            for (VibrationDto dto : dtoList) {
                String[] data = {String.valueOf(dto.getDate()), String.valueOf(dto.getTime()),
                        dto.getCarNum(), String.valueOf(dto.getVibration())};
                csvWriter.writeNext(data);
            }

            csvWriter.flush();
            csvWriter.close();
        } catch (IOException e) {
            if (BuildConfig.DEBUG) {
                e.printStackTrace();
            }
        }
    }
}
