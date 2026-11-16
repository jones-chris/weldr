package ai.weldr.spreadsheet_welder.service;

import ai.weldr.spreadsheet_welder.dao.TempFileWorkbookLoggerDaoImpl;
import ai.weldr.spreadsheet_welder.dao.WorkbookLoggerDao;
import ai.weldr.spreadsheet_welder.model.*;
import com.google.gson.Gson;
import com.opencsv.CSVReader;
import ai.weldr.spreadsheet_welder.Constants;
import ai.weldr.spreadsheet_welder.WorkbookValidator;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.SpreadsheetVersion;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.AreaReference;
import org.apache.poi.ss.util.CellReference;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;

@Slf4j
public class WorkbookCalculationEngine {

    private static final String EXCEL_TL_XLSX_RESOURCE_FILE_PATH = "/eel.xlsx";

    private static final String MANIFEST_RESOURCE_FILE_PATH = "/manifest.json";

    @Getter
    private static final WorkbookValidator.Manifest manifest;

    @Getter
    private WorkbookProxy workbookProxy;

    private final WorkbookLoggerDao workbookLoggerDao;

    static {
        try (InputStream manifestInputStream = WorkbookCalculationEngine.class.getResourceAsStream(MANIFEST_RESOURCE_FILE_PATH)) {
            assert manifestInputStream != null;

            String manifestJson = new String(manifestInputStream.readAllBytes(), StandardCharsets.UTF_8);

            manifest = new Gson().fromJson(manifestJson, WorkbookValidator.Manifest.class);
        } catch (Throwable e) {
            log.error("", e);

            throw new RuntimeException(e);
        }
    }

    public WorkbookCalculationEngine() {
        this.workbookLoggerDao = new TempFileWorkbookLoggerDaoImpl();

        this.loadXlsxFile();
    }

    public WorkbookCalculationEngine(WorkbookLoggerDao workbookLoggerDao) {
        this.workbookLoggerDao = workbookLoggerDao;

        this.loadXlsxFile();
    }

    public WorkbookCalculationEngine withInput(String namedRange, Object inputValue) {
        Name name = this.workbookProxy.getWorkbook()
                .getName(namedRange);

        String refersTo = name.getRefersToFormula();

        AreaReference namedRangeAreaReference = new AreaReference(refersTo, SpreadsheetVersion.EXCEL2007);

        // Get data type and comment (if it exists) for the single celled named range.
        CellReference cellReference = namedRangeAreaReference.getFirstCell();
        Cell cell = this.workbookProxy.getWorkbook().getSheet(cellReference.getSheetName())
                .getRow(cellReference.getRow())
                .getCell(cellReference.getCol());

        cell.setCellValue(inputValue.toString());

        return this;
    }

    public <T extends InputNamedRangeRepresentation> WorkbookCalculationEngine withInput(T input) {
        this.withInput(input.namedRangeName(), input.toNamedRangeInputValue());

        return this;
    }

    public WorkbookCalculationEngine withInput(
            List<? extends InputSheetRowRepresentation> rows
    ) {
        Iterable<String[]> rowValues = () -> rows.stream()
                .map(InputSheetRowRepresentation::toRowValues)
                .iterator();

        // All the rows are of the same type, so the sheet name will be the same for all of them.  So, it's safe to just
        // get the first row's sheet name and use that for the entire list of rows.
        String worksheetName = rows.getFirst().sheetName();

        return populateWorksheet(worksheetName, rowValues);
    }

    public WorkbookCalculationEngine withInput(
            String worksheetName,
            CSVReader csvReader
    ) {
        return populateWorksheet(worksheetName, csvReader);
    }

    private WorkbookCalculationEngine populateWorksheet(String worksheetName, Iterable<String[]> rows) {
        WorkbookValidator.SheetMetadata sheetMetadata = getManifest().inputSheetsMetadata()
                .stream()
                .filter(meta -> meta.name().equals(worksheetName))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Could not find input sheet metadata for " + worksheetName));

        final int inputSheetRowLimit = sheetMetadata.rowLimit();
        final List<WorkbookValidator.ColumnMetadata> columnsMetadata = sheetMetadata.columnsMetadata();

        try {
            Sheet sheet = Optional.ofNullable(this.workbookProxy.getWorkbook().getSheet(worksheetName))
                    .orElseThrow(() -> new RuntimeException("Could not find sheet with name " + worksheetName));

            Row headerRow = sheet.getRow(0);
            int numOfColumns = headerRow.getLastCellNum();

            // Ignore the sheet's header row (index 0) and start inserting data on the next row in the sheet (index 1).
            int rowIdx = 1;

            for (String[] nextLine : rows) {
                if (rowIdx >= inputSheetRowLimit) {
                    throw new RuntimeException("Input contains more rows than the row limit of " + inputSheetRowLimit + " for sheet " + worksheetName);
                }

                if (numOfColumns != nextLine.length) {
                    throw new RuntimeException("Input contains a row at index " + rowIdx + " that is not " + numOfColumns + " items");
                }

                Row row = sheet.getRow(rowIdx);
                if (row == null) {
                    row = sheet.createRow(rowIdx);
                }

                for (int cellIdx = 0; cellIdx < numOfColumns; cellIdx++) {
                    String expectedType = columnsMetadata.get(cellIdx).dataType();
                    String value = nextLine[cellIdx];

                    validateCellValue(value, expectedType, worksheetName, rowIdx, cellIdx);

                    Cell cell = row.getCell(cellIdx, Row.MissingCellPolicy.CREATE_NULL_AS_BLANK);

                    if (value == null || value.isBlank()) {
                        continue;
                    }

                    log.debug(
                            "Attempting to set cell {}:{} (rowIdx {} and cellIdx {}) to {}",
                            worksheetName, cell.getAddress().formatAsString(), rowIdx, cellIdx, value
                    );

                    Cell templateCell = headerRow.getCell(cellIdx);
                    CellType cellType = (templateCell != null) ? templateCell.getCellType() : CellType.STRING;

                    setCellValue(cell, cellType, value);
                }

                rowIdx++;
            }

            return this;
        } catch (Exception e) {
            log.error("Error populating worksheet {}", worksheetName, e);
            throw new RuntimeException(e);
        }
    }

    private void validateCellValue(String value, String expectedType, String worksheetName, int rowIdx, int cellIdx) {
        var validator = Constants.BUILT_IN_TYPE_VALIDATORS.get(expectedType);
        if (validator == null) {
            throw new RuntimeException(
                    "Could not find validator for expected type " + expectedType +
                            " for row index " + rowIdx + " cell index " + cellIdx + " in sheet " + worksheetName
            );
        }

        if (!validator.apply(value)) {
            throw new RuntimeException(
                    "Value " + value + " at row index " + rowIdx + " cell index " + cellIdx +
                            " in sheet " + worksheetName + " is not of expected type " + expectedType
            );
        }
    }

    private void setCellValue(Cell cell, CellType cellType, String value) {
        switch (cellType) {
            case STRING -> cell.setCellValue(value);
            case NUMERIC -> cell.setCellValue(Double.parseDouble(value));
            case BOOLEAN -> cell.setCellValue(Boolean.parseBoolean(value));
            default -> throw new IllegalArgumentException(
                    "Unsupported input data type: " + cellType +
                            " for cell " + cell.getAddress().formatAsString() + " with value " + value
            );
        }
    }

    public WorkbookOutput runWorkbook() throws IOException {
        WorkbookOutput workbookOutput = null;

        try {
            // Calculate all formulas.
            this.workbookProxy.getWorkbook().getCreationHelper().createFormulaEvaluator().evaluateAll();
            log.info("Workbook calculation completed successfully.");

            workbookOutput = workbookProxy.getOutputs();
            log.info("Workbook output retrieved successfully.");
        } catch (Throwable t) {
            log.error("", t);

            if (workbookOutput == null) {
                workbookOutput = new WorkbookOutput();
            }

            workbookOutput.setError(t);
        } finally {
            // Write workbook to disk so that the user can open it for debugging, if needed.
            Optional<StorageLocation> logStorageLocation = this.workbookLoggerDao.log(workbookProxy.getWorkbook(), manifest.name());
            log.info("workbookOutput is {} and storage location is present? {}", workbookOutput, logStorageLocation.isPresent());
            if (workbookOutput != null && logStorageLocation.isPresent()) {
                log.info("Workbook output storage location is {}", logStorageLocation.get().key());

                workbookOutput.setStorageLocation(logStorageLocation.get());
            }

            this.workbookProxy.close();
        }

        return workbookOutput;
    }

    private void loadXlsxFile() {
        try (InputStream inputStream = WorkbookCalculationEngine.class.getResourceAsStream(EXCEL_TL_XLSX_RESOURCE_FILE_PATH)) {
            this.workbookProxy = new WorkbookProxy(new XSSFWorkbook(inputStream));
        } catch (IOException e) {
            log.error("", e);

            throw new RuntimeException(e);
        }
    }

}
