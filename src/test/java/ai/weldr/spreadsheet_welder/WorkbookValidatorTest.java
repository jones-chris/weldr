package ai.weldr.spreadsheet_welder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;

import java.io.FileInputStream;
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

}