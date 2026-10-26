package com.inventory.msp.services;

import com.inventory.msp.model.*;
import com.inventory.msp.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ExcelUploadService {

    private final LocationRepository locationRepository;
    private final ApproachRoadRepository approachRoadRepository;
    private final DeviceRepository deviceRepository;
    private final DeviceHistoryRepository historyRepository;

    public void uploadExcel(MultipartFile file) throws Exception {

        Workbook workbook = new XSSFWorkbook(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        Row headerRow = sheet.getRow(5);   // Row 6 contains headers
        int dataStartRow = 7;              // Row 8 contains first data row

        for (int r = dataStartRow; r <= sheet.getLastRowNum(); r++) {

            Row row = sheet.getRow(r);
            if (row == null || isRowEmpty(row)) continue;

            List<DeviceEntry> devices = extractDeviceCells(row, headerRow);
            if (devices.isEmpty()) continue;

            String locationName = getMergedCellValue(sheet, r, 1);
            String approachName = getMergedCellValue(sheet, r, 2);

            if (locationName == null || locationName.trim().isEmpty()) continue;

            if (approachName == null || approachName.trim().isEmpty())
                approachName = locationName + "-ROAD-" + r;

            boolean poles = parsePoles(row.getCell(3));
            boolean ecbPresent = convertToBoolean(row.getCell(10));
            JunctionBoxType junctionBox = parseJunctionBox(row.getCell(11));

            String lat = getString(row.getCell(13));
            String lon = getString(row.getCell(14));

            // ✅ Create or fetch location
            Location location = locationRepository.findByName(locationName)
                    .orElseGet(() -> locationRepository.save(
                            Location.builder()
                                    .name(locationName)
                                    .junctionBox(JunctionBoxType.NONE)
                                    .build()
                    ));

            // **************************************
            // ✅ FIXED: Prevent duplicate approach roads
            // **************************************
            String finalApproachName = approachName;
            ApproachRoad road = approachRoadRepository
                    .findByRoadNameIgnoreCaseAndLocationId(approachName.trim(), location.getId())
                    .orElseGet(() -> {
                        ApproachRoad newRoad = new ApproachRoad();
                        newRoad.setRoadName(finalApproachName.trim());
                        newRoad.setLocation(location);
                        return approachRoadRepository.save(newRoad);
                    });

            // Create Device entries
            for (DeviceEntry entry : devices) {

                String rawVal = entry.value;
                boolean placeholder = false;
                String finalSerial;

                if ("M".equalsIgnoreCase(rawVal)) {
                    finalSerial = "MISSING_" + entry.type + "_" + r + "_" + entry.col;
                    placeholder = true;
                } else {
                    finalSerial = rawVal;
                }

                if (deviceRepository.existsBySerialNumber(finalSerial)) continue;

                Device device = new Device();
                device.setSerialNumber(finalSerial);
                device.setDeviceType(DeviceType.valueOf(entry.type));
                device.setEcbPresent(ecbPresent);
                device.setPoles(poles);
                device.setJunctionBoxType(junctionBox);
                device.setLatitude(lat);
                device.setLongitude(lon);
                device.setPlaceholder(placeholder);
                device.setStatus("Installed");
                device.setLocation(location);
                device.setApproachRoad(road);
                device.setCreatedAt(LocalDateTime.now());
                device.setUpdatedAt(LocalDateTime.now());

                Device saved = deviceRepository.save(device);

                DeviceHistory history = DeviceHistory.builder()
                        .deviceId(saved.getId())
                        .action("Installed")
                        .oldSerial(null)
                        .newSerial(saved.getSerialNumber())
                        .oldLocation(null)
                        .newLocation(locationName)
                        .replacedDeviceSerial(rawVal)
                        .referenceId("EXCEL-IMPORT")
                        .createdAt(LocalDateTime.now())
                        .build();

                historyRepository.save(history);
            }
        }

        workbook.close();
    }

    private List<DeviceEntry> extractDeviceCells(Row row, Row headerRow) {

        List<DeviceEntry> list = new ArrayList<>();
        int lastCol = headerRow.getLastCellNum();

        for (int c = 0; c < lastCol; c++) {

            String header = getString(headerRow.getCell(c));
            if (header == null) continue;

            header = safeUpper(header);

            boolean isDeviceCol =
                    header.contains("ANPR") ||
                            header.contains("RLVD") ||
                            header.contains("PTZ") ||
                            header.contains("FIXED") ||
                            header.contains("ANALYTICAL");

            if (!isDeviceCol) continue;

            String cellVal = getString(row.getCell(c));
            if (cellVal == null) continue;

            if (isValidSerial(cellVal) || cellVal.equalsIgnoreCase("M")) {

                String type =
                        header.contains("ANPR") ? "ANPR" :
                                header.contains("RLVD") ? "RLVD" :
                                        header.contains("PTZ") ? "PTZ" :
                                                "FIXED";

                list.add(new DeviceEntry(c, type, cellVal));
            }
        }

        return list;
    }

    private boolean isValidSerial(String s) {
        return s != null && s.trim().matches("^[A-Za-z0-9]{16}$");
    }

    private boolean parsePoles(Cell cell) {
        String val = getString(cell);
        return "1".equals(val);
    }

    private boolean convertToBoolean(Cell cell) {
        String s = getString(cell);
        if (s == null) return false;
        s = safeUpper(s);
        return s.equals("1") || s.equals("YES") || s.equals("TRUE");
    }

    private JunctionBoxType parseJunctionBox(Cell cell) {
        String s = getString(cell);
        if (s == null) return JunctionBoxType.NONE;
        s = safeUpper(s);
        return s.equals("1") || s.contains("SMALL") ? JunctionBoxType.SMALL : JunctionBoxType.NONE;
    }

    private String getString(Cell cell) {
        if (cell == null) return null;

        try {
            return switch (cell.getCellType()) {
                case STRING -> cell.getStringCellValue().trim();
                case NUMERIC -> {
                    double d = cell.getNumericCellValue();
                    long l = (long) d;
                    yield (d == l) ? String.valueOf(l) : String.valueOf(d);
                }
                case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
                default -> null;
            };
        } catch (Exception e) {
            return null;
        }
    }

    private String safeUpper(String s) {
        return s == null ? "" : s.toUpperCase();
    }

    private String getMergedCellValue(Sheet sheet, int row, int col) {
        for (int i = 0; i < sheet.getNumMergedRegions(); i++) {
            CellRangeAddress range = sheet.getMergedRegion(i);
            if (range.isInRange(row, col)) {
                Cell firstCell = sheet.getRow(range.getFirstRow()).getCell(range.getFirstColumn());
                return getString(firstCell);
            }
        }
        return getString(sheet.getRow(row).getCell(col));
    }

    private boolean isRowEmpty(Row row) {
        for (int c = 0; c < row.getLastCellNum(); c++) {
            if (getString(row.getCell(c)) != null) return false;
        }
        return true;
    }

    private static class DeviceEntry {
        int col;
        String type;
        String value;

        DeviceEntry(int c, String type, String value) {
            this.col = c;
            this.type = type;
            this.value = value;
        }
    }
}
