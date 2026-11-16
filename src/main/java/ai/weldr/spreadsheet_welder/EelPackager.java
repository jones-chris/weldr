package ai.weldr.spreadsheet_welder;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ooxml.POIXMLProperties;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.openxmlformats.schemas.officeDocument.x2006.customProperties.CTProperty;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

@Slf4j
public class EelPackager {

    private static final String MANIFEST_JSON_FILE_NAME = "manifest.json";

    private static final String MCPB_MANIFEST_JSON_FILE_NAME = "mcpb_manifest.json";

    private static final String AUTHOR = "Author";

    private static final Gson gson = new GsonBuilder().setPrettyPrinting().create();

    /**
     * An alternative entry point that exposes the {@link EelPackager#createManifest)} method to scripts.
     *
     * @param args {@link String[]}
     */
    public static void main(String[] args) throws IOException {
        // Get Excel file name.
        final String excelFileName = args[0];

        final UUID id = UUID.randomUUID();

        // Generate the app's manifest, serialize it to JSON, and write it to a file.
        WorkbookValidator.Manifest manifest = createManifest(excelFileName, id);
        String manifestJson = gson.toJson(manifest);

        Path tmpFilePath = new File(MANIFEST_JSON_FILE_NAME).toPath();
        Files.write(tmpFilePath, manifestJson.getBytes());
        log.info("Manifest written to: {}", tmpFilePath.toAbsolutePath());

        // Generate the MCPB manifest, serialize it to JSON, and write it to a file.
        WorkbookValidator.McpbManifest mcpbManifest = WorkbookValidator.McpbManifest.fromAppManifest(manifest);
        String mcpbManifestJson = gson.toJson(mcpbManifest);

        Path mcpbTmpFilePath = new File(MCPB_MANIFEST_JSON_FILE_NAME).toPath();
        Files.write(mcpbTmpFilePath, mcpbManifestJson.getBytes());
        log.info("MCPB Manifest written to: {}", mcpbTmpFilePath.toAbsolutePath());
    }

    /**
     * Validates a XLSX workbook and creates the manifest from a XLSX file's {@link String} path.
     *
     * @param excelFileName The file path {@link String} of the Excel workbook.
     */
    public static WorkbookValidator.Manifest createManifest(
            final String excelFileName,
            final UUID id
    ) throws IOException {
        Workbook workbook = WorkbookFactory.create(new File(excelFileName));

        POIXMLProperties.CoreProperties coreProperties = ((XSSFWorkbook) workbook).getProperties().getCoreProperties();
        POIXMLProperties.CustomProperties customProperties = ((XSSFWorkbook) workbook).getProperties().getCustomProperties();

        // Fetch the specific metadata fields.  The following 3 fields are required...
        final String title = Optional.ofNullable(coreProperties.getTitle())
                .orElseThrow(() -> new IllegalArgumentException("XLSX file is missing the 'Title' property in its core properties"));

        final String description = Optional.ofNullable(coreProperties.getDescription())
                .orElseThrow(() -> new IllegalArgumentException("XLSX file is missing the 'Description' property in its core properties"));

        final String creator = Optional.ofNullable(customProperties.getProperty(AUTHOR))
                .map(CTProperty::getLpwstr)
                .orElseThrow(() -> new IllegalArgumentException("XLSX file is missing the 'Author' property in its custom properties"));

        // ...and this property is optional - although some spreadsheet applications will increment it automatically, such
        // as LibreOffice.
        final int version = Optional.ofNullable(coreProperties.getRevision())
                        .map(Integer::parseInt)
                        .orElse(0);

        return createManifest(workbook, creator, title, version, id, description);
    }

    private static WorkbookValidator.Manifest createManifest(
        final Workbook workbook,
        final String author,
        final String name,
        final Integer version,
        final UUID id,
        final String description
    ) {
        return new WorkbookValidator(workbook, author, name, version, id, description)
                .assertIsValid()
                .createManifest();
    }

    /**
     * Dynamically generates a ".java" file from the given {@link ai.weldr.spreadsheet_welder.WorkbookValidator.Manifest}
     * and compiles it into a ".class" file and then packages it into a ".jar" file.  The generated ".class" file will
     * expose the workbook's inputs and outputs as static methods, allowing for easy integration into Java applications.
     *
     * @param manifest {@link ai.weldr.spreadsheet_welder.WorkbookValidator.Manifest}
     * @return {@link File} to the generated ".java" file.  Note that a separate script needs to call javac to compile it
     * to a ".class" file and then package it into a ".jar" file.
     */
    private static File createJavaApiFile(WorkbookValidator.Manifest manifest) {
        try {
            File tempFile = File.createTempFile("java_api", ".jar");
            tempFile.deleteOnExit();
            return tempFile;
        } catch (IOException e) {
            log.error("Failed to create temporary file for Java API", e);
            throw new RuntimeException("Failed to create temporary file for Java API", e);
        }
    }

}

