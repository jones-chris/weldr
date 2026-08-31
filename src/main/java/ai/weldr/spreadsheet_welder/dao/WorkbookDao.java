package ai.weldr.spreadsheet_welder.dao;

import ai.weldr.spreadsheet_welder.model.WorkbookProxy;
import org.apache.poi.ss.usermodel.Workbook;

import java.util.Optional;

public interface WorkbookDao {

    Optional<WorkbookProxy> getWorkbook(String bucket, String key);

    void save(Workbook workbook, String bucket, String key);

}
