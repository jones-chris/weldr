package ai.weldr.spreadsheet_welder.model;

public record WorkbookMetadata(
        String author,
        String name,
        int version
) { }
