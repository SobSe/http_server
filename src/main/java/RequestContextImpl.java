import org.apache.commons.fileupload.UploadContext;

import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;

public class RequestContextImpl implements UploadContext {
    private final RequestParser request;

    public RequestContextImpl(RequestParser request) {
        this.request = request;
    }

    @Override
    public long contentLength() {
        return Long.parseLong(request.getContentLength());
    }

    @Override
    public String getCharacterEncoding() {
        if(request.getContentType() == null) {
            return null;
        } else {
            return Arrays.stream(request.getContentType().split(";"))
                    .filter(o -> o.startsWith("charset"))
                    .map(o -> o.substring(o.indexOf("=")))
                    .findFirst()
                    .orElse(null);
        }
    }

    @Override
    public String getContentType() {
        return request.getContentType();
    }

    @Override
    public InputStream getInputStream() throws IOException {
        return request.getIn();
    }

    @Override
    public int getContentLength() {
        return Integer.parseInt(request.getContentLength());
    }
}
