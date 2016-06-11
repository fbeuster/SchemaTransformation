import schemaExtraction.Extraction;

public class Main {
    public static void main(String[] args) {
        Extraction extraction = new Extraction("test", "thesis_data_user");
        extraction.run();
    }
}
