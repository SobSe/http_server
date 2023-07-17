public class Request {
    private String typeRequest;
    private String method;

    public Request(String typeRequest, String method) {
        this.typeRequest = typeRequest;
        this.method = method;
    }

    public String getTypeRequest() {
        return typeRequest;
    }

    public String getMethod() {
        return method;
    }
}
