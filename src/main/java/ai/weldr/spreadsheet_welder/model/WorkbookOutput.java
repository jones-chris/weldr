package ai.weldr.spreadsheet_welder.model;

import lombok.Getter;

import java.util.List;
import java.util.Map;
import java.util.Optional;

@Getter
public class WorkbookOutput {

    private Optional<StorageLocation> storageLocation = Optional.empty();

    private final Map<String, List<String>> headers;

    private final Map<String, Object[][]> output;

    public WorkbookOutput(Map<String, Object[][]> output, Map<String, List<String>> headers) {
        this.output = output;

        if (headers.isEmpty()) {
            throw new IllegalArgumentException("headers cannot be empty");
        }
        this.headers = headers;
    }

    public void setStorageLocation(StorageLocation storageLocation) {
        if (storageLocation == null) {
            this.storageLocation = Optional.empty();
        } else {
            this.storageLocation = Optional.of(storageLocation);
        }
    }

}
