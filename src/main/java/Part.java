public class Part {
    private final boolean isField;
    private String value;
    private byte[] data;

    public Part(boolean isField, byte[] data) {
        this.isField = isField;
        this.data = data;
    }

    public Part(boolean isField, String value) {
        this.isField = isField;
        this.value = value;
    }

    public String getValue() {
        if (!isField){
            return null;
        }
        return value;
    }

    public byte[] getData() {
        if (isField) {
            return null;
        }
        return data;
    }
}
