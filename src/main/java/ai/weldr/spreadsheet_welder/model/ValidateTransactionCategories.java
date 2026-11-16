package ai.weldr.spreadsheet_welder.model;

import ai.weldr.spreadsheet_welder.codegen.CellValueConverter;
import ai.weldr.spreadsheet_welder.service.WorkbookCalculationEngine;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

public class ValidateTransactionCategories {

    public enum InputNames {

        INPUT_CATEGORIES("input_categories"),
        INPUT_TRANSACTIONS("input_transactions");

        final String name;

        InputNames(String name) {
            this.name = name;
        }
    }

    public enum OutputNames {

        OUTPUT_INVALID_TRANSACTIONS("output_invalid_transactions");

        final String name;

        OutputNames(String name) {
            this.name = name;
        }
    }

    private final WorkbookCalculationEngine engine = new WorkbookCalculationEngine();

    private WorkbookOutput workbookOutput;

    public ValidateTransactionCategories withInputCategories(
            List<input_categories> inputCategories
    ) {
        engine.withInput(inputCategories);
        return this;
    }

    public ValidateTransactionCategories withInputTransactions(
            List<input_transactions> inputTransactions
    ) {
        engine.withInput(inputTransactions);
        return this;
    }

    public ValidateTransactionCategories run() throws IOException {
         this.workbookOutput = this.engine.runWorkbook();

         return this;
    }

    // todo: create one of these methods for each output array/sheet in workbook.
    public List<output_invalid_transactions> getOutputInvalidTransactions() throws IOException {
        return getOutputRows(
                OutputNames.OUTPUT_INVALID_TRANSACTIONS.name,
                output_invalid_transactions::from
        );
    }

    private <T> List<T> getOutputRows(String outputName, Function<Object[], T> rowMapper) throws IOException {
        if (this.workbookOutput == null) {
            this.run();
        }

        Object[][] rawRows = this.workbookOutput.getOutput().get(outputName);
        if (rawRows == null) {
            throw new IllegalStateException("Output for '" + outputName + "' is not available.");
        }

        return Arrays.stream(rawRows)
                .map(rowMapper)
                .toList();
    }


    // todo:  sample method to get a single value from the output workbook.  This is just an example, and will need to be modified for each output value in the workbook.
    // todo: create one of these methods for each singlular output value (ie: named range) in the workbook.
    public Object getOutputMonth() throws IOException {
        if (this.workbookOutput == null) {
            this.run();
        }

        // todo:  Cast or desrialize the output value to the appropriate type (ie: String, Integer, etc.) based on the expected output type in the workbook.
        Object outputMonth = this.workbookOutput.getOutput().get("output_month");
        if (outputMonth == null) {
            throw new IllegalStateException("Output for 'output_month' is not available or is empty.");
        }

        return outputMonth; // Assuming the month is in the first cell of the first row.
    }

}

record input_categories(
        String category
) implements InputSheetRowRepresentation {

    public static input_categories from(Object[] data) {
        if (data.length != 1) {
            throw new IllegalArgumentException("Expected 1 element in the data array, but got " + data.length);
        }

        return new input_categories(
                (String) data[0]
        );
    }

    @Override
    public String sheetName() {
        return "input_categories";
    }

    @Override
    public String[] toRowValues() {
        return new String[] {
                this.category
        };
    }

}

record input_transactions(
    Date Date,
    String Account,
    String Reviewed,
    String Payee,
    String Category,
    String Exclusion,
    double Amount
) implements InputSheetRowRepresentation {

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");

    public static input_transactions from(Object[] data) {
        if (data.length != 7) {
            throw new IllegalArgumentException("Expected 7 elements in the data array, but got " + data.length);
        }

        return new input_transactions(
                (Date) data[0],
                (String) data[1],
                (String) data[2],
                (String) data[3],
                (String) data[4],
                (String) data[5],
                (double) data[6]
        );
    }

    @Override
    public String sheetName() {
        return "input_transactions";
    }

    @Override
    public String[] toRowValues() {
        return new String[] {
                Date != null ? DATE_FORMAT.format(Date) : null,
                this.Account,
                this.Reviewed,
                this.Payee,
                this.Category,
                this.Exclusion,
                String.valueOf(this.Amount)
        };
    }
}

record output_invalid_transactions(
        Date Date,
        String Account,
        String Payee,
        String Category,
        double Amount
) {

    public static output_invalid_transactions from(Object[] data) {
        if (data.length != 5) {
            throw new IllegalArgumentException("Expected 5 elements in the data array, but got " + data.length);
        }

        return new output_invalid_transactions(
                CellValueConverter.asDate(data[0]),
                CellValueConverter.asString(data[1]),
                CellValueConverter.asString(data[2]),
                CellValueConverter.asString(data[3]),
                CellValueConverter.asDouble(data[4])
        );
    }
}

