import org.apache.commons.fileupload.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class RequestFileUpload  extends FileUpload {
    private static final String POST_METHOD = "POST";

    public static boolean isMultipartContent(Request request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && FileUploadBase.isMultipartContent(new RequestRequestContext(request));
    }

    public RequestFileUpload() {
    }

    public RequestFileUpload(FileItemFactory fileItemFactory) {
        super(fileItemFactory);
    }

    public List<FileItem> parseRequest(Request request) throws FileUploadException {
        return this.parseRequest(new RequestRequestContext(request));
    }

    public Map<String, List<FileItem>> parseParameterMap(Request request) throws FileUploadException {
        return this.parseParameterMap(new RequestRequestContext(request));
    }

    public FileItemIterator getItemIterator(Request request) throws FileUploadException, IOException {
        return super.getItemIterator(new RequestRequestContext(request));
    }
}
