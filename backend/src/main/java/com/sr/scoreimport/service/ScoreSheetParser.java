package com.sr.scoreimport.service;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.text.Normalizer;
import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellType;
import org.apache.poi.ss.usermodel.DateUtil;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.ss.util.CellReference;
import org.springframework.stereotype.Component;

import com.sr.scoreimport.dto.response.ImportIssueResponse;
import com.sr.scoreimport.model.ParsedClassBlock;
import com.sr.scoreimport.model.ParsedSession;
import com.sr.scoreimport.model.ParsedSheet;
import com.sr.scoreimport.model.ParsedStudent;

/*
 * Read the "QUẢN LÝ HỌC TẬP" score sheet. Every class is a block:
 * - row h - 1: month titles ("THÁNG 7/2026" or a date)
 * - row h: "STT" in column A and the date of every session
 * - row h + 1: class code in column A and "15p" | "BTVN" column pairs
 * - row h + 2...: students (B = student code, C = full name)
 * Attendance and average columns are ignored because only "15p" | "BTVN"
 * pairs are read
 */
@Component
public class ScoreSheetParser {

    private static final String SCORE_SHEET_KEYWORD = "BẢNG ĐIỂM";
    private static final String HEADER_MARKER = "STT";
    private static final String CLASS_TITLE_PREFIX = "LỚP";
    private static final String TEST_HEADER = "15P";
    private static final String HOMEWORK_HEADER = "BTVN";

    private static final int CLASS_COLUMN = 0;
    private static final int STUDENT_CODE_COLUMN = 1;
    private static final int FULL_NAME_COLUMN = 2;

    private static final Pattern DAY_MONTH = Pattern.compile("(\\d{1,2})/(\\d{1,2})(?:/(\\d{2,4}))?");
    private static final Pattern MONTH_YEAR = Pattern.compile("(\\d{1,2})\\s*/\\s*(\\d{4})");

    private static final BigDecimal MAX_SCORE = BigDecimal.TEN;

    private record SessionColumn(int testColumn, int homeworkColumn, LocalDate date) {}

    public ParsedSheet parse(InputStream input) {
        Workbook workbook;

        try {
            workbook = WorkbookFactory.create(input);
        } catch (Exception exception) {
            throw new IllegalArgumentException("Không đọc được file. Hãy chọn file Excel (.xlsx)");
        }

        try (workbook) {
            Sheet sheet = findScoreSheet(workbook);

            if (sheet == null) {
                throw new IllegalArgumentException(
                    "Không tìm thấy bảng điểm trong file. Bảng điểm cần có hàng tiêu đề bắt đầu bằng \"STT\""
                );
            }

            List<ImportIssueResponse> issues = new ArrayList<>();
            List<Integer> headerRows = findHeaderRows(sheet);
            List<ParsedClassBlock> blocks = new ArrayList<>();

            for (int i = 0; i < headerRows.size(); i++) {
                int headerRow = headerRows.get(i);
                /*
                 * Get the end row depend on if there are more than 1 class block
                 */
                int endRow = i + 1 < headerRows.size() ? headerRows.get(i + 1) - 1 : sheet.getLastRowNum();

                ParsedClassBlock block = parseBlock(sheet, headerRow, endRow, issues);

                if (block != null) {
                    blocks.add(block);
                }
            }

            return new ParsedSheet(sheet.getSheetName(), blocks, issues);
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    /*
     * Prefer the sheet named "... BẢNG ĐIỂM", otherwise the first sheet that has a
     * header row
     */
    private Sheet findScoreSheet(Workbook workbook) {
        Sheet fallback = null;

        for (Sheet sheet : workbook) {
            if (findHeaderRows(sheet).isEmpty()) {
                continue;
            }

            if (normalize(sheet.getSheetName()).contains(SCORE_SHEET_KEYWORD)) {
                return sheet;
            }

            if (fallback == null) {
                fallback = sheet;
            }
        }

        return fallback;
    }

    /*
     * Finding the first row that has "STT"
     */
    private List<Integer> findHeaderRows(Sheet sheet) {
        List<Integer> headerRows = new ArrayList<>();

        for (Row row : sheet) {
            if (HEADER_MARKER.equals(normalize(text(row.getCell(CLASS_COLUMN))))) {
                headerRows.add(row.getRowNum());
            }
        }

        return headerRows;
    }

    private ParsedClassBlock parseBlock(
        Sheet sheet,
        int headerRow,
        int endRow,
        List<ImportIssueResponse> issues
    ) {
        Row dateRow = sheet.getRow(headerRow);
        Row subHeaderRow = sheet.getRow(headerRow + 1);

        String classCode = findClassCode(sheet, headerRow, subHeaderRow);

        if (classCode == null) {
            issues.add(
                ImportIssueResponse.error(
                    "Dòng " + (headerRow + 1),
                    "Không xác định được mã lớp của bảng điểm này, bảng bị bỏ qua"
                )
            );
            return null;
        }

        String location = "Lớp " + classCode;

        List<SessionColumn> columns = findSessionColumns(sheet, headerRow, dateRow, subHeaderRow, location, issues);

        if (columns.isEmpty()) {
            issues.add(ImportIssueResponse.warning(location, "Không tìm thấy cột điểm 15p / BTVN nào"));
        }

        List<ParsedStudent> students = new ArrayList<>();
        Set<SessionColumn> undatedColumnsWithScores = new LinkedHashSet<>();

        for (int rowIndex = headerRow + 2; rowIndex <= endRow; rowIndex++) {
            Row row = sheet.getRow(rowIndex);

            if (row == null) {
                continue;
            }

            if (normalize(text(row.getCell(CLASS_COLUMN))).startsWith(CLASS_TITLE_PREFIX)) {
                break;
            }

            String studentCode = text(row.getCell(STUDENT_CODE_COLUMN));
            String fullName = text(row.getCell(FULL_NAME_COLUMN));
            String rowLocation = location + " · dòng " + (rowIndex + 1);

            if (studentCode == null) {
                if (fullName != null) {
                    issues.add(ImportIssueResponse.warning(rowLocation, "Thiếu mã học sinh, dòng này bị bỏ qua"));
                }
                continue;
            }

            List<ParsedSession> sessions = new ArrayList<>();

            for (SessionColumn column : columns) {
                BigDecimal testScore = score(row.getCell(column.testColumn()));
                BigDecimal homeworkScore = score(row.getCell(column.homeworkColumn()));

                /*
                 * Skip the session only when both cells are empty
                 */
                if (testScore == null && homeworkScore == null) {
                    continue;
                }

                if (column.date() == null) {
                    undatedColumnsWithScores.add(column);
                    continue;
                }

                testScore = testScore == null ? BigDecimal.ZERO.setScale(2) : testScore;
                homeworkScore = homeworkScore == null ? BigDecimal.ZERO.setScale(2) : homeworkScore;

                if (!isValidScore(testScore) || !isValidScore(homeworkScore)) {
                    issues.add(
                        ImportIssueResponse.error(
                            rowLocation + " · ô " + cellName(rowIndex, column.testColumn()),
                            "Điểm phải nằm trong khoảng 0 đến 10, buổi này bị bỏ qua"
                        )
                    );
                    continue;
                }

                sessions.add(new ParsedSession(column.date(), homeworkScore, testScore));
            }

            students.add(new ParsedStudent(rowIndex + 1, studentCode, fullName, sessions));
        }

        for (SessionColumn column : undatedColumnsWithScores) {
            issues.add(
                ImportIssueResponse.error(
                    location + " · ô " + cellName(headerRow, column.testColumn()),
                    "Không xác định được ngày của buổi học, điểm của cột này bị bỏ qua"
                )
            );
        }

        return new ParsedClassBlock(classCode, headerRow + 1, students);
    }

    /*
     * Class code is in column A under "STT" (e.g. "12T1"), otherwise in the class
     * title above (e.g. "LỚP 12T1")
     */
    private String findClassCode(Sheet sheet, int headerRow, Row subHeaderRow) {
        String code = subHeaderRow == null ? null : text(subHeaderRow.getCell(CLASS_COLUMN));

        if (code != null) {
            return code;
        }

        for (int rowIndex = headerRow - 1; rowIndex >= 0; rowIndex--) {
            Row row = sheet.getRow(rowIndex);
            String title = row == null ? null : text(row.getCell(CLASS_COLUMN));

            if (title == null) {
                continue;
            }

            if (HEADER_MARKER.equals(normalize(title))) {
                return null;
            }

            if (normalize(title).startsWith(CLASS_TITLE_PREFIX)) {
                String titleCode = title.substring(CLASS_TITLE_PREFIX.length()).trim();
                return titleCode.isEmpty() ? null : titleCode;
            }
        }

        return null;
    }

    private List<SessionColumn> findSessionColumns(
        Sheet sheet,
        int headerRow,
        Row dateRow,
        Row subHeaderRow,
        String location,
        List<ImportIssueResponse> issues
    ) {
        List<SessionColumn> columns = new ArrayList<>();

        if (subHeaderRow == null) {
            return columns;
        }

        Row monthRow = sheet.getRow(headerRow - 1);
        Set<LocalDate> dates = new HashSet<>();

        for (int column = 0; column < subHeaderRow.getLastCellNum(); column++) {
            boolean isTestColumn = TEST_HEADER.equals(normalize(text(subHeaderRow.getCell(column))));
            boolean nextIsHomework = HOMEWORK_HEADER.equals(normalize(text(subHeaderRow.getCell(column + 1))));

            if (!isTestColumn || !nextIsHomework) {
                continue;
            }

            LocalDate date = resolveDate(dateRow, monthRow, column);
            String cellLocation = location + " · ô " + cellName(headerRow, column);

            /*
             * Column without date is kept, it is only reported when a student has scores in
             * it (template columns are usually empty)
             */
            if (date != null && !dates.add(date)) {
                issues.add(
                    ImportIssueResponse.error(
                        cellLocation,
                        "Ngày " + date + " bị trùng với một buổi khác, cột điểm này bị bỏ qua"
                    )
                );
                continue;
            }

            columns.add(new SessionColumn(column, column + 1, date));
        }

        return columns;
    }

    /*
     * Session date is either a real date or a text like "T4 23/6". For a text the
     * year comes from the nearest month title on the left
     */
    private LocalDate resolveDate(Row dateRow, Row monthRow, int column) {
        Object value = dateRow == null ? null : value(dateRow.getCell(column));

        if (value instanceof LocalDate date) {
            return date;
        }

        if (value instanceof BigDecimal number && DateUtil.isValidExcelDate(number.doubleValue())
            && number.intValue() > 30000) {
            return DateUtil.getLocalDateTime(number.doubleValue()).toLocalDate();
        }

        if (!(value instanceof String text)) {
            return null;
        }

        Matcher matcher = DAY_MONTH.matcher(text);

        if (!matcher.find()) {
            return null;
        }

        int day = Integer.parseInt(matcher.group(1));
        int month = Integer.parseInt(matcher.group(2));
        Integer year = matcher.group(3) != null ? Integer.valueOf(matcher.group(3)) : findYear(monthRow, column);

        if (year == null) {
            return null;
        }

        try {
            return LocalDate.of(year < 100 ? 2000 + year : year, month, day);
        } catch (DateTimeException exception) {
            return null;
        }
    }

    private Integer findYear(Row monthRow, int column) {
        if (monthRow == null) {
            return null;
        }

        for (int index = column; index >= 0; index--) {
            Object value = value(monthRow.getCell(index));

            if (value instanceof LocalDate date) {
                return date.getYear();
            }

            if (value instanceof String text) {
                Matcher matcher = MONTH_YEAR.matcher(text);

                if (matcher.find()) {
                    return Integer.valueOf(matcher.group(2));
                }
            }
        }

        return null;
    }

    /*
     * Empty cell is null. Text such as "-", "Nghỉ", "KL" means 0
     */
    private BigDecimal score(Cell cell) {
        Object value = value(cell);

        if (value == null) {
            return null;
        }

        if (value instanceof BigDecimal number) {
            return number.setScale(2, RoundingMode.HALF_UP);
        }

        if (value instanceof String text) {
            try {
                return new BigDecimal(text.replace(',', '.')).setScale(2, RoundingMode.HALF_UP);
            } catch (NumberFormatException exception) {
                return BigDecimal.ZERO.setScale(2);
            }
        }

        return BigDecimal.ZERO.setScale(2);
    }

    private boolean isValidScore(BigDecimal score) {
        return score.signum() >= 0 && score.compareTo(MAX_SCORE) <= 0;
    }

    /*
     * Cell value as LocalDate, BigDecimal or String. Formula cells use the value
     * cached by Excel
     */
    private Object value(Cell cell) {
        if (cell == null) {
            return null;
        }

        CellType type = cell.getCellType() == CellType.FORMULA ? cell.getCachedFormulaResultType() : cell.getCellType();

        return switch (type) {
        case NUMERIC -> DateUtil.isCellDateFormatted(cell)
            ? cell.getLocalDateTimeCellValue().toLocalDate()
            : BigDecimal.valueOf(cell.getNumericCellValue());
        case STRING -> {
            String text = Normalizer.normalize(cell.getStringCellValue(), Normalizer.Form.NFC).trim();
            yield text.isEmpty() ? null : text;
        }
        case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
        case ERROR -> "#ERROR";
        default -> null;
        };
    }

    private String text(Cell cell) {
        Object value = value(cell);

        if (value instanceof BigDecimal number) {
            return number.stripTrailingZeros().toPlainString();
        }

        return value == null ? null : value.toString();
    }

    private String normalize(String value) {
        return value == null ? "" : Normalizer.normalize(value, Normalizer.Form.NFC).trim().toUpperCase(Locale.ROOT);
    }

    private String cellName(int rowIndex, int column) {
        return new CellReference(rowIndex, column).formatAsString(false);
    }
}
