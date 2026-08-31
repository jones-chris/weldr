package ai.weldr.spreadsheet_welder.dao;

import ai.weldr.spreadsheet_welder.model.StorageLocation;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Optional;

public interface WorkbookLoggerDao {

    Optional<StorageLocation> log(Workbook workbook, String workbookName);

}
