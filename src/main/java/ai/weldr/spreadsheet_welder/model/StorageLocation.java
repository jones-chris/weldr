package ai.weldr.spreadsheet_welder.model;

public record StorageLocation(
        String bucket,
        String key,
        String flowId,
        String inputSheetName
) { }