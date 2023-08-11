import org.apache.commons.fileupload.*;

import java.io.IOException;
import java.util.List;
import java.util.Map;

public class FileUploadImpl extends FileUpload {
    private static final String POST_METHOD = "POST";

    public static boolean isMultipartContent(RequestParser request) {
        return "POST".equalsIgnoreCase(request.getMethod()) && FileUploadBase.isMultipartContent(new RequestContextImpl(request));
    }

    public FileUploadImpl() {
    }

    public FileUploadImpl(FileItemFactory fileItemFactory) {
        super(fileItemFactory);
    }

    public List<FileItem> parseRequest(RequestParser request) throws FileUploadException {
        return this.parseRequest(new RequestContextImpl(request));
    }

    public Map<String, List<FileItem>> parseParameterMap(RequestParser request) throws FileUploadException {
        return this.parseParameterMap(new RequestContextImpl(request));
    }

    public FileItemIterator getItemIterator(RequestParser request) throws FileUploadException, IOException {
        return super.getItemIterator(new RequestContextImpl(request));
    }
}
