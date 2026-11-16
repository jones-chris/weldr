package ai.weldr.spreadsheet_welder.codegen;

import ai.weldr.spreadsheet_welder.WorkbookValidator;
import ai.weldr.spreadsheet_welder.service.WorkbookCalculationEngine;
import com.palantir.javapoet.*;

import javax.lang.model.element.Modifier;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

/**
 * Generates a Java SDK wrapper for the WorkbookCalculationEngine based on the provided manifest.
 * The generated SDK provides type-safe methods for setting inputs and retrieving outputs from the workbook.
 * <p>Here is an example of what the generated SDK might look like:</p>
 * {@snippet :
 * package ai.weldr.spreadsheet_welder.model;
 *
 * import ai.weldr.spreadsheet_welder.codegen.CellValueConverter;
 * import ai.weldr.spreadsheet_welder.service.WorkbookCalculationEngine;
 *
 * import java.io.IOException;
 * import java.text.SimpleDateFormat;
 * import java.util.Arrays;
 * import java.util.Date;
 * import java.util.List;
 * import java.util.function.Function;
 *
 * public class ValidateTransactionCategories {
 *
 *     public enum InputNames {
 *
 *         INPUT_CATEGORIES("input_categories"),
 *         INPUT_TRANSACTIONS("input_transactions");
 *
 *         final String name;
 *
 *         InputNames(String name) {
 *             this.name = name;
 *         }
 *     }
 *
 *     public enum OutputNames {
 *
 *         OUTPUT_INVALID_TRANSACTIONS("output_invalid_transactions");
 *
 *         final String name;
 *
 *         OutputNames(String name) {
 *             this.name = name;
 *         }
 *     }
 *
 *     private final WorkbookCalculationEngine engine = new WorkbookCalculationEngine();
 *
 *     private WorkbookOutput workbookOutput;
 *
 *     public ValidateTransactionCategories withInputCategories(
 *             List<input_categories> inputCategories
 *     ) {
 *         engine.withInput(inputCategories);
 *         return this;
 *     }
 *
 *     public ValidateTransactionCategories withInputTransactions(
 *             List<input_transactions> inputTransactions
 *     ) {
 *         engine.withInput(inputTransactions);
 *         return this;
 *     }
 *
 *     public void run() throws IOException {
 *          this.workbookOutput = this.engine.runWorkbook();
 *     }
 *
 *     // todo: create one of these methods for each output array/sheet in workbook.
 *     public List<output_invalid_transactions> getOutputInvalidTransactions() throws IOException {
 *         return getOutputRows(
 *                 OutputNames.OUTPUT_INVALID_TRANSACTIONS.name,
 *                 output_invalid_transactions::from
 *         );
 *     }
 *
 *     private <T> List<T> getOutputRows(String outputName, Function<Object[], T> rowMapper) throws IOException {
 *         if (this.workbookOutput == null) {
 *             this.run();
 *         }
 *
 *         Object[][] rawRows = this.workbookOutput.getOutput().get(outputName);
 *         if (rawRows == null) {
 *             throw new IllegalStateException("Output for '" + outputName + "' is not available.");
 *         }
 *
 *         return Arrays.stream(rawRows)
 *                 .map(rowMapper)
 *                 .toList();
 *     }
 *
 *
 *     // todo:  sample method to get a single value from the output workbook.  This is just an example, and will need to be modified for each output value in the workbook.
 *     // todo: create one of these methods for each singlular output value (ie: named range) in the workbook.
 *     public Object getOutputMonth() throws IOException {
 *         if (this.workbookOutput == null) {
 *             this.run();
 *         }
 *
 *         // todo:  Cast or desrialize the output value to the appropriate type (ie: String, Integer, etc.) based on the expected output type in the workbook.
 *         Object outputMonth = this.workbookOutput.getOutput().get("output_month");
 *         if (outputMonth == null) {
 *             throw new IllegalStateException("Output for 'output_month' is not available or is empty.");
 *         }
 *
 *         return outputMonth; // Assuming the month is in the first cell of the first row.
 *     }
 *
 * }
 *
 * record input_categories(
 *         String category
 * ) implements InputSheetRowRepresentation {
 *
 *     public static input_categories from(Object[] data) {
 *         if (data.length != 1) {
 *             throw new IllegalArgumentException("Expected 1 element in the data array, but got " + data.length);
 *         }
 *
 *         return new input_categories(
 *                 (String) data[0]
 *         );
 *     }
 *
 *     @Override
 *     public String sheetName() {
 *         return "input_categories";
 *     }
 *
 *     @Override
 *     public String[] toRowValues() {
 *         return new String[] {
 *                 this.category
 *         };
 *     }
 *
 * }
 *
 * record input_transactions(
 *     Date Date,
 *     String Account,
 *     String Reviewed,
 *     String Payee,
 *     String Category,
 *     String Exclusion,
 *     double Amount
 * ) implements InputSheetRowRepresentation {
 *
 *     private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
 *
 *     public static input_transactions from(Object[] data) {
 *         if (data.length != 7) {
 *             throw new IllegalArgumentException("Expected 7 elements in the data array, but got " + data.length);
 *         }
 *
 *         return new input_transactions(
 *                 (Date) data[0],
 *                 (String) data[1],
 *                 (String) data[2],
 *                 (String) data[3],
 *                 (String) data[4],
 *                 (String) data[5],
 *                 (double) data[6]
 *         );
 *     }
 *
 *     @Override
 *     public String sheetName() {
 *         return "input_transactions";
 *     }
 *
 *     @Override
 *     public String[] toRowValues() {
 *         return new String[] {
 *                 Date != null ? DATE_FORMAT.format(Date) : null,
 *                 this.Account,
 *                 this.Reviewed,
 *                 this.Payee,
 *                 this.Category,
 *                 this.Exclusion,
 *                 String.valueOf(this.Amount)
 *         };
 *     }
 * }
 *
 * record output_invalid_transactions(
 *         Date Date,
 *         String Account,
 *         String Payee,
 *         String Category,
 *         double Amount
 * ) {
 *
 *     public static output_invalid_transactions from(Object[] data) {
 *         if (data.length != 5) {
 *             throw new IllegalArgumentException("Expected 5 elements in the data array, but got " + data.length);
 *         }
 *
 *         return new output_invalid_transactions(
 *                 CellValueConverter.asDate(data[0]),
 *                 CellValueConverter.asString(data[1]),
 *                 CellValueConverter.asString(data[2]),
 *                 CellValueConverter.asString(data[3]),
 *                 CellValueConverter.asDouble(data[4])
 *         );
 *     }
 * }
 * }
 */
public class WorkbookSdkGenerator {

    private static final ClassName CELL_CONVERTER = ClassName.get("ai.weldr.spreadsheet_welder.codegen", "CellValueConverter");
    private static final ClassName INPUT_ROW_INTERFACE = ClassName.get("ai.weldr.spreadsheet_welder.model", "InputSheetRowRepresentation");
    private static final ClassName WORKBOOK_OUTPUT = ClassName.get("ai.weldr.spreadsheet_welder.model", "WorkbookOutput");

    public static JavaFile generate(String packageName, String className, WorkbookValidator.Manifest manifest) {
        ClassName generatedClass = ClassName.get(packageName, className);

        TypeSpec.Builder classBuilder = TypeSpec.classBuilder(className)
                .addModifiers(Modifier.PUBLIC)
                .addJavadoc("Auto-generated SDK wrapper for workbook calculation engine.\n");

        // 1. Add InputNames & OutputNames enums
        classBuilder.addType(buildNameEnum("InputNames", manifest.inputSheetsMetadata().stream()
                .map(WorkbookValidator.SheetMetadata::name).toList()));
        classBuilder.addType(buildNameEnum("OutputNames", manifest.outputSheetsMetadata().stream()
                .map(WorkbookValidator.SheetMetadata::name).toList()));

        // 2. Add fields: engine & workbookOutput
        classBuilder.addField(FieldSpec.builder(WorkbookCalculationEngine.class, "engine", Modifier.PRIVATE, Modifier.FINAL)
                .initializer("new $T()", WorkbookCalculationEngine.class)
                .build());
        classBuilder.addField(FieldSpec.builder(WORKBOOK_OUTPUT, "workbookOutput", Modifier.PRIVATE)
                .build());

        // 3. Generate Input Records + fluent withInputXxx(...) methods
        for (var sheet : manifest.inputSheetsMetadata()) {
            String recordName = toPascalCase(sheet.name());
            ClassName recordType = generatedClass.nestedClass(recordName);

            classBuilder.addType(buildInputRecord(recordName, sheet));
            classBuilder.addMethod(MethodSpec.methodBuilder("with" + recordName)
                    .addModifiers(Modifier.PUBLIC)
                    .returns(generatedClass)
                    .addParameter(ParameterizedTypeName.get(ClassName.get(List.class), recordType), "rows")
                    .addStatement("this.engine.withInput(rows)")
                    .addStatement("return this")
                    .build());
        }

        // 4. Add run() method
        classBuilder.addMethod(MethodSpec.methodBuilder("run")
                .addModifiers(Modifier.PUBLIC)
                .addException(IOException.class)
                .addStatement("this.workbookOutput = this.engine.runWorkbook()")
                .build());

        // 5. Generate Output Records + getOutputXxx() methods
        for (var sheet : manifest.outputSheetsMetadata()) {
            String recordName = toPascalCase(sheet.name());
            ClassName recordType = generatedClass.nestedClass(recordName);
            String enumConst = sheet.name().toUpperCase();

            classBuilder.addType(buildOutputRecord(recordName, sheet));
            classBuilder.addMethod(MethodSpec.methodBuilder("get" + recordName)
                    .addModifiers(Modifier.PUBLIC)
                    .addException(IOException.class)
                    .returns(ParameterizedTypeName.get(ClassName.get(List.class), recordType))
                    .addStatement("return getOutputRows(OutputNames.$L.name, $T::from)", enumConst, recordType)
                    .build());
        }

        // 6. Add generic getOutputRows(...) and getScalarOutput(...) helpers
        classBuilder.addMethod(buildGetOutputRowsHelper());
        classBuilder.addMethod(buildGetScalarOutputHelper());

        return JavaFile.builder(packageName, classBuilder.build())
                .indent("    ")
                .build();
    }

    private static TypeSpec buildInputRecord(String recordName, WorkbookValidator.SheetMetadata sheet) {
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder();
        CodeBlock.Builder rowValuesBlock = CodeBlock.builder().add("return new $T[] {\n$>", String.class);

        var cols = sheet.columnsMetadata();
        for (int i = 0; i < cols.size(); i++) {
            var col = cols.get(i);
            String fieldName = toCamelCase(col.name());
            TypeName javaType = mapJavaType(col.dataType());
            ctor.addParameter(javaType, fieldName);

            String comma = (i < cols.size() - 1) ? "," : "";
            if (javaType.equals(TypeName.get(Date.class))) {
                rowValuesBlock.add("this.$L != null ? DATE_FORMAT.format(this.$L) : null$L\n", fieldName, fieldName, comma);
            } else if (javaType.equals(TypeName.get(String.class))) {
                rowValuesBlock.add("this.$L$L\n", fieldName, comma);
            } else {
                rowValuesBlock.add("$T.valueOf(this.$L)$L\n", String.class, fieldName, comma);
            }
        }
        rowValuesBlock.add("$<};\n");

        TypeSpec.Builder recordBuilder = TypeSpec.recordBuilder(recordName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .addSuperinterface(INPUT_ROW_INTERFACE)
                .recordConstructor(ctor.build());

        // Add DATE_FORMAT only if at least one column is a Date
        boolean hasDate = cols.stream().anyMatch(c -> mapJavaType(c.dataType()).equals(TypeName.get(Date.class)));
        if (hasDate) {
            recordBuilder.addField(FieldSpec.builder(SimpleDateFormat.class, "DATE_FORMAT",
                            Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
                    .initializer("new $T($S)", SimpleDateFormat.class, "yyyy-MM-dd")
                    .build());
        }

        recordBuilder.addMethod(MethodSpec.methodBuilder("sheetName")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String.class)
                .addStatement("return $S", sheet.name())
                .build());

        recordBuilder.addMethod(MethodSpec.methodBuilder("toRowValues")
                .addAnnotation(Override.class)
                .addModifiers(Modifier.PUBLIC)
                .returns(String[].class)
                .addCode(rowValuesBlock.build())
                .build());

        return recordBuilder.build();
    }

    private static TypeSpec buildOutputRecord(String recordName, WorkbookValidator.SheetMetadata sheet) {
        MethodSpec.Builder ctor = MethodSpec.constructorBuilder();
        var cols = sheet.columnsMetadata();

        CodeBlock.Builder fromArgs = CodeBlock.builder();
        for (int i = 0; i < cols.size(); i++) {
            var col = cols.get(i);
            String fieldName = toCamelCase(col.name());
            TypeName javaType = mapJavaType(col.dataType());
            ctor.addParameter(javaType, fieldName);

            String converterMethod = switch (col.dataType().toLowerCase()) {
                case "date" -> "asDate";
                case "number", "double", "numeric" -> "asDouble";
                case "integer", "int" -> "asInt";
                case "boolean" -> "asBoolean";
                default -> "asString";
            };
            String comma = (i < cols.size() - 1) ? ",\n" : "\n";
            fromArgs.add("$T.$L(data[$L])$L", CELL_CONVERTER, converterMethod, i, comma);
        }

        MethodSpec fromMethod = MethodSpec.methodBuilder("from")
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .returns(ClassName.bestGuess(recordName))
                .addParameter(Object[].class, "data")
                .beginControlFlow("if (data.length != $L)", cols.size())
                .addStatement("throw new $T($S + data.length)",
                        IllegalArgumentException.class,
                        "Expected " + cols.size() + " elements in the data array, but got ")
                .endControlFlow()
                .addStatement("return new $L(\n$>$L$<)", recordName, fromArgs.build())
                .build();

        return TypeSpec.recordBuilder(recordName)
                .addModifiers(Modifier.PUBLIC, Modifier.STATIC)
                .recordConstructor(ctor.build())
                .addMethod(fromMethod)
                .build();
    }

    private static TypeSpec buildNameEnum(String enumName, List<String> names) {
        TypeSpec.Builder enumBuilder = TypeSpec.enumBuilder(enumName)
                .addModifiers(Modifier.PUBLIC)
                .addField(String.class, "name", Modifier.FINAL)
                .addMethod(MethodSpec.constructorBuilder()
                        .addParameter(String.class, "name")
                        .addStatement("this.name = name")
                        .build());

        for (String name : names) {
            enumBuilder.addEnumConstant(name.toUpperCase(),
                    TypeSpec.anonymousClassBuilder("$S", name).build());
        }
        return enumBuilder.build();
    }

    private static MethodSpec buildGetOutputRowsHelper() {
        TypeVariableName t = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("getOutputRows")
                .addModifiers(Modifier.PRIVATE)
                .addTypeVariable(t)
                .addException(IOException.class)
                .returns(ParameterizedTypeName.get(ClassName.get(List.class), t))
                .addParameter(String.class, "outputName")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Function.class), TypeName.get(Object[].class), t), "rowMapper")
                .beginControlFlow("if (this.workbookOutput == null)")
                .addStatement("this.run()")
                .endControlFlow()
                .addStatement("$T[][] rawRows = this.workbookOutput.getOutput().get(outputName)", Object.class)
                .beginControlFlow("if (rawRows == null)")
                .addStatement("throw new $T($S + outputName + $S)", IllegalStateException.class, "Output for '", "' is not available.")
                .endControlFlow()
                .addStatement("return $T.stream(rawRows).map(rowMapper).toList()", Arrays.class)
                .build();
    }

    private static MethodSpec buildGetScalarOutputHelper() {
        TypeVariableName t = TypeVariableName.get("T");
        return MethodSpec.methodBuilder("getScalarOutput")
                .addModifiers(Modifier.PRIVATE)
                .addTypeVariable(t)
                .addException(IOException.class)
                .returns(t)
                .addParameter(String.class, "outputName")
                .addParameter(ParameterizedTypeName.get(ClassName.get(Function.class), TypeName.get(Object.class), t), "converter")
                .beginControlFlow("if (this.workbookOutput == null)")
                .addStatement("this.run()")
                .endControlFlow()
                .addStatement("$T[][] rawRows = this.workbookOutput.getOutput().get(outputName)", Object.class)
                .beginControlFlow("if (rawRows == null || rawRows.length == 0 || rawRows[0].length == 0)")
                .addStatement("throw new $T($S + outputName + $S)", IllegalStateException.class, "Output for '", "' is not available or is empty.")
                .endControlFlow()
                .addStatement("return converter.apply(rawRows[0][0])")
                .build();
    }

    private static TypeName mapJavaType(String manifestType) {
        return switch (manifestType.toLowerCase()) {
            case "date" -> TypeName.get(Date.class);
            case "number", "double", "numeric" -> TypeName.DOUBLE;
            case "integer", "int" -> TypeName.INT;
            case "boolean" -> TypeName.BOOLEAN;
            default -> TypeName.get(String.class);
        };
    }

    private static String toPascalCase(String snakeOrRaw) {
        String[] parts = snakeOrRaw.replaceAll("[^a-zA-Z0-9]", "").split("");
        StringBuilder sb = new StringBuilder();
        for (String p : parts) {
            if (!p.isEmpty()) {
                sb.append(Character.toUpperCase(p.charAt(0))).append(p.substring(1).toLowerCase());
            }
        }
        return sb.toString();
    }

    private static String toCamelCase(String raw) {
        String pascal = toPascalCase(raw);
        return pascal.isEmpty() ? raw : Character.toLowerCase(pascal.charAt(0)) + pascal.substring(1);
    }

}