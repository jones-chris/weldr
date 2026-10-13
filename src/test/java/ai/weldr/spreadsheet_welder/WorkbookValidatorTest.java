package ai.weldr.spreadsheet_welder;

import ai.weldr.spreadsheet_welder.model.WorkbookOutput;
import ai.weldr.spreadsheet_welder.service.WorkbookCalculationEngine;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.opencsv.CSVReader;
import com.opencsv.CSVReaderBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@Slf4j
public class WorkbookValidatorTest {

    Gson gson = new GsonBuilder().setPrettyPrinting().create();

    @Test
    public void createManifestIsSuccessful() throws IOException {
        Workbook workbook = new XSSFWorkbook(new FileInputStream("/home/pc/Documents/budget/automation/2__compare_budget_to_actuals/eel.xlsx"));

        WorkbookValidator.Manifest manifest = new WorkbookValidator(workbook, "author", "name", 0, UUID.randomUUID(), "")
                .assertIsValid()
                .createManifest();

        log.info(gson.toJson(manifest));
        assertNotNull(manifest);
    }

    @Test
    public void runWorkbookIsSuccessful() throws IOException {
        WorkbookCalculationEngine engine = new WorkbookCalculationEngine()
                .withInput(
                        "input_transactions",
                        new CSVReaderBuilder(new FileReader("/home/pc/Downloads/Simplifi - Transactions (3).csv"))
                                .withSkipLines(1) // Skip header row.
                                .build()
                )
                .withInput(
                        "input_categories",
                        new CSVReaderBuilder(new FileReader("/tmp/output_categories17081877326103095313/1054207675386475678.csv"))
                                .withSkipLines(1) // Skip header row.
                                .build()
                );

        WorkbookOutput workbookOutput = engine.runWorkbook();

        assertNotNull(workbookOutput);
    }

}