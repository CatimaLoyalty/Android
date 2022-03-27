package protect.card_locker.importexport;

public class ImportExportResult {
    private ImportExportResultType resultType;
    private String developerDetails;

    public ImportExportResult(ImportExportResultType resultType) {
        this(resultType, null);
    }

    public ImportExportResult(ImportExportResultType resultType, String developerDetails) {
        this.resultType = resultType;
        this.developerDetails = developerDetails;
    }

    public ImportExportResultType resultType() {
        return resultType;
    }

    public String developerDetails() {
        return developerDetails;
    }
}
