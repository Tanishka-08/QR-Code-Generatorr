package com.example.qrgenerator.controller;

import com.example.qrgenerator.service.QrCodeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import java.io.File;
import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Base64;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;

@RestController
@RequestMapping("/api/qr")
public class QrCodeController {

    @Autowired
    private QrCodeService qrCodeService;

    @Autowired
    private Environment environment;

    // Directory to save uploaded images and generated QR codes
    private final String UPLOAD_DIR = "uploads/";
    private final String QR_DIR = "qrcodes/";

    public QrCodeController() {
        // Create directories if they don't exist
        new File(UPLOAD_DIR).mkdirs();
        new File(QR_DIR).mkdirs();
    }

    @PostMapping("/generate")
    public ResponseEntity<Map<String, String>> generateQrCode(
            @RequestParam(value = "image", required = false) MultipartFile imageFile,
            @RequestParam(value = "content", required = false) String content,
            @RequestParam(value = "type", defaultValue = "text") String type) {
        try {
            String dataToEncode = "";
            Map<String, String> response = new HashMap<>();

            if ("image".equalsIgnoreCase(type) || "pdf".equalsIgnoreCase(type)) {
                if (imageFile == null || imageFile.isEmpty()) {
                    response.put("error", "File is required for type '" + type + "'");
                    return ResponseEntity.badRequest().body(response);
                }
                // 1. Save the uploaded file
                String originalFileName = StringUtils.cleanPath(imageFile.getOriginalFilename());
                String fileExtension = "";
                if (originalFileName != null && originalFileName.contains(".")) {
                    fileExtension = originalFileName.substring(originalFileName.lastIndexOf("."));
                }

                String uniqueFileName = UUID.randomUUID().toString() + fileExtension;
                Path uploadPath = Paths.get(UPLOAD_DIR + uniqueFileName);
                Files.copy(imageFile.getInputStream(), uploadPath, StandardCopyOption.REPLACE_EXISTING);

                // 2. Extract information (LAN IP URL)
                String hostName = InetAddress.getLocalHost().getHostAddress();
                String port = environment.getProperty("local.server.port");
                String protocol = "http";
                dataToEncode = String.format("%s://%s:%s/uploads/%s", protocol, hostName, port, uniqueFileName);

            } else if ("url".equalsIgnoreCase(type)) {
                if (content == null || content.isBlank()) {
                    response.put("error", "Content is required for type 'url'");
                    return ResponseEntity.badRequest().body(response);
                }
                dataToEncode = content.trim();
                // Simple validation
                if (!dataToEncode.startsWith("http://") && !dataToEncode.startsWith("https://")) {
                    dataToEncode = "https://" + dataToEncode;
                }

            } else {
                // Default to text
                if (content == null || content.isBlank()) {
                    response.put("error", "Content is required for type 'text'");
                    return ResponseEntity.badRequest().body(response);
                }
                dataToEncode = content;
            }

            // 3. Generate QR Code
            String qrFileName = "qr_" + UUID.randomUUID().toString() + ".png";
            String qrFilePath = QR_DIR + qrFileName;
            qrCodeService.generateQrCode(dataToEncode, 350, 350, qrFilePath);

            // Construct JSON Response
            response.put("status", "success");
            response.put("qrCodeUrl", "/qrcodes/" + qrFileName);

            String message = "";
            if ("image".equalsIgnoreCase(type) || "pdf".equalsIgnoreCase(type)) {
                message = "Scan to view file: " + dataToEncode;
            } else {
                message = "Content: " + dataToEncode;
            }
            response.put("message", message);
            response.put("savedAt", Paths.get(qrFilePath).toAbsolutePath().toString());

            return ResponseEntity.ok(response);

        } catch (IOException | com.google.zxing.WriterException e) {
            e.printStackTrace();
            Map<String, String> errorResponse = new HashMap<>();
            errorResponse.put("error", "Error generating QR code: " + e.getMessage());
            return ResponseEntity.internalServerError().body(errorResponse);
        }
    }
}
