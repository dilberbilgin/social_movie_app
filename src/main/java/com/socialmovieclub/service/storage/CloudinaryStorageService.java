//package com.socialmovieclub.service.storage;
//
//import com.cloudinary.Cloudinary;
//import com.cloudinary.utils.ObjectUtils;
//import com.socialmovieclub.exception.BusinessException;
//import lombok.RequiredArgsConstructor;
//import org.springframework.stereotype.Service;
//import org.springframework.web.multipart.MultipartFile;
//
//import java.io.IOException;
//import java.util.Map;
//
//@Service
//@RequiredArgsConstructor
//public class CloudinaryStorageService implements StorageService {
//
//    private final Cloudinary cloudinary;
//
//    @Override
//    public String uploadFile(MultipartFile file, String folder) {
//        try {
//            Map<?, ?> uploadResult = cloudinary.uploader().upload(file.getBytes(),
//                    ObjectUtils.asMap(
//                            "folder", "social-movie-app/" + folder,
//                            "resource_type", "auto"
//                    ));
//            return uploadResult.get("secure_url").toString();
//        } catch (IOException e) {
//            throw new BusinessException("File upload failed: " + e.getMessage());
//        }
//    }
//
//    @Override
//    public void deleteFile(String fileUrl) {
//        // Cloudinary public_id üzerinden silme işlemi buraya eklenebilir
//    }
//}