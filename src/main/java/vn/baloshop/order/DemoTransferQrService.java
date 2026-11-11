package vn.baloshop.order;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.Map;

@Service
public class DemoTransferQrService {
    public String createSvg(String content) {
        try {
            BitMatrix matrix = new QRCodeWriter().encode(content, BarcodeFormat.QR_CODE, 0, 0,
                    Map.of(EncodeHintType.MARGIN, 2, EncodeHintType.CHARACTER_SET,
                            StandardCharsets.UTF_8.name()));
            StringBuilder path = new StringBuilder();
            for (int y = 0; y < matrix.getHeight(); y++) {
                for (int x = 0; x < matrix.getWidth(); x++) {
                    if (matrix.get(x, y)) {
                        path.append('M').append(x).append(' ').append(y).append("h1v1h-1z");
                    }
                }
            }
            return "<svg xmlns=\"http://www.w3.org/2000/svg\" viewBox=\"0 0 " + matrix.getWidth() + " "
                    + matrix.getHeight()
                    + "\" role=\"img\" aria-label=\"Mã QR thanh toán demo\"><rect width=\"100%\" height=\"100%\""
                    + " fill=\"white\"/><path fill=\"black\" d=\"" + path + "\"/></svg>";
        } catch (WriterException exception) {
            throw new IllegalStateException("Không thể tạo mã QR thanh toán demo.", exception);
        }
    }
}
